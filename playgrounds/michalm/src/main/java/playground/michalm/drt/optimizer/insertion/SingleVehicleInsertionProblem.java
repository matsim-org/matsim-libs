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

package playground.michalm.drt.optimizer.insertion;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.locationchoice.router.BackwardMultiNodePathCalculator;
import org.matsim.core.router.MultiNodePathCalculator;

import com.google.common.collect.Lists;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.optimizer.VehicleData;

/**
 * @author michalm
 */
public class SingleVehicleInsertionProblem {
	public static class Insertion {
		public final int i;
		public final int j;

		public Insertion(int i, int j) {
			this.i = i;
			this.j = j;
		}
	}

	private final OneToManyPathSearch forwardPathSearch;
	private final OneToManyPathSearch backwardPathSearch;

	private final List<Insertion> evaluatedInsertions = new ArrayList<>();

	private PathData[] pathsToPickup;
	private PathData[] pathsFromPickup;
	private PathData[] pathsToDropoff;
	private PathData[] pathsFromDropoff;

	public SingleVehicleInsertionProblem(MultiNodePathCalculator router,
			BackwardMultiNodePathCalculator backwardRouter) {
		forwardPathSearch = OneToManyPathSearch.createForwardSearch(router);
		backwardPathSearch = OneToManyPathSearch.createBackwardSearch(backwardRouter);
	}

	public Object findBestInsertion(NDrtRequest drtRequest, VehicleData.Entry vEntry) {
		// TODO what if fromLink == toLink??
		initPathData(drtRequest, vEntry);

		return null;
	}

	private final double pickupDuration = 0;// TODO how to define it????????????
	private final double dropoffDuration = 0;// TODO how to define it????????????

	private void initPathData(NDrtRequest drtRequest, VehicleData.Entry vEntry) {
		Link[] stopLinks = new Link[vEntry.stops.size()];
		for (int i = 0; i < stopLinks.length; i++) {
			stopLinks[i] = vEntry.stops.get(i).task.getLink();
		}
		double minPickupTime = drtRequest.getEarliestStartTime();// == now (for immediate requests); over-optimistic

		// calc backward dijkstra from pickup to ends of all stop + start
		// TODO exclude inserting pickup after fully occupied stops
		pathsToPickup = backwardPathSearch.calcPaths(drtRequest.getFromLink(),
				Lists.asList(vEntry.start.link, stopLinks), minPickupTime);

		// calc forward dijkstra from pickup to beginnings of all stops + dropoff
		// TODO exclude inserting before fully occupied stops (unless the new request's dropoff is located there)
		pathsFromPickup = forwardPathSearch.calcPaths(drtRequest.getFromLink(),
				Lists.asList(drtRequest.getToLink(), stopLinks), minPickupTime);

		PathData pickupToDropoffPath = pathsFromPickup[0];// only if no other passengers on board (optimistic)
		double minTravelTime = pickupToDropoffPath.path.travelTime + pickupToDropoffPath.firstAndLastLinkTT;
		double minDropoffTime = minPickupTime + minTravelTime + pickupDuration; // over-optimistic
		List<Link> stopLinkList = Arrays.asList(stopLinks);

		// calc backward dijkstra from dropoff to ends of all stops
		// TODO exclude inserting dropoff after fully occupied stops (unless the new request's dropoff is located there)
		pathsToDropoff = backwardPathSearch.calcPaths(drtRequest.getToLink(), stopLinkList, minDropoffTime);

		// calc forward dijkstra from dropoff to beginnings of all stops
		// TODO exclude inserting dropoff before fully occupied stops
		pathsFromDropoff = forwardPathSearch.calcPaths(drtRequest.getFromLink(), stopLinkList, minDropoffTime);
	}

	private void iteratePickupDropoffInsertions(NDrtRequest drtRequest, VehicleData.Entry vEntry) {
		for (int i = 0; i < pathsToPickup.length; i++) {
			// pickup is inserted after node i, where
			// node 0 is 'start' (current position/immediate diversion point)
			// node i>0 is (i-1)th 'stop task'
			// replacing arc i -> i+1 with arc pair i -> pickup -> i+1 means all following stop tasks are affected
			// (==> calc delay for tasks i to n ==> calc cost)

			int occupancy = (i == 0) ? vEntry.startOccupancy : vEntry.stops.get(i - 1).outputOccupancy;
			if (occupancy == vEntry.vehicle.getCapacity()) {
				// (after initPathData() is optimised, it will be also covered by pathsToPickup[i] == null)
				continue;// skip fully loaded arc
			}
			if (pathsToPickup[i] == null) {
				continue;// skip full
			}

			if (drtRequest.getFromLink() == vEntry.stops.get(i - 1).task.getLink()) {
				// if the request's pickup location is at the (i-1)th stop, adding it after the stop is equivalent
				// to adding it before this stop ==> we only evaluate the latter case
				continue;// optimize for cases where pickup is at the same link as nodes i or i+1
			}

			iterateDropoffInsertions(drtRequest, vEntry, i);
		}
	}

	private void iterateDropoffInsertions(NDrtRequest drtRequest, VehicleData.Entry vEntry, int i) {
		for (int j = i; j < pathsToDropoff.length; i++) {
			// dropoff is inserted after node j, where
			// node j=i is 'pickup'
			// node j>i is (j-1)th 'stop task'
			// replacing j -> j+1 with j -> dropoff -> j+1 ==> all following stop tasks are affected
			// (==> calc delay for tasks j to n ==> calc cost)

			if (j > i) {
				int occupancy = vEntry.stops.get(j - 1).outputOccupancy;
				if (occupancy == vEntry.vehicle.getCapacity()) {
					return;// stop iterating dropoffs for pickup i
				}
				evaluateInsertion(drtRequest, vEntry, i, j);
			}
		}
	}

	private void evaluateInsertion(NDrtRequest drtRequest, VehicleData.Entry vEntry, int i, int j) {
		// lastLinkTT must be taken into account (may be significant for longer links)
		double pickupInsertionStartTime = (i == 0) ? vEntry.start.time : vEntry.stops.get(i - 1).task.getEndTime();
		double toPickupTT = pathsToPickup[i].path.travelTime + pathsToPickup[i].firstAndLastLinkTT;

		int fromPickupPathIdx = i == j ? 0 : i + 1;
		double fromPickupTT = pathsFromPickup[fromPickupPathIdx].path.travelTime
				+ pathsFromPickup[fromPickupPathIdx].firstAndLastLinkTT;

		double toDropoffTT = i == j ? 0
				: pathsToDropoff[j - 1].path.travelTime + pathsToDropoff[j - 1].firstAndLastLinkTT;

		double fromDropoffTT = j == pathsFromDropoff.length - 1 ? 0
				: pathsFromPickup[j].path.travelTime + pathsFromPickup[j].firstAndLastLinkTT;

		if (i < pathsToPickup.length - 1) {// tasks i to n are delayed by pickup inserted after i-th point
			double pickupInsertionEndTime = vEntry.stops.get(i).task.getBeginTime();
			double delay = (pickupInsertionEndTime - pickupInsertionStartTime) // old duration
					+ toPickupTT + pickupDuration + fromPickupTT; // new duration
		}

		if (i == j) {
			// ??
		} else {
			double dropoffInsertionStartTime = vEntry.stops.get(j - 1).task.getEndTime();

			if (j < pathsToPickup.length - 1) {
				double dropoffInsertionEndTime = vEntry.stops.get(j).task.getBeginTime();
				double delay = (dropoffInsertionStartTime - dropoffInsertionStartTime) // old duration
						+ toDropoffTT + dropoffDuration + fromDropoffTT; // new duration
			}
		}

	}
}
