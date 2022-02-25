/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.selective;

import static org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import static org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Futures;

/**
 * @author Michal Maciejewski (michalm)
 */
class SingleInsertionDetourPathCalculator implements MobsimBeforeCleanupListener {

	public static final int MAX_THREADS = 4;

	private final TravelTime travelTime;

	private final LeastCostPathCalculator toPickupPathSearch;
	private final LeastCostPathCalculator fromPickupPathSearch;
	private final LeastCostPathCalculator toDropoffPathSearch;
	private final LeastCostPathCalculator fromDropoffPathSearch;

	private final ExecutorService executorService;

	SingleInsertionDetourPathCalculator(Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, DrtConfigGroup drtCfg) {
		this(network, travelTime, travelDisutility, drtCfg.getNumberOfThreads(), new SpeedyALTFactory());
	}

	@VisibleForTesting
	SingleInsertionDetourPathCalculator(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
			int numberOfThreads, LeastCostPathCalculatorFactory pathCalculatorFactory) {
		this.travelTime = travelTime;

		toPickupPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		fromPickupPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		toDropoffPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		fromDropoffPathSearch = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime);
		executorService = Executors.newFixedThreadPool(Math.min(numberOfThreads, MAX_THREADS));
	}

	InsertionDetourData calculatePaths(DrtRequest drtRequest, Insertion insertion) {
		Link pickup = drtRequest.getFromLink();
		Link dropoff = drtRequest.getToLink();

		double earliestPickupTime = drtRequest.getEarliestStartTime(); // optimistic
		double latestDropoffTime = drtRequest.getLatestArrivalTime(); // pessimistic

		// TODO use times from InsertionWithDetourData<Double> as approximate departure times for Dijkstra (will require
		//  passing it as an argument, instead of Insertion)

		Future<PathData> toPickupFuture = executorService.submit(
				() -> calcPathData(toPickupPathSearch, insertion.pickup.previousWaypoint.getLink(), pickup,
						earliestPickupTime));

		Future<PathData> fromPickupFuture = executorService.submit(
				() -> calcPathData(fromPickupPathSearch, pickup, insertion.pickup.nextWaypoint.getLink(),
						earliestPickupTime));

		Future<PathData> toDropoffFuture = insertion.dropoff.previousWaypoint instanceof Waypoint.Pickup ?
				Futures.immediateFuture(null) :
				executorService.submit(
						() -> calcPathData(toDropoffPathSearch, insertion.dropoff.previousWaypoint.getLink(), dropoff,
								latestDropoffTime));

		Future<PathData> fromDropoffFuture = insertion.dropoff.nextWaypoint instanceof Waypoint.End ?
				Futures.immediateFuture(PathData.EMPTY) :
				executorService.submit(
						() -> calcPathData(fromDropoffPathSearch, dropoff, insertion.dropoff.nextWaypoint.getLink(),
								latestDropoffTime));

		try {
			return new InsertionDetourData(toPickupFuture.get(), fromPickupFuture.get(), toDropoffFuture.get(),
					fromDropoffFuture.get());
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
		double firstAndLastLinkTT = FIRST_LINK_TT + VrpPaths.getLastLinkTT(travelTime, toLink,
				departureTime + FIRST_LINK_TT + path.travelTime);

		return new PathData(path, firstAndLastLinkTT);
	}
}
