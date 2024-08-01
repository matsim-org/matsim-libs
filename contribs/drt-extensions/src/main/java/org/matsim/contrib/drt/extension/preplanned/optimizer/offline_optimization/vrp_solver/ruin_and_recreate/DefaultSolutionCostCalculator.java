package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver.ruin_and_recreate;


import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.FleetSchedules;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.TimetableEntry;

import java.util.List;

/**
 * The default solution cost calculator return the total drive time of the fleet counted from "now", plus a
 * high penalty for each rejected requests *
 */
public class DefaultSolutionCostCalculator implements SolutionCostCalculator {
    private static final double REJECTION_COST = 1e6;

    @Override
    public double calculateSolutionCost(FleetSchedules fleetSchedules, double now) {
        double totalDrivingTime = 0;
        for (List<TimetableEntry> timetable : fleetSchedules.vehicleToTimetableMap().values()) {
            double departureTime = now;
            for (TimetableEntry stop : timetable) {
                totalDrivingTime += stop.getArrivalTime() - departureTime;
                departureTime = stop.getDepartureTime();
            }
        }
        return totalDrivingTime + REJECTION_COST * fleetSchedules.pendingRequests().size();
    }
}
