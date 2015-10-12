/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.ctsim.simulation.physics;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import playground.gregor.casim.simulation.physics.CAVehicle;
import playground.gregor.ctsim.simulation.CTNetworkFactory;
import playground.gregor.ctsim.simulation.CTWalkerDepatureHandler;

public class CTNetsimEngine implements MobsimEngine {

	private static final Logger log = Logger.getLogger(CTNetsimEngine.class);

	private final Scenario scenario;
	private final QSim sim;

	private final DepartureHandler dpHandler;
	private final CTNetworkFactory fac;
	private CTNetwork ctNet;
	private InternalInterface internalInterface;

	public CTNetsimEngine(QSim sim, CTNetworkFactory fac) {
		this.scenario = sim.getScenario();
		this.fac = fac;
		this.sim = sim;
		this.dpHandler = new CTWalkerDepatureHandler(this, this.scenario);

	}

	@Override
	public void doSimStep(double time) {
		this.ctNet.doSimStep(time);

	}

	@Override
	public void onPrepareSim() {
		log.info("prepare");
		this.ctNet = fac.createCANetwork(sim.getScenario().getNetwork(),
				sim.getEventsManager(), this);
	}

	@Override
	public void afterSim() {
		log.info("after sim");

		this.ctNet.afterSim();

	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	public void letVehicleArrive(CAVehicle veh) {
		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		MobsimDriverAgent driver = veh.getDriver();
		internalInterface
				.getMobsim()
				.getEventsManager()
				.processEvent(
						new PersonLeavesVehicleEvent(now, driver.getId(), veh
								.getId()));
		// reset vehicles driver
		veh.setDriver(null);
		driver.endLegAndComputeNextState(now);
		this.internalInterface.arrangeNextAgentState(driver);
	}

	public DepartureHandler getDepartureHandler() {
		return this.dpHandler;
	}

	public CTNetwork getCTNetwork() {
		return this.ctNet;
	}

	public QSim getMobsim() {
		return this.sim;
	}

}
