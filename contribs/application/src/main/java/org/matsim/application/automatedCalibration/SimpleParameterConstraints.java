package org.matsim.application.automatedCalibration;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;

public class SimpleParameterConstraints {
    private final double carMarginalTravelUtility;

    public SimpleParameterConstraints(Config config) {
        this.carMarginalTravelUtility = config.planCalcScore().getModes().get(TransportMode.car).
                getMarginalUtilityOfTraveling();
    }

    /**
     * Constant cost (ASC) should be smaller than or equal to 0
     */
    public double processASC(double asc) {
        return Math.min(asc, 0);
    }

    /**
     * Marginal utility should be smaller than or equal to 0
     */
    public double processMarginalTravelUtility(double marginalTravelUtility) {
        return Math.min(marginalTravelUtility, 0);
    }

    /**
     * Car should be the most comfortable modes of travel. As a result, the marginal utility of other modes
     * should be lower than car (i.e. more negative).
     */
    public double processRelationAmongDifferentModes(double marginalTravelUtility) {
        return Math.min(marginalTravelUtility, carMarginalTravelUtility);
    }

}
