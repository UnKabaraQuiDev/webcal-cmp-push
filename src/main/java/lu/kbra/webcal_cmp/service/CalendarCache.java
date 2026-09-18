package lu.kbra.webcal_cmp.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lu.kbra.webcal_cmp.data.CachedCalendar;

@Service
@RequiredArgsConstructor
public class CalendarCache {

	private CachedCalendar events;
	private String transformed;

	public synchronized CachedCalendar get() {
		return events;
	}

	public synchronized void set(CachedCalendar events) {
		this.events = events;
	}

	public synchronized String getTransformed() {
		return transformed;
	}

	public synchronized void setTransformed(String transformed) {
		this.transformed = transformed;
	}

}