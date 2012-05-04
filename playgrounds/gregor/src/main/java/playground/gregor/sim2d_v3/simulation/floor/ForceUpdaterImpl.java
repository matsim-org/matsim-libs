/* *********************************************************************** *
 * project: org.matsim.*
 * ForceUpdater.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.simulation.floor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.gbl.Gbl;

import playground.gregor.sim2d_v3.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.ForceModule;

public class ForceUpdaterImpl implements ForceUpdater {
	
	private final Set<Agent2D> agents;
	private final List<ForceModule> forceModules;
	private final List<DynamicForceModule> dynamicForceModules;
	private final int numOfThreads;
	
	private CyclicBarrier startBarrier;
	private CyclicBarrier endBarrier;
	private Thread[] threads;
	private MultiThreadedForceUpdater[] updaters;
	
	public ForceUpdaterImpl(Set<Agent2D> agents, int numOfThreads) {
		this.agents = agents;
		this.numOfThreads = numOfThreads;
		
		this.forceModules = new ArrayList<ForceModule>();
		this.dynamicForceModules = new ArrayList<DynamicForceModule>();
		
		if (this.numOfThreads > 1) this.initUpdateForcesThreads();
	}
	
	@Override
	public boolean addForceModule(ForceModule forceModule) {
		return this.forceModules.add(forceModule);
	}

	@Override
	public boolean addDynamiccForceModule(DynamicForceModule dynamicForceModule) {
		return this.dynamicForceModules.add(dynamicForceModule);
	}

	@Override
	public void init() {
		for (ForceModule m : this.forceModules) {
			m.init();
		}
		
		for (ForceModule m : this.dynamicForceModules) {
			m.init();
		}
	}
	
	public void afterSim() {
		
		// If the forces are updated single threaded, nothing to do here.
		if (numOfThreads <= 1) return;
		
		/*
		 * Calling the afterSim Method of the MultiThreadedForceUpdaters
		 * will set their simulationRunning flag to false.
		 */
		for (MultiThreadedForceUpdater updater : this.updaters) {
			updater.afterSim();
		}

		/*
		 * Triggering the startBarrier of the MultiThreadedForceUpdaters.
		 * They will check whether the Simulation is still running.
		 * It is not, so the Threads will stop running.
		 */
		try {
			this.startBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
	}
	
	@Override
	public void updateForces(double time) {
		updateDynamicForces();

		if (this.numOfThreads > 1) {
			updateForcesMultiThreaded(time);
		} else {
			for (Agent2D agent : this.agents) {
				updateForces(agent, time);
			}
		}
	}
	
	@Override
	public void updateForces(Agent2D agent, double time) {
		for (ForceModule m : this.dynamicForceModules) {
			m.run(agent, time);
		}

		for (ForceModule m : this.forceModules) {
			m.run(agent, time);
		}
	}

	@Override
	public void updateDynamicForces(double time) {
		
		for (DynamicForceModule m : this.dynamicForceModules) {
			m.update(time);
		}
	}
	
	@Override
	public void updateDynamicForces() {
		
		for (DynamicForceModule m : this.dynamicForceModules) {
			m.forceUpdate();
		}
	}
	
	private void initUpdateForcesThreads() {
		
		this.threads = new Thread[numOfThreads];
		this.updaters = new MultiThreadedForceUpdater[numOfThreads] ;

		this.startBarrier = new CyclicBarrier(numOfThreads + 1);
		this.endBarrier = new CyclicBarrier(numOfThreads + 1);
		
		// setup threads
		for (int i = 0; i < numOfThreads; i++) {
			MultiThreadedForceUpdater updater = new MultiThreadedForceUpdater(this.forceModules, this.dynamicForceModules,
					this.startBarrier, this.endBarrier);

			Thread thread = new Thread(updater);
			thread.setName(updater.getClass().toString() + i);

			thread.setDaemon(true);	// make the Thread Daemons so they will terminate automatically
			this.threads[i] = thread;
			this.updaters[i] = updater;

			thread.start();
		}
	}

	private void updateForcesMultiThreaded(double time) {

		try {
			// set current Time
			for (MultiThreadedForceUpdater updater : this.updaters) {
				updater.setTime(time);
			}
			
			// assign agents to the threads
			int roundRobin = 0;
			for (Agent2D agent : this.agents) {
				this.updaters[roundRobin % this.numOfThreads].addAgent(agent);
				roundRobin++;
			}

			this.startBarrier.await();

			this.endBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
	}

}