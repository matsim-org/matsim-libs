package org.matsim.application.automatedCalibration;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * This simple tuner will tune the ASC and marginal traveling utility by analyzing the difference between actual
 * mode share and reference mode share. One mode will be tuned each time. The distance group with the largest error
 * (non-absolute value!) and smallest error (non-absolute value!) will form an equation set. Solving for the equation
 * set, we will get the new parameters for the given mode.
 */
public class SimpleParameterTuner implements ParameterTuner {
    private final Map<String, Double> speedMap = new HashMap<>();
    private final double targetError;

    public SimpleParameterTuner(double targetError) {
        this.targetError = targetError;
        speedMap.put(TransportMode.car, 13.8);  // 50 km/h
        speedMap.put(TransportMode.ride, 13.8);
        speedMap.put(TransportMode.pt, 10.0); // 36 km/h
        speedMap.put(TransportMode.bike, 4.2); // 15 km/h
        speedMap.put(TransportMode.walk, 1.4); // 5 km/h
    }

    @Override
    public void tune(Config config, Map<String, Map<Double, Double>> errorMap) {
        String[] modes = new String[]{TransportMode.car, TransportMode.ride, TransportMode.pt, TransportMode.bike, TransportMode.walk};
        double maxError = 0;
        String modeToTune = null;
        for (String mode : modes) {
            double error = errorMap.get(mode).values().stream().mapToDouble(Math::abs).max().orElseThrow();
            if (error > maxError) {
                maxError = error;
                modeToTune = mode;
            }
        }
        double speed = speedMap.get(modeToTune);

        double upperBound = -1.0;
        double upperBoundDistance = -1;
        double lowerBound = 1.0;
        double lowerBoundDistance = -1;

        // Calculate upper bound and lower bound
        Map<Double, Double> errorMapForModeToTune = errorMap.get(modeToTune);
        for (double distance : errorMapForModeToTune.keySet()) {
            double error = errorMapForModeToTune.get(distance);
            if (error > upperBound) {
                upperBound = error;
                upperBoundDistance = distance;
            }

            if (error < lowerBound) {
                lowerBound = error;
                lowerBoundDistance = distance;
            }
        }

        double upperBoundAdjustment = calculateAdjustment(upperBound);
        double lowerBoundAdjustment = calculateAdjustment(lowerBound);

        double a0 = config.planCalcScore().getModes().get(modeToTune).getMarginalUtilityOfTraveling();
        double b0 = config.planCalcScore().getModes().get(modeToTune).getConstant();
        double t1 = upperBoundDistance / speed;
        double t2 = lowerBoundDistance / speed;
        double deltaC1 = (a0 * t1 + b0) * upperBoundAdjustment;
        double deltaC2 = (a0 * t2 + b0) * lowerBoundAdjustment;

        double deltaA = (deltaC2 - deltaC1) / (t2 - t1);
        double deltaB = deltaC1 - deltaA * t1;

        double a = a0 + deltaA;
        double b = b0 + deltaB;

        // Apply constriants
        SimpleParameterConstraints simpleParameterConstraints = new SimpleParameterConstraints(config);
        a = simpleParameterConstraints.processMarginalTravelUtility(a);
        b = simpleParameterConstraints.processASC(b);

        // Tune the parameter
        config.planCalcScore().getModes().get(modeToTune).setMarginalUtilityOfTraveling(a);
        config.planCalcScore().getModes().get(modeToTune).setConstant(b);
    }

    private double calculateAdjustment(double error) {
        if (error < -4 * targetError) {
            return -0.5;
        }
        if (error < -2 * targetError) {
            return -0.2;
        }
        if (error < -1 * targetError) {
            return -0.1;
        }
        if (error > 4 * targetError) {
            return 0.5;
        }
        if (error > 2 * targetError) {
            return 0.2;
        }
        if (error > targetError) {
            return 0.1;
        }
        return 0;
    }
}
