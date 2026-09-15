package lu.kbra.webcal_cmp.data;

import java.util.List;

public record CalendarChanges(List<CalendarEvent> added, List<CalendarEvent> removed, List<CalendarEventChange> modified) {

	public boolean hasChanges() {
		return !added.isEmpty() || !removed.isEmpty() || !modified.isEmpty();
	}

}