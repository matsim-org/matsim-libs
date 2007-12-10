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

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.network.algorithms.NetworkTransform;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;

public class CleanNetwork {

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(String[] args) {

		System.out.println("RUN: cleanNetwork");

		Scenario.setUpScenarioConfig();

//		Gbl.createConfig(args);

		System.out.println("  reading the network...");
		NetworkLayer network = null;
		NetworkLayerBuilder.setNetworkLayerType(NetworkLayerBuilder.NETWORK_DEFAULT);
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE,null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		System.out.println("  done.");

		System.out.println("  running Network Validation and cleaning algorithms... ");
//		network.addAlgorithm(new NetworkSummary());
//		network.addAlgorithm(new NetworkAdaptCHNavtec());
//		network.addAlgorithm(new NetworkSummary());
//		network.addAlgorithm(new NetworkCleaner(false));
//		network.addAlgorithm(new NetworkSummary());
//		network.addAlgorithm(new NetworkMergeDoubleLinks());
//		network.addAlgorithm(new NetworkSummary());
//		network.addAlgorithm(new NetworkCalcTopoType());
//		network.addAlgorithm(new NetworkSummary());
//		NetworkWriteAsTable nwat = new NetworkWriteAsTable();
//		network.addAlgorithm(nwat);
//		network.addAlgorithm(new NetworkSummary());
//		network.runAlgorithms();
//		nwat.close();

		new NetworkSummary().run(network);
		new NetworkTransform(new CH1903LV03toWGS84()).run(network);
		new NetworkSummary().run(network);
		System.out.println("  done.");

//		System.out.println("  writing the network...");
//		NetworkWriter network_writer = new NetworkWriter(network);
//		network_writer.write();
//		System.out.println("  done.");
//
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

		cleanNetwork(args);

		Gbl.printElapsedTime();
	}
}
