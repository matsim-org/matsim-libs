/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningThread.java
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

package playground.christoph.withinday.replanning.parallel;

import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Counter;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.interfaces.AgentCounterI;

import playground.christoph.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;
import playground.christoph.withinday.replanning.replanners.tools.ReplanningTask;

/*
 * Typical Replanner Implementations should be able to use this 
 * Class method without any changes.
 * Override the doReplanning() method to implement the replanning functionality.
 */
public abstract class ReplanningThread extends Thread {

	private final static Logger log = Logger.getLogger(ReplanningThread.class);
	
	protected Counter counter;
	protected double time = 0.0;
	protected AgentCounterI agentCounter;
	protected boolean simulationRunning = true;
	
	/*
	 *  The original WithinDayReplanners are initialized and assigned
	 *  to the Agents. They are cloned to run on parallel replanning
	 *  Threads. Each agents has references to the original Replanners,
	 *  so we have to identify the corresponding clone! 
	 */
	protected Map<Id, WithinDayReplanner<? extends AgentsToReplanIdentifier>> withinDayReplanners = new TreeMap<Id, WithinDayReplanner<? extends AgentsToReplanIdentifier>>();
	
	protected LinkedList<ReplanningTask> replanningTasks = new LinkedList<ReplanningTask>();
	protected WithinDayReplanner<AgentsToReplanIdentifier> withinDayReplanner;
	
	protected CyclicBarrier timeStepStartBarrier;
	protected CyclicBarrier timeStepEndBarrier;
	
	public ReplanningThread(String counterText) {
		counter = new Counter(counterText);
	}
	
	public final void setTime(double time) {
		this.time = time;
	}
	
	public final void setAgentCounter(AgentCounterI agentCounter) {
		this.agentCounter = agentCounter;
	}
	
	public final void setSimulationRunning(boolean simulationRunning) {
		this.simulationRunning = simulationRunning;
	}
	
	public final void setCyclicTimeStepStartBarrier(CyclicBarrier barrier) {
		this.timeStepStartBarrier = barrier;
	}
	
	public final void setCyclicTimeStepEndBarrier(CyclicBarrier barrier) {
		this.timeStepEndBarrier = barrier;
	}

	public final void addReplanningTask(ReplanningTask replanningTask) {			
		replanningTasks.add(replanningTask);
	}
	
	public final void addWithinDayReplanner(WithinDayReplanner<? extends AgentsToReplanIdentifier> withinDayReplanner) {
		this.withinDayReplanners.put(withinDayReplanner.getId(), withinDayReplanner);
	}
	
	/*
	 * Typical Replanner Implementations should be able to use 
	 * this method without any Changes.
	 */
	private void doReplanning() {
		ReplanningTask replanningTask;
		while((replanningTask = replanningTasks.poll()) != null) {
			Id id = replanningTask.getWithinDayReplannerId();
			WithinDayAgent withinDayAgent = replanningTask.getAgentToReplan();
			
			if (id == null) {
				log.error("WithinDayReplanner Id is null!");
				return;
			}
			
			if (withinDayAgent == null) {
				log.error("WithinDayAgent is null!");
				return;
			}
			
			WithinDayReplanner<? extends AgentsToReplanIdentifier> withinDayReplanner = this.withinDayReplanners.get(id);

			if (withinDayReplanner != null) {
				/*
				 * Check whether the current Agent should be replanned based on the 
				 * replanning probability. If not, continue with the next one.
				 */
				if (!withinDayReplanner.replanAgent()) continue;
				
				withinDayReplanner.setTime(time);
				boolean replanningSuccessful = withinDayReplanner.doReplanning(withinDayAgent);
				
				if (!replanningSuccessful) {
					log.error("Replanning was not successful!");
				}
				else counter.incCounter();
			}
			else {
				log.error("WithinDayReplanner is null!");
			}

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

				doReplanning();
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			} catch (BrokenBarrierException e) {
            	Gbl.errorMsg(e);
            }

		}	// while Simulation Running
		
	}	// run()
	
}
