package playground.vsp.pt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.PositiveOrZero;
import java.util.Map;

/**
 * @author Chengqi Lu (luchengqi7)
 * The parameters for the distance-based PT trip fare calculation.
 * The default values are set based on the fitting results of the trip and fare data collected on September 2021
 * The values are based on the standard unit of meter (m) and Euro (EUR)
 */
public class DistanceBasedPtFareParams extends ReflectiveConfigGroup {
    public static final String PT_DISTANCE_BASED_FARE = "ptDistanceBasedFare";

    public static final String SET_NAME = "distanceBasedPtFare";
    public static final String MIN_FARE = "minFare";
    public static final String SHORT_TRIP_SLOPE = "shortTripSlope";
    public static final String SHORT_TRIP_INTERCEPT = "shortTripIntercept";
    public static final String LONG_TRIP_THRESHOLD = "longTripThreshold";
    public static final String LONG_TRIP_SLOPE = "longTripSlope";
    public static final String LONG_TRIP_INTERCEPT = "longTripIntercept";

    @PositiveOrZero
    private double minFare = 2.0;
    @PositiveOrZero
    private double shortTripIntercept = 1.6;
    @PositiveOrZero
    private double shortTripSlope = 0.00017;
    @PositiveOrZero
    private double longTripThreshold = 50000.0;
    @PositiveOrZero
    private double longTripIntercept = 30.0;
    @PositiveOrZero
    private double longTripSlope = 0.00025;

    public DistanceBasedPtFareParams() {
        super(SET_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(MIN_FARE, "Minimum fare for a PT trip " +
                "(e.g. Kurzstrecke/short distance ticket in cities, ticket for 1 zone in rural areas)");
        map.put(SHORT_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for short trips");
        map.put(SHORT_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for short trips");
        map.put(LONG_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for long trips");
        map.put(LONG_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for long trips");
        map.put(LONG_TRIP_THRESHOLD, "Threshold of the long trips in meters. Below this value, " +
                "the trips are mostly regional trips. Above this value, the trips are usually inter-city trips");
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

    @StringGetter(SHORT_TRIP_SLOPE)
    public double getShortTripSlope() {
        return shortTripSlope;
    }

    @StringSetter(SHORT_TRIP_SLOPE)
    public void setShortTripSlope(double shortTripSlope) {
        this.shortTripSlope = shortTripSlope;
    }

    @StringGetter(SHORT_TRIP_INTERCEPT)
    public double getShortTripIntercept() {
        return shortTripIntercept;
    }

    @StringSetter(SHORT_TRIP_INTERCEPT)
    public void setShortTripIntercept(double shortTripIntercept) {
        this.shortTripIntercept = shortTripIntercept;
    }

    @StringGetter(LONG_TRIP_SLOPE)
    public double getLongTripSlope() {
        return longTripSlope;
    }

    @StringSetter(LONG_TRIP_SLOPE)
    public void setLongTripSlope(double longTripSlope) {
        this.longTripSlope = longTripSlope;
    }

    @StringGetter(LONG_TRIP_INTERCEPT)
    public double getLongTripIntercept() {
        return longTripIntercept;
    }

    @StringSetter(LONG_TRIP_INTERCEPT)
    public void setLongTripIntercept(double longTripIntercept) {
        this.longTripIntercept = longTripIntercept;
    }

    @StringGetter(LONG_TRIP_THRESHOLD)
    public double getLongTripThreshold() {
        return longTripThreshold;
    }

    @StringSetter(LONG_TRIP_THRESHOLD)
    public void setLongTripThreshold(double longTripThreshold) {
        this.longTripThreshold = longTripThreshold;
    }
}
