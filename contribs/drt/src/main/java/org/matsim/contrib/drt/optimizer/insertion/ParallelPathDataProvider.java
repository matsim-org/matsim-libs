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

import static org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Named;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class ParallelPathDataProvider implements PathDataProvider, MobsimBeforeCleanupListener {

	private static class DetourLinksSet {
		final Map<Id<Link>, Link> pickupDetourStartLinks;
		final Map<Id<Link>, Link> pickupDetourEndLinks;
		final Map<Id<Link>, Link> dropoffDetourStartLinks;
		final Map<Id<Link>, Link> dropoffDetourEndLinks;

		public DetourLinksSet(List<Insertion> filteredInsertions) {
			pickupDetourStartLinks = new HashMap<>();
			pickupDetourEndLinks = new HashMap<>();
			dropoffDetourStartLinks = new HashMap<>();
			dropoffDetourEndLinks = new HashMap<>();

			filteredInsertions.forEach(insertion -> {
				addLink(pickupDetourStartLinks, insertion.pickup.previousLink);
				addLink(pickupDetourEndLinks, insertion.pickup.nextLink);
				addLink(dropoffDetourStartLinks, insertion.dropoff.previousLink);
				addLink(dropoffDetourEndLinks, insertion.dropoff.nextLink);
			});
		}

		private void addLink(Map<Id<Link>, Link> map, Link link) {
			if (link != null) {
				map.put(link.getId(), link);
			}
		}
	}

	public static final int MAX_THREADS = 4;

	private final OneToManyPathSearch toPickupPathSearch;
	private final OneToManyPathSearch fromPickupPathSearch;
	private final OneToManyPathSearch toDropoffPathSearch;
	private final OneToManyPathSearch fromDropoffPathSearch;

	private final double stopDuration;

	private final ExecutorService executorService;

	public ParallelPathDataProvider(Network network, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			TravelDisutility travelDisutility, DrtConfigGroup drtCfg) {
		toPickupPathSearch = OneToManyPathSearch.createBackwardSearch(network, travelTime, travelDisutility);
		fromPickupPathSearch = OneToManyPathSearch.createForwardSearch(network, travelTime, travelDisutility);
		toDropoffPathSearch = OneToManyPathSearch.createBackwardSearch(network, travelTime, travelDisutility);
		fromDropoffPathSearch = OneToManyPathSearch.createForwardSearch(network, travelTime, travelDisutility);
		stopDuration = drtCfg.getStopDuration();
		executorService = Executors.newFixedThreadPool(Math.min(drtCfg.getNumberOfThreads(), MAX_THREADS));
	}

	@Override
	public DetourData<PathData> getPathData(DrtRequest drtRequest, List<Insertion> filteredInsertions) {
		Link pickup = drtRequest.getFromLink();
		Link dropoff = drtRequest.getToLink();

		double earliestPickupTime = drtRequest.getEarliestStartTime(); // optimistic
		double minTravelTime = 15 * 60; // FIXME inaccurate temp solution: fixed 15 min
		double earliestDropoffTime = earliestPickupTime + minTravelTime + stopDuration;

		// with vehicle insertion filtering -- pathsToPickup is the most computationally demanding task, while
		// pathsFromDropoff is the least demanding one

		//TODO move extraction of links from filteredInsertions to each Callable task
		DetourLinksSet detourLinksSet = new DetourLinksSet(filteredInsertions);

		// calc backward dijkstra from pickup to ends of selected stops + starts
		// highest computation time (approx. 45% total CPU time)
		Future<Map<Link, PathData>> pathsToPickupFuture = executorService.submit(
				() -> toPickupPathSearch.calcPathDataMap(pickup, detourLinksSet.pickupDetourStartLinks.values(),
						earliestPickupTime));

		// calc forward dijkstra from pickup to beginnings of selected stops + dropoff
		// medium computation time (approx. 25% total CPU time)
		Future<Map<Link, PathData>> pathsFromPickupFuture = executorService.submit(
				() -> fromPickupPathSearch.calcPathDataMap(pickup, detourLinksSet.pickupDetourEndLinks.values(),
						earliestPickupTime));

		// calc backward dijkstra from dropoff to ends of selected stops
		// medium computation time (approx. 25% total CPU time)
		Future<Map<Link, PathData>> pathsToDropoffFuture = executorService.submit(
				() -> toDropoffPathSearch.calcPathDataMap(dropoff, detourLinksSet.dropoffDetourStartLinks.values(),
						earliestDropoffTime));

		// calc forward dijkstra from dropoff to beginnings of selected stops
		// lowest computation time (approx. 5% total CPU time)
		Future<Map<Link, PathData>> pathsFromDropoffFuture = executorService.submit(
				() -> fromDropoffPathSearch.calcPathDataMap(dropoff, detourLinksSet.dropoffDetourEndLinks.values(),
						earliestDropoffTime));

		try {
			return new DetourData<>(pathsToPickupFuture.get(), pathsFromPickupFuture.get(), pathsToDropoffFuture.get(),
					pathsFromDropoffFuture.get());
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		executorService.shutdown();
	}
}
