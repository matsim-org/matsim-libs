/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package uam.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunUamOnlyScenario {
	public static void run(Config config, boolean otfvis, int lastIteration) {
		config.controler().setLastIteration(lastIteration);

		// load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// setup controler
		Controler controler = new Controler(scenario);

		String mode = TaxiConfigGroup.getSingleModeTaxiConfig(config).getMode();
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeTaxiModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(mode));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule()); // OTFVis visualisation
		}

		// run simulation
		controler.run();
	}

	public static void runLimitedFleetMinPickupTime(Config config, boolean otfvis, int lastIteration) {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setTaxisFile("uam_fleet_6x1.xml");
		RuleBasedTaxiOptimizerParams taxiOptimizerParams = (RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams();
		taxiOptimizerParams.setGoal(RuleBasedRequestInserter.Goal.MIN_PICKUP_TIME);

		config.plans().setInputFile("uam_only_population_6x5x10.xml");

		config.controler().setOutputDirectory("output/uam/uam_only_scenario_limitedFleet_minPickupTime");
		run(config, otfvis, lastIteration);
	}

	public static void runLimitedFleetMinWaitTime(Config config, boolean otfvis, int lastIteration) {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setTaxisFile("uam_fleet_6x1_60h.xml");
		RuleBasedTaxiOptimizerParams taxiOptimizerParams = (RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams();
		taxiOptimizerParams.setGoal(RuleBasedRequestInserter.Goal.MIN_WAIT_TIME);

		config.plans().setInputFile("uam_only_population_6x5x10.xml");

		config.controler().setOutputDirectory("output/uam/uam_only_scenario_limitedFleet_minWaitTime");
		run(config, otfvis, lastIteration);
	}

	public static void runUnlimitedFleetMinPickupTime(Config config, boolean otfvis, int lastIteration) {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setTaxisFile("uam_fleet_6x500.xml");
		RuleBasedTaxiOptimizerParams taxiOptimizerParams = (RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams();
		taxiOptimizerParams.setGoal(RuleBasedRequestInserter.Goal.MIN_PICKUP_TIME);

		config.plans().setInputFile("uam_only_population_6x5x100.xml");

		config.controler().setOutputDirectory("output/uam/uam_only_scenario_unlimitedFleet_minPickupTime");
		run(config, otfvis, lastIteration);
	}

	public static void runUnlimitedFleetMinWaitTime(Config config, boolean otfvis, int lastIteration) {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.setTaxisFile("uam_fleet_6x500.xml");
		RuleBasedTaxiOptimizerParams taxiOptimizerParams = (RuleBasedTaxiOptimizerParams)taxiCfg.getTaxiOptimizerParams();
		taxiOptimizerParams.setGoal(RuleBasedRequestInserter.Goal.MIN_WAIT_TIME);

		config.plans().setInputFile("uam_only_population_6x5x100.xml");

		config.controler().setOutputDirectory("output/uam/uam_only_scenario_unlimitedFleet_minWaitTime");
		run(config, otfvis, lastIteration);
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("input/uam/uam_only_config.xml", new MultiModeTaxiConfigGroup(),
				new DvrpConfigGroup(), new OTFVisConfigGroup());
		run(config, false, 0);
		//		runLimitedFleetMinPickupTime(config, false, 0);
		//		runLimitedFleetMinWaitTime(config, false, 0);
		//		runUnlimitedFleetMinPickupTime(config, false, 0);
		//		runUnlimitedFleetMinWaitTime(config, false, 0);
	}
}
