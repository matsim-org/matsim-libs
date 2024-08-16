package playground.vsp.pt.fare;

import jakarta.validation.constraints.PositiveOrZero;
import org.apache.commons.math.stat.regression.SimpleRegression;

import java.util.Map;

/**
 * @author Chengqi Lu (luchengqi7)
 * The parameters for the distance-based PT trip fare calculation.
 * The default values are set based on the fitting results of the trip and fare data collected on September 2021
 * The values are based on the standard unit of meter (m) and Euro (EUR)
 */
public class DistanceBasedPtFareParams extends PtFareParams {
	public static final DistanceBasedPtFareParams GERMAN_WIDE_FARE = germanWideFare();

	public static final String SET_NAME = "ptFareCalculationDistanceBased";
	public static final String MIN_FARE = "minFare";
	public static final String NORMAL_TRIP_SLOPE = "normalTripSlope";
	public static final String NORMAL_TRIP_INTERCEPT = "normalTripIntercept";
	public static final String LONG_DISTANCE_TRIP_THRESHOLD = "longDistanceTripThreshold";
	public static final String LONG_DISTANCE_TRIP_SLOPE = "longDistanceTripSlope";
	public static final String LONG_DISTANCE_TRIP_INTERCEPT = "longDistanceTripIntercept";

	@PositiveOrZero
	private double minFare = 2.0;
	@PositiveOrZero
	private double normalTripIntercept = 1.6;
	@PositiveOrZero
	private double normalTripSlope = 0.00017;
	@PositiveOrZero
	private double longDistanceTripThreshold = 50000.0;
	@PositiveOrZero
	private double longDistanceTripIntercept = 30.0;
	@PositiveOrZero
	private double longDistanceTripSlope = 0.00025;

	public DistanceBasedPtFareParams() {
		super(SET_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MIN_FARE, "Minimum fare for a PT trip (e.g. Kurzstrecke/short distance ticket in cities, ticket for 1 zone in rural areas). " +
			"Default is 2.0EUR.");
		map.put(NORMAL_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for normal trips (e.g. within the city or region). Default is 0.00017" +
			"EUR.");
		map.put(NORMAL_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for normal trips. Default is 1.6EUR.");
		map.put(LONG_DISTANCE_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for long distance trips (e.g. intercity trips). Default is 0" +
			".00025EUR.");
		map.put(LONG_DISTANCE_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for long trips. Default is 30.0EUR.");
		map.put(LONG_DISTANCE_TRIP_THRESHOLD, "Threshold of the long trips in meters. Below this value, the trips are considered as normal trips. " +
			"Above this value, the trips are considered as inter-city trips. Default is 50000.0m.");
		return map;
	}

	@StringGetter(MIN_FARE)
	public double getMinFare() {
		return minFare;
	}

	@StringSetter(MIN_FARE)
	public void setMinFare(double minFare) {
		this.minFare = minFare;
	}

	@StringGetter(NORMAL_TRIP_SLOPE)
	public double getNormalTripSlope() {
		return normalTripSlope;
	}

	@StringSetter(NORMAL_TRIP_SLOPE)
	public void setNormalTripSlope(double normalTripSlope) {
		this.normalTripSlope = normalTripSlope;
	}

	@StringGetter(NORMAL_TRIP_INTERCEPT)
	public double getNormalTripIntercept() {
		return normalTripIntercept;
	}

	@StringSetter(NORMAL_TRIP_INTERCEPT)
	public void setNormalTripIntercept(double normalTripIntercept) {
		this.normalTripIntercept = normalTripIntercept;
	}

	@StringGetter(LONG_DISTANCE_TRIP_SLOPE)
	public double getLongDistanceTripSlope() {
		return longDistanceTripSlope;
	}

	@StringSetter(LONG_DISTANCE_TRIP_SLOPE)
	public void setLongDistanceTripSlope(double longDistanceTripSlope) {
		this.longDistanceTripSlope = longDistanceTripSlope;
	}

	@StringGetter(LONG_DISTANCE_TRIP_INTERCEPT)
	public double getLongDistanceTripIntercept() {
		return longDistanceTripIntercept;
	}

	@StringSetter(LONG_DISTANCE_TRIP_INTERCEPT)
	public void setLongDistanceTripIntercept(double longDistanceTripIntercept) {
		this.longDistanceTripIntercept = longDistanceTripIntercept;
	}

	@StringGetter(LONG_DISTANCE_TRIP_THRESHOLD)
	public double getLongDistanceTripThreshold() {
		return longDistanceTripThreshold;
	}

	@StringSetter(LONG_DISTANCE_TRIP_THRESHOLD)
	public void setLongDistanceTripThreshold(double longDistanceTripThreshold) {
		this.longDistanceTripThreshold = longDistanceTripThreshold;
	}

	// in Deutschlandtarif, the linear function for the prices above 100km seem to have a different steepness
	// hence the following difference in data points
	// prices taken from https://deutschlandtarifverbund.de/wp-content/uploads/2024/07/20231201_TBDT_J_10_Preisliste_V07.pdf
	private static DistanceBasedPtFareParams germanWideFare() {
		final double MIN_FARE = 1.70;

		SimpleRegression normalDistanceTrip = new SimpleRegression();
		normalDistanceTrip.addData(1, MIN_FARE);
		normalDistanceTrip.addData(2, 1.90);
		normalDistanceTrip.addData(3, 2.00);
		normalDistanceTrip.addData(4, 2.10);
		normalDistanceTrip.addData(5, 2.20);
		normalDistanceTrip.addData(6, 3.20);
		normalDistanceTrip.addData(7, 3.70);
		normalDistanceTrip.addData(8, 3.80);
		normalDistanceTrip.addData(9, 3.90);
		normalDistanceTrip.addData(10, 4.10);
		normalDistanceTrip.addData(11, 5.00);
		normalDistanceTrip.addData(12, 5.40);
		normalDistanceTrip.addData(13, 5.60);
		normalDistanceTrip.addData(14, 5.80);
		normalDistanceTrip.addData(15, 5.90);
		normalDistanceTrip.addData(16, 6.40);
		normalDistanceTrip.addData(17, 6.50);
		normalDistanceTrip.addData(18, 6.60);
		normalDistanceTrip.addData(19, 6.70);
		normalDistanceTrip.addData(20, 6.90);
		normalDistanceTrip.addData(30, 9.90);
		normalDistanceTrip.addData(40, 13.70);
		normalDistanceTrip.addData(50, 16.30);
		normalDistanceTrip.addData(60, 18.10);
		normalDistanceTrip.addData(70, 20.10);
		normalDistanceTrip.addData(80, 23.20);
		normalDistanceTrip.addData(90, 26.20);
		normalDistanceTrip.addData(100, 28.10);

		SimpleRegression longDistanceTrip = new SimpleRegression();
		longDistanceTrip.addData(100, 28.10);
		longDistanceTrip.addData(200, 47.20);
		longDistanceTrip.addData(300, 59.70);
		longDistanceTrip.addData(400, 71.70);
		longDistanceTrip.addData(500, 83.00);
		longDistanceTrip.addData(600, 94.60);
		longDistanceTrip.addData(700, 106.30);
		longDistanceTrip.addData(800, 118.20);
		longDistanceTrip.addData(900, 130.10);
		longDistanceTrip.addData(1000, 141.00);
		longDistanceTrip.addData(1100, 148.60);
		longDistanceTrip.addData(1200, 158.10);
		longDistanceTrip.addData(1300, 169.20);
		longDistanceTrip.addData(1400, 179.80);
		longDistanceTrip.addData(1500, 190.10);
		longDistanceTrip.addData(1600, 201.50);
		longDistanceTrip.addData(1700, 212.80);
		longDistanceTrip.addData(1800, 223.30);
		longDistanceTrip.addData(1900, 233.90);
		longDistanceTrip.addData(2000, 244.00);

		var params = new DistanceBasedPtFareParams();
		params.setNormalTripSlope(normalDistanceTrip.getSlope());
		params.setNormalTripIntercept(normalDistanceTrip.getIntercept());
		params.setLongDistanceTripSlope(longDistanceTrip.getSlope());
		params.setLongDistanceTripIntercept(longDistanceTrip.getIntercept());
		params.setLongDistanceTripThreshold(100_000.);
		params.setTransactionPartner("Deutschlandtarif");
		params.setMinFare(MIN_FARE);

		return params;
	}
}
