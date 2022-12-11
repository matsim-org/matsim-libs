/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.mobsim.qsim;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;

class ParallelActivityEngine implements ActivityEngine {

	private static final Logger log = LogManager.getLogger( ParallelActivityEngine.class ) ;
	
	private final QSim sim;
	private final List<ActivityEngineRunner> runners = new ArrayList<>();
	private final int numOfThreads;
	private final ExecutorService pool;
	private final byte[] assignedEngine;
	
	@Inject
	ParallelActivityEngine(final QSim sim, final EventsManager eventsManager) {
		this.sim = sim;
		this.numOfThreads = this.sim.getScenario().getConfig().qsim().getNumberOfThreads();
		this.pool = Executors.newFixedThreadPool(this.numOfThreads);
		
		AtomicInteger counter = new AtomicInteger(0);
		for (int i = 0; i < this.numOfThreads; i++) this.runners.add(new ActivityEngineRunner(eventsManager, counter));
		
		this.assignedEngine = new byte[Id.getNumberOfIds(Link.class)];
	}

	private InternalInterface internalInterface;

	// See handleActivity for the reason for this.
	private boolean beforeFirstSimStep = true;

	@Override
	public void onPrepareSim() {
		try {
			/*
			 * Use the same logic to assign the engines as the QNetsimEngine.
			 * TODO: solve this in a cleaner way.
			 */
			int roundRobin = 0;
			for (NetsimNode node : this.sim.getNetsimNetwork().getNetsimNodes().values()) {
				int i = roundRobin % this.numOfThreads;
				for (Id<Link> linkId : node.getNode().getOutLinks().keySet()) this.assignedEngine[linkId.index()] = (byte) i;
				roundRobin++;
			}
		} catch (NullPointerException e) { // Should only happen in one of the unit tests.
			log.warn("Caught NullPointerException while preparing ParallelActivityEngine: " + e.getMessage(), e);
		}
	}

	@Override
	public void doSimStep(double time) {
		this.beforeFirstSimStep = false;
		
		List<Callable<Boolean>> list = this.runners.stream().map(runner -> new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				runner.doSimStep(time);
				return true;
			}
		}).collect(Collectors.toList());
		
		try {
			for (Future<Boolean> future : pool.invokeAll(list)) {
				future.get();
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			log.error(e.getMessage(), e);
			if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
			else throw new RuntimeException(e);
		}
	}

	@Override
	public void afterSim() {
		List<Callable<Boolean>> list = this.runners.stream().map(runner -> new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				runner.afterSim();
				return true;
			}
		}).collect(Collectors.toList());
		
		try {
			for (Future<Boolean> future : pool.invokeAll(list)) {
				future.get();
			}
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			log.error(e.getMessage(), e);
			if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
			else throw new RuntimeException(e);
		}
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		for (ActivityEngineRunner runner : this.runners) runner.setInternalInterface(internalInterface);
		
		this.internalInterface = internalInterface;
	}

	
	/**
	 * 
	 * This method is called by QSim to pass in agents which then "live" in the activity layer until they are handed out again
	 * through the internalInterface.
	 * 
	 * It is called not before onPrepareSim() and not after afterSim(), but it may be called before, after, or from doSimStep(),
	 * and even from itself (i.e. it must be reentrant), since internalInterface.arrangeNextAgentState() may trigger
	 * the next Activity.
	 * 
	 */
	@Override
	public boolean handleActivity(MobsimAgent agent) {
		if (agent.getActivityEndTime() == Double.POSITIVE_INFINITY) {
			// This is the last planned activity.
			// So the agent goes to sleep.
			this.internalInterface.getMobsim().getAgentCounter().decLiving();
		} else if (agent.getActivityEndTime() <= this.internalInterface.getMobsim().getSimTimer().getTimeOfDay() && !this.beforeFirstSimStep) {
			// This activity is already over (planned for 0 duration)
			// So we proceed immediately.
			agent.endActivityAndComputeNextState(this.internalInterface.getMobsim().getSimTimer().getTimeOfDay());
			this.internalInterface.arrangeNextAgentState(agent) ;
		} else {
			// This is where we need to pass the agent to the runner.
			// So far, we do not connect ActivityEngine and QNetsimEngine which is what we need to do to make it "right".
			// However, for first tests this should be sufficient to see whether the number of time where we need to wait for a look should be still reduced.
			this.runners.get(this.assignedEngine[agent.getCurrentLinkId().index()]).handleActivity(agent);
		}
		// Why beforeFirstSimStep matters:
		// - If this class has never had a doSimStep() when this method is called, this means that this Agent is having its
		// "overnight", i.e. first Activity.
		// - This means that this ActivityEngine should not just pass this MobsimAgent along if its activityEndTime has already ended,
		// but queue it in with other such Agents and let them all leave on doSimStep(), because we expect those Agents to leave
		// in the order specified by the activityQueue (no matter if it is a good order or not, see comment there). The order in which new Agents enter
		// the simulation and are passed into this method is a different one, so this matters.
		// - This is safe (Agents will not miss a second), simply because doSimStep for this time step has not yet happened.
		// - It also means that e.g. OTFVis will probably display all Agents while they are in their first Activity before you press play.
		// - On the other hand, agents whose first activity is also their last activity go right to sleep "inside" this engine.
		return true;
	}

	/**
	 * For within-day replanning. Tells this engine that the activityEndTime the agent reports may have changed since
	 * the agent was added to this engine through handleActivity.
	 * May be merged with handleActivity, since this engine can know by itself if it was called the first time
	 * or not.
	 *
	 * @param agent The agent.
	 */
	@Override
	public void rescheduleActivityEnd(final MobsimAgent agent) {
		if ( agent.getState()!=State.ACTIVITY ) {
			return ;
		}
		
		this.runners.get(this.assignedEngine[agent.getCurrentLinkId().index()]).rescheduleActivityEnd(agent);
	}
}
