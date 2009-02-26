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
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.config.MatsimConfigReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
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

	public static void writerConfig(final Config config, final String filename) {
		ConfigWriter configWriter = new ConfigWriter(config, filename);
		configWriter.write();
	}

	public static NetworkLayer loadNetwork(final String filename) {
		NetworkLayer network = new NetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);
		new MatsimNetworkReader(network).readFile(filename);
		Gbl.getWorld().complete();
		return network;
	}


	public static Population loadPlans(final String filename) {
		Population plans = new PopulationImpl(PopulationImpl.NO_STREAMING);
		log.info("  reading plans xml file... ");
		PopulationReader plansReader = new MatsimPopulationReader(plans);
		plansReader.readFile(filename);
		plans.printPlansCount();
		log.info("  done");
		return plans;
	}


	public static void writePlans(final Population plans, final String filename) {
		if (Gbl.getConfig() == null) {
			Gbl.createConfig(null);
		}
		PopulationWriter pwriter = new PopulationWriter(plans, filename, "v4");
//		pwriter.setWriterHandler(new PlansWriterHandlerImplV4());
		pwriter.write();
	}

}
