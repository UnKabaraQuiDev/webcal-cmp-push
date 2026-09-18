package lu.kbra.webcal_cmp.endpoint;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.kbra.webcal_cmp.data.CalendarEvent;
import lu.kbra.webcal_cmp.service.CalendarCache;
import lu.kbra.webcal_cmp.service.CheckCalendarService;
import lu.kbra.webcal_cmp.service.FetchService;
import lu.kbra.webcal_cmp.service.IcsParser;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookEndpoint {

	private final CheckCalendarService checkCalendarService;
	private final CalendarCache calendarCache;
	private final IcsParser parser;
	private final FetchService calendarService;

	@Value("${calendar.key}")
	private String key;

	@PostMapping("/refresh")
	public void refreshCalendar() {
		this.checkCalendarService.checkCalendar(false);
	}

	@GetMapping(value = "/webcal", produces = "text/calendar")
	public String getCalendar(@RequestParam final String key) throws Exception {
		if (!Objects.equals(this.key, key)) {
			return null;
		}
		if (this.calendarCache.getTransformed() == null) {
			final String ics;
			try {
				ics = this.calendarService.downloadCalendar();
			} catch (final ResourceAccessException e) {
				WebhookEndpoint.log.error("Couldn't download calendar.", e);
				return null;
			}

			final List<CalendarEvent> events = this.parser.parse(ics);

			this.calendarCache.setTransformed(this.parser.calToString(this.parser.toCal(events)));
		}
		return this.calendarCache.getTransformed();
	}

}
