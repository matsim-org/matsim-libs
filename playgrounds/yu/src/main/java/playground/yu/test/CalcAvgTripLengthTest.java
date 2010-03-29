/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAvgTripLengthTest.java
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

package playground.yu.test;

import org.matsim.analysis.CalcAverageTripLength;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;

import playground.yu.analysis.MyCalcAverageTripLength;

public class CalcAvgTripLengthTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch-osm.xml";
		String popFilename = "../matsimTests/Calibration/fi4/1800.plans.xml.gz";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer net = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		PopulationImpl pop = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(popFilename);

		CalcAverageTripLength catl = new MyCalcAverageTripLength(net);
		catl.run(pop);
		System.out.println("avg. trip length :\t" + catl.getAverageTripLength()
				+ " [m]");
	}

}
