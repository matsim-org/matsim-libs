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

package peoplemover.multiModeDRT;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.analysis.DrtModeAnalysisModule;
import org.matsim.contrib.drt.routing.MultiModeDrtMainModeIdentifier;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.DrtModeModule;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 * @author michal.mac
 */
public class RunMultiModeDrtBSWOBExample {

	private static final String INPUTDIR = "D:/BS_DRT/input/";

	public static void run() {
		Config config = ConfigUtils.loadConfig(INPUTDIR + "/multimode-drt-config_run2.xml",
				new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		String runId = "multiModeDRT2";
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(INPUTDIR + "../output/" + runId);

		MultiModeDrtConfigGroup multiModeDrtCfg = MultiModeDrtConfigGroup.get(config);
		DrtConfigs.adjustMultiModeDrtConfig(multiModeDrtCfg, config.planCalcScore());
		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);

		List<String> modes = new ArrayList<>();
		for (DrtConfigGroup drtCfg : multiModeDrtCfg.getDrtConfigGroups()) {
			modes.add(drtCfg.getMode());
			controler.addOverridingModule(new DrtModeModule(drtCfg));
			controler.addOverridingModule(new DrtModeAnalysisModule(drtCfg));
		}

		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(multiModeDrtCfg));

		controler.addOverridingModule(new SwissRailRaptorModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TravelDisutilityFactory.class).annotatedWith(Drt.class)
						.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));
				bind(MainModeIdentifier.class).toInstance(new MultiModeDrtMainModeIdentifier(multiModeDrtCfg));

			}
		});

		controler.addQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Drt.class);
			}
		});

		controler.run();
	}

	public static void main(String[] args) {
		run();
	}
}
