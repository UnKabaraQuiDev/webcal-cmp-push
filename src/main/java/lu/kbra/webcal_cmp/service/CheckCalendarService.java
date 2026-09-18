package lu.kbra.webcal_cmp.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.kbra.webcal_cmp.data.CachedCalendar;
import lu.kbra.webcal_cmp.data.CalendarChanges;
import lu.kbra.webcal_cmp.data.CalendarEvent;
import lu.kbra.webcal_cmp.data.CalendarEventWarning;
import lu.kbra.webcal_cmp.data.CalendarEventWarning.WarningType;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCalendarService {

	private final ZoneId zone;

	private final FetchService calendarService;
	private final IcsParser parser;
	private final CalendarCache cache;
	private final CalendarComparator comparator;
	private final PushNotificationService pushService;

	private boolean previousFail = false;

	public void checkCalendar(final boolean notifySpecialEvents) {
		try {
			final boolean isWarningNextDay = this.isConsideringNextDay();
			final LocalDate effectiveDate = (isWarningNextDay ? ZonedDateTime.now(this.zone).plusDays(1) : ZonedDateTime.now(this.zone))
					.toLocalDate();

			final String ics;
			try {
				ics = this.calendarService.downloadCalendar();
			} catch (final ResourceAccessException e) {
				CheckCalendarService.log.error("Couldn't download calendar.", e);
				if (!this.previousFail) {
					this.pushService.sendFail(e);
					this.previousFail = true;
				}
				return;
			}

			final List<CalendarEvent> events = this.parser.parse(ics);
			final List<CalendarEvent> todayEvents = this.eventsForDate(events, effectiveDate);

			this.cache.setTransformed(this.parser.calToString(this.parser.toCal(events)));

			final CachedCalendar previous = this.cache.get();

			// First run, or first run of a new effective day.
			if (previous == null || !previous.date().equals(effectiveDate)) {
				this.cache.set(new CachedCalendar(effectiveDate, this.toMap(todayEvents)));

				return;
			}

			final CalendarChanges changes = this.comparator.compare(previous.events().values().stream().toList(), todayEvents);

			// if got any modofied:
			// remove modified events that are cancelled/tps/kept
			// add them to warnings
			if (!changes.modified().isEmpty()) {
				changes.modified().removeIf(e -> {
					if (e.newEvent().summary().toLowerCase().contains("suspendus")
							&& !e.oldEvent().summary().toLowerCase().contains("suspendus")) {
						changes.warning().add(new CalendarEventWarning(e.newEvent(), WarningType.CANCELLED));
						return true;
					}
					if (e.oldEvent().summary().toLowerCase().contains("suspendus")
							&& !e.newEvent().summary().toLowerCase().contains("suspendus")) {
						changes.warning().add(new CalendarEventWarning(e.newEvent(), WarningType.KEPT_ON));
						return true;
					}
					if (e.newEvent().summary().toLowerCase().contains("tp") && !e.oldEvent().summary().toLowerCase().contains("tp")) {
						changes.warning().add(new CalendarEventWarning(e.newEvent(), WarningType.TP));
						return true;
					}
					return false;
				});
			}
			// if the next day:
			// remove modified events that are cancelled/tps
			// add cancelled/tps to warnings
			if (isWarningNextDay) {
				todayEvents.stream()
						.filter(e -> e.summary().toLowerCase().contains("suspendus"))
						.map(c -> new CalendarEventWarning(c, WarningType.CANCELLED))
						.forEach(changes.warning()::add);
				todayEvents.stream()
						.filter(e -> e.summary().toLowerCase().contains("tp"))
						.map(c -> new CalendarEventWarning(c, WarningType.TP))
						.forEach(changes.warning()::add);
			}

			if (changes.hasChanges()) {
				CheckCalendarService.log.info("Found changes: {}", changes);
				this.pushService.sendCalendarChanged(changes);
			}

			this.cache.set(new CachedCalendar(effectiveDate, this.toMap(todayEvents)));
		} catch (final Exception e) {
			CheckCalendarService.log.error("Error while checking calendar.", e);
			if (!this.previousFail) {
				this.pushService.sendFail(e);
				this.previousFail = true;
			}
			return;
		}
		if (this.previousFail) {
			this.pushService.sendOk();
		}
		this.previousFail = false;
	}

	private boolean isConsideringNextDay() {
		final ZonedDateTime now = ZonedDateTime.now(this.zone);

		return now.getHour() >= 18;
	}

	private List<CalendarEvent> eventsForDate(final List<CalendarEvent> events, final LocalDate date) {
		final Instant start = date.atStartOfDay(this.zone).toInstant();

		final Instant end = date.plusDays(1).atStartOfDay(this.zone).toInstant();

		return events.stream().filter(event -> event.start().isBefore(end) && (event.end() == null || event.end().isAfter(start))).toList();
	}

	private Map<String, CalendarEvent> toMap(final List<CalendarEvent> events) {
		return events.stream().collect(Collectors.toMap(CalendarEvent::uid, Function.identity()));
	}

}
