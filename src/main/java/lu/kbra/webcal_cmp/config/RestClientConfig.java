package lu.kbra.webcal_cmp.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	RestClient.Builder restClientBuilder(
			@Value("${calendar.proxy.enabled}") final boolean useProxy,
			@Value("${calendar.proxy.host}") final String host,
			@Value("${calendar.proxy.port}") final int port,
			@Value("${calendar.proxy.username}") final String username,
			@Value("${calendar.proxy.password}") final String password) {
		if (!useProxy) {
			return RestClient.builder();
		}

		final HttpHost proxy = new HttpHost(host, port);
		final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(new AuthScope(host, port), new UsernamePasswordCredentials(username, password.toCharArray()));
		final RequestConfig requestConfig = RequestConfig.custom().setProxy(proxy).build();
		final CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(requestConfig)
				.setDefaultCredentialsProvider(credentialsProvider)
				.build();
		final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		return RestClient.builder().requestFactory(requestFactory);
	}

}