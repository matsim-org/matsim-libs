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

package playground.michalm.euro2016;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.*;

public class RunTaxiEuro2016 {
	public static void run(String configFile, int runs, String demand) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup());

		String baseDir = "../../../shared-svn/projects/maciejewski/Mielec/";
		config.plans().setInputFile(baseDir + "2014_02_base_scenario/plans_taxi/plans_only_taxi_" + demand + ".xml.gz");
		config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "_" + demand);

		RunTaxiBenchmark.createControler(config, runs).run();
	}

	public static void main(String[] args) {
		// run("./src/main/resources/one_taxi_benchmark/one_taxi_benchmark_config.xml", 20);

		int iter = 20;
		String dir = "../../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/";
		String cfg = dir + "mielec_taxi_benchmark_config_RULE_BASED.xml";
		run(cfg, iter, "1.0");
		run(cfg, iter, "1.5");
		run(cfg, iter, "2.0");
		run(cfg, iter, "2.5");
		run(cfg, iter, "3.0");
		run(cfg, iter, "3.5");
		run(cfg, iter, "4.0");

		cfg = dir + "mielec_taxi_benchmark_config_ASSIGNMENT.xml";
		run(cfg, iter, "1.0");
		run(cfg, iter, "1.5");
		run(cfg, iter, "2.0");
		run(cfg, iter, "2.5");
		run(cfg, iter, "3.0");
		run(cfg, iter, "3.5");
		run(cfg, iter, "4.0");
	}

}
