package lu.kbra.webcal_cmp.service;

import org.springframework.stereotype.Service;

import lu.kbra.webcal_cmp.data.CachedCalendar;

@Service
public class CalendarCache {

	private CachedCalendar events;

	public synchronized CachedCalendar get() {
		return events;
	}

	public synchronized void set(CachedCalendar events) {
		this.events = events;
	}

}