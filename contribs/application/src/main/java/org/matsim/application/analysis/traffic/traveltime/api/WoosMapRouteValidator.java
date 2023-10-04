package org.matsim.application.analysis.traffic.traveltime.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * Validator for woos map. Appears to use HERE as datasource.
 */
public class WoosMapRouteValidator extends AbstractRouteValidator {

	private static final String URL = "https://api.woosmap.com/distance/route/json";



	public WoosMapRouteValidator(String apiKey) {
		super(apiKey);
	}

	@Override
	public String name() {
		return "woosmap";
	}

	@Override
	public Result retrieve(Coord from, Coord to, int hour) {

		// Rate limit of 10 request per seconds
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// https://developers.woosmap.com/products/distance-api/route-endpoint/

		ClassicHttpRequest req = ClassicRequestBuilder.get(URL)
			.addParameter("key", apiKey)
			.addParameter("origin", String.format(Locale.US, "%.6f,%.6f", from.getY(), from.getX()))
			.addParameter("destination", String.format(Locale.US, "%.6f,%.6f", to.getY(), to.getX()))
			.addParameter("departure_time", String.valueOf(RouteValidator.createDateTime(hour).toEpochSecond()))
			.addHeader("Referer", "https://matsim.org")
			.build();

		try {
			JsonNode data = httpClient.execute(req, resp -> mapper.readTree(resp.getEntity().getContent()));
			JsonNode route = data.get("routes").get(0).get("legs").get(0);

			return new Result(hour, route.get("duration").get("value").asInt(), route.get("distance").get("value").asInt());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
