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

package playground.michalm.drt.optimizer.insertion;

import java.util.*;
import java.util.concurrent.*;

import playground.michalm.drt.data.NDrtRequest;
import playground.michalm.drt.optimizer.VehicleData;
import playground.michalm.drt.optimizer.VehicleData.Entry;
import playground.michalm.drt.optimizer.insertion.SingleVehicleInsertionProblem.BestInsertion;

/**
 * @author michalm
 */
public class ParallelMultiVehicleInsertionProblem {
	private static class TaskGroup {
		private final List<Entry> vEntries = new ArrayList<>();
		private final MultiVehicleInsertionProblem multiInsertionProblem;

		private TaskGroup(SingleVehicleInsertionProblem singleInsertionProblem) {
			this.multiInsertionProblem = new MultiVehicleInsertionProblem(singleInsertionProblem);
		}

		private BestInsertion findBestInsertion(NDrtRequest drtRequest) {
			BestInsertion bestInsertion = multiInsertionProblem.findBestInsertion(drtRequest, vEntries);
			vEntries.clear();
			return bestInsertion;
		}
	}

	private final int threads;
	private final TaskGroup[] taskGroups;
	private final ExecutorService executorService;

	public ParallelMultiVehicleInsertionProblem(SingleVehicleInsertionProblem[] singleInsertionProblems) {
		threads = singleInsertionProblems.length;
		this.taskGroups = new TaskGroup[threads];
		for (int i = 0; i < threads; i++) {
			taskGroups[i] = new TaskGroup(singleInsertionProblems[i]);
		}
		executorService = Executors.newFixedThreadPool(threads);
	}

	public BestInsertion findBestInsertion(final NDrtRequest drtRequest, VehicleData vData) {
		int idx = 0;
		for (Entry vEntry : vData.getEntries()) {
			taskGroups[idx++ % threads].vEntries.add(vEntry);
		}

		List<Future<BestInsertion>> bestInsertionFutures = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			final TaskGroup taskGroup = taskGroups[i];
			bestInsertionFutures.add(executorService.submit(() -> taskGroup.findBestInsertion(drtRequest)));
		}

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
		executorService.shutdown();
	}
}
