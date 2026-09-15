package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.TimetableEntry;

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
