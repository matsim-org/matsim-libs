package org.matsim.contrib.analysis.vsp.traveltimedistance;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

public class GoogleMapRouteValidator implements TravelTimeDistanceValidator {
    private static final Logger log = LogManager.getLogger(GoogleMapRouteValidator.class);

    private final String mode;
    private final String apiAccessKey;
    private final String date;
    private final CoordinateTransformation ct;

    /**
     * Travel time and distance validator based on Google Map API.
     *
     * @param apiAccessKey You can apply for free API key on the Google Map API website.
     * @param date         Google Map API only accept date in the future. If a past date is used, today in the next week will be used as the date by default.
     * @param ct           The target CRS of the Coordinate Transformation should be WGS84.
     */
    public GoogleMapRouteValidator(String outputFolder, String mode, String apiAccessKey, String date, CoordinateTransformation ct) {
        this.mode = mode == null ? "DRIVING" : mode;
        this.apiAccessKey = apiAccessKey;
        this.date = date;
        this.ct = ct;
    }

    @Override
    public Tuple<Double, Double> getTravelTime(NetworkTrip trip) {
        String tripId = trip.getPersonId().toString() + "_" + trip.getDepartureTime();
        return getTravelTime(trip.getDepartureLocation(), trip.getArrivalLocation(), trip.getDepartureTime(), tripId);
    }

    @Override
    public Tuple<Double, Double> getTravelTime(Coord fromCoord, Coord toCoord, double departureTime, String tripId) {
        double adjustedDepartureTime = calculateGoogleDepartureTime(departureTime);
        Coord from = ct.transform(fromCoord);
        Coord to = ct.transform(toCoord);

        Locale locale = new Locale("en", "UK");
        String pattern = "###.#######";

        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(locale);
        df.applyPattern(pattern);

        String urlString = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                "?departure_time=" + Long.toString((long) adjustedDepartureTime) +
                "&mode=" + mode +
                "&destinations=" + df.format(to.getY()) + "%2C" + df.format(to.getX()) +
                "&origins=" + df.format(from.getY()) + "%2C" + df.format(from.getX()) +
                "&key=" + apiAccessKey;

		log.info(urlString);

		Optional<Tuple<Double, Double>> result;
        try {
            URL url = new URL(urlString);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			result = readFromJson(in);
        } catch (IOException e) {
            log.error("The contents on the URL cannot be read properly. Please check your API or check the contents on URL manually", e);
			result = Optional.empty();
        }
        return result.orElse(new Tuple<>(0.0, 0.0));
    }

	Optional<Tuple<Double, Double>> readFromJson(BufferedReader reader) throws IOException {
		JsonElement jsonElement = JsonParser.parseReader(reader);
		if(!jsonElement.isJsonObject()){
			return Optional.empty();
		}
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		JsonArray rows = jsonObject.getAsJsonArray("rows");

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		JsonObject results = rows.get(0).getAsJsonObject().get("elements").getAsJsonArray().get(0).getAsJsonObject();

		if (!results.has("distance") && !results.has("duration_in_traffic")) {
			return Optional.empty();
		}

		JsonObject distanceResults = results.get("distance").getAsJsonObject();
		double distance = distanceResults.get("value").getAsDouble();
		JsonObject timeResults = results.get("duration_in_traffic").getAsJsonObject();
		double travelTime = timeResults.get("value").getAsDouble();

		return Optional.of(new Tuple<>(travelTime, distance));
	}

    private double calculateGoogleDepartureTime(double departureTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate googleDate0 = LocalDate.parse("1970-01-01", dateTimeFormatter);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate nextWeek = LocalDate.now().plusWeeks(1);
        LocalDate validationDate = LocalDate.parse(date, dateTimeFormatter);
        if (validationDate.isBefore(tomorrow)) {
            log.warn("Google map only accept date in the future for validation. ");
            log.warn("Changing validation date to today in the next week (i.e. today + 7 days)" +
                    "Please make sure today is the weekday you want to validate against (e.g. working days instead of weekend)!");
            validationDate = nextWeek; // Google Map API only accept date and time in the future.
        }
        long numOfDays = ChronoUnit.DAYS.between(googleDate0, validationDate);
        return numOfDays * 86400 + departureTime;
    }
}
