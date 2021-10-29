package playground.vsp.pt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.PositiveOrZero;

import java.util.Map;

/**
 * @author Chengqi Lu (luchengqi7)
 * The parameters for the distance-based PT trip fare calculation.
 * The default values are set based on the fitting results of the trip and fare data collected on September 2021
 * The values are based on the standard unit of meter (m) and Euro (EUR)
 */
public class DistanceBasedPtFareParams extends ReflectiveConfigGroup {
    public static final String PT_FARE_DISTANCE_BASED = "distance based pt fare";

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
        map.put(MIN_FARE, "Minimum fare for a PT trip " +
                "(e.g. Kurzstrecke/short distance ticket in cities, ticket for 1 zone in rural areas)");
        map.put(NORMAL_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for normal trips (e.g. within the city or region)");
        map.put(NORMAL_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for normal trips");
        map.put(LONG_DISTANCE_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for long distance trips (e.g. intercity trips)");
        map.put(LONG_DISTANCE_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for long trips");
        map.put(LONG_DISTANCE_TRIP_THRESHOLD, "Threshold of the long trips in meters. Below this value, " +
                "the trips are considered as normal trips. Above this value, the trips are considered as " +
                "inter-city trips");
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
}
