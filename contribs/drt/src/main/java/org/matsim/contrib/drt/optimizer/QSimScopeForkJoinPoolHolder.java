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

import java.util.concurrent.ForkJoinPool;

import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

/**
 * Keeps a reference to a pool and shuts it down on MobsimBeforeCleanupListener event
 *
 * @author Michal Maciejewski (michalm)
 */
public class QSimScopeForkJoinPoolHolder implements MobsimBeforeCleanupListener {
	private final ForkJoinPool forkJoinPool;

	public QSimScopeForkJoinPoolHolder(int numberOfThreads) {
		forkJoinPool = new ForkJoinPool(numberOfThreads);
	}

	public ForkJoinPool getPool() {
		return forkJoinPool;
	}

	@Override
	public void notifyMobsimBeforeCleanup(@SuppressWarnings("rawtypes") MobsimBeforeCleanupEvent e) {
		forkJoinPool.shutdown();
	}
}
