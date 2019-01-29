/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.examples.onetaxionetruck;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiModule;
import org.matsim.contrib.dvrp.examples.onetruck.OneTruckModule;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunOneTaxiOneTruckExample {
	private static final String CONFIG_FILE = "./src/main/resources/one_taxi_one_truck/one_taxi_one_truck_config.xml";
	private static final String TAXIS_FILE = "one_taxi_vehicles.xml";
	private static final String TRUCKS_FILE = "one_truck_vehicles.xml";

	public static void run(boolean otfvis, int lastIteration) {
		// load config
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.controler().setLastIteration(lastIteration);
		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// setup controler
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new OneTaxiModule(TAXIS_FILE));
		controler.addOverridingModule(new OneTruckModule(TRUCKS_FILE));
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(TransportMode.taxi, TransportMode.truck));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule()); // OTFVis visualisation
		}

		// run simulation
		controler.run();
	}

	public static void main(String... args) {
		run(true, 0); // switch to 'false' to turn off visualisation
	}
}
