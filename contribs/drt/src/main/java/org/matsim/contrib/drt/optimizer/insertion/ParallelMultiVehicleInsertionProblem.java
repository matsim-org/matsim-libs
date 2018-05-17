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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem {
	private final PrecalculablePathDataProvider pathDataProvider;
	private final DrtConfigGroup drtCfg;
	private final MobsimTimer timer;
	private final InsertionCostCalculator insertionCostCalculator;
	private final ForkJoinPool forkJoinPool;

	public ParallelMultiVehicleInsertionProblem(PrecalculablePathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer) {
		this.pathDataProvider = pathDataProvider;
		this.drtCfg = drtCfg;
		this.timer = timer;
		insertionCostCalculator = new InsertionCostCalculator(drtCfg, timer);
		forkJoinPool = new ForkJoinPool(drtCfg.getNumberOfThreads());

	}

	// Stats on dijkstra searches:
	// SummaryStatistics toPickupMean = new SummaryStatistics();
	// SummaryStatistics fromPickupMean = new SummaryStatistics();
	// SummaryStatistics toDropoffMean = new SummaryStatistics();
	// SummaryStatistics fromDropoffMean = new SummaryStatistics();

	@Override
	public Optional<BestInsertion> findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		DetourLinksProvider detourLinksProvider = new DetourLinksProvider(drtCfg, timer);
		forkJoinPool.submit(() -> vEntries.parallelStream()//
				.forEach(e -> detourLinksProvider.addDetourLinks(drtRequest, e)))//
				.join();

		// DetourLinksSet set = detourLinksProvider.getDetourLinksSet();
		// toPickupMean.addValue(set.pickupDetourStartLinks.size());
		// fromPickupMean.addValue(set.pickupDetourEndLinks.size());
		// toDropoffMean.addValue(set.dropoffDetourStartLinks.size());
		// fromDropoffMean.addValue(set.dropoffDetourEndLinks.size());

		pathDataProvider.precalculatePathData(drtRequest, detourLinksProvider.getDetourLinksSet());

		Map<Id<Vehicle>, List<Insertion>> filteredInsertionsPerVehicle = detourLinksProvider
				.getFilteredInsertionsPerVehicle();
		return forkJoinPool.submit(() -> vEntries.parallelStream()//
				.map(v -> new SingleVehicleInsertionProblem(pathDataProvider, insertionCostCalculator)
						.findBestInsertion(drtRequest, v, filteredInsertionsPerVehicle.get(v.vehicle.getId())))//
				.filter(Optional::isPresent)//
				.map(Optional::get)//
				.min(Comparator.comparing(i -> i.cost)))//
				.join();
	}

	public void shutdown() {
		// System.out.println("================");
		// System.out.println("toPickupMean=" + toPickupMean);
		// System.out.println("================");
		// System.out.println("fromPickupMean=" + fromPickupMean);
		// System.out.println("================");
		// System.out.println("toDropoffMean=" + toDropoffMean);
		// System.out.println("================");
		// System.out.println("fromDropoffMean=" + fromDropoffMean);
		// System.out.println("================");
		forkJoinPool.shutdown();
	}
}
