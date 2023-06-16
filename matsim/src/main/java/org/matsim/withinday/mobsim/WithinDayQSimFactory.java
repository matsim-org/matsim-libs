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

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTime;

import com.google.inject.Provider;

public class WithinDayQSimFactory implements Provider<Mobsim> {

	private static final Logger log = LogManager.getLogger(WithinDayQSimFactory.class);

	private final Scenario scenario;
	private final EventsManager eventsManager;
	private final WithinDayEngine withinDayEngine;
	private final FixedOrderSimulationListener fixedOrderSimulationListener;
	private final WithinDayTravelTime withinDayTravelTime;

	@Inject
	WithinDayQSimFactory(Scenario scenario, EventsManager eventsManager, WithinDayEngine withinDayEngine,
			FixedOrderSimulationListener fixedOrderSimulationListener, WithinDayTravelTime WithinDayTravelTime) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.withinDayEngine = withinDayEngine;
		this.fixedOrderSimulationListener = fixedOrderSimulationListener;
		this.withinDayTravelTime = WithinDayTravelTime;
	}

	@Override
	public Mobsim get() {
		Config config = scenario.getConfig();

		log.info("Adding WithinDayEngine to Mobsim.");
		return new QSimBuilder(config) //
							 .useDefaults() //
							 .addQSimModule(
						new WithinDayQSimModule(withinDayEngine, fixedOrderSimulationListener, withinDayTravelTime)) //
							 .configureQSimComponents(WithinDayQSimModule::configureComponents ) //
							 .build(scenario, eventsManager);
	}
}
