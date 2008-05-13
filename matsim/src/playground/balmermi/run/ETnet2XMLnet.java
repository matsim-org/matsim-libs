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

package playground.balmermi.run;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.NetworkSummary;
import org.matsim.network.algorithms.NetworkWriteAsTable;

import playground.balmermi.Scenario;
import playground.balmermi.modules.ivtch.NetworkParseETNet;

public class ETnet2XMLnet {

	//////////////////////////////////////////////////////////////////////
	// run
	//////////////////////////////////////////////////////////////////////

	public static void run(String[] args) {

		System.out.println("RUN:");

		Scenario.input_directory = args[0];
		Scenario.output_directory = args[1];
		Scenario.setUpScenarioConfig();
		NetworkLayer network = new NetworkLayer();
		
		//////////////////////////////////////////////////////////////////////

		System.out.println("  running Network modules... ");
		new NetworkParseETNet(Scenario.input_directory+"nodes.txt",Scenario.input_directory+"linksET.txt").run(network);
		new NetworkSummary().run(network);
		System.out.println("  done.");

		//////////////////////////////////////////////////////////////////////

		NetworkWriteAsTable nwat = new NetworkWriteAsTable(Scenario.output_directory);
		nwat.run(network);
		nwat.close();
		Scenario.writeNetwork(network);

		//////////////////////////////////////////////////////////////////////

		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		Gbl.startMeasurement();
		Gbl.printElapsedTime();
		
		if (args.length != 2) { Gbl.errorMsg("\n\nUSAGE: ETnet2XMLnet input_dir/ output_dir/\n\n"); }

		run(args);

		Gbl.printElapsedTime();
	}
}
