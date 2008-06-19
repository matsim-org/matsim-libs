/* *********************************************************************** *
 * project: org.matsim.*
 * DblLinkDetect.java
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

package playground.gregor;

import org.apache.log4j.Logger;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.network.algorithms.NetworkSegmentDoubleLinks;
import org.matsim.world.World;



public class DblLinkDetect {
	private static final Logger log = Logger.getLogger(DblLinkDetect.class);
	
	
	public static void main(final String [] args) {
		
		
		final String file = "./networks/padang_net_v20080618.xml";
		final Config conf = Gbl.createConfig(null);
		final World world = Gbl.createWorld();

	log.info("loading network.");
	final NetworkFactory fc = new NetworkFactory();
	fc.setLinkPrototype(TimeVariantLinkImpl.class);
	
	final NetworkLayer network = new NetworkLayer(fc);
	new MatsimNetworkReader(network).readFile(file);
	world.setNetworkLayer(network);
	world.complete();
	log.info("done.");
	
	new NetworkSegmentDoubleLinks().run(network);
	
	new NetworkWriter(network,"converted.xml").write();
		
	}
}
