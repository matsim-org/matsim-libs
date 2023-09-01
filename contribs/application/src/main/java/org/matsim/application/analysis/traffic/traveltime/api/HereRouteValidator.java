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
 * Access data from HERE api.
 */
public class HereRouteValidator extends AbstractRouteValidator {

	private static final String URL = "https://router.hereapi.com/v8/routes";

	private final DateTimeFormatter rfc3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

	public HereRouteValidator(String apiKey) {
		super(apiKey);
	}

	@Override
	public String name() {
		return "here";
	}

	@Override
	public Result retrieve(Coord from, Coord to, int hour) {

		// Rate limit of 10 request per seconds
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		// https://developer.here.com/documentation/routing-api/dev_guide/topics/use-cases/duration-typical-time-of-day.html

		ClassicHttpRequest req = ClassicRequestBuilder.get(URL)
			.addParameter("apikey", apiKey)
			.addParameter("origin", String.format(Locale.US, "%.6f,%.6f", from.getY(), from.getX()))
			.addParameter("destination", String.format(Locale.US, "%.6f,%.6f", to.getY(), to.getX()))
			.addParameter("departureTime", RouteValidator.createDateTime(hour).format(rfc3339))
			.addParameter("return", "summary,typicalDuration")
			.addParameter("transportMode", "car")
			.build();

		try {
			JsonNode data = httpClient.execute(req, resp -> mapper.readTree(resp.getEntity().getContent()));
			JsonNode route = data.get("routes").get(0).get("sections").get(0).get("summary");
			// Dynamic traffic information is not considered. Instead, the duration is calculated using speeds that are typical for the given time of day/day of week, based on historical traffic data.
			return new Result(hour, route.get("typicalDuration").asInt(), route.get("length").asInt());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
