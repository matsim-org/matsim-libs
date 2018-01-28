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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
	public static ParallelMultiVehicleInsertionProblem create(Network network, TravelTime travelTime,
			TravelDisutility travelDisutility, DrtConfigGroup drtCfg, MobsimTimer mobsimTimer) {
		ParallelPathDataProvider pathDataProvider = new ParallelPathDataProvider(network, travelTime, travelDisutility,
				drtCfg);
		return new ParallelMultiVehicleInsertionProblem(pathDataProvider, drtCfg, mobsimTimer);
	}

	private static class TaskGroup {
		private final List<Entry> vEntries = new ArrayList<>();
		private final SequentialMultiVehicleInsertionProblem multiInsertionProblem;

		private TaskGroup(PathDataProvider pathDataProvider, DrtConfigGroup drtCfg, MobsimTimer timer) {
			this.multiInsertionProblem = new SequentialMultiVehicleInsertionProblem(pathDataProvider, drtCfg, timer);
		}

		private BestInsertion findBestInsertion(DrtRequest drtRequest) {
			BestInsertion bestInsertion = multiInsertionProblem.findBestInsertion(drtRequest, vEntries);
			vEntries.clear();
			return bestInsertion;
		}
	}

	private final ParallelPathDataProvider pathDataProvider;

	private final int threads;
	private final TaskGroup[] taskGroups;
	private final ExecutorService executorService;

	private ParallelMultiVehicleInsertionProblem(ParallelPathDataProvider pathDataProvider, DrtConfigGroup drtCfg,
			MobsimTimer timer) {
		this.pathDataProvider = pathDataProvider;

		threads = drtCfg.getNumberOfThreads();
		this.taskGroups = new TaskGroup[threads];
		for (int i = 0; i < threads; i++) {
			taskGroups[i] = new TaskGroup(pathDataProvider, drtCfg, timer);
		}
		executorService = Executors.newFixedThreadPool(threads);
	}

	@Override
	public BestInsertion findBestInsertion(DrtRequest drtRequest, Collection<Entry> vEntries) {
		pathDataProvider.calcPathData(drtRequest, vEntries);
		divideTasksIntoGroups(vEntries);
		return findBestInsertion(submitTasks(drtRequest));
	}

	private void divideTasksIntoGroups(Collection<Entry> filteredVehicles) {
		Iterator<Entry> vEntryIter = filteredVehicles.iterator();
		int div = filteredVehicles.size() / threads;
		int mod = filteredVehicles.size() % threads;

		for (int i = 0; i < threads; i++) {
			int count = div + (i < mod ? 1 : 0);
			for (int j = 0; j < count; j++) {
				taskGroups[i].vEntries.add(vEntryIter.next());
			}
		}

		if (vEntryIter.hasNext()) {
			throw new RuntimeException();
		}
	}

	private List<Future<BestInsertion>> submitTasks(final DrtRequest drtRequest) {
		List<Future<BestInsertion>> bestInsertionFutures = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			final TaskGroup taskGroup = taskGroups[i];
			bestInsertionFutures.add(executorService.submit(new Callable<BestInsertion>() {
				public BestInsertion call() {
					return taskGroup.findBestInsertion(drtRequest);
				}
			}));
		}

		return bestInsertionFutures;
	}

	private BestInsertion findBestInsertion(List<Future<BestInsertion>> bestInsertionFutures) {
		double minCost = Double.MAX_VALUE;
		BestInsertion fleetBestInsertion = null;
		for (Future<BestInsertion> bestInsertionFuture : bestInsertionFutures) {
			try {
				BestInsertion bestInsertion = bestInsertionFuture.get();
				if (bestInsertion != null && bestInsertion.cost < minCost) {
					fleetBestInsertion = bestInsertion;
					minCost = bestInsertion.cost;
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
		return fleetBestInsertion;
	}

	public void shutdown() {
		pathDataProvider.shutdown();
		executorService.shutdown();
	}
}
