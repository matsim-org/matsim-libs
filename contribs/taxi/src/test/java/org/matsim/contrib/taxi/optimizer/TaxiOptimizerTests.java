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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

public class TaxiOptimizerTests {
	public record TaxiConfigVariant
			(boolean destinationKnown, boolean vehicleDiversion, double pickupDuration, double dropoffDuration, boolean onlineVehicleTracker) {
		void updateTaxiConfig(TaxiConfigGroup taxiCfg) {
			taxiCfg.destinationKnown = destinationKnown;
			taxiCfg.vehicleDiversion = vehicleDiversion;
			taxiCfg.pickupDuration = pickupDuration;
			taxiCfg.dropoffDuration = dropoffDuration;
			taxiCfg.onlineVehicleTracker = onlineVehicleTracker;
		}
	}

	public static class PreloadedBenchmark {
		private final Config config;
		private final Controler controler;

		public PreloadedBenchmark(String plansSuffix, String taxisSuffix) {
			URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_benchmark_config.xml");

			config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
			TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);

			config.plans().setInputFile("plans_only_taxi_mini_benchmark_" + plansSuffix + ".xml.gz");
			taxiCfg.taxisFile = "taxis_mini_benchmark-" + taxisSuffix + ".xml";

			controler = RunTaxiBenchmark.createControler(config, 1);
		}
	}

	public static void runBenchmark(TaxiConfigVariant variant, AbstractTaxiOptimizerParams taxiOptimizerParams, PreloadedBenchmark benchmark,
			String outputDir) {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(benchmark.config);
		Optional.ofNullable(taxiCfg.getTaxiOptimizerParams()).ifPresent(taxiCfg::removeParameterSet);
		taxiCfg.addParameterSet(taxiOptimizerParams);

		variant.updateTaxiConfig(taxiCfg);
		benchmark.config.controler().setOutputDirectory(outputDir);

		benchmark.controler.run();
	}
}
