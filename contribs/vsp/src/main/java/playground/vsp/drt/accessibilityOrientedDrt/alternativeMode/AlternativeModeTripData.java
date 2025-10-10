package playground.vsp.drt.accessibilityOrientedDrt.alternativeMode;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.matsim.api.core.v01.Coord;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public record AlternativeModeTripData(String id, double departureTime, Coord fromCoord, Coord toCoord,
									  double directCarTravelTime,
									  double actualTotalTravelTime, String mode, double totalWalkDistance) {

	public static final String ID = "id";
	public static final String DEPARTURE_TIME = "departure_time";
	public static final String FROM_X = "from_x";
	public static final String FROM_Y = "from_y";
	public static final String TO_X = "to_x";
	public static final String TO_Y = "to_y";
	public static final String DIRECT_CAR_TRAVEL_TIME = "direct_car_travel_time";
	public static final String ACTUAL_TOTAL_TRAVEL_TIME = "actual_total_travel_time";
	public static final String MODE = "mode";
	public static final String TOTAL_WALK_DISTANCE = "total_walk_distance";

	public static final List<String> ALTERNATIVE_TRIP_DATA_TITLE_ROW = Arrays.asList(
		ID, DEPARTURE_TIME, FROM_X, FROM_Y, TO_X, TO_Y,
		DIRECT_CAR_TRAVEL_TIME, ACTUAL_TOTAL_TRAVEL_TIME, MODE, TOTAL_WALK_DISTANCE
	);

	public void printData(CSVPrinter printer) throws IOException {
		printer.printRecord(
			id,
			Double.toString(departureTime),
			Double.toString(fromCoord.getX()),
			Double.toString(fromCoord.getY()),
			Double.toString(toCoord.getX()),
			Double.toString(toCoord.getY()),
			Double.toString(directCarTravelTime),
			Double.toString(actualTotalTravelTime),
			mode,
			Double.toString(totalWalkDistance)
		);
	}

	public static AlternativeModeTripData readData(CSVRecord record) {
		return new AlternativeModeTripData(
			record.get(ID),
			Double.parseDouble(record.get(DEPARTURE_TIME)),
			new Coord(Double.parseDouble(record.get(FROM_X)), Double.parseDouble(record.get(FROM_Y))),
			new Coord(Double.parseDouble(record.get(TO_X)), Double.parseDouble(record.get(TO_Y))),
			Double.parseDouble(record.get(DIRECT_CAR_TRAVEL_TIME)),
			Double.parseDouble(record.get(ACTUAL_TOTAL_TRAVEL_TIME)),
			record.get(MODE),
			Double.parseDouble(record.get(TOTAL_WALK_DISTANCE))
		);
	}
}
