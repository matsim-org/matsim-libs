package playground.vsp.pt.fare;

import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.PositiveOrZero;
import java.util.Map;

public class distanceBasedPtFareParams extends ReflectiveConfigGroup {
    public static final String SET_NAME = "distanceBasedPtFare";
    public static final String BASEFARE = "baseFare";
    public static final String DISTANCE_FARE = "distanceFare";

    @PositiveOrZero
    private double baseFare;
    @PositiveOrZero
    private double distanceFare;


    public distanceBasedPtFareParams(double baseFare, double distanceFare) {
        super(SET_NAME);
        this.baseFare = baseFare;
        this.distanceFare = distanceFare;
    }


    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(BASEFARE, "Basefare per trip (positive or zero value)");
        map.put(DISTANCE_FARE,
                "Price per Kilometer on top of the base fare");
        return map;
    }

    @StringGetter(BASEFARE)
    public double getBaseFare() {
        return baseFare;
    }

    @StringSetter(BASEFARE)
    public void setBaseFare(double baseFare) {
        this.baseFare = baseFare;
    }

    @StringGetter(DISTANCE_FARE)
    public double getDistanceFare() {
        return distanceFare;
    }

    @StringSetter(DISTANCE_FARE)
    public void setDistanceFare(double distanceFare) {
        this.distanceFare = distanceFare;
    }


}
