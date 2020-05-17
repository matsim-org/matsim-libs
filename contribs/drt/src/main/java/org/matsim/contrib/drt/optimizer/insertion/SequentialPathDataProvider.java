/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class SequentialPathDataProvider implements DetourDataProvider<PathData> {
	private final OneToManyPathSearch forwardPathSearch;
	private final OneToManyPathSearch backwardPathSearch;
	private final double stopDuration;

	public SequentialPathDataProvider(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
			DrtConfigGroup drtCfg) {
		forwardPathSearch = OneToManyPathSearch.createForwardSearch(network, travelTime, travelDisutility);
		backwardPathSearch = OneToManyPathSearch.createBackwardSearch(network, travelTime, travelDisutility);
		stopDuration = drtCfg.getStopDuration();
	}

	@Override
	public DetourDataSet<PathData> getDetourDataSet(DrtRequest drtRequest, Entry vEntry) {
		ArrayList<Link> links = new ArrayList<>(vEntry.stops.size() + 1);
		links.add(null);// special link
		for (Stop s : vEntry.stops) {
			links.add(s.task.getLink());
		}

		double earliestPickupTime = drtRequest.getEarliestStartTime();// over-optimistic

		// calc backward dijkstra from pickup to ends of all stop + start
		// TODO exclude inserting pickup after fully occupied stops
		links.set(0, vEntry.start.link);
		Map<Link, PathData> pathsToPickup = backwardPathSearch.calcPathDataMap(drtRequest.getFromLink(), links,
				earliestPickupTime);

		// calc forward dijkstra from pickup to beginnings of all stops + dropoff
		// TODO exclude inserting before fully occupied stops (unless the new request's dropoff is located there)
		links.set(0, drtRequest.getToLink());
		Map<Link, PathData> pathsFromPickup = forwardPathSearch.calcPathDataMap(drtRequest.getFromLink(), links,
				earliestPickupTime);

		PathData pickupToDropoffPath = pathsFromPickup.get(
				drtRequest.getToLink().getId());// only if no other passengers on board (optimistic)
		double minTravelTime = pickupToDropoffPath.getTravelTime();
		double earliestDropoffTime = earliestPickupTime + minTravelTime + stopDuration; // over-optimistic

		// calc backward dijkstra from dropoff to ends of all stops
		// TODO exclude inserting dropoff after fully occupied stops (unless the new request's dropoff is located there)
		links.set(0, drtRequest.getToLink());// TODO change to null (after nulls are supported by OneToManyPathSearch)
		Map<Link, PathData> pathsToDropoff = backwardPathSearch.calcPathDataMap(drtRequest.getToLink(), links,
				earliestDropoffTime);

		// calc forward dijkstra from dropoff to beginnings of all stops
		// TODO exclude inserting dropoff before fully occupied stops
		Map<Link, PathData> pathsFromDropoff = forwardPathSearch.calcPathDataMap(drtRequest.getToLink(), links,
				earliestDropoffTime);

		return DetourDataProvider.getDetourDataSet(drtRequest, vEntry, pathsToPickup::get, pathsFromPickup::get,
				pathsToDropoff::get, pathsFromDropoff::get);
	}
}
