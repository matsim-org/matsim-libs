package org.matsim.contrib.drt.routing;

import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtRouteConstraintsCalculator {

    double getMaxTravelTime(DrtOptimizationConstraintsSet constraintsSet, double unsharedRideTime);

    double getMaxRideTime(DrtOptimizationConstraintsSet constraintsSet, double unsharedRideTime);
}
