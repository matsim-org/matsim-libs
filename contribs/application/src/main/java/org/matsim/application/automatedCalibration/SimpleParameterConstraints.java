package org.matsim.application.automatedCalibration;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;

public class SimpleParameterConstraints {
    private final double carMarginalTravelUtility;

    public SimpleParameterConstraints(Config config) {
        this.carMarginalTravelUtility = Math.min
                (0, config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
    }

    /**
     * Constant cost (ASC) should be smaller than or equal to 0
     */
    public double processASC(double asc) {
        return Math.min(asc, 0);
    }

    /**
     * Car should have the lowest marginal time travel dis-utility (i.e., -1 * utility)
     */
    public double processMarginalTravelUtility(double marginalTravelUtility) {
        return Math.min(marginalTravelUtility, carMarginalTravelUtility);
    }


}
