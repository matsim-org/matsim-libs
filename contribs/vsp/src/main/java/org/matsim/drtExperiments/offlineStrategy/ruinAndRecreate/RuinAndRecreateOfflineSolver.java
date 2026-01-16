package org.matsim.drtExperiments.offlineStrategy.ruinAndRecreate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.router.util.TravelTime;
import org.matsim.drtExperiments.basicStructures.FleetSchedules;
import org.matsim.drtExperiments.basicStructures.GeneralRequest;
import org.matsim.drtExperiments.basicStructures.OnlineVehicleInfo;
import org.matsim.drtExperiments.offlineStrategy.InsertionCalculator;
import org.matsim.drtExperiments.offlineStrategy.LinkToLinkTravelTimeMatrix;
import org.matsim.drtExperiments.offlineStrategy.OfflineSolver;
import org.matsim.drtExperiments.offlineStrategy.OfflineSolverRegretHeuristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public record RuinAndRecreateOfflineSolver(int maxIterations, Network network, TravelTime travelTime,
                                           DrtConfigGroup drtConfigGroup, Random random) implements OfflineSolver {
    private static final Logger log = LogManager.getLogger(RuinAndRecreateOfflineSolver.class);

    @Override
    public FleetSchedules calculate(FleetSchedules previousSchedules,
                                    Map<Id<DvrpVehicle>, OnlineVehicleInfo> onlineVehicleInfoMap, List<GeneralRequest> newRequests,
                                    double time) {
        // Initialize fleet schedule when it is null
        if (previousSchedules == null) {
            previousSchedules = FleetSchedules.initializeFleetSchedules(onlineVehicleInfoMap);
        }

        // If there is no new request, simply keep the schedules unchanged
        if (newRequests.isEmpty()) {
            return previousSchedules;
        }

        // Initialize all the necessary objects
        RecreateSolutionAcceptor solutionAcceptor = new SimpleAnnealingThresholdAcceptor();
        RuinSelector ruinSelector = new RandomRuinSelector(random);
        SolutionCostCalculator solutionCostCalculator = new DefaultSolutionCostCalculator();

        // Prepare link to link travel time matrix for relevant links
        LinkToLinkTravelTimeMatrix linkToLinkTravelTimeMatrix = LinkToLinkTravelTimeMatrix.
                prepareLinkToLinkTravelMatrix(network, travelTime, previousSchedules, onlineVehicleInfoMap, newRequests, time);

        // update schedules based on the latest travel time estimation and current locations
        previousSchedules.updateFleetSchedule(network, linkToLinkTravelTimeMatrix, onlineVehicleInfoMap);

        // Create insertion calculator
        InsertionCalculator insertionCalculator = new InsertionCalculator(network, drtConfigGroup.getStopDuration(), linkToLinkTravelTimeMatrix);

        // Initialize regret inserter
        OfflineSolverRegretHeuristic regretInserter = new OfflineSolverRegretHeuristic(network, travelTime, drtConfigGroup);

        // Calculate initial solution
        FleetSchedules initialSolution = regretInserter.performRegretInsertion(insertionCalculator, previousSchedules, onlineVehicleInfoMap, newRequests);
        double initialScore = solutionCostCalculator.calculateSolutionCost(initialSolution, time);

        // Initialize the best solution (set to initial solution)
        FleetSchedules currentBestSolution = initialSolution;
        double currentBestScore = initialScore;

        // Initialize the fall back solution (set to the initial solution)
        FleetSchedules currentSolution = initialSolution;
        double currentScore = initialScore;

        int displayCounter = 1;
        for (int i = 1; i < maxIterations + 1; i++) {
            // Create a copy of current solution
            FleetSchedules newSolution = currentSolution.copySchedule();

            // Ruin the plan by removing some requests from the schedule
            List<GeneralRequest> requestsToRemove = ruinSelector.selectRequestsToBeRuined(newSolution);
            if (requestsToRemove.isEmpty()) {
                log.info("There is no request to remove! All the following iterations will be skipped");
                break;
            }
            for (GeneralRequest request : requestsToRemove) {
                Id<DvrpVehicle> vehicleId = newSolution.requestIdToVehicleMap().get(request.getPassengerId());
                insertionCalculator.removeRequestFromSchedule(onlineVehicleInfoMap.get(vehicleId), request, newSolution);
            }

            // Recreate: try to re-insert all the removed requests, along with rejected requests, back into the schedule
            List<GeneralRequest> requestsToReinsert = new ArrayList<>(newSolution.pendingRequests().values());
            newSolution.pendingRequests().clear();
            newSolution = regretInserter.performRegretInsertion(insertionCalculator, newSolution, onlineVehicleInfoMap, requestsToReinsert);

            // Score the new solution
            double newScore = solutionCostCalculator.calculateSolutionCost(newSolution, time);
            if (solutionAcceptor.acceptSolutionOrNot(newScore, currentScore, i, maxIterations)) {
                currentSolution = newSolution;
                currentScore = newScore;
                if (newScore < currentBestScore) {
                    currentBestScore = newScore;
                    currentBestSolution = newSolution;
                }
            }

            if (i % displayCounter == 0) {
                log.info("Ruin and Recreate iterations #" + i + ": new score = " + newScore + ", accepted = " + solutionAcceptor.acceptSolutionOrNot(newScore, currentScore, i, maxIterations) + ", current best score = " + currentBestScore);
                displayCounter *= 2;
            }

        }
        log.info(maxIterations + " ruin and Recreate iterations complete!");

        return currentBestSolution;
    }
}
