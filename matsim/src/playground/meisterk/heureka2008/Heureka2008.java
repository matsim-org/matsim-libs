/* *********************************************************************** *
 * project: org.matsim.*
 * Heureka2008.java
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

package playground.meisterk.heureka2008;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Plans;

import playground.meisterk.MyRuns;

public class Heureka2008 {

	public static void analyseInitialTimes() {
		
		// initialize scenario with events from a given events file
		// - network
		NetworkLayer network = MyRuns.initWorldNetwork();
		// - population
		Plans matsimAgentPopulation = MyRuns.initMatsimAgentPopulation(Plans.USE_STREAMING, null);
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Gbl.createConfig(args);
		Heureka2008.analyseInitialTimes();
		
	}

}
