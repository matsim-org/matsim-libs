/* *********************************************************************** *
 * project: org.matsim.*
 * PersonCounterTest.java
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

/**
 *
 */
package playground.yu.test;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;

/**
 * @author ychen
 * 
 */
public class PersonCounter extends AbstractPersonAlgorithm {
	private int cnt, nullCnt;

	/**
	 * 
	 */
	public PersonCounter() {
		cnt = 0;
		nullCnt = 0;
	}

	@Override
	public void run(final Person person) {
		if (person != null)
			cnt++;
		else
			nullCnt++;
	}

	@Override
	public String toString() {
		return "There are " + cnt + " persons and " + nullCnt
				+ " (null)persons";
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename = "./test/yu/test/input/network.xml";
		final String plansFilename = "./test/yu/test/input/10pctZrhCarPtPlans.xml.gz";
		Gbl.createConfig(new String[] { "./test/yu/test/configTest.xml" });

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		Population population = new Population();
		PersonCounter pc = new PersonCounter();
		population.addAlgorithm(pc);
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);
		population.runAlgorithms();
		System.out.println(pc.toString());
	}
}
