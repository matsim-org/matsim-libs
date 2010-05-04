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

import java.util.EnumSet;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;

public class CleanNetwork {

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(final String[] args) {

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(args[0]);

//		Counts counts = new Counts();
//		new MatsimCountsReader(counts).readFile("../../input/counts.xml.gz");

		NetworkLayer subNet = new NetworkLayer();

		new TransportModeNetworkFilter(network).filter(subNet,EnumSet.of(TransportMode.car));
//		new NetworkAdaptLength().run(network);
//		new NetworkDoubleLinks("-dl").run(network);
//		new NetworkThinner().run(network,counts);
		new NetworkCleaner().run(subNet);

		for (Link l : network.getLinks().values()) {
			LinkImpl l2 = (LinkImpl) subNet.getLinks().get(l.getId());
			if (l2 != null) {
				l2.setOrigId(((LinkImpl) l).getOrigId());
				l2.setType(((LinkImpl) l).getType());
			}
		}

//		new NetworkShiftFreespeed().run(network);
//
//		for (LinkImpl l : network.getLinks().values()) {
//			if (l.getType().startsWith("2-") || l.getType().startsWith("3-")) {
//				if (l.getNumberOfLanes(Time.UNDEFINED_TIME) == 2) {
//					l.setNumberOfLanes(1);
//					l.setCapacity(2000);
//				}
//				if (l.getNumberOfLanes(Time.UNDEFINED_TIME) > 1) {
//					System.out.println(l.toString());
//				}
//			}
//		}

		NetworkWriteAsTable nwat = new NetworkWriteAsTable(args[1]);
		nwat.run(subNet);

//		new CountsWriter(counts).writeFile("../../output/output_counts.xml.gz");
		new NetworkWriter(subNet).write(args[1]+"/network.xml.gz");
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
