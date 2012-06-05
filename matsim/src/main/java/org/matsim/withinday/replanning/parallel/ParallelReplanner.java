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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.withinday.replanning.identifiers.interfaces.Identifier;
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
public abstract class ParallelReplanner<T extends WithinDayReplannerFactory<? extends Identifier>> { 

	private final static Logger log = Logger.getLogger(ParallelReplanner.class);

	protected int numOfThreads = 1;	// use by default only one thread
	protected Set<T> replannerFactories = new LinkedHashSet<T>();
	protected ReplanningRunnable[] replanningRunnables;
	protected String replannerName;
	protected int roundRobin = 0;
	private int lastRoundRobin = 0;
	protected EventsManager eventsManager;
	protected CyclicBarrier timeStepStartBarrier;
	protected CyclicBarrier betweenReplannerBarrier;
	protected CyclicBarrier timeStepEndBarrier;
	
	public ParallelReplanner(int numOfThreads) {
		this.setNumberOfThreads(numOfThreads);
	}

	public final void setEventsManager(EventsManager eventsManager) {
		for (ReplanningRunnable replanningThread : replanningRunnables) replanningThread.setEventsManager(eventsManager);
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

			replanningRunnables[i] = replanningRunnable;
		}
	}

	public final void onPrepareSim() {
		
		Thread[] replanningThreads = new Thread[numOfThreads];
		
		// initialize threads
		for (int i = 0; i < numOfThreads; i++) {
			Thread replanningThread = new Thread(replanningRunnables[i]);
			replanningThread.setName(replannerName + i);
			replanningThreads[i] = replanningThread;
		}
		
		// finalize thread setup and start them
		for (int i = 0; i < numOfThreads; i++) {
			Thread replanningThread = replanningThreads[i];
			replanningThread.setDaemon(true);
			replanningThread.start();
		}

		/*
		 * After initialization the threads are waiting at the
		 * TimeStepEndBarrier. We trigger this Barrier once so
		 * they wait at the TimeStepStartBarrier what has to be
		 * their state if the run() method is called.
		 */
		try {
			this.timeStepEndBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
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

		try {
			// set current time
			for (ReplanningRunnable replannerThread : replanningRunnables) {
				replannerThread.setTime(time);
			}

			this.timeStepStartBarrier.await();

			this.timeStepEndBarrier.await();

		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
	      	Gbl.errorMsg(e);
		}
	}

	public final void afterSim() {

		// reset counters
		roundRobin = 0;
		lastRoundRobin = 0;
		
		/*
		 * Calling the afterSim Method of the QSimEngineThreads
		 * will set their simulationRunning flag to false.
		 */
		for (ReplanningRunnable runnable : this.replanningRunnables) {
			runnable.afterSim();
		}

		/*
		 * Triggering the startBarrier of the QSimEngineThreads.
		 * They will check whether the Simulation is still running.
		 * It is not, so the Threads will stop running.
		 */
		try {
			this.timeStepStartBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
	}
	
	public final void addWithinDayReplannerFactory(T factory) {
		this.replannerFactories.add(factory);

		for (ReplanningRunnable replanningThread : this.replanningRunnables) {
			WithinDayReplanner<? extends Identifier> newInstance = factory.createReplanner();
			replanningThread.addWithinDayReplanner(newInstance);
		}
	}

	public final void removeWithinDayReplannerFactory(T factory) {
		this.replannerFactories.remove(factory);
		
		for (ReplanningRunnable replanningThread : this.replanningRunnables) {
			replanningThread.removeWithinDayReplanner(factory.getId());
		}
	}
	
	public final void resetReplanners() {
		for (ReplanningRunnable replanningThread : this.replanningRunnables) {
			replanningThread.resetReplanners();
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

		log.info("Using " + numOfThreads + " parallel threads to replan routes.");

		/*
		 *  Throw error message if the number of threads is bigger than the number of available CPUs.
		 *  This should not speed up calculation anymore.
		 */
		if (numOfThreads > Runtime.getRuntime().availableProcessors()) {
			log.error("The number of parallel running replanning threads is bigger than the number of available CPUs/Cores!");
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
}
