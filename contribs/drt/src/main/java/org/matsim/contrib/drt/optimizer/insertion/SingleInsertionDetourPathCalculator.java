/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import static org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Named;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SingleInsertionDetourPathCalculator implements DetourPathCalculator, MobsimBeforeCleanupListener {

	public static final int MAX_THREADS = 4;

	private final LeastCostPathCalculator toPickupPathSearch;
	private final LeastCostPathCalculator fromPickupPathSearch;
	private final LeastCostPathCalculator toDropoffPathSearch;
	private final LeastCostPathCalculator fromDropoffPathSearch;

	private final double stopDuration;

	private final ExecutorService executorService;

	public SingleInsertionDetourPathCalculator(Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, TravelDisutility travelDisutility,
			DrtConfigGroup drtCfg) {
		LeastCostPathCalculatorFactory pathCalculatorFactory = new FastAStarLandmarksFactory(
				drtCfg.getNumberOfThreads());
		toPickupPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		fromPickupPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		toDropoffPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		fromDropoffPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		stopDuration = drtCfg.getStopDuration();
		executorService = Executors.newFixedThreadPool(Math.min(drtCfg.getNumberOfThreads(), MAX_THREADS));
	}

	@Override
	public DetourData<PathData> calculatePaths(DrtRequest drtRequest, List<Insertion> filteredInsertions) {
		Link pickup = drtRequest.getFromLink();
		Link dropoff = drtRequest.getToLink();

		Preconditions.checkArgument(filteredInsertions.size() == 1);
		Insertion insertion = filteredInsertions.get(0);

		double earliestPickupTime = drtRequest.getEarliestStartTime(); // optimistic
		double minTravelTime = 15 * 60; // FIXME inaccurate temp solution: fixed 15 min
		double earliestDropoffTime = earliestPickupTime + minTravelTime + stopDuration;

		// TODO use times from InsertionWithDetourData<Double> as approximate departure times for Dijkstra (will require
		//  passing it as an argument, instead of Insertion)

		Future<Map<Link, PathData>> pathsToPickupFuture = executorService.submit(
				() -> Map.of(insertion.pickup.previousLink,
						calcPathData(toPickupPathSearch, insertion.pickup.previousLink, pickup, earliestPickupTime)));

		Future<Map<Link, PathData>> pathsFromPickupFuture = executorService.submit(
				() -> Map.of(insertion.pickup.nextLink,
						calcPathData(fromPickupPathSearch, pickup, insertion.pickup.nextLink, earliestPickupTime)));

		Future<Map<Link, PathData>> pathsToDropoffFuture = insertion.dropoff.previousLink == null ?
				Futures.immediateFuture(ImmutableMap.of()) :
				executorService.submit(() -> Map.of(insertion.dropoff.previousLink,
						calcPathData(toDropoffPathSearch, insertion.dropoff.previousLink, dropoff,
								earliestDropoffTime)));

		Future<Map<Link, PathData>> pathsFromDropoffFuture = insertion.dropoff.nextLink == null ?
				Futures.immediateFuture(ImmutableMap.of()) :
				executorService.submit(() -> Map.of(insertion.dropoff.nextLink,
						calcPathData(fromDropoffPathSearch, dropoff, insertion.dropoff.nextLink, earliestDropoffTime)));

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

	private PathData calcPathData(LeastCostPathCalculator router, Link fromLink, Link toLink, double departureTime) {
		if (fromLink == toLink) {
			return PathData.EMPTY;
		}

		Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), departureTime + FIRST_LINK_TT,
				null, null);
		double firstAndLastLinkTT = FIRST_LINK_TT + VrpPaths.getLastLinkTT(toLink,
				departureTime + FIRST_LINK_TT + path.travelTime);

		return new PathData(path, firstAndLastLinkTT);
	}
}
