/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayQSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.withinday.mobsim;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

import javax.inject.Inject;

public class WithinDayQSimFactory implements Provider<Mobsim> {

	private static final Logger log = Logger.getLogger(WithinDayQSimFactory.class);

	private final Scenario scenario;
	private final EventsManager eventsManager;
	private final WithinDayEngine withinDayEngine;
	private final FixedOrderSimulationListener fixedOrderSimulationListener;
	private final WithinDayTravelTime WithinDayTravelTime;
	private MobsimTimer mobsimTimer;
	private AgentCounter agentCounter;
	
	@Inject
	WithinDayQSimFactory(Scenario scenario, EventsManager eventsManager, WithinDayEngine withinDayEngine, FixedOrderSimulationListener fixedOrderSimulationListener, WithinDayTravelTime WithinDayTravelTime, MobsimTimer mobsimTimer, AgentCounter agentCounter) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.withinDayEngine = withinDayEngine;
		this.fixedOrderSimulationListener = fixedOrderSimulationListener;
		this.WithinDayTravelTime = WithinDayTravelTime;
		this.mobsimTimer = mobsimTimer;
		this.agentCounter = agentCounter;
	}

	@Override
	public Mobsim get() {
		QSim mobsim = QSimUtils.createDefaultQSim(scenario, eventsManager, mobsimTimer, agentCounter);
		log.info("Adding WithinDayEngine to Mobsim.");
		mobsim.addMobsimEngine(withinDayEngine);
		mobsim.addQueueSimulationListeners(fixedOrderSimulationListener);
		mobsim.addQueueSimulationListeners(WithinDayTravelTime);
		return mobsim;
	}
}
