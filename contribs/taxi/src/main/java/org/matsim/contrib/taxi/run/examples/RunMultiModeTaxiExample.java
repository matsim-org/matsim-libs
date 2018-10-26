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

package org.matsim.contrib.taxi.run.examples;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.contrib.taxi.run.MultiModeTaxiQSimModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunMultiModeTaxiExample {
	private static final String CONFIG_FILE = "multi_mode_one_taxi/multi_mode_one_taxi_config.xml";

	public static void run(boolean otfvis, int lastIteration) {
		// load config
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.controler().setLastIteration(lastIteration);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// setup controler
		Controler controler = new Controler(scenario);

		MultiModeTaxiConfigGroup multiModeTaxiCfg = MultiModeTaxiConfigGroup.get(config);
		List<DvrpModeQSimModule> dvrpModeQSimModules = new ArrayList<>();
		for (TaxiConfigGroup taxiCfg : multiModeTaxiCfg.getTaxiConfigGroups()) {
			dvrpModeQSimModules.add(new DvrpModeQSimModule.Builder(taxiCfg.getMode()).build());
			controler.addQSimModule(new MultiModeTaxiQSimModule(taxiCfg));
			controler.addOverridingModule(new MultiModeTaxiModule(taxiCfg));
		}

		controler.addOverridingModule(new DvrpModule(dvrpModeQSimModules.stream().toArray(DvrpModeQSimModule[]::new)));

		controler.addQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(),
						DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER);
			}
		});

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule()); // OTFVis visualisation
		}

		// run simulation
		controler.run();
	}

	public static void main(String[] args) {
		RunMultiModeTaxiExample.run(false, 0); // switch to 'true' to turn on visualisation
	}
}
