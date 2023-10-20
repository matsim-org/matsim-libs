package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.vrp_solver;

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.InsertionCalculator;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.FleetSchedules;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.LinkToLinkTravelTimeMatrix;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.OnlineVehicleInfo;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.basic_structures.GeneralRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.router.util.TravelTime;


import java.util.*;

/**
 * The parallel insertion strategy with regression heuristic *
 */
public class VrpSolverRegretHeuristic implements VrpSolver {
    private final Network network;
    private final TravelTime travelTime;
    private final double stopDuration;

    public VrpSolverRegretHeuristic(Network network, TravelTime travelTime, DrtConfigGroup drtConfigGroup) {
        this.network = network;
        this.travelTime = travelTime;
        this.stopDuration = drtConfigGroup.stopDuration;
    }

    @Override
    public FleetSchedules calculate(FleetSchedules previousSchedules, Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap, List<GeneralRequest> newRequests, double time) {
        // Initialize fleet schedule when it is null
        if (previousSchedules == null) {
            previousSchedules = FleetSchedules.initializeFleetSchedules(onlineVehicleInfoMap);
        }

        // If there is no new request, simply return the previous fleet schedule
        if (newRequests.isEmpty()) {
            return previousSchedules;
        }

        // Prepare link to link travel time matrix based on all relevant locations (links)
        LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix = LinkToLinkTravelTimeMatrix.
                prepareLinkToLinkTravelMatrix(network, travelTime, previousSchedules, onlineVehicleInfoMap, newRequests, time);

        // Update the schedule to the current situation (e.g., errors caused by those 1s differences; traffic situation...)
        previousSchedules.updateFleetSchedule(network, linkToLinkTravelTimeMatrix, onlineVehicleInfoMap);

        // Create insertion calculator
        InsertionCalculator insertionCalculator = new InsertionCalculator(network, stopDuration, linkToLinkTravelTimeMatrix);

        // Perform regret insertion
        return performRegretInsertion(insertionCalculator, previousSchedules, onlineVehicleInfoMap, newRequests);
    }

    public FleetSchedules performRegretInsertion(InsertionCalculator insertionCalculator, FleetSchedules previousSchedules,
                                                 Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap, List<GeneralRequest> newRequests) {
        Preconditions.checkArgument(!newRequests.isEmpty(), "There is no new request to insert!");
        // Initialize the matrix (LinkedHashMap is used to preserved order of the matrix -> reproducible results even if there are plans with same max regret/score)
        Map<GeneralRequest, Map<OnlineVehicleInfo, InsertionCalculator.InsertionData>> insertionMatrix = new LinkedHashMap<>();
        for (GeneralRequest request : newRequests) {
            insertionMatrix.put(request, new LinkedHashMap<>());
            for (OnlineVehicleInfo vehicleInfo : onlineVehicleInfoMap.values()) {
                InsertionCalculator.InsertionData insertionData = insertionCalculator.computeInsertionData(vehicleInfo, request, previousSchedules);
                insertionMatrix.get(request).put(vehicleInfo, insertionData);
            }
        }

        // Insert each request recursively
        boolean finished = false;
        while (!finished) {
            // Get the request with the highest regret and insert it to the best vehicle
            double largestRegret = -1;
            GeneralRequest requestWithLargestRegret = null;
            for (GeneralRequest request : insertionMatrix.keySet()) {
                double regret = getRegret(request, insertionMatrix);
                if (regret > largestRegret) {
                    largestRegret = regret;
                    requestWithLargestRegret = request;
                }
            }

            assert requestWithLargestRegret != null;
            InsertionCalculator.InsertionData bestInsertionData = getBestInsertionForRequest(requestWithLargestRegret, insertionMatrix);

            if (bestInsertionData.cost() < InsertionCalculator.NOT_FEASIBLE_COST) {
                // Formally insert the request to the timetable
                previousSchedules.requestIdToVehicleMap().put(requestWithLargestRegret.getPassengerId(), bestInsertionData.vehicleInfo().vehicle().getId());
                previousSchedules.vehicleToTimetableMap().put(bestInsertionData.vehicleInfo().vehicle().getId(), bestInsertionData.candidateTimetable());

                // Remove the request from the insertion matrix
                insertionMatrix.remove(requestWithLargestRegret);

                // Update insertion data for the rest of the request and the selected vehicle
                for (GeneralRequest request : insertionMatrix.keySet()) {
                    InsertionCalculator.InsertionData updatedInsertionData = insertionCalculator.computeInsertionData(bestInsertionData.vehicleInfo(), request, previousSchedules);
                    insertionMatrix.get(request).put(bestInsertionData.vehicleInfo(), updatedInsertionData);
                }
            } else {
                // The best insertion is already infeasible. Reject this request
                previousSchedules.pendingRequests().put(requestWithLargestRegret.getPassengerId(), requestWithLargestRegret);
                // Remove the request from the insertion matrix
                insertionMatrix.remove(requestWithLargestRegret);
            }
            finished = insertionMatrix.isEmpty();
        }
        return previousSchedules;
    }

    // private methods
    private double getRegret(GeneralRequest request, Map<GeneralRequest, Map<OnlineVehicleInfo, InsertionCalculator.InsertionData>> insertionMatrix) {
        List<InsertionCalculator.InsertionData> insertionDataList = new ArrayList<>(insertionMatrix.get(request).values());
        insertionDataList.sort(Comparator.comparingDouble(InsertionCalculator.InsertionData::cost));
        return insertionDataList.get(1).cost() + insertionDataList.get(2).cost() - 2 * insertionDataList.get(0).cost();
        //  regret-3 is used here. It can also be switched to regret-2, regret-4, regret-5 ... regret-q
    }

    private InsertionCalculator.InsertionData getBestInsertionForRequest(
            GeneralRequest request, Map<GeneralRequest, Map<OnlineVehicleInfo, InsertionCalculator.InsertionData>> insertionMatrix) {
        double minInsertionCost = Double.MAX_VALUE;
        InsertionCalculator.InsertionData bestInsertion = null;
        for (InsertionCalculator.InsertionData insertionData : insertionMatrix.get(request).values()) {
            if (insertionData.cost() < minInsertionCost) {
                minInsertionCost = insertionData.cost();
                bestInsertion = insertionData;
            }
        }
        return bestInsertion;
    }
}
