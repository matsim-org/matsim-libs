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

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.algorithms.NetworkSummary;
import org.matsim.core.network.algorithms.NetworkWriteAsTable;

public class CleanNetwork {

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(final String[] args) {

		System.out.println("RUN:");

//		Config config = Gbl.createConfig(null);
//		config.config().setOutputFile("output_config.xml");
		Scenario.setUpScenarioConfig();
		NetworkLayer network = Scenario.readNetwork();
//		Counts counts = Scenario.readCounts();
//		NetworkLayer network = new NetworkLayer();

//		Iterator<? extends Link> l_it = network.getLinks().values().iterator();
//		while (l_it.hasNext()) {
//			Link l = l_it.next();
//			if (l.getLength()<75.0) { l.setLength(75.0); }
//		}

		System.out.println("  running Network modules... ");
//		new NetworkParseETNet("../../input/nodes.txt","../../input/linksET.txt").run(network);
//		new NetworkCalibrationWithCounts("../../output/greentimes.xml",counts).run(network);
//		new NetworkSetDefaultCapacities().run(network);
		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);

		ArrayList<Link> links = new ArrayList<Link>();
		Iterator<? extends Link> l_it = network.getLinks().values().iterator();
		while (l_it.hasNext()) { Link l = l_it.next(); if (Integer.parseInt(l.getType())>48) { links.add(l); } }
		System.out.println("    removing " + links.size() + " links...");
		for (int i=0; i<links.size(); i++) { network.removeLink(links.get(i)); }
		System.out.println("    done.");

		NetworkCalcTopoType topoTypes = new NetworkCalcTopoType();
		topoTypes.run(network);
		new NetworkSummary().run(network);

		ArrayList<Node> nodes = new ArrayList<Node>();
		Iterator<? extends Node> n_it = network.getNodes().values().iterator();
		while (n_it.hasNext()) { Node n = n_it.next(); if (topoTypes.getTopoType(n) == NetworkCalcTopoType.EMPTY.intValue()) { nodes.add(n); } }
		System.out.println("    removing " + nodes.size() + " nodes...");
		for (int i=0; i<nodes.size(); i++) { network.removeNode(nodes.get(i)); }
		System.out.println("    done.");

		new NetworkCalcTopoType().run(network);
		new NetworkSummary().run(network);

//		new NetworkSimplifyAttributes().run(network);
//		new NetworkAdaptCHNavtec().run(network);
		new NetworkCleaner().run(network);
		new NetworkMergeDoubleLinks().run(network);
		new NetworkCalcTopoType().run(network);
//		new NetworkTransform(new CH1903LV03toWGS84()).run(network);
		new NetworkSummary().run(network);
		System.out.println("  done.");

//		NetworkWriteETwithCounts nwetwc = new NetworkWriteETwithCounts(Counts.getSingleton());
//		nwetwc.run(network);
//		nwetwc.close();
		NetworkWriteAsTable nwat = new NetworkWriteAsTable(Scenario.output_directory);
		nwat.run(network);
		nwat.close();

		Scenario.writeNetwork(network);
//		Scenario.writeConfig();

		System.out.println("RUN: cleanNetwork finished.");
		System.out.println();
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
