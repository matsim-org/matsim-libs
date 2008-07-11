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

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;


/**
 * @author dgrether
 *
 */
public class MatsimIo {

	
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

	public static void writePlans(Plans plans, String filename) {
		PlansWriter pwriter = new PlansWriter(plans, filename, "v4");
//		pwriter.setWriterHandler(new PlansWriterHandlerImplV4());
		pwriter.write();	
	}
	
}
