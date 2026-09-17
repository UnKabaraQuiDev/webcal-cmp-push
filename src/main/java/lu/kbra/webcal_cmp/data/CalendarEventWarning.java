package lu.kbra.webcal_cmp.data;

import java.time.Instant;

public record CalendarEventWarning(CalendarEvent event, WarningType type) implements EventData {

	public static enum WarningType {

		TP,
		KEPT_ON("KEPT ON"),
		CANCELLED;

		private final String name;

		WarningType() {
			this.name = this.name();
		}

		WarningType(final String string) {
			this.name = string;
		}

		@Override
		public String toString() {
			return this.name;
		}

	}

	@Override
	public Instant getNewStartTime() {
		return this.event.start();
	}

}
