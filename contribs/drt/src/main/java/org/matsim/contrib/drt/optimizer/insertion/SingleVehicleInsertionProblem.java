/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.optimizer.insertion;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.MultiNodePathCalculator;

/**
 * @author michalm
 */
public class SingleVehicleInsertionProblem {
	public static class Insertion {
		public final int pickupIdx;
		public final int dropoffIdx;
		public final PathData pathToPickup;
		public final PathData pathFromPickup;
		public final PathData pathToDropoff;// null if dropoff inserted directly after pickup
		public final PathData pathFromDropoff;// null if dropoff inserted at the end

		public Insertion(int pickupIdx, int dropoffIdx, PathData pathToPickup, PathData pathFromPickup,
				PathData pathToDropoff, PathData pathFromDropoff) {
			this.pickupIdx = pickupIdx;
			this.dropoffIdx = dropoffIdx;
			this.pathToPickup = pathToPickup;
			this.pathFromPickup = pathFromPickup;
			this.pathToDropoff = pathToDropoff;
			this.pathFromDropoff = pathFromDropoff;
		}

		@Override
		public String toString() {
			return "Insertion: pickupIdx=" + pickupIdx + ", dropoffIdx=" + dropoffIdx;
		}
	}

	public static class BestInsertion {
		public final Insertion insertion;
		public final VehicleData.Entry vehicleEntry;
		public final double cost;

		public BestInsertion(Insertion insertion, VehicleData.Entry vehicleEntry, double cost) {
			this.insertion = insertion;
			this.vehicleEntry = vehicleEntry;
			this.cost = cost;
		}
	}

	private final OneToManyPathSearch forwardPathSearch;
	private final OneToManyPathSearch backwardPathSearch;
	private final double stopDuration;
	private final MobsimTimer timer;
	private final InsertionCostCalculator costCalculator;

	///

	private List<Insertion> insertions;
	private int stopCount;

	// path[0] is a special entry; path[i] corresponds to stop i-1, for 1 <= i <= stopCount
	private PathData[] pathsToPickup;
	private PathData[] pathsFromPickup;
	private PathData[] pathsToDropoff;
	private PathData[] pathsFromDropoff;

	// TODO filter out duplicated insertion when pickup/dropoff is at one of existing stops
	// filter out stops located too far away (e.g. straight-line distance); with the exception for the last stop???
	// filter out pickups at stops with outgoingOccupancy equal to the vehicle capacity
	// filter out dropoffs at stops with incomingOccupancy equal to the vehicle capacity
	// (but still we need to check the capacity constraints on all drives between the pickup and dropoff)
	//
	// TODO maxWaitTime
	// filter out stops which are visited too late
	//
	// private boolean[] considerPickupInsertion;
	// private boolean[] considerDropoffInsertion;

	public SingleVehicleInsertionProblem(MultiNodePathCalculator router, BackwardMultiNodePathCalculator backwardRouter,
			double stopDuration, double maxWaitTime, MobsimTimer timer) {
		forwardPathSearch = OneToManyPathSearch.createForwardSearch(router);
		backwardPathSearch = OneToManyPathSearch.createBackwardSearch(backwardRouter);

		this.stopDuration = stopDuration;
		this.timer = timer;
		costCalculator = new InsertionCostCalculator(stopDuration, maxWaitTime);
	}

	public BestInsertion findBestInsertion(DrtRequest drtRequest, VehicleData.Entry vEntry) {
		initPathData(drtRequest, vEntry);
		findPickupDropoffInsertions(drtRequest, vEntry);
		return selectBestInsertion(drtRequest, vEntry);
	}

	private void initPathData(DrtRequest drtRequest, VehicleData.Entry vEntry) {
		stopCount = vEntry.stops.size();

		ArrayList<Link> links = new ArrayList<>(stopCount + 1);
		links.add(null);// special link
		for (int i = 0; i < stopCount; i++) {
			links.add(vEntry.stops.get(i).task.getLink());
		}

		double minPickupTime = drtRequest.getEarliestStartTime();// == now (for immediate requests); over-optimistic

		// calc backward dijkstra from pickup to ends of all stop + start
		// TODO exclude inserting pickup after fully occupied stops
		links.set(0, vEntry.start.link);
		pathsToPickup = backwardPathSearch.calcPaths(drtRequest.getFromLink(), links, minPickupTime);

		// calc forward dijkstra from pickup to beginnings of all stops + dropoff
		// TODO exclude inserting before fully occupied stops (unless the new request's dropoff is located there)
		links.set(0, drtRequest.getToLink());
		pathsFromPickup = forwardPathSearch.calcPaths(drtRequest.getFromLink(), links, minPickupTime);

		PathData pickupToDropoffPath = pathsFromPickup[0];// only if no other passengers on board (optimistic)
		double minTravelTime = pickupToDropoffPath.path.travelTime + pickupToDropoffPath.firstAndLastLinkTT;
		double minDropoffTime = minPickupTime + minTravelTime + stopDuration; // uses (over-)optimistic components

		// calc backward dijkstra from dropoff to ends of all stops
		// TODO exclude inserting dropoff after fully occupied stops (unless the new request's dropoff is located there)
		links.set(0, drtRequest.getFromLink());
		// TODO change the above line into the following one (after nulls are supported by OneToManyPathSearch)
		// links.set(0, null);
		pathsToDropoff = backwardPathSearch.calcPaths(drtRequest.getToLink(), links, minDropoffTime);

		// calc forward dijkstra from dropoff to beginnings of all stops
		// TODO exclude inserting dropoff before fully occupied stops
		pathsFromDropoff = forwardPathSearch.calcPaths(drtRequest.getToLink(), links, minDropoffTime);
	}

	private void findPickupDropoffInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry) {
		insertions = new ArrayList<>();
		for (int i = 0; i <= stopCount; i++) {
			// pickup is inserted after node i, where
			// node 0 is 'start' (current position/immediate diversion point)
			// node i > 0 is (i-1)th 'stop task'
			// replacing i -> i+1 with i -> pickup -> i+1 means all following stop tasks are affected
			// (==> calc delay for tasks i to n ==> calc cost)

			int occupancy = (i == 0) ? vEntry.startOccupancy : vEntry.stops.get(i - 1).outputOccupancy;
			if (occupancy == vEntry.vehicle.getCapacity()) {
				// (after initPathData() is optimised, it will be also covered by pathsToPickup[i] == null)
				continue;// skip fully loaded arcs
			}
			if (pathsToPickup[i] == null) {
				continue;// skip fully loaded arcs
			}

			if (i < stopCount && // has next stop
					drtRequest.getFromLink() == vEntry.stops.get(i).task.getLink()) {// next stop is at the same link
				// optimize for cases where the pickup is at the same link as stop i (i.e. node i+1)
				// in this case inserting the pickup either before and after the stop is equivalent
				// ==> only evaluate insertion _after_ stop i (node i+1)
				continue;
			}

			iterateDropoffInsertions(drtRequest, vEntry, i);
		}
	}

	private void iterateDropoffInsertions(DrtRequest drtRequest, VehicleData.Entry vEntry, int i) {
		for (int j = i; j <= stopCount; j++) {
			// dropoff is inserted after node j, where
			// node j=i is 'pickup'
			// node j>i is (j-1)th 'stop task'
			// replacing j -> j+1 with j -> dropoff -> j+1 ==> all following stop tasks are affected
			// (==> calc delay for tasks j to n ==> calc cost)

			// no checking the capacity constraints if i == j
			if (j > i && // i -> pickup -> i+1 && j -> dropoff -> j+1
					vEntry.stops.get(j - 1).outputOccupancy == vEntry.vehicle.getCapacity()) {
				return;// stop iterating -- cannot insert dropoff after node j
			}

			if (j < stopCount && // has next stop
					drtRequest.getToLink() == vEntry.stops.get(j).task.getLink()) {// next stop is at the same link
				// optimize for cases where the dropoff is at the same link as stop j-1 (i.e. node j)
				// in this case inserting the dropoff either before and after the stop is equivalent
				// ==> only evaluate insertion _after_ stop j (node j+1)
				continue;
			}

			addInsertion(drtRequest, vEntry, i, j);
		}
	}

	private void addInsertion(DrtRequest drtRequest, VehicleData.Entry vEntry, int i, int j) {
		// i -> pickup
		PathData toPickup = pathsToPickup[i]; // i -> pickup
		PathData fromPickup = pathsFromPickup[i == j ? 0 : i + 1]; // pickup -> (dropoff | i+1)
		PathData toDropoff = i == j ? null // pickup followed by dropoff
				: pathsToDropoff[j]; // j -> dropoff
		PathData fromDropoff = j == stopCount ? null // dropoff inserted at the end
				: pathsFromDropoff[j + 1];

		insertions.add(new Insertion(i, j, toPickup, fromPickup, toDropoff, fromDropoff));
	}

	private BestInsertion selectBestInsertion(DrtRequest drtRequest, VehicleData.Entry vEntry) {
		double minCost = Double.MAX_VALUE;
		Insertion bestInsertion = null;
		for (Insertion insertion : insertions) {
			double cost = costCalculator.calculate(drtRequest, vEntry, insertion, timer.getTimeOfDay());
			if (cost < minCost) {
				bestInsertion = insertion;
				minCost = cost;
			}
		}
		return new BestInsertion(bestInsertion, vEntry, minCost);
	}
}
