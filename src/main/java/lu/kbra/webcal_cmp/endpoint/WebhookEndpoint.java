package lu.kbra.webcal_cmp.endpoint;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lu.kbra.webcal_cmp.service.CheckCalendarService;

@RestController
@RequiredArgsConstructor
public class WebhookEndpoint {

	private final CheckCalendarService checkCalendarService;

	@PostMapping("/refresh")
	public void refreshCalendar() {
		this.checkCalendarService.checkCalendar(false);
	}

}
