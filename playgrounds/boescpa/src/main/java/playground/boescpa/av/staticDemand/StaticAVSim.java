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
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.misc.Counter;
import playground.boescpa.lib.tools.tripReader.Trip;

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
	private static List<Trip> pendingRequests;
	private static List<AutonomousVehicle> vehiclesInUse;
	private static List<AutonomousVehicle> availableVehicles;
	private static boolean vehiclesInUseChanged;
	private static Comparator<AutonomousVehicle> vehicleComparator;

	// results:
	private static Stats stats;

	public static void main(String[] args) {
		final String tripFile = args[0];
		final double shareOfOriginalFleet = Double.parseDouble(args[1]);

		StaticDemand staticDemand = new StaticDemand(tripFile, new String[]{"car"});
		List<Trip> demand = staticDemand.getSortedDemand();
		List<AutonomousVehicle> fleet = InitialSupplyCreator.createInitialAVSupply(shareOfOriginalFleet, staticDemand);

		stats = new Stats(demand.size());
		resetSim();
		simulate(demand, fleet);
		stats.printResults();
	}

	private static void resetSim() {
		log.info("Simulation reset...");
		pendingRequests = new ArrayList<>();
		vehiclesInUse = new ArrayList<>();
		availableVehicles = new ArrayList<>();
		vehiclesInUseChanged = false;
		vehicleComparator = new Comparator<AutonomousVehicle>() {
			@Override
			public int compare(AutonomousVehicle o1, AutonomousVehicle o2) {
				return o1.getArrivalTime() - o2.getArrivalTime();
			}
		};
		log.info("Simulation reset... done.");
	}

	private static void simulate(List<Trip> demand, List<AutonomousVehicle> fleet) {
		log.info("Simulation...");

		// Initialize Simulation
		pendingRequests.addAll(demand);
		availableVehicles.addAll(fleet);

		// Time-Step-Based Simulation
		Counter timeCounter = new Counter(" TimeStep ");
		for (time = 0; time < 86400; time++) {
			releaseArrivingVehicles();
			if (pendingRequests.size() > 0) {
				int requestToHandle = pendingRequests.size() - 1;
				while (pendingRequests.get(requestToHandle).startTime - Constants.getLevelOfService() <= time) {
					handleRequest(requestToHandle);
					requestToHandle--;
				}
				sortVehiclesInUse();
			}
			timeCounter.incCounter();
		}
		timeCounter.printCounter();

		log.info("Simulation... done.");
	}

	private static void releaseArrivingVehicles() {
		int arrivingVehicles = 0;
		while (arrivingVehicles < vehiclesInUse.size() && vehiclesInUse.get(arrivingVehicles).getArrivalTime() < time) {
			arrivingVehicles++;
		}
		for (int i = (arrivingVehicles - 1); i > -1; i--) {
			AutonomousVehicle arrivingVehicle = vehiclesInUse.get(i);
			arrivingVehicle.setArrivalTime(-1);
			vehiclesInUse.remove(i);
			availableVehicles.add(arrivingVehicle);
		}
	}

	private static void handleRequest(final int posOfRequestToHandle) {
		Trip requestToHandle = pendingRequests.get(posOfRequestToHandle);
		final int assignedVehicle = AVAssignment.assignVehicleToRequest(requestToHandle, availableVehicles);
		if (assignedVehicle > -1) {
			// We have a vehicle and it's getting on the way.
			// 1. Get the vehicle:
			AutonomousVehicle usedVehicle = availableVehicles.get(assignedVehicle);
			availableVehicles.remove(assignedVehicle);
			vehiclesInUse.add(usedVehicle);
			vehiclesInUseChanged = true;
			// 2. Move vehicle to agent:
			double travelTime = usedVehicle.moveTo(new Coord(requestToHandle.startXCoord, requestToHandle.startYCoord));
			double waitingTime = (Constants.getLevelOfService() - (requestToHandle.startTime - time)) + travelTime;
			stats.incWaitingTimeMetDemand(waitingTime);
			stats.setMaxWaitingTimeMetDemand(waitingTime);
			// 3. Agent boards vehicle:
			travelTime += Constants.getBoardingTime();
			// 4. Move vehicle with agent:
			travelTime += requestToHandle.endTime - requestToHandle.startTime;
			usedVehicle.moveTo(new Coord(requestToHandle.endXCoord, requestToHandle.endYCoord));
			// 5. Agents unboards vehicle and thus frees the vehicle:
			travelTime += Constants.getUnboardingTime();
			usedVehicle.setArrivalTime(time + (int)travelTime);
			// remove handled demand:
			pendingRequests.remove(posOfRequestToHandle);
			stats.incMetDemand();
		} else {
			// There is currently no suitable vehicle available.
			if (requestToHandle.startTime > time) {
				handleUnmetRequests(posOfRequestToHandle);
			} // Else we leave the request unhandled and try again in a second.
		}
	}

	private static void handleUnmetRequests(final int positionInRequests) {
		pendingRequests.remove(positionInRequests);
		stats.incUnmetDemand();
	}

	private static void sortVehiclesInUse() {
		if (vehiclesInUseChanged) {
//			vehiclesInUse.sort(vehicleComparator);
			Collections.sort( vehiclesInUse, vehicleComparator );
			// can't say if it does the right thing, but it compiles ... kai, sep'15
			
			vehiclesInUseChanged = false;
			throw new RuntimeException( "not sure if the above does the right thing, but it compiles.  Remove this warning if "
					+ "it works as desired. kai, sep'15") ;
		}
	}
}
