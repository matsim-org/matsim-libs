/* *********************************************************************** *
 * project: org.matsim.*
 * PlanFileCompare
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.tests;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;


/**
 * @author dgrether
 *
 */
public class PlanFileCompare {

	public void comparePlans(String networkFile, String plansFile1, String plansFile2) {
		ScenarioImpl scenario1 = new ScenarioImpl();
		
		NetworkLayer network = scenario1.getNetwork();
		MatsimNetworkReader netReader = new MatsimNetworkReader(network);
		netReader.readFile(networkFile);

		Population pop1 = scenario1.getPopulation();
		MatsimPopulationReader reader1 = new MatsimPopulationReader(scenario1);
		reader1.readFile(plansFile1);
		
		ScenarioImpl scenario2 = new ScenarioImpl();
		scenario2.setNetwork(scenario1.getNetwork());
		Population pop2 = scenario2.getPopulation();
		MatsimPopulationReader reader2 = new MatsimPopulationReader(scenario2);
		reader2.readFile(plansFile2);
		
		
		
		
	}

}
