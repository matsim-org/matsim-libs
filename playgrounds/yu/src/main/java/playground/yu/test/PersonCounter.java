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

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
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

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimConfigReader(scenario.getConfig()).readFile("./test/yu/test/configTest.xml");

		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population population = scenario.getPopulation();
		PopulationReader plansReader = new MatsimPopulationReader(scenario);
		plansReader.readFile(plansFilename);

		PersonCounter pc = new PersonCounter();
		pc.run(population);
		System.out.println(pc.toString());
	}
}
