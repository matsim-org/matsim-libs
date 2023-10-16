package org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.vrpSolver;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.InsertionCalculator;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offlineOptimization.basicStructures.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.router.util.TravelTime;


import java.util.List;
import java.util.Map;

public class VrpSolverSeqInsertion implements VrpSolver {
    private final Network network;
    private final TravelTime travelTime;
    private final double stopDuration;

    public VrpSolverSeqInsertion(Network network, TravelTime travelTime, DrtConfigGroup drtConfigGroup) {
        this.network = network;
        this.travelTime = travelTime;
        this.stopDuration = drtConfigGroup.stopDuration;
    }

    @Override
    public FleetSchedules calculate(FleetSchedules previousSchedules,
									Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap,
									List<GeneralRequest> newRequests, double time) {
        if (previousSchedules == null) {
            previousSchedules = FleetSchedules.initializeFleetSchedules(onlineVehicleInfoMap);
        }

        if (newRequests.isEmpty()) {
            return previousSchedules;
        }

        // Prepare link to link travel time matrix based on all relevant locations (links)
        LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix = LinkToLinkTravelTimeMatrix.
                prepareLinkToLinkTravelMatrix(network, travelTime, previousSchedules, onlineVehicleInfoMap, newRequests, time);

        // Update the schedule to the current situation (e.g., errors caused by those 1s differences; traffic situation...)
        previousSchedules.updateFleetSchedule(network, linkToLinkTravelTimeMatrix, onlineVehicleInfoMap);

        // Initialize insertion calculator
        InsertionCalculator insertionCalculator = new InsertionCalculator(network, stopDuration, linkToLinkTravelTimeMatrix);

        // Perform insertion
        for (GeneralRequest request : newRequests) {
            // Try to find the best insertion
            double bestInsertionCost = Double.MAX_VALUE;
            Id<DvrpVehicle> selectedVehicleId = null;
            List<TimetableEntry> updatedTimetable = null;

            for (Id<DvrpVehicle> vehicleId : previousSchedules.vehicleToTimetableMap().keySet()) {
                InsertionCalculator.InsertionData insertionData = insertionCalculator.computeInsertionData(onlineVehicleInfoMap.get(vehicleId), request, previousSchedules);
                if (insertionData.cost() < bestInsertionCost) {
                    bestInsertionCost = insertionData.cost();
                    selectedVehicleId = vehicleId;
                    updatedTimetable = insertionData.candidateTimetable();
                }
            }

            if (selectedVehicleId == null) {
                previousSchedules.pendingRequests().put(request.getPassengerId(), request);
            } else {
                previousSchedules.vehicleToTimetableMap().put(selectedVehicleId, updatedTimetable);
                previousSchedules.requestIdToVehicleMap().put(request.getPassengerId(), selectedVehicleId);
            }
        }
        return previousSchedules;
    }
}
