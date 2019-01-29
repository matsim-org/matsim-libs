/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.parallel;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentSelector;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplannerFactory;
import org.matsim.withinday.replanning.replanners.tools.ReplanningTask;

/*
 * Abstract class that contains the basic elements that are needed
 * to do parallel replanning within the QSim.
 *
 * Features like the creation of parallel running threads and the
 * split up of the replanning actions have to be implemented in
 * the subclasses.
 */
public abstract class ParallelReplanner<T extends WithinDayReplannerFactory<? extends AgentSelector>> { 

	private final static Logger log = Logger.getLogger(ParallelReplanner.class);

	/*
	 * All replanners from the same type can either share one queue that contains all 
	 * ReplanningTasks or use a separate queue per replanner object. A shared queue
	 * should result in a better load balancing but also might become a bottleneck when
	 * many threads are accessing it at the same time. When using a shared queue, a 
	 * LinkedBlockingQueue is used. Otherwise, each replanner uses a LinkedList.
	 * Both approaches should produce the same simulation results.
	 */
	private final boolean shareReplannerQueue = true;
	
	protected final EventsManager eventsManager;
	protected int numOfThreads;
	
	protected Set<T> replannerFactories = new LinkedHashSet<T>();
	protected ReplanningRunnable[] replanningRunnables;
	protected String replannerName;
	protected int roundRobin = 0;
	private int lastRoundRobin = 0;
	protected AtomicBoolean hadException;
	protected ExceptionHandler uncaughtExceptionHandler;
	protected CyclicBarrier timeStepStartBarrier;
	protected CyclicBarrier betweenReplannerBarrier;
	protected CyclicBarrier timeStepEndBarrier;
	
	protected boolean simIsRunning = false;
	
	public ParallelReplanner(int numOfThreads, EventsManager eventsManager) {
		this.setNumberOfThreads(numOfThreads);
		this.eventsManager = eventsManager;
	}
	
	public final void init(String replannerName) {
		
		this.replannerName = replannerName;
		
		replanningRunnables = new InternalReplanningRunnable[numOfThreads];

		this.timeStepStartBarrier = new CyclicBarrier(numOfThreads + 1);
		this.betweenReplannerBarrier = new CyclicBarrier(numOfThreads);
		this.timeStepEndBarrier = new CyclicBarrier(numOfThreads + 1);

		// Do initial Setup of the Runnables
		for (int i = 0; i < numOfThreads; i++) {
			ReplanningRunnable replanningRunnable = new InternalReplanningRunnable(replannerName + " Thread" + i + " replanned plans: ");
			replanningRunnable.setCyclicTimeStepStartBarrier(this.timeStepStartBarrier);
			replanningRunnable.setBetweenReplannerBarrier(betweenReplannerBarrier);
			replanningRunnable.setCyclicTimeStepEndBarrier(this.timeStepEndBarrier);
			replanningRunnable.setEventsManager(eventsManager);
			
			replanningRunnables[i] = replanningRunnable;
		}
	}

	public final void onPrepareSim() {
		
		/*
		 * Moved this here from addWithinDayReplannerFactory(...).
		 * By doing so, the Replanners are created after the mobsim has been initialized.
		 * Moreover, the Replanners are now re-created from scratch for each iteration.
		 * cdobler, jul'13
		 */
		for (T factory : this.replannerFactories) {
			if (shareReplannerQueue) {
				Queue<ReplanningTask> queue = new LinkedBlockingQueue<ReplanningTask>();
				for (ReplanningRunnable replanningRunnable : this.replanningRunnables) {
					WithinDayReplanner<? extends AgentSelector> newInstance = factory.createReplanner();
					replanningRunnable.addWithinDayReplanner(newInstance, queue);
				}
			} else {
				for (ReplanningRunnable replanningRunnable : this.replanningRunnables) {
					WithinDayReplanner<? extends AgentSelector> newInstance = factory.createReplanner();
					replanningRunnable.addWithinDayReplanner(newInstance, new LinkedList<ReplanningTask>());
				}
			}			
		}
		
		this.hadException = new AtomicBoolean(false);
		this.uncaughtExceptionHandler = new ExceptionHandler(this.hadException, this.timeStepStartBarrier, 
				this.betweenReplannerBarrier, this.timeStepEndBarrier);
		
		Thread[] replanningThreads = new Thread[numOfThreads];
		
		// initialize threads
		for (int i = 0; i < numOfThreads; i++) {
			Thread replanningThread = new Thread(replanningRunnables[i]);
			Thread.setDefaultUncaughtExceptionHandler(this.uncaughtExceptionHandler);
			replanningThread.setName(replannerName + i);
			replanningThreads[i] = replanningThread;
		}
		
		// finalize thread setup and start them
		for (int i = 0; i < numOfThreads; i++) {
			replanningRunnables[i].beforeSim();
			Thread replanningThread = replanningThreads[i];
			replanningThread.setDaemon(true);
			replanningThread.start();
		}

		this.simIsRunning = true;
		
		/*
		 * After initialization the threads are waiting at the
		 * TimeStepEndBarrier. We trigger this Barrier once so
		 * they wait at the TimeStepStartBarrier what has to be
		 * their state if the run() method is called.
		 */
		try {
			this.timeStepEndBarrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * Typical Implementations should be able to use this Method
	 * "as it is"...
	 */
	public final void run(double time) {
		// no Agents to Replan
		if (lastRoundRobin == roundRobin) return;
		else lastRoundRobin = roundRobin;

		/*
		 * If an exception occurred, at least one of the events replanning threads
		 * has crashed. Therefore the remaining threads would get stuck at the
		 * CyclicBarrier.
		 */
		if (hadException.get()) {
			return;
		}
		
		try {
			// set current time
			for (ReplanningRunnable replanningRunnable : replanningRunnables) {
				replanningRunnable.setTime(time);
			}

			this.timeStepStartBarrier.await();

			this.timeStepEndBarrier.await();

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
	      	throw new RuntimeException(e);
		}
	}

	public final void afterSim() {

		this.simIsRunning = false;
		
		if (this.hadException.get()) {
			throw new RuntimeException("Exception while replanning. " +
					"Cannot guarantee that all replanning operations have been fully processed.");
		}
		
		// reset counters
		roundRobin = 0;
		lastRoundRobin = 0;
		
		/*
		 * Calling the afterSim Method of the QSimEngineThreads
		 * will set their simulationRunning flag to false.
		 */
		for (ReplanningRunnable runnable : this.replanningRunnables) {
			runnable.afterSim();
			
			/*
			 * Remove replanners from the runnables - now they are re-created from scratch
			 * for each iteration.
			 * cdobler, jul'13
			 */
			for (T factory : this.replannerFactories) {
				runnable.removeWithinDayReplanner(factory.getId());
			}
		}

		/*
		 * Triggering the startBarrier of the QSimEngineThreads.
		 * They will check whether the Simulation is still running.
		 * It is not, so the Threads will stop running.
		 */
		try {
			this.timeStepStartBarrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}
	
	public final void addWithinDayReplannerFactory(T factory) {
		this.replannerFactories.add(factory);
		
		/*
		 * This is necessary for timed within-day replanners. They are added while the
		 * simulation is already running. Theirfore, now Queue<ReplanningTask> is created
		 * in the onPrepare() method.
		 * cdobler, dec'13
		 */
		if (simIsRunning) {
			if (shareReplannerQueue) {
				Queue<ReplanningTask> queue = new LinkedBlockingQueue<ReplanningTask>();
				for (ReplanningRunnable replanningRunnable : this.replanningRunnables) {
					WithinDayReplanner<? extends AgentSelector> newInstance = factory.createReplanner();
					replanningRunnable.addWithinDayReplanner(newInstance, queue);
				}
			} else {
				for (ReplanningRunnable replanningRunnable : this.replanningRunnables) {
					WithinDayReplanner<? extends AgentSelector> newInstance = factory.createReplanner();
					replanningRunnable.addWithinDayReplanner(newInstance, new LinkedList<ReplanningTask>());
				}
			}						
		}
	}

	public final void removeWithinDayReplannerFactory(T factory) {
		this.replannerFactories.remove(factory);
		
		for (ReplanningRunnable replanningRunnable : this.replanningRunnables) {
			replanningRunnable.removeWithinDayReplanner(factory.getId());
		}
	}
	
	public final void resetReplanners() {
		for (ReplanningRunnable replanningRunnable : this.replanningRunnables) {
			replanningRunnable.resetReplanners();
		}
	}
	
	public final Set<T> getWithinDayReplannerFactories() {
		return Collections.unmodifiableSet(this.replannerFactories);
	}

	public final void addReplanningTask(ReplanningTask replanningTask) {	
		this.replanningRunnables[this.roundRobin % this.numOfThreads].addReplanningTask(replanningTask);
		this.roundRobin++;
	}

	private final void setNumberOfThreads(int numberOfThreads) {
		numOfThreads = Math.max(numberOfThreads, 1); // it should be at least 1 here; we allow 0 in other places for "no threads"

		log.info("Using " + numOfThreads + " threads for parallel within-day replanning.");

		/*
		 *  Throw error message if the number of threads is bigger than the number of available CPUs.
		 *  This should not speed up calculation anymore.
		 */
		if (numOfThreads > Runtime.getRuntime().availableProcessors()) {
			log.warn("The number of parallel running replanning threads is bigger than the number of available CPUs/Cores!");
		}
	}
		
	/*
	 * The thread class that really handles the replanning.
	 */
	/*package*/ static final class InternalReplanningRunnable extends ReplanningRunnable {		
		
		public InternalReplanningRunnable(String counterText) {
			super(counterText);
		}
				
	}	// InternalReplanningThread
	
	/**
	 * @author mrieser
	 */
	private static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicBoolean hadException;
		private final CyclicBarrier timeStepStartBarrier;
		private final CyclicBarrier betweenReplannerBarrier;
		private final CyclicBarrier timeStepEndBarrier;

		public ExceptionHandler(final AtomicBoolean hadException, CyclicBarrier timeStepStartBarrier,
				CyclicBarrier betweenReplannerBarrier, CyclicBarrier timeStepEndBarrier) {
			this.hadException = hadException;
			this.timeStepStartBarrier = timeStepStartBarrier;
			this.betweenReplannerBarrier = betweenReplannerBarrier;
			this.timeStepEndBarrier = timeStepEndBarrier;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			this.hadException.set(true);
			log.error("Thread " + t.getName() + " died with exception while replanning.", e);

			/*
			 * By reseting the barriers, they will throw a BrokenBarrierException
			 * which again will stop the events processing threads.
			 */
			this.timeStepStartBarrier.reset();
			this.betweenReplannerBarrier.reset();
			this.timeStepEndBarrier.reset();
		}

	}
}
