/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioParsing.java
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

package playground.balmermi.similarities;

import java.util.Date;

import org.matsim.network.NetworkLayer;

import playground.balmermi.Scenario;

public class RouteSetAnalysis {

	//////////////////////////////////////////////////////////////////////
	// test run 01
	//////////////////////////////////////////////////////////////////////

	public static void dilZhFilter() {

		System.out.println("running dilZhFilter... " + (new Date()));
		
		Scenario.setUpScenarioConfig();
//		World world = Scenario.readWorld();
//		Facilities facilities = Scenario.readFacilities();
		NetworkLayer network = Scenario.readNetwork();
//		Counts counts = Scenario.readCounts();
//		Matrices matrices = Scenario.readMatrices();
//		Plans plans = Scenario.readPlans();

		//////////////////////////////////////////////////////////////////////
		
		new NetworkAnalyseRouteSet().run(network);
		
		//////////////////////////////////////////////////////////////////////
		
//		Scenario.writePlans(plans);
//		Scenario.writeMatrices(matrices);
//		Scenario.writeCounts(counts);
		Scenario.writeNetwork(network);
//		Scenario.writeFacilities(facilities);
//		Scenario.writeWorld(world);
		Scenario.writeConfig();

		System.out.println("done. " + (new Date()));
	}

	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		dilZhFilter();
	}
}
