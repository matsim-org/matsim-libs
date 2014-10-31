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

package playground.gregor.casim.simulation;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DEnvironment;
//import org.matsim.core.mobsim.qsim.qnetsimengine.QSim

public class CAEngine implements MobsimEngine {

	private static final Logger log = Logger.getLogger(CAEngine.class);

	private final Scenario scenario;
	private final QSim sim;
	private InternalInterface internalInterface;


	private final Map<Id,PhysicalSim2DEnvironment> penvs = new HashMap<Id,PhysicalSim2DEnvironment>();

	private final double qSimStepSize;



	private final DepartureHandler dpHandler;

	public CAEngine(QSim sim) {
		this.scenario = sim.getScenario();

		this.sim = sim;
		this.qSimStepSize = this.scenario.getConfig().qsim().getTimeStepSize();
		this.dpHandler = new CAWalkerDepatureHandler(this);
		
	}


	@Override
	public void doSimStep(double time) {
//		this.caNet.runUntil(time + this.qSimStepSize);
	}

	@Override
	public void onPrepareSim() {
		log.info("prepare");
//		this.caNet = new CANetwork(this.scenario.getNetwork(), this.sim.getEventsManager());
		
//		for (Sim2DEnvironment  env: this.sim2dsc.getSim2DEnvironments()) {
//			PhysicalSim2DEnvironment e = new PhysicalSim2DEnvironment(env, this.sim2dsc, this.sim.getEventsManager());
//			this.penvs.put(env.getId(),e);
//			this.sim.addQueueSimulationListeners(e);
//		}
//		for (QSim2DTransitionLink hiResLink : this.hiResLinks) {
//			Id id = this.sim2dsc.getSim2DEnvironment(hiResLink.getLink()).getId();
//			PhysicalSim2DEnvironment penv = this.penvs.get(id);
//			penv.createAndAddPhysicalTransitionSection(hiResLink);
//		}


	}

	@Override
	public void afterSim() {
		log.info("after sim");

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}


	public DepartureHandler getDepartureHandler() {
		return this.dpHandler;
	}

//	public void registerHiResLink(QSimCATransitionLink hiResLink) {
//		this.hiResLinks.add(hiResLink);
//		
//	}
//
//
//	public void registerLowResLink(CAQTransitionLink lowResLink) {
//		this.lowResLinks.put(lowResLink.getLink().getId(),lowResLink);
//	}
	


}
