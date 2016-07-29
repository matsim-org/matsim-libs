package playground.gregor.scenariogen.hhwvehicles;
/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;

/**
 * Created by laemmel on 10/11/15.
 */
public class ResetDepartureTimes {

	private static final String RAW_INPUT = "/Users/laemmel/arbeit/papers/2015/TRBwFZJ/hybridsim_trb2016/analysis/vehicles/output/";
	private static final String NEW_DIR = "/Users/laemmel/arbeit/papers/2015/TRBwFZJ/hybridsim_trb2016/analysis/sens-vehicles/";

	public static void main(String[] args) throws IOException {

		Config c = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(c, RAW_INPUT + "output_config.xml.gz");

		Scenario sc = ScenarioUtils.createScenario(c);
//		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc.getNetwork()).readFile(RAW_INPUT + "/output_network.xml.gz");
		new PopulationReader(sc).readFile(RAW_INPUT + "output_plans.xml.gz");
		dropDepTimes(sc.getPopulation());

		c.controler().setOutputDirectory(NEW_DIR + "output/");
		c.network().setInputFile(NEW_DIR + "input/network.xml.gz");
		c.plans().setInputFile(NEW_DIR + "input/population.xml.gz");

		new NetworkWriter(sc.getNetwork()).write(c.network().getInputFile());
		new PopulationWriter(sc.getPopulation(), sc.getNetwork()).write(c.plans().getInputFile());
		new ConfigWriter(c).write(NEW_DIR + "input/config.xml");

		c.controler().setLastIteration(100);

		Controler cntr = new Controler(sc);
		cntr.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		cntr.run();

		Analyzer.main(new String[]{"/Users/laemmel/arbeit/papers/2015/TRBwFZJ/hybridsim_trb2016/analysis/sens-vehicles/output/ITERS/it.100/100.events.xml.gz", "/Users/laemmel/arbeit/papers/2015/TRBwFZJ/hybridsim_trb2016/analysis/sens-vehicles_plot_data"});


	}

	private static void dropDepTimes(Population population) {
		for (Person pers : population.getPersons().values()) {
			for (Plan p : pers.getPlans()) {
				double depTime = getDepTime();
				((Activity) p.getPlanElements().get(0)).setEndTime(depTime);
			}
		}
	}

	public static double getDepTime() {
		final double Ex = 0.5;
		final double varEx = 0.25;
		final double sigma = Math.sqrt(Math.log(1 + varEx / Math.pow(Ex, 2)));
		final double mu = Math.log(Ex - 1 / 2 * Math.pow(sigma, 2));
		double rnd = MatsimRandom.getRandom().nextGaussian() * sigma + mu;
		double dpTm = Math.exp(rnd) * 3600;
		while (dpTm < 0 || dpTm > 3600) {
			rnd = MatsimRandom.getRandom().nextGaussian() * sigma + mu;
			dpTm = Math.exp(rnd) * 3600;
		}
		return dpTm;
	}
//	private static void dropDepTimes(Population population) {
//		for (Person pers : population.getPersons().values()) {
//			for (Plan p : pers.getPlans()) {
//				((Activity) p.getPlanElements().get(0)).setEndTime(0);
//			}
//		}
//	}


}
