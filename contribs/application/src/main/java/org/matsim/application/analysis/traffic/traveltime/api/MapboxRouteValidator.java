package org.matsim.application.analysis.traffic.traveltime.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

/**
 * Access route data from mapbox.
 */
public class MapboxRouteValidator extends AbstractRouteValidator {

	private static final String URL = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/";

	public MapboxRouteValidator(String apiKey) {
		super(apiKey);
	}

	@Override
	public String name() {
		return "mapbox";
	}

	@Override
	public Result retrieve(Coord from, Coord to, int hour) {

		// https://docs.mapbox.com/api/overview/#rate-limits
		// 300 per minute
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// https://docs.mapbox.com/api/navigation/directions/

		ClassicHttpRequest req = ClassicRequestBuilder.get(URL + String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", from.getX(), from.getY(), to.getX(), to.getY()))
			.addParameter("access_token", apiKey)
			.addParameter("overview", "simplified")
			.addParameter("steps", "false")
			.addParameter("depart_at", RouteValidator.createLocalDateTime(hour).toString())
			.build();

		try {
			JsonNode data = httpClient.execute(req, resp -> mapper.readTree(resp.getEntity().getContent()));
			JsonNode route = data.get("routes").get(0);

			return new Result(hour, route.get("duration_typical").asInt(), route.get("distance").asInt());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
