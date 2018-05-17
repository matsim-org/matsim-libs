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

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.insertion.DetourLinksProvider.DetourLinksSet;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem implements MultiVehicleInsertionProblem {

	@SuppressWarnings("unused")
	private static class DetourLinksStats {
		private final SummaryStatistics toPickupMean = new SummaryStatistics();
		private final SummaryStatistics fromPickupMean = new SummaryStatistics();
		private final SummaryStatistics toDropoffMean = new SummaryStatistics();
		private final SummaryStatistics fromDropoffMean = new SummaryStatistics();

		private void addSet(DetourLinksSet set) {
			toPickupMean.addValue(set.pickupDetourStartLinks.size());
			fromPickupMean.addValue(set.pickupDetourEndLinks.size());
			toDropoffMean.addValue(set.dropoffDetourStartLinks.size());
			fromDropoffMean.addValue(set.dropoffDetourEndLinks.size());
		}

		private void printStats() {
			System.out.println("================");
			System.out.println("toPickupMean=" + toPickupMean);
			System.out.println("================");
			System.out.println("fromPickupMean=" + fromPickupMean);
			System.out.println("================");
			System.out.println("toDropoffMean=" + toDropoffMean);
			System.out.println("================");
			System.out.println("fromDropoffMean=" + fromDropoffMean);
			System.out.println("================");
		}
	}

	private final PrecalculablePathDataProvider pathDataProvider;
	private final DrtConfigGroup drtCfg;
	private final MobsimTimer timer;
	private final InsertionCostCalculator insertionCostCalculator;
	private final ForkJoinPool forkJoinPool;
	// private final DetourLinksStats detourLinksStats = new DetourLinksStats();

	public ParallelMultiVehicleInsertionProblem(PrecalculablePathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer) {
		this.pathDataProvider = pathDataProvider;
		this.drtCfg = drtCfg;
		this.timer = timer;
		insertionCostCalculator = new InsertionCostCalculator(drtCfg, timer);
		forkJoinPool = new ForkJoinPool(drtCfg.getNumberOfThreads());
	}

	@Override
	public Optional<BestInsertion> findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		DetourLinksProvider detourLinksProvider = new DetourLinksProvider(drtCfg, timer);
		forkJoinPool.submit(() -> vEntries.parallelStream()//
				.forEach(e -> detourLinksProvider.addDetourLinks(drtRequest, e)))//
				.join();

		DetourLinksSet detourLinksSet = detourLinksProvider.getDetourLinksSet();
		// detourLinksStats.addSet(detourLinksSet);
		pathDataProvider.precalculatePathData(drtRequest, detourLinksSet);

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
		forkJoinPool.shutdown();
		// detourLinksStats.printStats();
	}
}
