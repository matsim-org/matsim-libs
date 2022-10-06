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
	public static class TaxiConfigVariant {
		final boolean destinationKnown;
		final boolean vehicleDiversion;
		final double pickupDuration;
		final double dropoffDuration;
		final boolean onlineVehicleTracker;

		private TaxiConfigVariant(boolean destinationKnown, boolean vehicleDiversion, double pickupDuration,
				double dropoffDuration, boolean onlineVehicleTracker) {
			this.destinationKnown = destinationKnown;
			this.vehicleDiversion = vehicleDiversion;
			this.pickupDuration = pickupDuration;
			this.dropoffDuration = dropoffDuration;
			this.onlineVehicleTracker = onlineVehicleTracker;
		}

		void updateTaxiConfig(TaxiConfigGroup taxiCfg) {
			taxiCfg.destinationKnown = destinationKnown;
			taxiCfg.vehicleDiversion = vehicleDiversion;
			taxiCfg.pickupDuration = pickupDuration;
			taxiCfg.dropoffDuration = dropoffDuration;
			taxiCfg.onlineVehicleTracker = onlineVehicleTracker;
		}
	}

	public static List<TaxiConfigVariant> createDefaultTaxiConfigVariants(boolean diversionSupported) {
		List<TaxiConfigVariant> variants = new ArrayList<>();

		// onlineVehicleTracker == false ==> vehicleDiversion == false
		variants.add(new TaxiConfigVariant(false, false, 120, 60, false));
		variants.add(new TaxiConfigVariant(true, false, 1, 1, false));

		if (diversionSupported) {
			// onlineVehicleTracker == true
			variants.add(new TaxiConfigVariant(false, true, 1, 1, true));
			variants.add(new TaxiConfigVariant(true, true, 120, 60, true));
		} else {
			// onlineVehicleTracker == true
			variants.add(new TaxiConfigVariant(false, false, 1, 1, true));
			variants.add(new TaxiConfigVariant(true, false, 120, 60, true));
		}

		return variants;
	}

	public static class PreloadedBenchmark {
		private final Config config;
		private final Controler controler;

		public PreloadedBenchmark(String plansSuffix, String taxisSuffix) {
			URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"),
					"mielec_taxi_benchmark_config.xml");

			config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup());
			TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(config);

			config.plans().setInputFile("plans_only_taxi_mini_benchmark_" + plansSuffix + ".xml.gz");
			taxiCfg.taxisFile = "taxis_mini_benchmark-" + taxisSuffix + ".xml";

			controler = RunTaxiBenchmark.createControler(config, 1);
		}
	}

	public static void runBenchmark(List<TaxiConfigVariant> variants, AbstractTaxiOptimizerParams taxiOptimizerParams,
			PreloadedBenchmark benchmark, String outputDir) {
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.getSingleModeTaxiConfig(benchmark.config);
		Optional.ofNullable(taxiCfg.getTaxiOptimizerParams()).ifPresent(taxiCfg::removeParameterSet);
		taxiCfg.addParameterSet(taxiOptimizerParams);

		int i = 0;
		for (TaxiConfigVariant v : variants) {
			v.updateTaxiConfig(taxiCfg);
			benchmark.config.controler().setOutputDirectory(outputDir + "/" + i++);
			benchmark.controler.run();
		}
	}
}
