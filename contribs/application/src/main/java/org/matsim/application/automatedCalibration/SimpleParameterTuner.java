package org.matsim.application.automatedCalibration;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This simple tuner will tune the ASC and marginal traveling utility based on the error between simulated output
 * and reference data. All modes (except walk) are tuned. We will slightly modify the cost for each trip
 * and a new cost structure will be computed based on the linear regression. The cost for the walk mode is
 * fixed.
 */
public class SimpleParameterTuner implements ParameterTuner {
    private final String[] modes;
    private final DistanceGrouping distanceGrouping;
    private final Map<String, Map<String, Double>> cumulativeErrorMap;

    private final double pValue = 40.0; // TODO maybe make this configurable?
    private final double iValue = 10.0; // TODO maybe make this configurable?
    private final double alpha = 0.75; // TODO maybe make this configurable?

    public SimpleParameterTuner(String[] modes, DistanceGrouping distanceGrouping) {
        this.modes = modes;
        this.distanceGrouping = distanceGrouping;
        this.cumulativeErrorMap = createModeDistanceMatrix(modes, distanceGrouping);
    }

    @Override
    public void tune(Config config, Map<String, Map<String, Double>> errorMap, List<AutomaticScenarioCalibrator.Trip> trips) {
        // Calculate cost tuning matrix
        Map<String, Map<String, Double>> tuningRatioMatrix = createModeDistanceMatrix(modes, distanceGrouping);
        for (String mode : modes) {
            if (mode.equals(TransportMode.walk)) {
                for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                    tuningRatioMatrix.get(mode).put(distanceGroup, 1.0); // Walk will not be tuned (1.0 --> unchanged)
                }
            }

            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                double error = errorMap.get(mode).get(distanceGroup);
                double cumulativeError = error + cumulativeErrorMap.get(mode).get(distanceGroup) * alpha;
                cumulativeErrorMap.get(mode).put(distanceGroup, cumulativeError); // update cumulative error
                // calculate ratio
                double tuningRatio = calculateAdjustmentRatio(error, cumulativeError);
                tuningRatioMatrix.get(mode).put(distanceGroup, tuningRatio);
            }
        }

        // Initialize regression models
        Map<String, SimpleRegression> regressionMap = new HashMap<>();
        for (String mode : modes) {
            regressionMap.put(mode, new SimpleRegression());
        }

        // Go through all trips
        for (AutomaticScenarioCalibrator.Trip trip : trips) {
            String mode = trip.getMode();
            String distanceGroup = distanceGrouping.assignDistanceGroup(trip.getDistance());
            double travelTime = trip.getTravelTime() / 3600; // in the unit of hour

            double a0 = config.planCalcScore().getModes().get(mode).getMarginalUtilityOfTraveling();
            double b0 = config.planCalcScore().getModes().get(mode).getConstant();
            double originalCost = a0 * travelTime + b0;
            double newCost = originalCost * tuningRatioMatrix.get(mode).get(distanceGroup);

            regressionMap.get(mode).addData(travelTime, newCost);
        }

        for (String mode : modes) {
            if (mode.equals(TransportMode.walk)) {
                continue;
            }

            SimpleRegression regression = regressionMap.get(mode);
            double a = regression.getSlope();
            double b = regression.getIntercept();

            // Apply constraints
            SimpleParameterConstraints simpleParameterConstraints = new SimpleParameterConstraints(config);
            a = simpleParameterConstraints.processMarginalTravelUtility(a);
            b = simpleParameterConstraints.processASC(b);

            // Tune the parameter
            config.planCalcScore().getModes().get(mode).setMarginalUtilityOfTraveling(a);
            config.planCalcScore().getModes().get(mode).setConstant(b);
        }
    }

    private double calculateAdjustmentRatio(double error, double cumulativeError) {
        double ratio = pValue * error + iValue * cumulativeError + 1;
        if (ratio > 2.0) {
            ratio = 2.0;
        }
        if (ratio < 0.5) {
            ratio = 0.5;
        }
        return ratio;
    }

    private Map<String, Map<String, Double>> createModeDistanceMatrix(String[] modes, DistanceGrouping distanceGrouping) {
        Map<String, Map<String, Double>> modeDistanceMatrix = new HashMap<>();
        for (String mode : modes) {
            Map<String, Double> cumulativeErrorForOneMode = new HashMap<>();
            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                cumulativeErrorForOneMode.put(distanceGroup, 0.0);
            }
            modeDistanceMatrix.put(mode, cumulativeErrorForOneMode);
        }
        return modeDistanceMatrix;
    }

}
