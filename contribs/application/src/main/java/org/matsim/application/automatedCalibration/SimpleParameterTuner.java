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
 * and reference data. All modes (except walk) are tuned. Average travel time of trips for each distance group is
 * calculated and the cost of the trip can then be estimated. Based on the error, we will slightly modify the cost
 * and a new cost structure will be computed based on the linear regression. The walk cost for the walk mode is
 * fixed.
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
        for (String mode : modes) {
            if (mode.equals(TransportMode.walk)){
                continue; // The cost for walk is fixed
            }

            // Tuning the parameter
            Map<String, List<Double>> relevantTrips = new HashMap<>();
            Map<String, Double> averageTravelTimes = new HashMap<>();
            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                relevantTrips.put(distanceGroup, new ArrayList<>());
            }

            for (AutomaticScenarioCalibrator.Trip trip : trips) {
                if (trip.getMode().equals(mode)) {
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
            Map<String, Double> errorMapForModeToTune = errorMap.get(mode);
            double a0 = config.planCalcScore().getModes().get(mode).getMarginalUtilityOfTraveling();
            double b0 = config.planCalcScore().getModes().get(mode).getConstant();
            for (String distanceGroup : distanceGrouping.getDistanceGroupings()) {
                double x = averageTravelTimes.get(distanceGroup); // x-axis: travel time (unit: second). We use average travel time of the distance group
                double y0 = a0 * (x / 3600) + b0; // y-axis: the cost of the trip
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
            config.planCalcScore().getModes().get(mode).setMarginalUtilityOfTraveling(a);
            config.planCalcScore().getModes().get(mode).setConstant(b);
        }
    }

    private double calculateAdjustmentRatio(double error) {
        if (error < -4 * targetError) {
            return -0.1;
        }
        if (error > 4 * targetError) {
            return 0.1;
        }
        return error / (40 * targetError);
    }
}
