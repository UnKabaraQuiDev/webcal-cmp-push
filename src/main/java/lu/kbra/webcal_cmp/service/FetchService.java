package lu.kbra.webcal_cmp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FetchService {

	private final RestClient restClient;

	public FetchService(final RestClient.Builder builder) {
		this.restClient = builder.build();
	}

	public String downloadCalendar(final String url) {
		return this.restClient.get().uri(url).retrieve().body(String.class);
	}

}