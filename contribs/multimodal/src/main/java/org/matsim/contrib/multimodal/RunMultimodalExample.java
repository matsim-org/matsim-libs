/* *********************************************************************** *
 * project: org.matsim.*
 * Main.java
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
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.contrib.multimodal.tools.PrepareMultiModalScenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Map;

/**
 * 
 * This class shows an example how to set up a controler and with
 * an initialized multi-modal simulation.
 * 
 * As a sample scenario, one can replace the empty config with a 
 * multi-modal config for the berlin scenario:
 * 
 * Config config = ConfigUtils.loadConfig("../../matsim/src/test/resources/test/scenarios/berlin/config_multimodal.xml", new MultiModalConfigGroup()));
 * 
 * @author cdobler
 */
public class RunMultimodalExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
//		Config config = ConfigUtils.loadConfig("../../matsim/src/test/resources/test/scenarios/berlin/config_multimodal.xml", new MultiModalConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PrepareMultiModalScenario.run(scenario);
		Controler controler = new Controler(scenario);
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig());
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();	
	
		TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(controler.getScenario(), new FastDijkstraFactory());
		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(controler.getScenario(), multiModalTravelTimes,
                ControlerDefaults.createDefaultTravelDisutilityFactory(scenario), defaultDelegateFactory, new FastDijkstraFactory());

		final MultimodalQSimFactory qSimFactory = new MultimodalQSimFactory(scenario, controler.getEvents(), multiModalTravelTimes);
		controler.setTripRouterFactory(multiModalTripRouterFactory);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(qSimFactory);
			}
		});

		controler.run();
	}

}
