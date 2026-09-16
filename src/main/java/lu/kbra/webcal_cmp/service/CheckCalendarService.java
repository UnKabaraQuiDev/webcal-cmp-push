package lu.kbra.webcal_cmp.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.kbra.webcal_cmp.data.CachedCalendar;
import lu.kbra.webcal_cmp.data.CalendarChanges;
import lu.kbra.webcal_cmp.data.CalendarEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckCalendarService {

	private static final ZoneId ZONE = ZoneId.of("Europe/Luxembourg");

	private final FetchService calendarService;
	private final IcsParser parser;
	private final CalendarCache cache;
	private final CalendarComparator comparator;
	private final PushNotificationService pushService;

	@Value("${calendar.url}")
	private String calendarUrl;

	public void checkCalendar() {
		try {
			final LocalDate effectiveDate = this.getEffectiveDate();

			final String ics;
			try {
				ics = this.calendarService.downloadCalendar(this.calendarUrl);
			} catch (final ResourceAccessException e) {
				CheckCalendarService.log.error("Couldn't download calendar", e);
				return;
			}

			final List<CalendarEvent> events = this.parser.parse(ics);
			final List<CalendarEvent> todayEvents = this.eventsForDate(events, effectiveDate);

			final CachedCalendar previous = this.cache.get();

			// First run, or first run of a new effective day.
			if (previous == null || !previous.date().equals(effectiveDate)) {
				this.cache.set(new CachedCalendar(effectiveDate, this.toMap(todayEvents)));

				return;
			}

			final CalendarChanges changes = this.comparator.compare(previous.events().values().stream().toList(), todayEvents);

			if (changes.hasChanges()) {
				this.pushService.sendCalendarChanged(changes);
			}

			this.cache.set(new CachedCalendar(effectiveDate, this.toMap(todayEvents)));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private LocalDate getEffectiveDate() {
		final ZonedDateTime now = ZonedDateTime.now(CheckCalendarService.ZONE);

		if (now.getHour() >= 18) {
			return now.toLocalDate().plusDays(1);
		}

		return now.toLocalDate();
	}

	private List<CalendarEvent> eventsForDate(final List<CalendarEvent> events, final LocalDate date) {
		final Instant start = date.atStartOfDay(CheckCalendarService.ZONE).toInstant();

		final Instant end = date.plusDays(1).atStartOfDay(CheckCalendarService.ZONE).toInstant();

		return events.stream().filter(event -> event.start().isBefore(end) && (event.end() == null || event.end().isAfter(start))).toList();
	}

	private Map<String, CalendarEvent> toMap(final List<CalendarEvent> events) {
		return events.stream().collect(Collectors.toMap(CalendarEvent::uid, Function.identity()));
	}

}
