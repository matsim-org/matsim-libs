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
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import javax.inject.Inject;

public class WithinDayQSimFactory implements Provider<RunnableMobsim> {

	private static final Logger log = Logger.getLogger(WithinDayQSimFactory.class);

	private final Scenario scenario;
	private final EventsManager eventsManager;
	private final WithinDayEngine withinDayEngine;

	@Inject
	WithinDayQSimFactory(Scenario scenario, EventsManager eventsManager, WithinDayEngine withinDayEngine) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.withinDayEngine = withinDayEngine;
	}

	@Override
	public RunnableMobsim get() {
		QSim mobsim = QSimUtils.createDefaultQSim(scenario, eventsManager);
		log.info("Adding WithinDayEngine to Mobsim.");
		mobsim.addMobsimEngine(withinDayEngine);
		return mobsim;
	}
}
