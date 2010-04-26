/* *********************************************************************** *
 * project: org.matsim.*
 * CATLTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.analysis;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;

public class CATLTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ScenarioImpl si = new ScenarioImpl();
		NetworkImpl ni = si.getNetwork();
		new MatsimNetworkReader(si)
				.readFile("../matsim/examples/equil/network.xml");
		Population pop = si.getPopulation();
		new MatsimPopulationReader(si).readFile("../matsim/output/equil_P2/ITERS/it.10/10.plans.xml.gz");

		new MyCalcAverageTripLength(ni).run(pop);
	}

}
