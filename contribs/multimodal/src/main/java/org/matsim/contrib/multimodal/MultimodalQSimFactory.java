/* *********************************************************************** *
 * project: org.matsim.*
 * MultimodalQSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.simengine.MultiModalQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.RunnableMobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import java.util.Map;

public class MultimodalQSimFactory implements Provider<RunnableMobsim> {

	private Scenario scenario;
	private EventsManager eventsManager;
	private final Map<String, TravelTime> multiModalTravelTimes;

	@Inject
	MultimodalQSimFactory(Scenario scenario, EventsManager eventsManager, Map<String, TravelTime> multiModalTravelTimes) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	@Override
	public RunnableMobsim get() {
		QSim qSim = QSimUtils.createDefaultQSim(scenario, eventsManager);
		new MultiModalQSimModule(scenario.getConfig(), this.multiModalTravelTimes).configure(qSim);
		return qSim;
	}
}
