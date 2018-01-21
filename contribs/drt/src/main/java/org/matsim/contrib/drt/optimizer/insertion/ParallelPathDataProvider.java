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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * @author michalm
 */
public class ParallelPathDataProvider implements PathDataProvider {
	private static final int THREADS = 4;

	private final OneToManyPathSearch toPickupPathSearch;
	private final OneToManyPathSearch fromPickupPathSearch;
	private final OneToManyPathSearch toDropoffPathSearch;
	private final OneToManyPathSearch fromDropoffPathSearch;

	private final double stopDuration;

	private final ExecutorService executorService;

	// ==== recalculated by calcPathData()
	private Map<Id<Link>, PathData> pathsToPickupMap;
	private Map<Id<Link>, PathData> pathsFromPickupMap;
	private Map<Id<Link>, PathData> pathsToDropoffMap;
	private Map<Id<Link>, PathData> pathsFromDropoffMap;

	public ParallelPathDataProvider(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
			DrtConfigGroup drtCfg) {
		toPickupPathSearch = OneToManyPathSearch.createBackwardSearch(network, travelTime, travelDisutility);
		fromPickupPathSearch = OneToManyPathSearch.createForwardSearch(network, travelTime, travelDisutility);
		toDropoffPathSearch = OneToManyPathSearch.createBackwardSearch(network, travelTime, travelDisutility);
		fromDropoffPathSearch = OneToManyPathSearch.createForwardSearch(network, travelTime, travelDisutility);
		stopDuration = drtCfg.getStopDuration();
		executorService = Executors.newFixedThreadPool(THREADS);
	}

	public void calcPathData(DrtRequest drtRequest, Collection<VehicleData.Entry> vEntries) {
		Map<Id<Link>, Link> startLinks = new HashMap<>();
		Map<Id<Link>, Link> stopLinks = new HashMap<>();

		for (VehicleData.Entry vEntry : vEntries) {
			startLinks.put(vEntry.start.link.getId(), vEntry.start.link);

			for (Stop s : vEntry.stops) {
				// TODO consider a stop filter (replacement for/addition to DrtVehicleFilter)
				// the filtering could distinguish between pickup/dropoff...
				Link l = s.task.getLink();
				stopLinks.put(l.getId(), l);
			}
		}

		final double earliestPickupTime = drtRequest.getEarliestStartTime(); // optimistic
		final double minTravelTime = 15 * 60; // FIXME inaccurate temp solution: fixed 15 min
		final double earliestDropoffTime = earliestPickupTime + minTravelTime + stopDuration;
		ImmutableList<Link> stopLinkList = ImmutableList.copyOf(stopLinks.values());

		Future<Map<Id<Link>, PathData>> pathsToPickupFuture = executorService.submit(() -> {
			ImmutableList<Link> startAndStopLinkList = ImmutableList
					.copyOf(Iterables.concat(startLinks.values(), stopLinkList));
			// calc backward dijkstra from pickup to ends of all stop + start
			// TODO exclude inserting pickup after fully occupied stops
			return toPickupPathSearch.calcPathDataMap(drtRequest.getFromLink(), startAndStopLinkList,
					earliestPickupTime);
		});

		Future<Map<Id<Link>, PathData>> pathsFromPickupFuture = executorService.submit(() -> {
			ImmutableList<Link> dropoffAndStopLinkList = ImmutableList
					.<Link> builderWithExpectedSize(stopLinkList.size() + 1).add(drtRequest.getToLink())
					.addAll(stopLinkList).build();
			// calc forward dijkstra from pickup to beginnings of all stops + dropoff
			// TODO exclude inserting before fully occupied stops (unless the new request's dropoff is located there)
			return fromPickupPathSearch.calcPathDataMap(drtRequest.getFromLink(), dropoffAndStopLinkList,
					earliestPickupTime);
		});

		Future<Map<Id<Link>, PathData>> pathsToDropoffFuture = executorService.submit(() -> {
			// calc backward dijkstra from dropoff to ends of all stops
			// TODO exclude inserting dropoff after fully occupied stops (unless the new request's dropoff is located
			// there)
			return toDropoffPathSearch.calcPathDataMap(drtRequest.getToLink(), stopLinkList, earliestDropoffTime);
		});

		Future<Map<Id<Link>, PathData>> pathsFromDropoffFuture = executorService.submit(() -> {
			// calc forward dijkstra from dropoff to beginnings of all stops
			// TODO exclude inserting dropoff before fully occupied stops
			return fromDropoffPathSearch.calcPathDataMap(drtRequest.getToLink(), stopLinkList, earliestDropoffTime);
		});

		try {
			pathsToPickupMap = pathsToPickupFuture.get();
			pathsFromPickupMap = pathsFromPickupFuture.get();
			pathsToDropoffMap = pathsToDropoffFuture.get();
			pathsFromDropoffMap = pathsFromDropoffFuture.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PathDataSet getPathDataSet(DrtRequest drtRequest, Entry vEntry) {
		int length = vEntry.stops.size() + 1;
		PathData[] pathsToPickup = new PathData[length];
		PathData[] pathsFromPickup = new PathData[length];
		PathData[] pathsToDropoff = new PathData[length];
		PathData[] pathsFromDropoff = new PathData[length];

		pathsToPickup[0] = pathsToPickupMap.get(vEntry.start.link.getId());// start->pickup
		pathsFromPickup[0] = pathsFromPickupMap.get(drtRequest.getToLink().getId());// pickup->dropoff

		int i = 1;
		for (Stop s : vEntry.stops) {
			Id<Link> linkId = s.task.getLink().getId();
			pathsToPickup[i] = pathsToPickupMap.get(linkId);
			pathsFromPickup[i] = pathsFromPickupMap.get(linkId);
			pathsToDropoff[i] = pathsToDropoffMap.get(linkId);
			pathsFromDropoff[i] = pathsFromDropoffMap.get(linkId);
			i++;
		}

		return new PathDataSet(pathsToPickup, pathsFromPickup, pathsToDropoff, pathsFromDropoff);
	}

	public void shutdown() {
		executorService.shutdown();
	}
}
