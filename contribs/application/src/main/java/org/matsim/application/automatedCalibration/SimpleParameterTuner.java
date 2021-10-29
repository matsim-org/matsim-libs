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
 * and reference data. One mode will be tuned each time. Average travel time of trips for each distance group is
 * calculated and the cost of the trip can then be estimated. Based on the error, we will slightly modify the cost
 * and a new cost structure will be computed based on the linear regression. If the walk mode is being calibrated
 * only the marginal traveling utility will be tuned and the ASC for walk will always remain 0.
 */
public class SimpleParameterTuner implements ParameterTuner {
    private final String[] modes;
    private final double targetError;
    private final DistanceGrouping distanceGrouping = new StandardDistanceGrouping();

    public SimpleParameterTuner(double targetError, String[] modes) {
        this.targetError = targetError;
        this.modes = modes;
    }

    @Override
    public void tune(Config config, Map<String, Map<String, Double>> errorMap, List<AutomaticScenarioCalibrator.Trip> trips) {
        // Find the mode to tune
        double maxError = 0;
        String modeToTune = "unknown";
        for (String mode : modes) {
            double error = errorMap.get(mode).values().stream().mapToDouble(Math::abs).max().orElseThrow();
            if (error > maxError) {
                maxError = error;
                modeToTune = mode;
            }
        }

        // Calculate average travel time of the mode for each distance group
        if (!modeToTune.equals(TransportMode.walk)) {
            Map<String, List<Double>> relevantTrips = new HashMap<>();
            Map<String, Double> averageTravelTimes = new HashMap<>();
            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                relevantTrips.put(distanceGroup, new ArrayList<>());
            }

            for (AutomaticScenarioCalibrator.Trip trip : trips) {
                if (trip.getMode().equals(modeToTune)) {
                    String distanceGroup = distanceGrouping.assignDistanceGroup(trip.getDistance());
                    relevantTrips.get(distanceGroup).add(trip.getTravelTime());
                }
            }

            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                double average = relevantTrips.get(distanceGroup).stream().mapToDouble(t -> t).average().orElse(0);
                averageTravelTimes.put(distanceGroup, average);
            }

            // Tune the parameter by fitting the new cost structure
            SimpleRegression regression = new SimpleRegression();
            Map<String, Double> errorMapForModeToTune = errorMap.get(modeToTune);
            double a0 = config.planCalcScore().getModes().get(modeToTune).getMarginalUtilityOfTraveling();
            double b0 = config.planCalcScore().getModes().get(modeToTune).getConstant();
            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                double x = averageTravelTimes.get(distanceGroup); // x-axis: travel time. We use average travel time of the distance group
                double y0 = a0 * x + b0; // y-axis: the cost of the trip
                double alpha = calculateAdjustmentRatio(errorMapForModeToTune.get(distanceGroup));
                double y = (1 + alpha) * y0;
                regression.addData(x, y);
            }

            double a = regression.getSlope();
            double b = regression.getIntercept();

            // Apply constriants
            SimpleParameterConstraints simpleParameterConstraints = new SimpleParameterConstraints(config);
            a = simpleParameterConstraints.processMarginalTravelUtility(a);
            b = simpleParameterConstraints.processASC(b);

            // Tune the parameter
            config.planCalcScore().getModes().get(modeToTune).setMarginalUtilityOfTraveling(a);
            config.planCalcScore().getModes().get(modeToTune).setConstant(b);
        } else {
            // For walk, we only tune the marginal cost and constant is always 0
            double a0 = config.planCalcScore().getModes().get(modeToTune).getMarginalUtilityOfTraveling();
            double totalError = 0;
            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                totalError += errorMap.get(modeToTune).get(distanceGroup);
            }
            double a = Math.max(a0 + calculateAdjustmentForWalk(totalError), 0);
            config.planCalcScore().getModes().get(modeToTune).setMarginalUtilityOfTraveling(a);
        }
    }

    private double calculateAdjustmentRatio(double error) {
        if (error < -4 * targetError) {
            return -0.2;
        }
        if (error > 4 * targetError) {
            return 0.2;
        }
        return error / (20 * targetError);
    }

    private double calculateAdjustmentForWalk(double totalError) {
        if (totalError < -0.1) {
            return -1.0;
        }
        if (totalError > 0.1) {
            return 1.0;
        }
        return 10 * totalError;
    }
}
