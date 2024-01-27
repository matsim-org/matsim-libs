/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer;

import java.net.URL;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

public class TaxiOptimizerTests {
	public static void runBenchmark(boolean vehicleDiversion, AbstractTaxiOptimizerParams taxiOptimizerParams, MatsimTestUtils utils) {
		Id.resetCaches();
		// mielec taxi mini benchmark contains only the morning peak (6:00 - 12:00) that is shifted by -6 hours (i.e. 0:00 - 6:00).
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_mini_benchmark_config.xml");
		var config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		Optional.ofNullable(taxiCfg.getTaxiOptimizerParams()).ifPresent(taxiCfg::removeParameterSet);
		taxiCfg.addParameterSet(taxiOptimizerParams);
		taxiCfg.vehicleDiversion = vehicleDiversion;

		var controler = RunTaxiBenchmark.createControler(config, 1);
		// RunTaxiBenchmark.createControler() overrides some config params, this is a moment to adjust them
		config.controller().setWriteEventsInterval(1);
		config.controller().setDumpDataAtEnd(true);

		controler.run();

		{
			Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_plans.xml.gz");

			Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_plans.xml.gz");

			boolean result = PopulationUtils.comparePopulations(expected, actual);
			Assertions.assertTrue(result);
		}
		{
			String expected = utils.getInputDirectory() + "/output_events.xml.gz";
			String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
			EventsFileComparator.Result result = EventsUtils.compareEventsFiles(expected, actual);
			Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result);
		}
	}
}
