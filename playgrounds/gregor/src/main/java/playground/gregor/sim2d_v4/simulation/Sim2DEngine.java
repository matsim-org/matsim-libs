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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.gregor.sim2d_v3.config.Sim2DConfigGroup;

public class Sim2DEngine implements MobsimEngine {
	
	
	private final Scenario scenario;
	private final Sim2DConfigGroup sim2ConfigGroup;
	private final QSim sim;
	private final double sim2DStepSize;

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
	

	@Override
	public void doSimStep(double time) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		// TODO Auto-generated method stub

	}

}
