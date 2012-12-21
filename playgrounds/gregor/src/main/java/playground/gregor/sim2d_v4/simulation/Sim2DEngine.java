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
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QSim2DTransitionLink;

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

	private final List<PhysicalSim2DEnvironment> envs = new ArrayList<PhysicalSim2DEnvironment>();

	private final double qSimStepSize;

	public Sim2DEngine(QSim sim) {
		this.scenario = sim.getScenario();

		this.sim = sim;
		this.sim2dsc = this.scenario.getScenarioElement(Sim2DScenario.class);
		Sim2DConfig sim2dc = this.sim2dsc.getSim2DConfig();


		this.sim2DStepSize = sim2dc.getTimeStepSize();
		this.qSimStepSize = this.scenario.getConfig().getQSimConfigGroup().getTimeStepSize();
		double factor =  this.qSimStepSize / this.sim2DStepSize;
		if (factor != Math.round(factor)) {
			throw new RuntimeException("QSim time step size has to be a multiple of sim2d time step size");
		}
	}


	@Override
	public void doSimStep(double time) {
		log.info("do sim step");
		double sim2DTime = time;
		while (sim2DTime < time + this.qSimStepSize) {
			//TODO handle departures here!
			for (PhysicalSim2DEnvironment  env : this.envs) {
				env.doSimStep(time);
			}
			sim2DTime += this.sim2DStepSize;
		}
	}

	@Override
	public void onPrepareSim() {
		log.info("prepare");
		for (Sim2DEnvironment  env: this.sim2dsc.getSim2DEnvironments()) {
			this.envs.add(new PhysicalSim2DEnvironment(env));
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


	public void putDepartingAgentInLimbo(MobsimDriverAgent agent) {
		log.info(agent + " \n added!");

	}


	public void registerHiResLink(QSim2DTransitionLink hiResLink) {
		// TODO Auto-generated method stub
		
	}


}
