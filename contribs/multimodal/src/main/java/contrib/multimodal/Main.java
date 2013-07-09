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

package contrib.multimodal;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import contrib.multimodal.router.MultimodalTripRouterFactory;
import contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import contrib.multimodal.tools.PrepareMultiModalScenario;

public class Main {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PrepareMultiModalScenario.run(scenario);
		Controler controler = new Controler(scenario);
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig());
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();	
	
		MultimodalTripRouterFactory tripRouterFactory = new MultimodalTripRouterFactory(
				controler, multiModalTravelTimes);
		MultimodalQSimFactory qSimFactory = new MultimodalQSimFactory(multiModalTravelTimes);
		controler.setTripRouterFactory(tripRouterFactory);
		controler.setMobsimFactory(qSimFactory);

		controler.run();
	}

}
