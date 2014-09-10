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

package playground.gregor.sim2d_v4.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSim2DTransitionLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.Sim2DQAdapterLink;

import playground.gregor.sim2d_v4.scenario.Sim2DConfig;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2d_v4.scenario.Sim2DScenario;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DEnvironment;
//import org.matsim.core.mobsim.qsim.qnetsimengine.QSim

public class Sim2DEngine implements MobsimEngine {

	private static final Logger log = Logger.getLogger(Sim2DEngine.class);

	private final Scenario scenario;
	private final QSim sim;
	private final double sim2DStepSize;

	private InternalInterface internalInterface;

	private final Sim2DScenario sim2dsc;

	private final Map<Id,PhysicalSim2DEnvironment> penvs = new HashMap<Id,PhysicalSim2DEnvironment>();

	private final double qSimStepSize;

	private final List<QSim2DTransitionLink> hiResLinks = new ArrayList<QSim2DTransitionLink>();

	private final Map<Id<Link>,Sim2DQAdapterLink> lowResLinks = new HashMap<Id<Link>,Sim2DQAdapterLink>();

//	private VisDebugger debugger;

	public Sim2DEngine(QSim sim) {
		this.scenario = sim.getScenario();

		this.sim = sim;
		this.sim2dsc = (Sim2DScenario) this.scenario.getScenarioElement(Sim2DScenario.ELEMENT_NAME);
		Sim2DConfig sim2dc = this.sim2dsc.getSim2DConfig();


		this.sim2DStepSize = sim2dc.getTimeStepSize();
		this.qSimStepSize = this.scenario.getConfig().qsim().getTimeStepSize();
		double factor =  this.qSimStepSize / this.sim2DStepSize;
		if (factor != Math.round(factor)) {
			throw new RuntimeException("QSim time step size has to be a multiple of sim2d time step size");
		}
	}


	@Override
	public void doSimStep(double time) {
//		log.info("do sim step");
		double sim2DTime = time;
		while (sim2DTime < time + this.qSimStepSize) {
			for (PhysicalSim2DEnvironment  env : this.penvs.values()) { //element order undefined when iterating over a HashSet. will certainly cause problems with checksum tests
				env.doSimStep(sim2DTime);
				
//				if (this.debugger != null) {
//					env.debug(this.debugger);
//				}
			}
			
//			if (this.debugger != null) {
//				this.debugger.update(sim2DTime);
//			}
			sim2DTime += this.sim2DStepSize;
		}
	}

	@Override
	public void onPrepareSim() {
		log.info("prepare");
		for (Sim2DEnvironment  env: this.sim2dsc.getSim2DEnvironments()) {
			PhysicalSim2DEnvironment e = new PhysicalSim2DEnvironment(env, this.sim2dsc, this.sim.getEventsManager());
			this.penvs.put(env.getId(),e);
			this.sim.addQueueSimulationListeners(e);
		}
		for (QSim2DTransitionLink hiResLink : this.hiResLinks) {
			Id id = this.sim2dsc.getSim2DEnvironment(hiResLink.getLink()).getId();
			PhysicalSim2DEnvironment penv = this.penvs.get(id);
			penv.createAndAddPhysicalTransitionSection(hiResLink);
		}
//		for (Entry<Id, Sim2DQTransitionLink> e : this.lowResLinks.entrySet()) {
//			PhysicalSim2DEnvironment penv = null;
//			for (Link l : e.getValue().getLink().getFromNode().getInLinks().values()) {
//				if (l.getAllowedModes().contains(TransportMode.walk2d)) {
//					Sim2DEnvironment env = this.sim2dsc.getSim2DEnvironment(l);
//					Id id = env.getId();
//					penv = this.penvs.get(id);
//					penv.createAndAddPhysicalTransitionSection(e.getValue(),env.getSection(l),l);
//					break;
//				}
//			}
//		}
		
		for (PhysicalSim2DEnvironment penv : this.penvs.values()) {
			penv.registerLowResLinks(this.lowResLinks);
		}

	}

	@Override
	public void afterSim() {
		log.info("after sim");

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	public void registerHiResLink(QSim2DTransitionLink hiResLink) {
		this.hiResLinks.add(hiResLink);
		
	}


	public void registerLowResLink(Sim2DQAdapterLink qLink) {
		
		this.lowResLinks.put(qLink.getLink().getId(),qLink);
	}

//	public void debug(VisDebugger debugger) {
//		this.debugger = debugger;
//	}

}
