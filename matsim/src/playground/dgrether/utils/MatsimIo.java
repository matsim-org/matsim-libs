/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.xml.sax.SAXException;


/**
 * @author dgrether
 *
 */
public class MatsimIo {

	
	private static final Logger log = Logger.getLogger(MatsimIo.class);
	
	public static Config loadConfig(Config conf, String filename) {
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
	
	public static void writerConfig(Config config, String filename) {
		ConfigWriter configWriter = new ConfigWriter(config, filename);
		configWriter.write();
	}
	
	public static NetworkLayer loadNetwork(String filename) {
		NetworkLayer network = new NetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);
		new MatsimNetworkReader(network).readFile(filename);
		return network;
	}
	
	
	public static Plans loadPlans(String filename) {
		Plans plans = new Plans(Plans.NO_STREAMING);
		log.info("  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(plans);
		plansReader.readFile(filename);
		plans.printPlansCount();
		log.info("  done");
		return plans;
	}
	

	public static void writePlans(Plans plans, String filename) {
		if (Gbl.getConfig() == null) {
			Gbl.createConfig(null);
		}
		PlansWriter pwriter = new PlansWriter(plans, filename, "v4");
//		pwriter.setWriterHandler(new PlansWriterHandlerImplV4());
		pwriter.write();	
	}
	
}
