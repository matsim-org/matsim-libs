/* *********************************************************************** *
 * project: org.matsim.*
 * CleanNetwork.java
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

package playground.balmermi;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class CleanNetwork {

	private static final Logger log = Logger.getLogger(CleanNetwork.class);

	//////////////////////////////////////////////////////////////////////
	// member methods
	//////////////////////////////////////////////////////////////////////
	
	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(String[] args) {

		args = new String[] {
				"D:/tmp/proc0019/network-reducedChanged-cleaned/network-reducedChanged-cleaned.xml.gz",
				"D:/tmp/proc0019/network-reducedChanged-cleaned",
		};

		if (args.length != 2) {
			log.error(CleanNetwork.class.getCanonicalName()+" networkFile outputBase");
			System.exit(-1);
		}

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(args[0]);
		
//		NetworkImpl subNetwork = new ScenarioImpl().getNetwork();
//		Set<String> modes = new HashSet<String>();
//		modes.add(TransportMode.car);
//		new TransportModeNetworkFilter(scenario.getNetwork()).filter(subNetwork,modes);
//		new NetworkCleaner().run(subNetwork);
//		for (Link l : scenario.getNetwork().getLinks().values()) {
//			LinkImpl l2 = (LinkImpl)subNetwork.getLinks().get(l.getId());
//			if (l2 != null) {
//				l2.setOrigId(((LinkImpl) l).getOrigId());
//				l2.setType(((LinkImpl) l).getType());
//			}
//		}
//		new NetworkDoubleLinks("-dl").run(subNetwork);
//		new NetworkAdaptLength().run(subNetwork);

//		new NetworkWriteAsTable(args[1]).run(subNetwork);
//		new NetworkWriter(subNetwork).write(args[1]+"/network.xml.gz");

		new NetworkWriteAsTable(args[1],5.0).run(scenario.getNetwork());
		new NetworkWriter(scenario.getNetwork()).write(args[1]+"/network.xml.gz");
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

		cleanNetwork(args);

		Gbl.printElapsedTime();
	}
}
