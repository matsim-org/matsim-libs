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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import javax.inject.Named;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.collect.Maps;

import ch.sbb.matsim.routing.graph.Graph;

/**
 * @author michalm
 */
public class MultiInsertionDetourPathCalculator implements DetourPathCalculator, MobsimBeforeCleanupListener {
	public static final int MAX_THREADS = 4;

	private final OneToManyPathSearch toPickupPathSearch;
	private final OneToManyPathSearch fromPickupPathSearch;
	private final OneToManyPathSearch toDropoffPathSearch;
	private final OneToManyPathSearch fromDropoffPathSearch;

	private final ExecutorService executorService;

	public MultiInsertionDetourPathCalculator(Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, TravelDisutility travelDisutility,
			DrtConfigGroup drtCfg) {
		Graph graph = new Graph(network);
		IdMap<Node, Node> nodeMap = new IdMap<>(Node.class);
		nodeMap.putAll(network.getNodes());

		toPickupPathSearch = OneToManyPathSearch.createSearch(graph, nodeMap, travelTime, travelDisutility, true);
		fromPickupPathSearch = OneToManyPathSearch.createSearch(graph, nodeMap, travelTime, travelDisutility, true);
		toDropoffPathSearch = OneToManyPathSearch.createSearch(graph, nodeMap, travelTime, travelDisutility, true);
		fromDropoffPathSearch = OneToManyPathSearch.createSearch(graph, nodeMap, travelTime, travelDisutility, true);
		executorService = Executors.newFixedThreadPool(Math.min(drtCfg.getNumberOfThreads(), MAX_THREADS));
	}

	@Override
	public DetourData<PathData> calculatePaths(DrtRequest drtRequest, List<Insertion> filteredInsertions) {
		// with vehicle insertion filtering -- pathsToPickup is the most computationally demanding task, while
		// pathsFromDropoff is the least demanding one
		var pathsToPickupFuture = executorService.submit(() -> calcPathsToPickup(drtRequest, filteredInsertions));
		var pathsFromPickupFuture = executorService.submit(() -> calcPathsFromPickup(drtRequest, filteredInsertions));
		var pathsToDropoffFuture = executorService.submit(() -> calcPathsToDropoff(drtRequest, filteredInsertions));
		var pathsFromDropoffFuture = executorService.submit(() -> calcPathsFromDropoff(drtRequest, filteredInsertions));

		try {
			return new DetourData<>(pathsToPickupFuture.get(), pathsFromPickupFuture.get(), pathsToDropoffFuture.get(),
					pathsFromDropoffFuture.get());
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<Link, PathData> calcPathsToPickup(DrtRequest drtRequest, List<Insertion> filteredInsertions) {
		// calc backward dijkstra from pickup to ends of selected stops + starts
		double earliestPickupTime = drtRequest.getEarliestStartTime(); // optimistic
		Collection<Link> toLinks = getDetourLinks(filteredInsertions, insertion -> insertion.pickup.previousLink);
		double maxTravelTime = drtRequest.getLatestStartTime() - earliestPickupTime;
		return toPickupPathSearch.calcPathDataMap(drtRequest.getFromLink(), toLinks, earliestPickupTime, false,
				maxTravelTime);
	}

	private Map<Link, PathData> calcPathsFromPickup(DrtRequest drtRequest, List<Insertion> filteredInsertions) {
		// calc forward dijkstra from pickup to beginnings of selected stops + dropoff
		double earliestPickupTime = drtRequest.getEarliestStartTime(); // optimistic
		Collection<Link> toLinks = getDetourLinks(filteredInsertions, insertion -> insertion.pickup.nextLink);
		return fromPickupPathSearch.calcPathDataMap(drtRequest.getFromLink(), toLinks, earliestPickupTime, true);
	}

	private Map<Link, PathData> calcPathsToDropoff(DrtRequest drtRequest, List<Insertion> filteredInsertions) {
		// calc backward dijkstra from dropoff to ends of selected stops
		double latestDropoffTime = drtRequest.getLatestArrivalTime(); // pessimistic
		Collection<Link> toLinks = getDetourLinks(filteredInsertions, insertion -> insertion.dropoff.previousLink);
		return toDropoffPathSearch.calcPathDataMap(drtRequest.getToLink(), toLinks, latestDropoffTime, false);
	}

	private Map<Link, PathData> calcPathsFromDropoff(DrtRequest drtRequest, List<Insertion> filteredInsertions) {
		// calc forward dijkstra from dropoff to beginnings of selected stops
		double latestDropoffTime = drtRequest.getLatestArrivalTime(); // pessimistic
		Collection<Link> toLinks = getDetourLinks(filteredInsertions, insertion -> insertion.dropoff.nextLink);
		return fromDropoffPathSearch.calcPathDataMap(drtRequest.getToLink(), toLinks, latestDropoffTime, true);
	}

	private Collection<Link> getDetourLinks(List<Insertion> filteredInsertions,
			Function<Insertion, Link> detourLinkExtractor) {
		Map<Id<Link>, Link> pickupDetourStartLinks = Maps.newHashMapWithExpectedSize(filteredInsertions.size());
		for (Insertion insertion : filteredInsertions) {
			Link link = detourLinkExtractor.apply(insertion);
			if (link != null) {
				pickupDetourStartLinks.putIfAbsent(link.getId(), link);
			}
		}
		return pickupDetourStartLinks.values();
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		executorService.shutdown();
	}
}
