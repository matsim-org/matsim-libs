/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DEngine.java
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
package playground.gregor.sim2d_v2.simulation;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.ptproject.qsim.InternalInterface;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.Netsim;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.controller.PedestrianSignal;
import playground.gregor.sim2d_v2.simulation.floor.Agent2D;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;

/**
 * @author laemmel
 * 
 */
public class Sim2DEngine implements MobsimEngine {

	private final Scenario scenario;

	private final Sim2DConfigGroup sim2ConfigGroup;
	private final double sim2DStepSize;
	private final QSim sim;

	private PhysicalFloor floor;
	
	private final Queue<Agent2D> activityEndsList = new PriorityQueue<Agent2D>(500,new Agent2DDepartureTimeComparator());
	
	private final Map<Id,PedestrianSignal> signals = new HashMap<Id,PedestrianSignal>();

	private InternalInterface internalInterface = null ;
	@Override
	public void setInternalInterface( InternalInterface internalInterface ) {
		this.internalInterface = internalInterface ;
	}

	/**
	 * @param sim
	 * @param random
	 */
	public Sim2DEngine(QSim sim) {
		this.scenario = sim.getScenario();
		this.sim2ConfigGroup = (Sim2DConfigGroup)this.scenario.getConfig().getModule("sim2d");
		this.sim = sim;
		this.sim2DStepSize = this.sim2ConfigGroup.getTimeStepSize();
		double factor = this.scenario.getConfig().getQSimConfigGroup().getTimeStepSize() / this.sim2DStepSize;
		if (factor != Math.round(factor)) {
			throw new RuntimeException("QSim time step size has to be a multiple of sim2d time step size");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.mobsim.framework.Steppable#doSimStep(double)
	 */
	@Override
	public void doSimStep(double time) {
//		long start = System.currentTimeMillis();
		double sim2DTime = time;
		while (sim2DTime < time + this.scenario.getConfig().getQSimConfigGroup().getTimeStepSize()) {
			handleDepartures(sim2DTime);
//			this.sim.getEventsManager().processEvent(new TickEvent(sim2DTime));
			this.floor.move(sim2DTime);

//			this.floor.drawGeometries(sim2DTime);
			
			sim2DTime += this.sim2DStepSize;
		}
//		long stop = System.currentTimeMillis();
//		long timet = (stop - start)/25;
//		System.out.println(1000/timet + "fps    " + this.floor.getAgents().size() +"agents" );
	}

	private void handleDepartures(double time) {
		while (this.activityEndsList.peek() != null) {
			Agent2D agent = this.activityEndsList.peek();
			if (agent.getRealActivityEndTime() <= time) {
				this.activityEndsList.poll();
				this.floor.agentDepart(agent);
			} else {
				return;
			}
		}
		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.SimEngine#afterSim()
	 */
	@Override
	public void afterSim() {
		// throw new RuntimeException("not (yet) implemented!");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.ptproject.qsim.interfaces.SimEngine#onPrepareSim()
	 */
	@Override
	public void onPrepareSim() {

		boolean emitEvents = true;
		this.floor = new PhysicalFloor(this.scenario, this.sim.getEventsManager(), emitEvents,this.signals,this.internalInterface);
		this.floor.init();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.matsim.ptproject.qsim.interfaces.SimEngine#getQSim()
	 */
	@Override
	public Netsim getMobsim() {
		return this.sim;
	}


	public void putDepartingAgentInLimbo(Agent2D agent) {
		this.activityEndsList.add(agent);
	}
	
	private static class Agent2DDepartureTimeComparator implements Comparator<Agent2D>, MatsimComparator {

		@Override
		public int compare(Agent2D agent1, Agent2D agent2) {
			int cmp = Double.compare(agent1.getRealActivityEndTime(), agent2.getRealActivityEndTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return agent2.getId().compareTo(agent1.getId());
			}
			return cmp;
		}
		
	}

	public void addSignal(PedestrianSignal sig) {
		this.signals.put(sig.getLinkId(), sig);
	}

}
