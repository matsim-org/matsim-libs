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
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
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
		new ConfigWriter(config).writeFile(filename);
	}

	public static NetworkLayer loadNetwork(final String filename) {
		NetworkLayer network = new NetworkLayer();
//		Gbl.getWorld().setNetworkLayer(network);
		new MatsimNetworkReader(network).readFile(filename);
//		Gbl.getWorld().complete();
		return network;
	}


	public static PopulationImpl loadPlans(final String filename, final NetworkLayer network) {
		PopulationImpl plans = new PopulationImpl();
		log.info("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		plansReader.readFile(filename);
		log.info("  done");
		return plans;
	}


	public static void writePlans(final Population plans, final String filename) {
		new PopulationWriter(plans).writeFile(filename);
//		pwriter.setWriterHandler(new PlansWriterHandlerImplV4());
	}

}
