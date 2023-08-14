package org.matsim.application.analysis.traffic.traveltime.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Access data from TomTom api.
 */
public class TomTomRouteValidator extends AbstractRouteValidator {

	private static final String URL = "https://api.tomtom.com/routing/1/calculateRoute/";

	private final DateTimeFormatter rfc3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

	public TomTomRouteValidator(String apiKey) {
		super(apiKey);
	}

	@Override
	public String name() {
		return "tomtom";
	}

	@Override
	public Result retrieve(Coord from, Coord to, int hour) {

		// Rate limit of 5 queries per seconds
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// https://developer.tomtom.com/routing-api/documentation/routing/calculate-route

		ClassicHttpRequest req = ClassicRequestBuilder.get(URL + String.format(Locale.US, "%.6f,%.6f:%.6f,%.6f/json", from.getY(), from.getX(), to.getY(), to.getX()))
			.addParameter("key", apiKey)
			.addParameter("traffic", "true")
			.addParameter("travelMode", "car")
			.addParameter("routeRepresentation", "summaryOnly")
			.addParameter("routeType", "fastest")
			.addParameter("computeTravelTimeFor", "all")
			.addParameter("departAt", RouteValidator.createDateTime(hour).format(rfc3339))
			.build();

		try {
			JsonNode data = httpClient.execute(req, resp -> {
				// usually quota exhausted
				if (resp.getCode() == 403) return null;

				return mapper.readTree(resp.getEntity().getContent());
			});

			if (data == null)
				throw new RouteValidator.Forbidden();

			JsonNode route = data.get("routes").get(0).get("summary");
			return new Result(hour, route.get("historicTrafficTravelTimeInSeconds").asInt(), route.get("lengthInMeters").asInt());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
