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
import org.apache.log4j.Logger;
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
		private static final Logger log = Logger.getLogger(DetourLinksStats.class);

		private final SummaryStatistics toPickupStats = new SummaryStatistics();
		private final SummaryStatistics fromPickupStats = new SummaryStatistics();
		private final SummaryStatistics toDropoffStats = new SummaryStatistics();
		private final SummaryStatistics fromDropoffStats = new SummaryStatistics();
		private final SummaryStatistics vEntriesStats = new SummaryStatistics();

		private void addSet(DetourLinksSet set, int vEntriesCount) {
			toPickupStats.addValue(set.pickupDetourStartLinks.size());
			fromPickupStats.addValue(set.pickupDetourEndLinks.size());
			toDropoffStats.addValue(set.dropoffDetourStartLinks.size());
			fromDropoffStats.addValue(set.dropoffDetourEndLinks.size());
			vEntriesStats.addValue(vEntriesCount);
		}

		private void printStats() {
			log.debug("toPickupStats:\n" + toPickupStats);
			log.debug("fromPickupStats:\n" + fromPickupStats);
			log.debug("toDropoffStats:\n" + toDropoffStats);
			log.debug("fromDropoffStats:\n" + fromDropoffStats);
			log.debug("vEntriesStats:\n" + vEntriesStats);
		}
	}

	private final PrecalculablePathDataProvider pathDataProvider;
	private final DrtConfigGroup drtCfg;
	private final MobsimTimer timer;
	private final InsertionCostCalculator insertionCostCalculator;
	private final ForkJoinPool forkJoinPool;
	private final DetourLinksStats detourLinksStats = new DetourLinksStats();

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
		detourLinksStats.addSet(detourLinksSet, vEntries.size());
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
//		detourLinksStats.printStats();
	}
}
