/* *********************************************************************** *
 * project: org.matsim.*
 * LSAGeneration.java
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

package playground.balmermi.lsa;

import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;

public class LSAGeneration {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void run() {

		System.out.println("run...");

//		Scenario.setUpScenarioConfig();
//		NetworkLayer network = Scenario.readNetwork();
		NetworkLayer network = null;

		//////////////////////////////////////////////////////////////////////
		
		NetworkCreateLSA nclsa = new NetworkCreateLSA(network);

		//////////////////////////////////////////////////////////////////////

//		Scenario.writeNetwork(network);
		
		System.out.println("done.");
		System.out.println();
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {

		Gbl.startMeasurement();

		run();

		Gbl.printElapsedTime();
	}
}
