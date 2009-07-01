/* *********************************************************************** *
 * project: org.matsim.*
 * WriteNetworkAsTable.java
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

import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.algorithms.NetworkSummary;

import playground.balmermi.algos.NetworkWriteVolumesAsTable;

public class WriteNetworkAsTable {

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(String[] args) {

		System.out.println("RUN: cleanNetwork");

		ScenarioLoader sl = new ScenarioLoader(args[0]);
		sl.loadNetwork();
		Network network = sl.getScenario().getNetwork();

		System.out.println("  running Network Validation and cleaning algorithms... ");
		new NetworkSummary().run(network);
		NetworkWriteVolumesAsTable nwvat = new NetworkWriteVolumesAsTable();
		nwvat.run(network);
		nwvat.close();
		new NetworkSummary().run(network);
		System.out.println("  done.");

		System.out.println("RUN: cleanNetwork finished.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();

		cleanNetwork(args);

		Gbl.printElapsedTime();
	}
}
