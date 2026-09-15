package lu.kbra.webcal_cmp.data;

import java.time.Instant;

public record CalendarEvent(String uid, String summary, Instant start, Instant end, String location) implements EventData {

	public Instant getNewStartTime() {
		return start();
	}

}