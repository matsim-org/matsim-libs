/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.boescpa.av.staticDemand.ForceModel.Force;
import playground.boescpa.analysis.trips.Trip;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class StaticAVSim {
    private static Logger log = Logger.getLogger(StaticAVSim.class);

    private static int time;
    public static int getTime() {
        return time;
    }

    private static List<Trip> comingRequests;
    private static List<Trip> pendingRequests;
    private static List<AutonomousVehicle> vehiclesInUse;
    private static Map<AutonomousVehicle, Force> redistributingVehicles;
    private static List<AutonomousVehicle> availableVehicles;
    private static boolean vehiclesInUseChanged;
    private static Comparator<AutonomousVehicle> vehicleComparator;
    private static List<Trip> redistRequestCache;

    private static boolean redistributionSwitch;
    private static Stats stats;
    private static Redistribution redistribution;
    private static int quickServedRequests;
    private static int servedRequests;
    private static int lateServedRequests;
    private static int unservedRequests;

    public static void main(String[] args) {
        final String tripFile = args[0];
        final double shareOfOriginalAgentsServedByAV = Double.valueOf(args[1]); //0.1; //
        final double shareOfOriginalFleetReplacedByAV = Double.valueOf(args[2]); //0.1; //
        final int randomSeed = Integer.valueOf(args[3]); //4711; //
        final boolean redistributionSwitch = false; //Boolean.valueOf(args[4]); //
        final String outputFile = args[4]; //args[5];

        final String outputFileExtended = outputFile.substring(0, outputFile.lastIndexOf("."))
                + "_A" + shareOfOriginalAgentsServedByAV + "_F" + shareOfOriginalFleetReplacedByAV
                //		+ "_RS" + randomSeed + "_RD" + redistributionSwitch + outputFile.substring(outputFile.lastIndexOf("."));
                + "_RS" + randomSeed + outputFile.substring(outputFile.lastIndexOf("."));

        final SimBase simBase = setUpSim(tripFile, shareOfOriginalAgentsServedByAV, shareOfOriginalFleetReplacedByAV,
                randomSeed, redistributionSwitch);
        resetSim(simBase);
        simulate(simBase);
        stats.printResults(outputFileExtended);
    }

    private static SimBase setUpSim(String tripFile, double shareOfOriginalAgentsServedByAV,
                                    double shareOfOriginalFleetReplacedByAV, int randomSeed, boolean redistributionSwitch) {
        MatsimRandom.reset(randomSeed);
        StaticDemand staticDemand = new StaticDemandAVImpl(tripFile, Constants.MODES_REPLACED_BY_AV, shareOfOriginalAgentsServedByAV);
        List<Trip> demand = staticDemand.getSortedDemand();
        List<AutonomousVehicle> fleet = InitialSupplyCreator.createInitialAVSupply(shareOfOriginalFleetReplacedByAV, staticDemand);
        StaticAVSim.redistributionSwitch = redistributionSwitch;
        return new SimBaseImpl(demand, fleet);
    }

    private static void resetSim(SimBase simBase) {
        log.info("Simulation reset...");
        stats = new Stats(simBase);
        resetRequestStats();
        redistribution = new AttractionRedistribution();
        comingRequests = new ArrayList<>();
        pendingRequests = new ArrayList<>();
        vehiclesInUse = new ArrayList<>();
        redistributingVehicles = new LinkedHashMap<>();
        availableVehicles = new ArrayList<>();
        vehiclesInUseChanged = false;
        vehicleComparator = new Comparator<AutonomousVehicle>() {
            @Override
            public int compare(AutonomousVehicle o1, AutonomousVehicle o2) {
                return o1.getArrivalTime() - o2.getArrivalTime();
            }
        };
        redistRequestCache = new ArrayList<>();
        log.info("Simulation reset... done.");
    }

    private static void simulate(SimBase simBase) {
        log.info("Simulation...");

        // Initialize Simulation
        comingRequests.addAll(simBase.getDemand());
        availableVehicles.addAll(simBase.getFleet());

        // Time-Step-Based Simulation
        for (time = 0; time < Constants.TOTAL_SIMULATION_TIME; time += Constants.SIMULATION_INTERVAL) {
            releaseArrivingVehicles();
            releaseRedistributingVehicles();
            tendRedistRequestCache();
            if ((comingRequests.size() > 0 && time < Constants.MAX_SIMTIME_NEW_REQUESTS)) {
                checkForNewRequests();
            }
            if (pendingRequests.size() > 0) {
                handlePendingRequests();
            }
            redistributeNonUsedVehicles();
            moveRedistributingVehicles();
            sortVehiclesInUse();
            if (time%Constants.STATS_INTERVAL == 0) {
                stats.recordStats(time, pendingRequests.size(), vehiclesInUse.size(), redistributingVehicles.size(),
                        availableVehicles.size(), redistRequestCache.size(), quickServedRequests, servedRequests,
                        lateServedRequests, unservedRequests);
                resetRequestStats();
            }
            if (time%3600 == 0) log.info("SimTime: " + time/3600 + ":00");
        }

        log.info("Simulation... done.");
    }

    private static void resetRequestStats() {
        quickServedRequests = 0;
        servedRequests = 0;
        lateServedRequests = 0;
        unservedRequests = 0;
    }

    private static void releaseArrivingVehicles() {
        int arrivingVehicles = 0;
        while (arrivingVehicles < vehiclesInUse.size() && vehiclesInUse.get(arrivingVehicles).getArrivalTime() < time) {
            arrivingVehicles++;
        }
        for (int i = (arrivingVehicles - 1); i > -1; i--) {
            AutonomousVehicle arrivingVehicle = vehiclesInUse.get(i);
            arrivingVehicle.resetArrivalTime();
            vehiclesInUse.remove(i);
            availableVehicles.add(arrivingVehicle);
        }
    }

    private static void releaseRedistributingVehicles() {
        if (!redistributingVehicles.isEmpty() && (time % Constants.REDISTRIBUTIONINTERVAL) >= Constants.DURATION_OF_REDISTRIBUTION) {
            for (AutonomousVehicle arrivingVehicle : redistributingVehicles.keySet()) {
                arrivingVehicle.resetArrivalTime();
            }
            redistributingVehicles.clear();
        }
    }

    private static void tendRedistRequestCache() {
        if (redistRequestCache.size() > 0 && time > Constants.REDIST_CACHE_INTERVAL) {
            int i = 0;
            while (i < redistRequestCache.size()
                    && ((time - (redistRequestCache.get(i).startTime - Constants.LEVEL_OF_SERVICE)) > Constants.REDIST_CACHE_INTERVAL)) {
                i++;
            }
            for (int j = i-1; j > -1; j--) {
                redistRequestCache.remove(j);
            }
        }
    }

    private static void checkForNewRequests() {
        int requestBecomingPending = comingRequests.size() - 1;
        while (time < Constants.MAX_SIMTIME_NEW_REQUESTS
                && requestBecomingPending > -1
                && (comingRequests.get(requestBecomingPending).startTime - Constants.LEVEL_OF_SERVICE) <= time) {
            pendingRequests.add(0, comingRequests.get(requestBecomingPending));
            redistRequestCache.add(comingRequests.get(requestBecomingPending));
            stats.incTotalDemand();
            comingRequests.remove(requestBecomingPending);
            requestBecomingPending--;
        }
    }

    private static void handlePendingRequests() {
        for (int pendingRequest = pendingRequests.size() - 1; pendingRequest > -1; pendingRequest--) {
            Trip requestToHandle = pendingRequests.get(pendingRequest);
            final int assignedVehicle = AVAssignment.assignVehicleToRequest(requestToHandle, availableVehicles);
            if (assignedVehicle > -1) {
                // We have a vehicle and it's getting on the way.
                handleMetRequests(pendingRequest, requestToHandle, assignedVehicle);
            } else {
                // There is currently no suitable vehicle available.
                if (requestToHandle.startTime + Constants.LEVEL_OF_SERVICE <= time) {
                    handleUnmetRequests(pendingRequest);
                } // Else we leave the request unhandled and try again in a second.
            }
        }
    }

    private static void handleMetRequests(int posOfRequestToHandle, Trip requestToHandle, int assignedVehicle) {
        // 1. Get the vehicle:
        AutonomousVehicle usedVehicle = availableVehicles.get(assignedVehicle);
        availableVehicles.remove(assignedVehicle);
        if (!redistributingVehicles.isEmpty() && redistributingVehicles.keySet().contains(usedVehicle)) {
            redistributingVehicles.remove(usedVehicle);
        }
        vehiclesInUse.add(usedVehicle);
        vehiclesInUseChanged = true;
        // 2. Move vehicle to agent:
        double travelTime = usedVehicle.moveTo(CoordUtils.createCoord(requestToHandle.startXCoord, requestToHandle.startYCoord));
        double waitingTimeForAssignment = Constants.LEVEL_OF_SERVICE - (requestToHandle.startTime - time);
        if (time < Constants.LEVEL_OF_SERVICE) {
            waitingTimeForAssignment = time;
        }
        double responseTime = waitingTimeForAssignment + travelTime;
        // 3. Agent boards vehicle:
        double waitingTimeForAgents = 0;
        if (time + travelTime < requestToHandle.startTime) {
            waitingTimeForAgents = requestToHandle.startTime - (time + travelTime);
        }
        travelTime += waitingTimeForAgents;
        travelTime += Constants.BOARDING_TIME;
        // 4. Move vehicle with agent:
        double travelTimeAgent = requestToHandle.endTime - requestToHandle.startTime;
        travelTime += travelTimeAgent;
        usedVehicle.moveTo(CoordUtils.createCoord(requestToHandle.endXCoord, requestToHandle.endYCoord));
        // 5. Agents unboards vehicle and thus frees the vehicle:
        travelTime += Constants.UNBOARDING_TIME;
        usedVehicle.setArrivalTime(time + (int) travelTime);
        // remove handled demand:
        pendingRequests.remove(posOfRequestToHandle);

        // Stats:
        Request handledRequest = new Request(requestToHandle);
        handledRequest.setIfMet(true);
        handledRequest.setAssignmentTime(waitingTimeForAssignment);
        handledRequest.setResponseTime(responseTime);
        stats.addRequest(handledRequest);
        if (responseTime <= Constants.LEVEL_OF_SERVICE) {
            stats.incMetDemand();
            stats.incWaitingTimeForAssignmentMetDemand(waitingTimeForAssignment);
            stats.incResponseTimeMetDemand(responseTime);
            stats.incTravelTimeMetDemand(travelTimeAgent);
            stats.incTravelDistanceMetDemand(requestToHandle.distance);
        } else {
            stats.incLateMetDemand();
            stats.incWaitingTimeForAssignmentLateMetDemand(waitingTimeForAssignment);
            stats.incResponseTimeLateMetDemand(responseTime);
            stats.incTravelTimeLateMetDemand(travelTimeAgent);
            stats.incTravelDistanceLateMetDemand(requestToHandle.distance);
            stats.incWaitingTime(responseTime - Constants.LEVEL_OF_SERVICE);
        }
        usedVehicle.incNumberOfServices();
        usedVehicle.incAccessTime(responseTime - waitingTimeForAssignment);
        usedVehicle.incAccessDistance((responseTime - waitingTimeForAssignment) * Constants.AV_SPEED / Constants.BEELINE_FACTOR_STREET);
        usedVehicle.incWaitingTime(waitingTimeForAgents);
        usedVehicle.incServiceTime(travelTimeAgent + Constants.BOARDING_TIME + Constants.UNBOARDING_TIME);
        usedVehicle.incServiceDistance(requestToHandle.distance);
        if (responseTime < 60) {
            quickServedRequests++;
        } else if (responseTime < Constants.LEVEL_OF_SERVICE) {
            servedRequests++;
        } else {
            lateServedRequests++;
        }
    }

    private static void handleUnmetRequests(final int positionInRequests) {
        Request unhandledRequest = new Request(pendingRequests.get(positionInRequests));
        pendingRequests.remove(positionInRequests);
        stats.incUnmetDemand();
        unhandledRequest.setIfMet(false);
        stats.addRequest(unhandledRequest);
        unservedRequests++;
    }

    private static void redistributeNonUsedVehicles() {
        if (!redistributionSwitch || redistRequestCache.size() < Constants.MIN_CACHE_SIZE_FOR_REDIST) { return; }
        double timeRelativeToRedistributionProcess = time % Constants.REDISTRIBUTIONINTERVAL;
        // Case: Redistributing process starts...
        if (timeRelativeToRedistributionProcess == 0) {
            List<AutonomousVehicle> vehiclesToRedistribute = redistribution.getRedistributingVehicles(availableVehicles, redistRequestCache);
            for (AutonomousVehicle vehicleToRedistribute : vehiclesToRedistribute) {
                vehicleToRedistribute.setArrivalTime(time + Constants.DURATION_OF_REDISTRIBUTION);
                redistributingVehicles.put(vehicleToRedistribute, null);
            }
            redistribution.updateCurrentForces(redistributingVehicles, redistRequestCache);
        }
        // Case: Redistributing process is ongoing and rerouting might be necessary...
        else if (timeRelativeToRedistributionProcess < Constants.DURATION_OF_REDISTRIBUTION
                && (timeRelativeToRedistributionProcess % Constants.REPLANNINGINTERVAL_REDISTRIBUTIONPROCESS) == 0
                && redistributingVehicles.size() > 0) {
            redistribution.updateCurrentForces(redistributingVehicles, redistRequestCache);
        }
    }

    private static void moveRedistributingVehicles() {
        if (!redistributingVehicles.isEmpty()) {
            for (AutonomousVehicle redistributingVehicle : redistributingVehicles.keySet()) {
                redistribution.moveRedistributingVehicle(redistributingVehicle, redistributingVehicles.get(redistributingVehicle));
            }
        }
    }

    private static void sortVehiclesInUse() {
        if (vehiclesInUseChanged) {
            Collections.sort( vehiclesInUse, vehicleComparator );
            vehiclesInUseChanged = false;
        }
    }

	public interface SimBase {
		List<Trip> getDemand();
		List<AutonomousVehicle> getFleet();
	}

    protected static class SimBaseImpl implements SimBase{
        private final List<Trip> demand;
        private final List<AutonomousVehicle> fleet;

        public SimBaseImpl(List<Trip> demand, List<AutonomousVehicle> fleet) {
            this.demand = demand;
            this.fleet = fleet;
        }

		@Override
		public List<Trip> getDemand() {
			return demand;
		}

		@Override
		public List<AutonomousVehicle> getFleet() {
			return fleet;
		}
	}
}
