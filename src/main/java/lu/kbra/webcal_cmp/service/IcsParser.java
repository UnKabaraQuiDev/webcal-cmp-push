package lu.kbra.webcal_cmp.service;

import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lu.kbra.webcal_cmp.data.CalendarEvent;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;

@Service
public class IcsParser {

	private static final ZoneId ZONE = ZoneId.of("Europe/Luxembourg");

	@Value("${calendar.class}")
	private String className;

	private Instant toInstant(final Temporal temporal) {
		return switch (temporal) {
		case final Instant instant -> instant;
		case final ZonedDateTime zonedDateTime -> zonedDateTime.toInstant();
		case final OffsetDateTime offsetDateTime -> offsetDateTime.toInstant();
		case final LocalDateTime localDateTime -> localDateTime.atZone(IcsParser.ZONE).toInstant();
		case final LocalDate localDate -> localDate.atStartOfDay(IcsParser.ZONE).toInstant();
		case null, default -> throw new IllegalArgumentException("Unsupported temporal type: " + temporal.getClass());
		};
	}

	public List<CalendarEvent> eventsForToday(final List<CalendarEvent> events) {
		final LocalDate today = LocalDate.now(IcsParser.ZONE);

		final Instant start = today.atStartOfDay(IcsParser.ZONE).toInstant();

		final Instant end = today.plusDays(1).atStartOfDay(IcsParser.ZONE).toInstant();

		return events.stream().filter(event -> event.start().isBefore(end) && (event.end() == null || event.end().isAfter(start))).toList();
	}

	public List<CalendarEvent> parse(final String ics) throws Exception {
		final CalendarBuilder builder = new CalendarBuilder();

		Calendar calendar;

		try (StringReader reader = new StringReader(ics)) {
			calendar = builder.build(reader);
		}

		final List<CalendarEvent> events = new ArrayList<>();

		for (final Component component : calendar.getComponents(Component.VEVENT)) {
			final VEvent event = (VEvent) component;

			final String uid = event.getUid().map(Uid::getValue).orElseThrow();

			String summary = event.getSummary().map(Summary::getValue).orElse("");

			final String[] split = summary.split(";\s+");
			final int index = Arrays.stream(split).filter(s -> s.contains(this.className)).findFirst().map(s -> {
				final String[] parts = s.split("/");
				return IntStream.range(0, parts.length).filter(i -> parts[i].contains(this.className)).findFirst().orElse(-1);
			}).orElse(-1);
			if (index >= 0 && index < split.length) {
				summary = split[index];
				summary += " [";
				summary += Arrays.stream(split[split.length - 2].split("/")).map(String::trim).collect(Collectors.joining(", ")); // classes
				summary += "] (";
				summary += split[split.length - 1].trim().toUpperCase(); // type
				summary += ")";
			}

			final String location = event.getLocation().map(Location::getValue).orElse("");

			final Instant start = event.getDateTimeStart().map(dt -> this.toInstant(dt.getDate())).orElseThrow();

			final Instant end = event.getDateTimeEnd().map(dt -> this.toInstant(dt.getDate())).orElse(null);

			events.add(new CalendarEvent(uid, summary, start, end, location));
		}

		return events;
	}

}