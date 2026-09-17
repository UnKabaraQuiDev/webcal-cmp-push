package lu.kbra.webcal_cmp.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FetchService {

	private final RestClient restClient;

	public FetchService(final RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	@Cacheable("calendars")
	public String downloadCalendar(final String url) {
		return this.restClient.get().uri(url).retrieve().body(String.class);
	}

}