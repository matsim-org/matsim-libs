package playground.dziemke.analysis.general.srv;

import org.matsim.api.core.v01.population.Leg;

/**
 * @author gthunig on 13.04.2017.
 */
public class LegUtils {

    private static final String CA_DISTANCE_ROUTED_M = "distanceRouted";
    private static final String CA_DISTANCE_BEELINE_M = "distanceBeeline";
    private static final String CA_SPEED_M_S = "speed";
    private static final String CA_WEIGHT = "weight";

    public static double getDistanceRoutedM(Leg leg) {
        return (double) leg.getAttributes().getAttribute(CA_DISTANCE_ROUTED_M);
    }

    static void setDistanceRoutedM(Leg leg, double distanceRouted) {
        leg.getAttributes().putAttribute(CA_DISTANCE_ROUTED_M, distanceRouted);
    }

    public static double getDistanceBeelineM(Leg leg) {
        return (double) leg.getAttributes().getAttribute(CA_DISTANCE_BEELINE_M);
    }

    static void setDistanceBeelineM(Leg leg, double distanceBeeline) {
        leg.getAttributes().putAttribute(CA_DISTANCE_BEELINE_M, distanceBeeline);
    }

    public static double getSpeedMS(Leg leg) {
        return (double) leg.getAttributes().getAttribute(CA_SPEED_M_S);
    }

    static void setSpeedMS(Leg leg, double speedMS) {
        leg.getAttributes().putAttribute(CA_SPEED_M_S, speedMS);
    }

    public static double getWeight(Leg leg) {
        return (double) leg.getAttributes().getAttribute(CA_WEIGHT);
    }

    static void setWeight(Leg leg, double weight) {
        leg.getAttributes().putAttribute(CA_WEIGHT, weight);
    }
}
