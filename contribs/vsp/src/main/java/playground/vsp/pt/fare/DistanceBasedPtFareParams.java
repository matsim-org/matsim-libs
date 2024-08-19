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
    public static final String LOCAL_TRIP_SLOPE = "localTripSlope";
    public static final String LOCAL_TRIP_INTERCEPT = "localTripIntercept";
    public static final String LONG_DISTANCE_TRIP_SLOPE = "longDistanceTripSlope";
    public static final String LONG_DISTANCE_TRIP_INTERCEPT = "longDistanceTripIntercept";
	public static final String THRESHOLD_FOR_LONG_DISTANCE_TRIP = "thresholdForLongDistanceTrip";
    public static final String FARE_ZONE_SHP = "fareZoneShp";

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
	private String fareZoneShp;

    public DistanceBasedPtFareParams() {
        super(SET_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(MIN_FARE, "Minimum fare for a PT trip " +
                "(e.g. Kurzstrecke/short distance ticket in cities, ticket for 1 zone in rural areas)");
        map.put(LOCAL_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for local trips (e.g. within the city or region)");
        map.put(LOCAL_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for local trips");
        map.put(LONG_DISTANCE_TRIP_SLOPE, "Linear model y = ax + b: the value of a, for long distance trips (e.g. intercity trips)");
        map.put(LONG_DISTANCE_TRIP_INTERCEPT, "Linear model y = ax + b: the value of b, for long trips");
        map.put(THRESHOLD_FOR_LONG_DISTANCE_TRIP, "Threshold for the long trips in meters. Below this value, " +
                "the trips are considered as local trips. Above this value, the trips are considered as " +
                "intercity / long-distance trips");
		map.put(FARE_ZONE_SHP, "Shp file with fare zone(s). This parameter is only used for PtFareCalculationModel 'fareZoneBased'.");
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

    @StringGetter(LOCAL_TRIP_SLOPE)
    public double getNormalTripSlope() {
        return normalTripSlope;
    }

    @StringSetter(LOCAL_TRIP_SLOPE)
    public void setNormalTripSlope(double normalTripSlope) {
        this.normalTripSlope = normalTripSlope;
    }

    @StringGetter(LOCAL_TRIP_INTERCEPT)
    public double getNormalTripIntercept() {
        return normalTripIntercept;
    }

    @StringSetter(LOCAL_TRIP_INTERCEPT)
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

    @StringGetter(THRESHOLD_FOR_LONG_DISTANCE_TRIP)
    public double getLongDistanceTripThreshold() {
        return longDistanceTripThreshold;
    }

    @StringSetter(THRESHOLD_FOR_LONG_DISTANCE_TRIP)
    public void setLongDistanceTripThreshold(double longDistanceTripThreshold) {
        this.longDistanceTripThreshold = longDistanceTripThreshold;
    }

	@StringGetter(FARE_ZONE_SHP)
	public String getFareZoneShp() {
		return fareZoneShp;
	}

	@StringSetter(FARE_ZONE_SHP)
	public void setFareZoneShp(String fareZoneShp) {
		this.fareZoneShp = fareZoneShp;
	}
}
