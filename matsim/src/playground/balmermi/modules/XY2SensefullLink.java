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

package playground.balmermi.modules;

import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.network.algorithms.NetworkWriteAsTable;

import playground.balmermi.Scenario;

public class XY2SensefullLink {

	//////////////////////////////////////////////////////////////////////
	// run
	//////////////////////////////////////////////////////////////////////

	public static void run(String[] args) {

		System.out.println("RUN:");

//		Config config = Gbl.createConfig(null);
//		config.config().setOutputFile("output_config.xml");
		Scenario.setUpScenarioConfig();
		NetworkLayer network = Scenario.readNetwork();
//		NetworkLayer network = new NetworkLayer();

		System.out.println("  running network modules...");
		new NetworkSummary().run(network);

		System.out.println("    removing links with type < 30 (Highways and zoll) and >= 90 (trains and ships)...");
		ArrayList<Link> useless_links = new ArrayList<Link>();
		Iterator<? extends Link> l_it = network.getLinks().values().iterator();
		while (l_it.hasNext()) {
			Link l = l_it.next();
			int type = Integer.parseInt(l.getType());
			if ((type < 30) || (type >= 90)) { useless_links.add(l); }
		}
		System.out.println("    "+useless_links.size()+" links to remove.");
		for (int i=0; i<useless_links.size(); i++) {
			boolean ok = network.removeLink(useless_links.get(i));
			if (!ok) { Gbl.errorMsg("Something is wrong!"); }
		}
		System.out.println("    done.");

		new NetworkSummary().run(network);
		
		System.out.println("    removing nodes with no incident links...");
		ArrayList<Node> useless_nodes = new ArrayList<Node>();
		Iterator<? extends Node> n_it = network.getNodes().values().iterator();
		while (n_it.hasNext()) {
			Node n = n_it.next();
			if (n.getIncidentLinks().isEmpty()) { useless_nodes.add(n); }
		}
		System.out.println("    "+useless_nodes.size()+" nodes to remove.");
		for (int i=0; i<useless_nodes.size(); i++) {
			boolean ok = network.removeNode(useless_nodes.get(i));
			if (!ok) { Gbl.errorMsg("Something is wrong again!"); }
		}
		System.out.println("    done.");

		new NetworkSummary().run(network);
		NetworkWriteAsTable nwat = new NetworkWriteAsTable();
		nwat.run(network);
		nwat.close();
		System.out.println("  done.");

		Scenario.writeNetwork(network);

		System.out.println("RUN: cleanNetwork finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

		run(args);

		Gbl.printElapsedTime();
	}
}
