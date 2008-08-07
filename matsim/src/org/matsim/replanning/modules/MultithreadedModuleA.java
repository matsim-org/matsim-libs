/* *********************************************************************** *
 * project: org.matsim.*
 * MultithreadedModuleA.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.replanning.modules;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * An abstract strategy module for running multiple plan algorithms in parallel.
 * Can be used to easily generate multi-threaded strategy modules, as long as the
 * real functionality of the strategy module is handled by a PlanAlgorithm.
 * Just overwrite getPlanAlgoInstance() to return an instance of your plan
 * algorithm.
 *
 * <code>init()</code> creates the threads, but does not yet start them.
 * <code>handlePlan(Plan)</code> distributes the plans equally to all threads.
 * <code>finish()</code> finally starts the threads and waits for all threads to be finished.
 * While this approach does not lead to optimal performance gains ("slow threads" vs.
 * "fast threads"), it helps building reproducible runs.  Additionally, as the threads are only
 * started after all to-be-handled plans are added, we can use unsynchronized data structures.
 *
 * @author mrieser
 */
abstract public class MultithreadedModuleA implements StrategyModuleI {
	private final int numOfThreads;

	private PlanAlgoThread[] algothreads = null;
	private Thread[] threads = null;
	private PlanAlgorithm directAlgo = null;
	private String name = null;

	private int counter = 0;
	private int nextCounter = 1;

	static final private Logger log = Logger.getLogger(MultithreadedModuleA.class);

	abstract public PlanAlgorithm getPlanAlgoInstance();

	public MultithreadedModuleA() {
		this.numOfThreads = Gbl.getConfig().global().getNumberOfThreads();
	}

	public MultithreadedModuleA(final int numOfThreads) {
		this.numOfThreads = numOfThreads;
	}

	public void init() {
		if (this.numOfThreads == 0) {
			// it seems, no threads are desired :(
			this.directAlgo = getPlanAlgoInstance();
		} else {
			initThreads();
		}
	}

	public void handlePlan(final Plan plan) {
		if (this.directAlgo == null) {
			this.algothreads[this.counter % this.numOfThreads].handlePlan(plan);
			this.counter++;
		} else {
			this.directAlgo.run(plan);
		}
	}

	public void finish() {
		if (this.directAlgo == null) {
			// only try to start threads if we did not directly work on all the plans
			log.info("[" + this.name + "] starting threads, handling " + this.counter + " plans");
			this.counter = 0;

			// start threads
			for (Thread thread : this.threads) {
				thread.start();
			}

			// wait until each thread is finished
			try {
				for (Thread thread : this.threads) {
					thread.join();
				}
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			}
			log.info("[" + this.name + "] all threads finished.");
		}
		// reset
		this.algothreads = null;
		this.threads = null;
		this.counter = 0;
		this.nextCounter = 1;
	}

	private void initThreads() {
		if (this.threads != null) {
			Gbl.errorMsg("threads are already initialized");
		}

		this.threads = new Thread[this.numOfThreads];
		this.algothreads = new PlanAlgoThread[this.numOfThreads];

		// setup threads
		for (int i = 0; i < this.numOfThreads; i++) {
			PlanAlgorithm algo = getPlanAlgoInstance();
			if (i == 0) {
				this.name = algo.getClass().getSimpleName();
			}
			PlanAlgoThread algothread = new PlanAlgoThread(i, algo);
			Thread thread = new Thread(algothread, this.name + "." + i);
			this.threads[i] = thread;
			this.algothreads[i] = algothread;
		}
	}

	synchronized /*package*/ void incCounter() {
		this.counter++;
		if (this.counter == this.nextCounter) {
			log.info("[" + this.name + "] handled plan # " + this.counter);
			this.nextCounter *= 2;
		}
	}

	private class PlanAlgoThread implements Runnable {

		public final int threadId;
		private final PlanAlgorithm planAlgo;
		private final List<Plan> plans = new LinkedList<Plan>();

		public PlanAlgoThread(final int i, final PlanAlgorithm algo) {
			this.threadId = i;
			this.planAlgo = algo;
		}

		public void handlePlan(final Plan plan) {
			this.plans.add(plan);
		}

		public void run() {
			for (Plan plan : this.plans) {
				this.planAlgo.run(plan);
				incCounter();
			}
		}
	}
}
