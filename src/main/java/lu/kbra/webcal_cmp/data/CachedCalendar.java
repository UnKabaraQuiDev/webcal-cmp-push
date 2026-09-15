package lu.kbra.webcal_cmp.data;

import java.time.LocalDate;
import java.util.Map;

public record CachedCalendar(LocalDate date, Map<String, CalendarEvent> events) {

}