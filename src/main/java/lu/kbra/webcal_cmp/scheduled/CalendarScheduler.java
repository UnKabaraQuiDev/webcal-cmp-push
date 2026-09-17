package lu.kbra.webcal_cmp.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lu.kbra.webcal_cmp.service.CheckCalendarService;

@Service
@RequiredArgsConstructor
public class CalendarScheduler {

	private final CheckCalendarService checkCalendarService;

	@Scheduled(cron = "0 0/5 7-17 * * *")
	public void checkCalendarDuringDay() {
		this.checkCalendarService.checkCalendar(false);
	}
	
	@Scheduled(cron = "0 5 18 * * *")
	public void nextDay() {
		this.checkCalendarService.checkCalendar(true);
	}

	@Scheduled(cron = "0 1 0 * * *")
	public void midnight() {
		this.checkCalendarService.checkCalendar(false);
	}

//	@EventListener(ApplicationReadyEvent.class)
//	public void onApplicationReady() {
//		this.checkCalendarService.checkCalendar(true);
//		this.checkCalendarService.checkCalendar(true);
//	}

}