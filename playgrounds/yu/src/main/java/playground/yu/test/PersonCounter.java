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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.ScenarioLoaderImpl;
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
		this.cnt = 0;
		this.nullCnt = 0;
	}

	@Override
	public void run(final Person person) {
		if (person != null)
			this.cnt++;
		else
			this.nullCnt++;
	}

	@Override
	public String toString() {
		return "There are " + this.cnt + " persons and " + this.nullCnt
				+ " (null)persons";
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String netFilename = "./test/yu/test/input/network.xml";
		final String plansFilename = "./test/yu/test/input/10pctZrhCarPtPlans.xml.gz";
		new ScenarioLoaderImpl("./test/yu/test/configTest.xml").loadScenario()
				.getConfig();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);

		PopulationImpl population = new PopulationImpl();
		PersonCounter pc = new PersonCounter();
		PopulationReader plansReader = new MatsimPopulationReader(population,
				network);
		plansReader.readFile(plansFilename);
		pc.run(population);
		System.out.println(pc.toString());
	}
}
