package lu.kbra.webcal_cmp.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lu.kbra.webcal_cmp.service.CheckCalendarService;

@Service
@RequiredArgsConstructor
public class CalendarScheduler {

	private final CheckCalendarService checkCalendarService;
//	private final CalendarCache cache;

	@Scheduled(cron = "0 0/5 7-17 * * *")
	public void checkCalendarDuringDay() {
		this.checkCalendarService.checkCalendar();
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void midnight() {
		this.checkCalendarService.checkCalendar();
	}

//	@EventListener(ApplicationReadyEvent.class)
//	public void onApplicationReady() {
//		this.checkCalendarService.checkCalendar();
//	}

}