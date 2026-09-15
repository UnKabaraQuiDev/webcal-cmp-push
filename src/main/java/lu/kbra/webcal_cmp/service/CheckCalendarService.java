package lu.kbra.webcal_cmp.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lu.kbra.webcal_cmp.data.CachedCalendar;
import lu.kbra.webcal_cmp.data.CalendarChanges;
import lu.kbra.webcal_cmp.data.CalendarEvent;

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
			final LocalDate today = LocalDate.now(CheckCalendarService.ZONE);

			final String ics = this.calendarService.downloadCalendar(this.calendarUrl);

			final List<CalendarEvent> events = this.parser.parse(ics);
			final List<CalendarEvent> todayEvents = this.eventsForToday(events);

			final CachedCalendar previous = this.cache.get();

			// First run, or first run of a new day.
			if (previous == null || !previous.date().equals(today)) {
				this.cache.set(new CachedCalendar(today, this.toMap(todayEvents)));

				return;
			}

			final CalendarChanges changes = this.comparator.compare(previous.events().values().stream().toList(), todayEvents);

			if (changes.hasChanges()) {
				this.pushService.sendCalendarChanged(changes);
			}

			this.cache.set(new CachedCalendar(today, this.toMap(todayEvents)));
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private List<CalendarEvent> eventsForToday(final List<CalendarEvent> events) {
		final Instant start = LocalDate.now(CheckCalendarService.ZONE).atStartOfDay(CheckCalendarService.ZONE).toInstant();

		final Instant end = LocalDate.now(CheckCalendarService.ZONE).plusDays(1).atStartOfDay(CheckCalendarService.ZONE).toInstant();

		return events.stream().filter(event -> event.start().isBefore(end) && (event.end() == null || event.end().isAfter(start))).toList();
	}

	private Map<String, CalendarEvent> toMap(final List<CalendarEvent> events) {
		return events.stream().collect(Collectors.toMap(CalendarEvent::uid, Function.identity()));
	}

}
