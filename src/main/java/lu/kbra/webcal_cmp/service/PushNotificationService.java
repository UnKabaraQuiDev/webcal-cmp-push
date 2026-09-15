package lu.kbra.webcal_cmp.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lu.kbra.webcal_cmp.data.CalendarChanges;
import lu.kbra.webcal_cmp.data.CalendarEvent;
import lu.kbra.webcal_cmp.data.CalendarEventChange;
import lu.kbra.webcal_cmp.data.EventData;

@Service
public class PushNotificationService {

	private final RestClient restClient;

	@Value("${ntfy.topic}")
	private String topic;

	public PushNotificationService(final RestClient.Builder restClientBuilder) {
		this.restClient = restClientBuilder.baseUrl("https://ntfy.sh").build();
	}

	public void sendCalendarChanged(final CalendarChanges changes) {
		final String body = this.buildMessage(changes);

		this.restClient.post()
				.uri("/" + this.topic)
				.header("X-Title", "Calendar changed")
				.header("X-Priority", "high")
				.header("X-Tags", "calendar")
				.header("X-Markdown", "true")
				.header("Content-Type", "text/plain; charset=utf-8")
				.body(body)
				.retrieve()
				.toBodilessEntity();
	}

	private String buildMessage(final CalendarChanges changes) {
		final StringBuilder message = new StringBuilder();
		message.append("\n");

		final PriorityQueue<EventData> queue = new PriorityQueue<>(Comparator.comparing(EventData::getNewStartTime));
		queue.addAll(changes.modified());
		queue.addAll(changes.added());
		queue.addAll(changes.removed());

		final Set<CalendarEvent> added = new HashSet<>(changes.added());

		for (EventData e : queue) {
			switch (e) {
			case CalendarEventChange change -> message.append("✏️ ").append(this.formatModifiedEvent(change)).append("\n\n");
			case CalendarEvent event -> {
				if (added.contains(event)) {
					message.append("➕ ").append(event.summary()).append("\n");

					message.append(this.formatEvent(event)).append("\n\n");
				} else {
					message.append("❌ ").append(event.summary()).append("\n");

					message.append(this.formatEvent(event)).append("\n\n");
				}
			}
			}
		}

		return message.toString().strip();

	}

	private String formatEvent(final CalendarEvent event) {
		final StringBuilder message = new StringBuilder();

		message.append("Time: ").append(this.formatDateTime(event.start()));

		if (event.end() != null) {
			message.append(" - ").append(this.formatDateTime(event.end()));
		}

		message.append("\n");

		if (event.location() != null && !event.location().isBlank()) {
			message.append("Location: ").append(event.location()).append("\n");
		}

		return message.toString().stripTrailing();
	}

	private String formatModifiedEvent(final CalendarEventChange change) {
		final CalendarEvent oldEvent = change.oldEvent();
		final CalendarEvent newEvent = change.newEvent();

		final StringBuilder message = new StringBuilder();

		message.append(newEvent.summary()).append("\n");

		if (!oldEvent.start().equals(newEvent.start())) {
			message.append("Start ")
					.append(this.formatDateTime(oldEvent.start()))
					.append(" -> **")
					.append(this.formatDateTime(newEvent.start()))
					.append("**\n");
		}

		if (!Objects.equals(oldEvent.end(), newEvent.end())) {
			message.append("End ")
					.append(this.formatDateTime(oldEvent.end()))
					.append(" -> **")
					.append(this.formatDateTime(newEvent.end()))
					.append("**\n");
		}

		if (!Objects.equals(oldEvent.location(), newEvent.location())) {
			message.append("Location ")
					.append(this.valueOrEmpty(oldEvent.location()))
					.append(" -> **")
					.append(this.valueOrEmpty(newEvent.location()))
					.append("**\n");
		}

		return message.toString().stripTrailing();
	}

	private String valueOrEmpty(final String value) {
		return value == null || value.isBlank() ? "(none)" : value;
	}

	private String formatDateTime(final Instant instant) {
		return instant.atZone(ZoneId.of("Europe/Luxembourg")).format(DateTimeFormatter.ofPattern("HH:mm"));
	}

}