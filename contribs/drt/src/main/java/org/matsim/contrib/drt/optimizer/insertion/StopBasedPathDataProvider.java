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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.DetourLinksProvider.DetourLinksSet;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.ManyToManyPathData;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.common.collect.ImmutableList;

/**
 * @author michalm
 */
public class StopBasedPathDataProvider implements PrecalculablePathDataProvider {
	private final double stopDuration;

	private final ManyToManyPathData manyToManyPathData;

	// ==== recalculated by calcPathData()
	private Map<Id<Link>, PathData> pathsToPickupMap;
	private Map<Id<Link>, PathData> pathsFromPickupMap;
	private Map<Id<Link>, PathData> pathsToDropoffMap;
	private Map<Id<Link>, PathData> pathsFromDropoffMap;

	@Inject
	public StopBasedPathDataProvider(@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, @Drt TravelDisutility travelDisutility,
			@Drt TransitSchedule schedule, TravelTimeCalculatorConfigGroup ttcConfig, DrtConfigGroup drtCfg) {
		stopDuration = drtCfg.getStopDuration();

		List<Link> stopLinks = schedule.getFacilities()
				.values()
				.stream()
				.map(tsf -> network.getLinks().get(tsf.getLinkId()))//
				.distinct()// more than one stop can be located on a link
				.collect(ImmutableList.toImmutableList());
		manyToManyPathData = new ManyToManyPathData(network, travelTime, travelDisutility, stopLinks,
				new TimeDiscretizer(ttcConfig), drtCfg.getNumberOfThreads());
	}

	@Override
	public void precalculatePathData(DrtRequest drtRequest, DetourLinksSet detourLinkSet) {
		Link pickup = drtRequest.getFromLink();
		Link dropoff = drtRequest.getToLink();

		final double earliestPickupTime = drtRequest.getEarliestStartTime(); // optimistic
		final double minTravelTime = 15 * 60; // FIXME inaccurate temp solution: fixed 15 min
		final double earliestDropoffTime = earliestPickupTime + minTravelTime + stopDuration;

		// NOTE: all paths are calculated forward from startTime (no backward Dijkstra used)
		pathsToPickupMap = manyToManyPathData.getIncomingPathData(pickup.getId(), earliestPickupTime);
		pathsFromPickupMap = manyToManyPathData.getOutgoingPathData(pickup.getId(), earliestPickupTime);
		pathsToDropoffMap = manyToManyPathData.getIncomingPathData(dropoff.getId(), earliestDropoffTime);
		pathsFromDropoffMap = manyToManyPathData.getOutgoingPathData(dropoff.getId(), earliestDropoffTime);
	}

	@Override
	public PathDataSet getPathDataSet(DrtRequest drtRequest, Entry vEntry) {
		return PrecalculablePathDataProvider.getPathDataSet(drtRequest, vEntry, pathsToPickupMap, pathsFromPickupMap,
				pathsToDropoffMap, pathsFromDropoffMap);
	}
}
