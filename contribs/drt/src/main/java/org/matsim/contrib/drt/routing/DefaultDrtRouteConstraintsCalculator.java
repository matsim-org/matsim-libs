package org.matsim.contrib.drt.routing;

import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;

/**
 * @author nkuehnel / MOIA
 */
public class DefaultDrtRouteConstraintsCalculator implements DrtRouteConstraintsCalculator {

    /**
     * Calculates the maximum travel time defined as: drtCfg.getMaxTravelTimeAlpha() * unsharedRideTime + drtCfg.getMaxTravelTimeBeta()
     *
     * @param constraintsSet
     * @param unsharedRideTime ride time of the direct (shortest-time) route
     * @return maximum travel time
     */
    @Override
    public double getMaxTravelTime(DrtOptimizationConstraintsSet constraintsSet, double unsharedRideTime) {
        if(constraintsSet instanceof DefaultDrtOptimizationConstraintsSet defaultSet) {
            return defaultSet.maxTravelTimeAlpha * unsharedRideTime
                + defaultSet.maxTravelTimeBeta;
        } else {
            throw new IllegalArgumentException("Constraint set is not a default set");
        }
    }

    /**
     * Calculates the maximum ride time defined as: drtCfg.maxDetourAlpha * unsharedRideTime + drtCfg.maxDetourBeta
     *
     * @param constraintsSet
     * @param unsharedRideTime ride time of the direct (shortest-time) route
     * @return maximum ride time
     */
    @Override
    public double getMaxRideTime(DrtOptimizationConstraintsSet constraintsSet, double unsharedRideTime) {
        if(constraintsSet instanceof DefaultDrtOptimizationConstraintsSet defaultSet) {
            return Math.min(unsharedRideTime + defaultSet.maxAbsoluteDetour,
                    defaultSet.maxDetourAlpha * unsharedRideTime
                            + defaultSet.maxDetourBeta);
        } else {
            throw new IllegalArgumentException("Constraint set is not a default set");
        }
    }
}