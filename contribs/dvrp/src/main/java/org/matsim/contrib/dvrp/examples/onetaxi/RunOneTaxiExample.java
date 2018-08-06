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

import java.util.Collections;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigConsistencyChecker;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michalm
 */
public final class RunOneTaxiExample {
	private static final String CONFIG_FILE = "./src/main/resources/one_taxi/one_taxi_config.xml";
	private static final String TAXIS_FILE = "one_taxi_vehicles.xml";

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
		controler.addOverridingModule(new DvrpModule(//
				cfg -> new AbstractQSimModule() {
					@Override
					protected void configureQSim() {
						// optimizer that dispatches taxis
						bind(VrpOptimizer.class).to(OneTaxiOptimizer.class).asEagerSingleton();
						// converts departures of the "taxi" mode into taxi requests
						bind(PassengerRequestCreator.class).to(OneTaxiRequestCreator.class).asEagerSingleton();
						// converts scheduled tasks into simulated actions (legs and activities)
						bind(DynActionCreator.class).to(OneTaxiActionCreator.class).asEagerSingleton();
					}
				}, //
				Collections.emptySet()));// no mobsim listeners necessary in OneTaxiExample

		controler.addOverridingModule(
				FleetProvider.createModule(ConfigGroup.getInputFileURL(config.getContext(), TAXIS_FILE)));

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
