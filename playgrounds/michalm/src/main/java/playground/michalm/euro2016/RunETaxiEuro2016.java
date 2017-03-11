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
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.*;

import playground.michalm.ev.EvConfigGroup;
import playground.michalm.taxi.run.RunETaxiBenchmark;

public class RunETaxiEuro2016 {
	public static void run(String configFile, int runs, String demand) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new EvConfigGroup());

		String baseDir = "../../../shared-svn/projects/maciejewski/Mielec/";
		config.plans().setInputFile(baseDir + "2014_02_base_scenario/plans_taxi/plans_only_taxi_" + demand + ".xml.gz");
		config.controler().setOutputDirectory(config.controler().getOutputDirectory() + "_" + demand);

		RunETaxiBenchmark.createControler(config, runs).run();
	}

	public static void main(String[] args) {
		int iter = 1;
		String dir = "../../../runs-svn/mielec_taxi/euro2016_etaxi/";
		String cfg = dir + "mielec_E_AP_benchmark_config.xml";
		run(cfg, iter, "1.0");
		run(cfg, iter, "1.5");
		run(cfg, iter, "2.0");
		run(cfg, iter, "2.5");
		run(cfg, iter, "3.0");
		run(cfg, iter, "3.5");
		run(cfg, iter, "4.0");

		cfg = dir + "mielec_E_RB_benchmark_config.xml";
		run(cfg, iter, "1.0");
		run(cfg, iter, "1.5");
		run(cfg, iter, "2.0");
		run(cfg, iter, "2.5");
		run(cfg, iter, "3.0");
		run(cfg, iter, "3.5");
		run(cfg, iter, "4.0");
	}
}
