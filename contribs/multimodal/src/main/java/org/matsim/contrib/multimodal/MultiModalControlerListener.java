/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalControlerListener.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.Map;

public class MultiModalControlerListener implements StartupListener {

	private Config config;
    private Scenario scenario;
    private Map<String, TravelTime> multiModalTravelTimes;

    @Inject
    MultiModalControlerListener(Config config, Scenario scenario, Map<String, TravelTime> multiModalTravelTimes) {
        this.config = config;
        this.scenario = scenario;
        this.multiModalTravelTimes = multiModalTravelTimes;
    }

    @Override
	public void notifyStartup(StartupEvent event) {
        if (!config.travelTimeCalculator().isFilterModes()) {
            throw new RuntimeException("Filtering analyzed modes is NOT enabled in the TravelTimeCalculatorConfigGroup. " +
                    "It must be enabled since otherwise also link travel times of multi-modal legs would be " +
                    "taken into account!");
        }

		MultiModalConfigGroup multiModalConfigGroup = ConfigUtils.addOrGetModule(config, MultiModalConfigGroup.GROUP_NAME, MultiModalConfigGroup.class);

		// ensure that NetworkRoutes are created for legs using one of the simulated modes
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory();
		for (String mode : CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes())) {
			routeFactory.setRouteFactory(mode, new LinkNetworkRouteFactory());
		}
	}

}