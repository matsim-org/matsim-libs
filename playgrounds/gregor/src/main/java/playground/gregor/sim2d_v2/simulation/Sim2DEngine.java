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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;
import org.matsim.ptproject.qsim.interfaces.Netsim;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.events.TickEvent;
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
		double sim2DTime = time;
		while (sim2DTime < time + this.scenario.getConfig().getQSimConfigGroup().getTimeStepSize()) {
			this.sim.getEventsManager().processEvent(new TickEvent(sim2DTime));
			this.floor.move(sim2DTime);

			sim2DTime += this.sim2DStepSize;
			//			System.out.println("++++++++++++++++++");
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
		//		if (this.sim.getIterationNumber() % this.sim2ConfigGroup.getEventsInterval() == 0) {
		//			emitEvents = true;
		//		}

		//		Map<MultiPolygon, List<Link>> flm = this.scenario.getScenarioElement(MyDataContainer.class).getMps();
		//		for (Entry<MultiPolygon, List<Link>> e : flm.entrySet()) {
		this.floor = new PhysicalFloor(this.scenario, this.sim.getEventsManager(), emitEvents);
		this.floor.init();
		//		}
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

	/**
	 * @param currentLinkId
	 * @return
	 */
	public PhysicalFloor getFloor(Id currentLinkId) {
		return this.floor;
	}


}
