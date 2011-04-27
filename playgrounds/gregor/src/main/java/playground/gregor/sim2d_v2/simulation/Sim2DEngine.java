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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.mobsim.framework.Steppable;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.ptproject.qsim.interfaces.MobsimEngine;

import com.vividsolutions.jts.geom.MultiPolygon;

import playground.gregor.sim2d_v2.calibration.Validator;
import playground.gregor.sim2d_v2.calibration.scenario.PhantomEvents;
import playground.gregor.sim2d_v2.calibration.simulation.floor.PhantomFloor;
import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.scenario.Scenario2DImpl;
import playground.gregor.sim2d_v2.simulation.floor.PhysicalFloor;
import playground.gregor.sim2d_v2.simulation.floor.Floor;

/**
 * @author laemmel
 * 
 */
public class Sim2DEngine implements MobsimEngine, Steppable {

	private final List<Floor> floors = new ArrayList<Floor>();
	private final Scenario2DImpl scenario;
	private final Sim2D sim;

	private final Map<Id, PhysicalFloor> linkIdFloorMapping = new HashMap<Id, PhysicalFloor>();
	private final Sim2DConfigGroup sim2ConfigGroup;
	private final double sim2DStepSize;
	private Id calibrationAgentId;
	private PhantomEvents phantomEvents;
	private Validator validator;

	/**
	 * @param sim
	 * @param random
	 */
	public Sim2DEngine(Sim2D sim, Random random) {
		this.scenario = (Scenario2DImpl) sim.getScenario();
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
			for (Floor floor : this.floors) {
				floor.move(sim2DTime);
			}

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

		boolean emitEvents = false;
		if (this.sim.getIterationNumber() % this.sim2ConfigGroup.getEventsInterval() == 0) {
			emitEvents = true;
		}


		for (Entry<MultiPolygon, List<Link>> e : this.scenario.getFloorLinkMapping().entrySet()) {
			PhysicalFloor f = null;
			if (this.sim2ConfigGroup.isCalibrationMode()) {
				f = new PhantomFloor(this.phantomEvents,this.calibrationAgentId, this.sim,e.getValue(),this.scenario, this.validator);
			} else {
				f = new PhysicalFloor(this.scenario, e.getValue(), this.sim, emitEvents);
			}
			this.floors.add(f);
			for (Link l : e.getValue()) {
				if (this.linkIdFloorMapping.get(l.getId()) != null) {
					throw new RuntimeException("Multiple floors per link not allowed! Link with Id: " + l.getId() + " already belongs to floor" + this.linkIdFloorMapping.get(l.getId()));
				}
				this.linkIdFloorMapping.put(l.getId(), f);
			}
			f.init();
		}
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
		return this.linkIdFloorMapping.get(currentLinkId);
	}

	public void setPhantomPopulationEvents(PhantomEvents phantomEvents) {
		this.phantomEvents = phantomEvents;
	}
	public void setCalibrationAgentId(Id id)  {
		this.calibrationAgentId = id;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;

	}


}
