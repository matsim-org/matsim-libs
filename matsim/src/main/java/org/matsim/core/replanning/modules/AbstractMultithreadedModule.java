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

package org.matsim.core.replanning.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.utils.misc.Counter;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract strategy module for running multiple plan algorithms in parallel.
 * Can be used to easily generate multi-threaded strategy modules, as long as the
 * real functionality of the strategy module is handled by a PlanAlgorithm.
 * Just overwrite getPlanAlgoInstance() to return an instance of your plan
 * algorithm.
 * <p></p>
 * <code>initThreads()</code> creates the threads, but does not yet start them.
 * <p></p>
 * <code>handlePlan(Plan)</code> distributes the plans equally to all threads.
 * <p></p>
 * <code>finishReplanning()</code> finally starts the threads and waits for all threads to be finished.
 * <p></p>
 * While this approach does not lead to optimal performance gains ("slow threads" vs.
 * "fast threads"), it helps building reproducible runs.  Additionally, as the threads are only
 * started after all to-be-handled plans are added, we can use unsynchronized data structures.
 * <p></p>
 * Design comments/questions:<ul>
 * <li> As a consequence of the design, the instances that getPlanAlgoInstance() returns, need to be thread-safe.  kai, dec'12
 * For an example with discussions, see {@link tutorial.programming.multiThreadedPlanStrategy.RunWithMultithreadedModule}
 * </ul>
 *
 * @author mrieser
 */
abstract public class AbstractMultithreadedModule implements PlanStrategyModule {
	private final int numOfThreads;

	private PlanAlgoThread[] algothreads = null;
	private Thread[] threads = null;
	private PlanAlgorithm directAlgo = null;
	private String name = null;

	private int count = 0;

	private final AtomicReference<Throwable> hadException = new AtomicReference<>(null);
	private final ExceptionHandler exceptionHandler = new ExceptionHandler(this.hadException);

	private ReplanningContext replanningContext;

	static final private Logger log = LogManager.getLogger(AbstractMultithreadedModule.class);

	/**
	 * Design comments:<ul>
	 * <li> The way I understand this, the instances that this method returns need to be thread-safe (i.e. independent from each other).  They can,
	 * for example, not rely on the same instance of the router.  kai, dec'12
	 * </ul>
	 */
	abstract public PlanAlgorithm getPlanAlgoInstance();

	public AbstractMultithreadedModule(GlobalConfigGroup globalConfigGroup) {
		this.numOfThreads = globalConfigGroup.getNumberOfThreads();
	}

	public AbstractMultithreadedModule(final int numOfThreads) {
		this.numOfThreads = numOfThreads;
	}
	
	protected void beforePrepareReplanningHook(@SuppressWarnings("unused") ReplanningContext replanningContextTmp) {
		// left empty for inheritance
	}

	protected void afterPrepareReplanningHook(@SuppressWarnings("unused") ReplanningContext replanningContextTmp) {
		// left empty for inheritance
	}

	@Override
	public final void prepareReplanning(ReplanningContext replanningContextTmp) {
		this.beforePrepareReplanningHook(replanningContextTmp);
		this.replanningContext = replanningContextTmp;
		if (this.numOfThreads == 0) {
			// it seems, no threads are desired :(
			this.directAlgo = getPlanAlgoInstance();
		} else {
			initThreads();
		}
		this.afterPrepareReplanningHook(replanningContextTmp);
	}

	protected final ReplanningContext getReplanningContext() {
		return replanningContext;
	}

	@Override
	public final void handlePlan(final Plan plan) {
		if (this.directAlgo == null) {
			this.algothreads[this.count % this.numOfThreads].addPlanToThread(plan);
			this.count++;
		} else {
			this.directAlgo.run(plan);
		}
	}

	protected void beforeFinishReplanningHook() {
		// left empty for inheritance
	}
	protected void afterFinishReplanningHook() {
		// left empty for inheritance
	}
	
	@Override
	public final void finishReplanning() {
		this.beforeFinishReplanningHook();
		
		if (this.directAlgo == null) {
			// only try to start threads if we did not directly work on all the plans
			log.info("[" + this.name + "] starting " + this.threads.length + " threads, handling " + this.count + " plans");

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
				throw new RuntimeException(e);
			}
			log.info("[" + this.name + "] all " + this.threads.length + " threads finished.");
			Throwable throwable = this.hadException.get();
			if (throwable != null) {
				throw new RuntimeException("Some threads crashed, thus not all plans may have been handled.", throwable);
			}
		}
		// reset
		this.algothreads = null;
		this.threads = null;
		this.replanningContext = null;
		this.count = 0;
		
		this.afterFinishReplanningHook();
	}

	private void initThreads() {
		if (this.threads != null) {
			throw new RuntimeException("threads are already initialized");
		}

		this.hadException.set(null);
		this.threads = new Thread[this.numOfThreads];
		this.algothreads = new PlanAlgoThread[this.numOfThreads];

		Counter counter = null;
		// setup threads
		for (int i = 0; i < this.numOfThreads; i++) {
			PlanAlgorithm algo = getPlanAlgoInstance();
			if (i == 0) {
				this.name = algo.getClass().getSimpleName();
				counter = new Counter("[" + this.name + "] handled plan # ");
			}
			PlanAlgoThread algothread = new PlanAlgoThread(algo, counter);
			Thread thread = new Thread(algothread, this.name + "." + i);
			thread.setUncaughtExceptionHandler(this.exceptionHandler);
			this.threads[i] = thread;
			this.algothreads[i] = algothread;
		}
	}

	/* package (for a test) */ final int getNumOfThreads() {
		return numOfThreads;
	}

	private final static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicReference<Throwable> hadException;

		public ExceptionHandler(final AtomicReference<Throwable> hadException) {
			this.hadException = hadException;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.error("Thread " + t.getName() + " died with exception. Will stop after all threads finished.", e);
			this.hadException.set(e);
		}

	}

	private final static class PlanAlgoThread implements Runnable {

		private final PlanAlgorithm planAlgo;
		private final List<Plan> plans = new LinkedList<>();
		private final Counter counter;

		public PlanAlgoThread(final PlanAlgorithm algo, final Counter counter) {
			this.planAlgo = algo;
			this.counter = counter;
		}

		public void addPlanToThread(final Plan plan) {
			this.plans.add(plan);
		}

		@Override
		public void run() {
			for (Plan plan : this.plans) {
				this.planAlgo.run(plan);
				this.counter.incCounter();
			}
		}
	}
}
