/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningRunnable.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.withinday.events.ReplanningEvent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentSelector;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import org.matsim.withinday.replanning.replanners.tools.ReplanningTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/*
 * Typical Replanner Implementations should be able to use this 
 * Class method without any changes.
 * Override the doReplanning() method to implement the replanning functionality.
 */
public abstract class ReplanningRunnable implements Runnable {

	private final static Logger log = LogManager.getLogger(ReplanningRunnable.class);
	
	private Counter counter;
	private double time = 0.0;
	private volatile boolean simulationRunning = false;
	
	/*
	 *  The original WithinDayReplanners are initialized and assigned
	 *  to the Agents. They are cloned to run on parallel replanning
	 *  Threads. Each agents has references to the original Replanners,
	 *  so we have to identify the corresponding clone! 
	 */
	protected Map<Id<WithinDayReplanner>, WithinDayReplanner<? extends AgentSelector>> withinDayReplanners = new HashMap<>();
	
	/*
	 * Use one List of ReplanningTasks per WithinDayReplanner. By doing so
	 * and by using a CyclicBarrier, it can be ensured that only instances
	 * of the same WithinDayReplanner are run in parallel. Otherwise two
	 * different Replanners on different Threads could try to replan the
	 * same Agent.
	 */
	protected Map<Id<WithinDayReplanner>, Queue<ReplanningTask>> replanningTasks = new TreeMap<>();
    protected EventsManager eventsManager;
	
	protected CyclicBarrier timeStepStartBarrier;
	protected CyclicBarrier betweenReplannerBarrier;
	protected CyclicBarrier timeStepEndBarrier;
	
	public ReplanningRunnable(String counterText) {
		counter = new Counter(counterText);
	}
	
	public final void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}
	
	public final void setTime(double time) {
		this.time = time;
	}
		
	public final void setCyclicTimeStepStartBarrier(CyclicBarrier barrier) {
		this.timeStepStartBarrier = barrier;
	}
	
	public final void setBetweenReplannerBarrier(CyclicBarrier barrier) {
		this.betweenReplannerBarrier = barrier;
	}
	
	public final void setCyclicTimeStepEndBarrier(CyclicBarrier barrier) {
		this.timeStepEndBarrier = barrier;
	}

	public final void addReplanningTask(ReplanningTask replanningTask) {
		Queue<ReplanningTask> queue = this.replanningTasks.get(replanningTask.getWithinDayReplannerId());
		queue.add(replanningTask);
	}
	
	public final void addWithinDayReplanner(WithinDayReplanner<? extends AgentSelector> withinDayReplanner, Queue<ReplanningTask> queue) {
		this.withinDayReplanners.put(withinDayReplanner.getId(), withinDayReplanner);
		this.replanningTasks.put(withinDayReplanner.getId(), queue);
	}
	
	public final void removeWithinDayReplanner(Id<WithinDayReplanner> replannerId) {
		this.withinDayReplanners.remove(replannerId);
		this.replanningTasks.remove(replannerId);
	}
	
	public final void resetReplanners() {
		this.counter.reset();
		for (WithinDayReplanner<? extends AgentSelector> withinDayReplanner : this.withinDayReplanners.values()) {
			withinDayReplanner.reset();
		}
	}
	
	public final void beforeSim() {
		this.simulationRunning = true;
	}
	
	public final void afterSim() {
		this.simulationRunning = false;
	}
	
	/*
	 * Typical Replanner Implementations should be able to use 
	 * this method without any Changes.
	 */
	private void doReplanning() throws InterruptedException, BrokenBarrierException {

		for (Entry<Id<WithinDayReplanner>, Queue<ReplanningTask>> entry : this.replanningTasks.entrySet()) {
			
			Id<WithinDayReplanner> withinDayReplannerId = entry.getKey();
			Queue<ReplanningTask> queue = entry.getValue();
			
			WithinDayReplanner<? extends AgentSelector> withinDayReplanner = this.withinDayReplanners.get(withinDayReplannerId);
			
			if (withinDayReplannerId == null) {
				log.error("WithinDayReplanner Id is null!");
				continue;
			} else if (withinDayReplanner == null) {
				log.error("WithinDayReplanner is null!");
				continue;
			}

			// set time once per replanner and time step
			withinDayReplanner.setTime(time);
			
			ReplanningTask replanningTask;
			while (true) {
				replanningTask = queue.poll();
				
				// if no more elements are left in the queue, end while loop
				if (replanningTask == null) break;

				MobsimAgent withinDayAgent = replanningTask.getAgentToReplan();
								
				if (withinDayAgent == null) {
					log.error("WithinDayAgent is null!");
					continue;
				}
				
				boolean replanningSuccessful = withinDayReplanner.doReplanning(withinDayAgent);
				
				if (!replanningSuccessful) {
					log.error("Replanning was not successful! Replanner " + withinDayReplanner.getClass().toString() + 
							", time " + Time.writeTime(time) + ", agent " + withinDayAgent.getId());
				}
				else {
					/*
					 * If the EventsManager is not null, we create an entry for the events log file.
					 */
					if (eventsManager != null) {
						ReplanningEvent replanningEvent = new ReplanningEvent(time, withinDayAgent.getId(), 
								withinDayReplanner.getClass().getSimpleName());
						eventsManager.processEvent(replanningEvent);
					}
					
					counter.incCounter();
				}
			}
			
			/*
			 * Wait here until all Threads have ended the replanning for the
			 * current WithinDayReplanner.
			 */
			this.betweenReplannerBarrier.await();
		}
	}
	
	@Override
	public final void run() {
		while (simulationRunning) {
			try {
				/*
				 * The End of the Replanning is synchronized with 
				 * the TimeStepEndBarrier. If all Threads reach this Barrier
				 * the main run() Thread can go on.
				 * 
				 * The Threads wait now at the TimeStepStartBarrier until
				 * they are triggered again in the next TimeStep by the main run()
				 * method.
				 */
				timeStepEndBarrier.await();
					
				timeStepStartBarrier.await();

				if (!simulationRunning) {
					Gbl.printCurrentThreadCpuTime();
					return;
				}
				
				doReplanning();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}

        }	// while Simulation Running

	}	// run()
	
}
