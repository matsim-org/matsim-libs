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

package org.matsim.contrib.drt.run.examples;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.drt.run.MultiModeDrtQSimModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michal.mac
 */
public class RunMultiModeDrtExample {

	private static final String CONFIG_FILE = "multi_mode_one_shared_taxi/multi_mode_one_shared_taxi_config.xml";

	public static void run(Config config, boolean otfvis) {
		MultiModeDrtConfigGroup multiModeDrtCfg = MultiModeDrtConfigGroup.get(config);
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getDrtConfigGroups()) {
			DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore());
		}
		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);

		List<DvrpModeQSimModule> dvrpModeQSimModules = new ArrayList<>();
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getDrtConfigGroups()) {
			dvrpModeQSimModules.add(new DvrpModeQSimModule.Builder(drtCfg.getMode()).build());
			controler.addQSimModule(new MultiModeDrtQSimModule(drtCfg));
			controler.addOverridingModule(new MultiModeDrtModule(drtCfg));
		}

		controler.addOverridingModule(new DvrpModule(dvrpModeQSimModules.stream().toArray(DvrpModeQSimModule[]::new)));

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TravelDisutilityFactory.class).annotatedWith(Drt.class)
						.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));
			}
		});

		controler.addQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Drt.class);
			}
		});

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(CONFIG_FILE, new MultiModeDrtConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.kinematicWaves);
		run(config, false);
	}
}
