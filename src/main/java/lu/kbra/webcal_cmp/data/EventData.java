package lu.kbra.webcal_cmp.data;

import java.time.Instant;

sealed public interface EventData permits CalendarEventChange, CalendarEvent {

	Instant getNewStartTime();
	
}
