/* *********************************************************************** *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimizer;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.extension.preplanned.optimizer.LinearStopDurationModule;
import org.matsim.contrib.drt.extension.preplanned.optimizer.offline_optimization.RollingHorizonDrtOperationModule;
import org.matsim.contrib.drt.extension.preplanned.run.PreplannedDrtControlerCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkTravelTimeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author michalm
 */
public class RunOfflineOptimizationTest {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void mielecTest() {
		String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml").toString();
		runScenario(configPath);
	}

	private void runScenario(String configPath) {
		Id.resetCaches();

		Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
		MultiModeDrtConfigGroup multiModeDrtConfig = MultiModeDrtConfigGroup.get(config);
		DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
		drtConfigGroup.vehiclesFile = "vehicles-10-cap-4-offline.xml";
		if (drtConfigGroup.getRebalancingParams().isPresent()) {
			drtConfigGroup.removeParameterSet(drtConfigGroup.getRebalancingParams().get());
		}
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		Controler controler = PreplannedDrtControlerCreator.createControler(config, false);
		controler.addOverridingModule(new DvrpModule(new DvrpBenchmarkTravelTimeModule()));

		Population prebookedPlans = controler.getScenario().getPopulation();

		for (DrtConfigGroup drtCfg : multiModeDrtConfig.getModalElements()) {
			controler.addOverridingQSimModule(new RollingHorizonDrtOperationModule(prebookedPlans, drtCfg,
				86400, 86400, 10, false, 1, RollingHorizonDrtOperationModule.OfflineSolverType.REGRET_INSERTION));
			controler.addOverridingModule(new LinearStopDurationModule(drtCfg));
		}
		controler.run();

		{
			Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "output_plans.xml.gz");

			Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "output_plans.xml.gz");

			boolean result = PopulationUtils.comparePopulations(expected, actual);
			Assert.assertTrue(result);
		}

		{
			String expected = utils.getInputDirectory() + "output_events.xml.gz";
			String actual = utils.getOutputDirectory() + "output_events.xml.gz";
			EventsFileComparator.Result result = EventsUtils.compareEventsFiles(expected, actual);
			Assert.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result);
		}
	}
}
