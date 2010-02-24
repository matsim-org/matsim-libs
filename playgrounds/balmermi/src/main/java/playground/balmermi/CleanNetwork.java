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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkAdaptLength;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;
import org.matsim.counts.MatsimCountsReader;

import playground.balmermi.modules.NetworkDoubleLinks;
import playground.balmermi.modules.NetworkShiftFreespeed;
import playground.balmermi.modules.NetworkThinner;

public class CleanNetwork {

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(final String[] args) {

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile("../../input/network.xml.gz");
		
		Counts counts = new Counts();
		new MatsimCountsReader(counts).readFile("../../input/counts.xml.gz");

		new NetworkAdaptLength().run(network);
		new NetworkDoubleLinks("-dl").run(network);
		new NetworkThinner().run(network,counts);
		new NetworkCleaner().run(network);

		new NetworkShiftFreespeed().run(network);
		
		for (LinkImpl l : network.getLinks().values()) {
			if (l.getType().startsWith("2-") || l.getType().startsWith("3-")) {
				if (l.getNumberOfLanes(Time.UNDEFINED_TIME) == 2) {
					l.setNumberOfLanes(1);
					l.setCapacity(2000);
				}
				if (l.getNumberOfLanes(Time.UNDEFINED_TIME) > 1) {
					System.out.println(l.toString());
				}
			}
		}
		
		NetworkWriteAsTable nwat = new NetworkWriteAsTable("../../output/");
		nwat.run(network);
		
		new CountsWriter(counts).writeFile("../../output/output_counts.xml.gz");
		new NetworkWriter(network).writeFile("../../output/output_network.xml.gz");
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
