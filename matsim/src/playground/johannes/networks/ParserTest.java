/* *********************************************************************** *
 * project: org.matsim.*
 * ParserTest.java
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

/**
 * 
 */
package playground.johannes.networks;

import java.util.List;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;

/**
 * @author illenberger
 *
 */
public class ParserTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = Gbl.createConfig(new String[]{"/Users/fearonni/vsp-work/eut/corridor/config/config.xml"});
		String networkFile = "/Users/fearonni/vsp-work/eut/corridor/data/net.2.xml";
		NetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile(networkFile);
		
		NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network);
//		List<NetworkChangeEvent> events = parser.parseEvents("/Users/fearonni/workspace/matsim_sf/output/netevents.xml");
		List<NetworkChangeEvent> events = parser.parseEvents("/Users/fearonni/workspace/matsim_sf/test/input/org/matsim/network/testNetworkChangeEvents.xml");
		NetworkChangeEventWriter writer = new NetworkChangeEventWriter();
		writer.write("output/netevents.xml", events);

	}

}
