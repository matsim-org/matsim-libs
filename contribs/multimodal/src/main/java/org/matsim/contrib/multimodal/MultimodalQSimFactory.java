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

import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.simengine.MultiModalQSimPlugin;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

public class MultimodalQSimFactory implements Provider<Mobsim> {

	private Scenario scenario;
	private EventsManager eventsManager;
	private Map<String, TravelTime> multiModalTravelTimes;

	@Inject
	MultimodalQSimFactory(Scenario scenario, EventsManager eventsManager, Map<String, TravelTime> multiModalTravelTimes) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	@Override
	public Mobsim get() {
		return new QSimBuilder(scenario.getConfig()) //
				.useDefaults()
				.addPlugin(new MultiModalQSimPlugin(scenario.getConfig(), multiModalTravelTimes))
				.configureComponents(components -> {
					components.activeMobsimEngines.add(MultiModalQSimPlugin.MULTIMODAL_ENGINE);
					components.activeDepartureHandlers.add(MultiModalQSimPlugin.MULTIMODAL_DEPARTURE_HANDLER);
				}) //
				.build(scenario, eventsManager);
	}
}
