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

package org.matsim.contrib.taxi.benchmark;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkControlerModule;
import org.matsim.contrib.dvrp.benchmark.DvrpBenchmarkModule;
import org.matsim.contrib.dvrp.fleet.FleetStatsCalculatorModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.FixedIntervalTimeVariantLinkFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;

/**
 * For a fair and consistent benchmarking of taxi dispatching algorithms we assume that link travel times are
 * deterministic. To simulate this property, we remove (1) all other traffic, and (2) link capacity constraints (e.g. by
 * increasing the capacities by 100+ times), as a result all vehicles move with the free-flow speed (which is the
 * effective speed).
 * <p>
 * </p>
 * To model the impact of traffic, we can use a time-variant network, where we specify different free-flow speeds for
 * each link over time. The default approach is to specify free-flow speeds in each time interval (usually 15 minutes).
 */
public class RunTaxiBenchmark {
	public static void run(String configFile, int runs) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup());
		createControler(config, runs).run();
	}

	public static Controler createControler(Config config, int runs) {
		config.controler().setLastIteration(runs - 1);
		config.controler().setDumpDataAtEnd(false);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setCreateGraphs(false);

		DvrpConfigGroup.get(config).setNetworkMode(null);// to switch off network filtering
		config.addConfigConsistencyChecker(new TaxiBenchmarkConfigConsistencyChecker());
		config.checkConsistency();

		String mode = TaxiConfigGroup.get(config).getMode();
		Scenario scenario = loadBenchmarkScenario(config, 15 * 60, 30 * 3600);

		Controler controler = new Controler(scenario);
		controler.setModules(new DvrpBenchmarkControlerModule());
		controler.addOverridingModule(new DvrpBenchmarkModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(mode));

		controler.addOverridingModule(new TaxiModule());

		controler.addOverridingModule(FleetStatsCalculatorModule.createModule(mode, TaxiBenchmarkStats.class,
				getter -> new TaxiBenchmarkStats(getter.get(OutputDirectoryHierarchy.class))));

		return controler;
	}

	public static Scenario loadBenchmarkScenario(Config config, int interval, int maxTime) {
		Scenario scenario = new ScenarioBuilder(config).build();

		if (config.network().isTimeVariantNetwork()) {
			((Network)scenario.getNetwork()).getFactory()
					.setLinkFactory(new FixedIntervalTimeVariantLinkFactory(interval, maxTime));
		}

		ScenarioUtils.loadScenario(scenario);
		return scenario;
	}

	public static void main(String[] args) {
		run("./src/main/resources/one_taxi_benchmark/one_taxi_benchmark_config.xml", 20);
	}
}
