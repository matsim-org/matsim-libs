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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class MatsimIo {


	private static final Logger log = Logger.getLogger(MatsimIo.class);

	public static Config loadConfig(final Config conf, final String filename) {
		MatsimConfigReader reader = new MatsimConfigReader(conf);
		try {
			reader.parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return conf;
	}

	public static void writeConfig(final Config config, final String filename) {
		new ConfigWriter(config).write(filename);
	}

	public static void loadNetwork(final String filename, final Scenario scenario) {
		new MatsimNetworkReader(scenario).readFile(filename);
	}


	public static Population loadPlans(final String filename, final NetworkImpl network) {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.setNetwork(network);
		Population plans = scenario.getPopulation();
		log.info("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(filename);
		log.info("  done");
		return plans;
	}


	public static void writePlans(final Population plans, final Network network, final String filename) {
		new PopulationWriter(plans, network).write(filename);
//		pwriter.setWriterHandler(new PlansWriterHandlerImplV4());
	}

}
