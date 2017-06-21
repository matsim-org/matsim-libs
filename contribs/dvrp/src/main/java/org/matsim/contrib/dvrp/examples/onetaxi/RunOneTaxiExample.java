/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michalm
 */
class RunOneTaxiExample {
	private static final String CONFIG_FILE = "./src/main/resources/one_taxi/one_taxi_config.xml";
	private static final String TAXIS_FILE = "./src/main/resources/one_taxi/one_taxi_vehicles.xml";

	public static void run(boolean otfvis, int lastIteration) {
		// load config
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.controler().setLastIteration(lastIteration);
		config.addConfigConsistencyChecker(new DvrpConfigConsistencyChecker());
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// load fleet
		final FleetImpl fleet = new FleetImpl();
		new VehicleReader(scenario.getNetwork(), fleet).readFile(TAXIS_FILE);

		// setup controler
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule( //
				OneTaxiOptimizer.class, // optimizer that dispatches taxis
				OneTaxiRequestCreator.class, // converts departures of the "taxi" mode into taxi requests
				OneTaxiActionCreator.class)); // converts scheduled tasks into simulated actions (legs and activities)
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Fleet.class).toInstance(fleet);
			}
		});

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
