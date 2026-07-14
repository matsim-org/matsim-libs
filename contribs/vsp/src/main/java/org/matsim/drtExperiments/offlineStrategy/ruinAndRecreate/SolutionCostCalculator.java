package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.drtExperiments.basicStructures.FleetSchedules;

public interface SolutionCostCalculator {
    double calculateSolutionCost(FleetSchedules fleetSchedules, double now);
}
