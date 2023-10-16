package org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.vrpSolver.ruinAndRecreate;

import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.basicStructures.FleetSchedules;

public interface SolutionCostCalculator {
    double calculateSolutionCost(FleetSchedules fleetSchedules, double now);
}
