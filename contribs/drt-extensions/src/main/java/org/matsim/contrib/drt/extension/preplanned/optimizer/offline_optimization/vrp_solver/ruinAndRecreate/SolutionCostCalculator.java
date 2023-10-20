package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.ruinAndRecreate;

import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.FleetSchedules;

public interface SolutionCostCalculator {
    double calculateSolutionCost(FleetSchedules fleetSchedules, double now);
}
