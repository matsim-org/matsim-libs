/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DEngine.java
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

package playground.gregor.sim2d_v3.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.comparators.PlanAgentDepartureTimeComparator;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.misc.Time;

import com.vividsolutions.jts.geom.Coordinate;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v3.controller.PedestrianSignal;
import playground.gregor.sim2d_v3.simulation.floor.Agent2D;
import playground.gregor.sim2d_v3.simulation.floor.DefaultVelocityCalculator;
import playground.gregor.sim2d_v3.simulation.floor.PhysicalAgentRepresentation;
import playground.gregor.sim2d_v3.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v3.simulation.floor.VelocityCalculator;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.LinkSwitcher;
import playground.gregor.sim2d_v3.simulation.floor.forces.deliberative.MentalLinkSwitcher;

/**
 * @author laemmel
 * 
 */
public class Sim2DEngine implements MobsimEngine {

	private static Logger log = Logger.getLogger(Sim2DEngine.class);

	private double infoTime = 0;

	private static final int INFO_PERIOD = 3600;
	
	private final Scenario scenario;

	private final Sim2DConfigGroup sim2ConfigGroup;
	private final double sim2DStepSize;
	private final QSim sim;

	private PhysicalFloor floor;
	
	private VelocityCalculator velocityCalculator;
	private final LinkSwitcher mlsw;
	
	private final Queue<MobsimAgent> activityEndsList = new PriorityQueue<MobsimAgent>(500, new PlanAgentDepartureTimeComparator());
	
	private final Map<Id,PedestrianSignal> signals = new HashMap<Id,PedestrianSignal>();
	private final Map<Id, Agent2D> agents2D = new HashMap<Id, Agent2D>();
	
	private InternalInterface internalInterface = null;
	
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
		
		/*
		 * Moved here from Sim2DAgentFactory which is not necessary anymore.
		 */
		this.velocityCalculator = new DefaultVelocityCalculator(scenario.getConfig().plansCalcRoute());
		Sim2DConfigGroup s2d = (Sim2DConfigGroup) scenario.getConfig().getModule("sim2d");
		
		if (s2d.isEnableMentalLinkSwitch()){
			this.mlsw = new MentalLinkSwitcher(scenario);
		} else {
			this.mlsw = new LinkSwitcher() {
				@Override
				public void checkForMentalLinkSwitch(Id curr, Id next, Agent2D agent) {
					// nothing to do here
				}
			};
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
		
		printSimLog(time);
	}

	/*package*/ void printSimLog(double time) {
		if (time >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			int nofActiveAgents = this.floor.getAgents().size();
			log.info("SIMULATION (Sim2DEngine) AT " + Time.writeTime(time) + " #agents=" + nofActiveAgents);
		}
	}
	
	private void handleDepartures(double time) {
		while (this.activityEndsList.peek() != null) {
			MobsimAgent mobsimAgent = this.activityEndsList.peek();
			
			if (mobsimAgent.getActivityEndTime() <= time) {
				this.activityEndsList.poll();
				Agent2D agent = agents2D.get(mobsimAgent.getId());
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
		this.floor.afterSim();
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
		this.floor = new PhysicalFloor(this.scenario, this.sim.getEventsManager(), emitEvents, this.signals, this.internalInterface);
		this.floor.init();
		
		// infoTime may be < simStartTime, this ensures to print out the info at the very first timestep already
		this.infoTime = Math.floor(internalInterface.getMobsim().getSimTimer().getSimStartTime() / INFO_PERIOD) * INFO_PERIOD;
	}

	public void putDepartingAgentInLimbo(MobsimDriverAgent agent) {
		/*
		 * Create Agents2D on the fly when required or create them for all
		 * agents when the simulation starts?
		 */
		if (!this.agents2D.containsKey(agent.getId())) {
			this.agents2D.put(agent.getId(), this.createAgent2DFromMobsimAgent(agent));
		}
		
		this.activityEndsList.add(agent);
	}
	
	public void addSignal(PedestrianSignal sig) {
		this.signals.put(sig.getLinkId(), sig);
	}
	
	public void setVelocityCalculator(VelocityCalculator velocityCalculator) {
		this.velocityCalculator = velocityCalculator;
	}
	
	/*
	 * Moved here from Sim2DAgentFactory which is not necessary anymore.
	 */
	private Agent2D createAgent2DFromMobsimAgent(MobsimDriverAgent mobsimDriverAgent) {
		
//		PhysicalAgentRepresentation par = new VelocityDependentEllipse();
		PhysicalAgentRepresentation par = new PhysicalAgentRepresentation() {
			@Override
			public void update(double v, double alpha, Coordinate pos) {
				// TODO Auto-generated method stub
			}
			@Override
			public void translate(Coordinate pos) {
				// TODO Auto-generated method stub
			}
		};
		
		Agent2D agent = new Agent2D(mobsimDriverAgent, this.scenario, this.velocityCalculator, this.mlsw, par);
		return agent;
	}

}
