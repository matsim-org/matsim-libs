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

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

public class TaxiOptimizerTests {
	public static void runBenchmark(boolean vehicleDiversion, AbstractTaxiOptimizerParams taxiOptimizerParams, String outputDir) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_benchmark_config.xml");
		var config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
		config.plans().setInputFile("plans_only_taxi_mini_benchmark_3.0.xml.gz");
		config.controler().setOutputDirectory(outputDir);

		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);
		taxiCfg.taxisFile = "taxis_mini_benchmark-25.xml";

		var controler = RunTaxiBenchmark.createControler(config, 1);
		// RunTaxiBenchmark.createControler() overrides some config params, this is a moment to reset them

		Optional.ofNullable(taxiCfg.getTaxiOptimizerParams()).ifPresent(taxiCfg::removeParameterSet);
		taxiCfg.addParameterSet(taxiOptimizerParams);

		taxiCfg.vehicleDiversion = vehicleDiversion;

		controler.run();
	}
}
