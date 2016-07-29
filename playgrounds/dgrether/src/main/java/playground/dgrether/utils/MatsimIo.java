/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;


/**
 * @author dgrether
 *
 */
public class MatsimIo {


	private static final Logger log = Logger.getLogger(MatsimIo.class);

	public static Config loadConfig(final Config conf, final String filename) {
		ConfigReader reader = new ConfigReader(conf);
		reader.readFile(filename);
		return conf;
	}

	public static void writeConfig(final Config config, final String filename) {
		new ConfigWriter(config).write(filename);
	}

	public static void loadNetwork(final String filename, final Scenario scenario) {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(filename);
	}


	public static Population loadPlans(final String filename, final Network network) {
		Scenario scenario = new ScenarioBuilder( ConfigUtils.createConfig() ).setNetwork(network).build() ;
		Population plans = scenario.getPopulation();
		log.info("  reading plans xml file... ");
		MatsimReader plansReader = new PopulationReader(scenario);
		plansReader.readFile(filename);
		log.info("  done");
		return plans;
	}


	public static void writePlans(final Population plans, final Network network, final String filename) {
		new PopulationWriter(plans, network).write(filename);
//		pwriter.setWriterHandler(new PlansWriterHandlerImplV4());
	}

}
