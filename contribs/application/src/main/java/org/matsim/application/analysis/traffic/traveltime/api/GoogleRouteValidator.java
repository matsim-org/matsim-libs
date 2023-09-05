package org.matsim.application.analysis.traffic.traveltime.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Fetch route information from Google Maps api.
 */
public class GoogleRouteValidator extends AbstractRouteValidator {

	private static final String URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

	public GoogleRouteValidator(String apiKey) {
		super(apiKey);
	}

	@Override
	public String name() {
		return "google";
	}

	@Override
	public Result retrieve(Coord from, Coord to, int hour) {

		try {

			ClassicRequestBuilder post = ClassicRequestBuilder.post(URL);

			post.addHeader("Content-Type", "application/json");
			post.addHeader("X-Goog-Api-Key", apiKey);
			post.addHeader("X-Goog-FieldMask", "routes.duration,routes.distanceMeters");

			String request = mapper.writeValueAsString(new Request(from, to, hour));

			post.setEntity(request, ContentType.APPLICATION_JSON);

			JsonNode data = httpClient.execute(post.build(), response -> {

				if (response.getCode() != 200) {
					response.getEntity().writeTo(System.err);
					throw new IllegalStateException("Non-success response:");
				}

				return mapper.readValue(response.getEntity().getContent(), JsonNode.class);
			});

			JsonNode route = data.get("routes").get(0);
			String duration = route.get("duration").asText().replace("s", "");

			return new Result(hour, (int) Double.parseDouble(duration), (int) route.get("distanceMeters").asDouble());

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static final class Request {

		Location origin;
		Location destination;
		String travelMode = "DRIVE";
		String routingPreference = "TRAFFIC_AWARE_OPTIMAL";
		ZonedDateTime departureTime;
		String units = "METRIC";

		Request(Coord from, Coord to, int hour) {
			origin = new Location(from);
			destination = new Location(to);
			departureTime = RouteValidator.createDateTime(hour);
		}
	}

	private static final class Location {
		final Map<String, Object> location;

		Location(Coord coord) {

			location = Map.of("latLng", Map.of(
				"latitude", coord.getY(),
				"longitude", coord.getX()
			));

		}
	}

}
