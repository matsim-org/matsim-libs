/* *********************************************************************** *
 * project: org.matsim.*
 * MultiThreadedForceUpdater.java
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.gbl.Gbl;

import playground.gregor.sim2d_v3.simulation.floor.forces.DynamicForceModule;
import playground.gregor.sim2d_v3.simulation.floor.forces.ForceModule;

public class MultiThreadedForceUpdater implements Runnable {
	
	private volatile boolean simulationRunning = true;
	
	private final List<DynamicForceModule> dynamicForceModules;
	private final List<ForceModule> forceModules;
	private final Collection<Agent2D> agents;
	private final CyclicBarrier startBarrier;
	private final CyclicBarrier endBarrier;
	
	private double time;

	public MultiThreadedForceUpdater(List<ForceModule> forceModules, List<DynamicForceModule> dynamicForceModules, 
			CyclicBarrier startBarrier, CyclicBarrier endBarrier) {
		this.forceModules = forceModules;
		this.dynamicForceModules = dynamicForceModules;
		this.startBarrier = startBarrier;
		this.endBarrier = endBarrier;
		
		this.agents = new ArrayList<Agent2D>();
	}
	
	public void afterSim() {
		this.simulationRunning = false;
	}
	
	public void setTime(double time) {
		this.time = time;
	}
	
	public void addAgent(Agent2D agent) {
		this.agents.add(agent);
	}
	
	@Override
	public void run(){
				
		/*
		 * The method is ended when the simulationRunning Flag is set to false.
		 */
		while(true) {
			try {
				/*
				 * The Threads wait at the startBarrier until they are triggered.
				 */
				this.startBarrier.await();

				/*
				 * Check if Simulation is still running. Otherwise print CPU usage and end Thread.
				 */
				if (!this.simulationRunning) {
					Gbl.printCurrentThreadCpuTime();
					return;
				}

				for (Agent2D agent : this.agents) {
					
					for (ForceModule m : this.dynamicForceModules) {
						m.run(agent, this.time);
					}
					for (ForceModule m : this.forceModules) {
						m.run(agent, this.time);
					}					
				}
				this.agents.clear();
				
				/*
				 * The End of the Moving is synchronized with the endBarrier. If 
				 * all Threads reach this Barrier the main Thread can go on.
				 */
				this.endBarrier.await();
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			} catch (BrokenBarrierException e) {
            	Gbl.errorMsg(e);
            }
		}
	}
}