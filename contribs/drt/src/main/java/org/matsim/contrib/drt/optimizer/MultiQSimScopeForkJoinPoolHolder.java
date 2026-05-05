/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer;

import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * Creates lazy new instances and manage the shutdown. Might be required for ParallelUnplannedRequestInserter
 * to provide more ForkJoinPool for insertion search.
 *
 * @author Steffen Axer
 */
public class MultiQSimScopeForkJoinPoolHolder implements QsimScopeForkJoinPool {
	private final int numberOfThreads;
	private final List<ForkJoinPool> forkJoinPoolList = new ArrayList<>();


	public MultiQSimScopeForkJoinPoolHolder(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public ForkJoinPool getPool() {
		ForkJoinPool forkJoinPool = new ForkJoinPool(this.numberOfThreads);
		this.forkJoinPoolList.add(forkJoinPool);
		return forkJoinPool;
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		this.forkJoinPoolList.forEach(ForkJoinPool::shutdown);
	}
}
