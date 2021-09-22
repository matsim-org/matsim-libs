package playground.vsp.pt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.PositiveOrZero;
import java.util.Map;

public class DistanceBasedPtFareParams extends ReflectiveConfigGroup {
    public static final String SET_NAME = "distanceBasedPtFare";
    public static final String BASE_FARE = "baseFare";
    public static final String DISTANCE_FARE_PER_METER = "distanceFarePerMeter";

    public static final String PT_DISTANCE_BASED_FARE= "pt_fare_distance_based";

    @PositiveOrZero
    private double baseFare = 2.0;  // TODO update this default value after the analysis
    @PositiveOrZero
    private double distanceFare = 0.0002; // TODO update this default value after the analysis

    public DistanceBasedPtFareParams() {
        super(SET_NAME);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(BASE_FARE, "Base fare per trip (positive or zero value)");
        map.put(DISTANCE_FARE_PER_METER,
                "Price per meter on top of the base fare");
        return map;
    }

    @StringGetter(BASE_FARE)
    public double getBaseFare() {
        return baseFare;
    }

    @StringSetter(BASE_FARE)
    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    @StringGetter(DISTANCE_FARE_PER_METER)
    public double getDistanceFare() {
        return distanceFare;
    }

    @StringSetter(DISTANCE_FARE_PER_METER)
    public void setDistanceFare(double distanceFare) {
        this.distanceFare = distanceFare;
    }


}
