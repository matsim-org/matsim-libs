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
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkCalcTopoType;
import org.matsim.network.algorithms.NetworkCleaner;
import org.matsim.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.network.algorithms.NetworkTransform;
import org.matsim.network.algorithms.NetworkWriteAsTable;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.balmermi.modules.ivtch.NetworkParseETNet;

public class CleanNetwork {

	//////////////////////////////////////////////////////////////////////
	// cleanNetwork
	//////////////////////////////////////////////////////////////////////

	public static void cleanNetwork(String[] args) {

		System.out.println("RUN:");

//		Config config = Gbl.createConfig(null);
//		config.config().setOutputFile("output_config.xml");
		Scenario.setUpScenarioConfig();
		NetworkLayer network = Scenario.readNetwork();
//		Counts counts = Scenario.readCounts();
//		NetworkLayer network = new NetworkLayer();

		System.out.println("  running Network modules... ");
//		new NetworkParseETNet("../../input/nodes.txt","../../input/linksET.txt").run(network);
//		new NetworkCalibrationWithCounts("../../output/greentimes.xml",counts).run(network);
//		new NetworkSetDefaultCapacities().run(network);
//		new NetworkSummary().run(network);
//		new NetworkSimplifyAttributes().run(network);
//		new NetworkAdaptCHNavtec().run(network);
//		new NetworkCleaner().run(network);
//		new NetworkMergeDoubleLinks().run(network);
//		new NetworkCalcTopoType().run(network);
		new NetworkTransform(new CH1903LV03toWGS84()).run(network);
//		new NetworkSummary().run(network);
		System.out.println("  done.");

//		NetworkWriteETwithCounts nwetwc = new NetworkWriteETwithCounts(Counts.getSingleton());
//		nwetwc.run(network);
//		nwetwc.close();
//		NetworkWriteAsTable nwat = new NetworkWriteAsTable(Scenario.output_directory);
//		nwat.run(network);
//		nwat.close();

		Scenario.writeNetwork(network);
//		Scenario.writeConfig();

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
