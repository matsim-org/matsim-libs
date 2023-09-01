package org.matsim.application.analysis.traffic.traveltime.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

/**
 * Abstract class with http and json functionality.
 */
abstract class AbstractRouteValidator implements RouteValidator {

	protected final String apiKey;
	protected final ObjectMapper mapper;

	protected final CloseableHttpClient httpClient;

	AbstractRouteValidator(String apiKey) {
		this.apiKey = apiKey;

		mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		httpClient = HttpClients.createDefault();
	}

	@Override
	public void close() throws Exception {
		httpClient.close();
	}
}
