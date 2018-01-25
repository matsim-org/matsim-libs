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

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem {
	private final ParallelPathDataProvider pathDataProvider;
	private final InsertionCostCalculator insertionCostCalculator;
	private final ForkJoinPool forkJoinPool;

	public ParallelMultiVehicleInsertionProblem(Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, DrtConfigGroup drtCfg, MobsimTimer timer) {
		pathDataProvider = new ParallelPathDataProvider(network, travelTime, travelDisutility, drtCfg);
		insertionCostCalculator = new InsertionCostCalculator(drtCfg, timer);
		forkJoinPool = new ForkJoinPool(drtCfg.getNumberOfThreads());
	}

	@Override
	public Optional<BestInsertion> findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		pathDataProvider.calcPathData(drtRequest, vEntries);// 4 multiNodeDijkstras run in parallel
		return forkJoinPool.submit(() -> vEntries.parallelStream()//
				.map(v -> findBestInsertionImpl(drtRequest, v))//
				.min(Comparator.comparing(i -> i.cost)))//
				.join();
	}

	private BestInsertion findBestInsertionImpl(DrtRequest drtRequest, Entry vEntry) {
		return new SingleVehicleInsertionProblem(pathDataProvider, insertionCostCalculator)
				.findBestInsertion(drtRequest, vEntry);
	}

	public void shutdown() {
		pathDataProvider.shutdown();
		forkJoinPool.shutdown();
	}
}
