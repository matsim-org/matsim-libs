/* *********************************************************************** *
 * project: org.matsim.*
 * PopWithSameScores.java
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

/**
 * 
 */
package playground.yu.newPlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;

/**
 * @author yu
 * 
 */
public class PopWithSameScores extends NewPopulation {
	private double score;

	/**
	 * @param network
	 * @param plans
	 */
	public PopWithSameScores(Network network, Population plans, double score) {
		super(network, plans);
		this.score = score;
	}

	/**
	 * @param network
	 * @param population
	 * @param filename
	 *            for the ouput-popfile
	 */
	public PopWithSameScores(Network network, Population population,
			String filename, double score) {
		super(network, population, filename);
		this.score = score;
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			plan.setScore(this.score);
		}
		this.pw.writePerson(person);
	}

	public static void main(String[] args) {
		String netFilename = "../schweiz-ivtch-SVN/baseCase/network/ivtch.xml", //
		popFilename = "../runs-svn/run663/it.500/500.plans4plans.xml.gz", //
		newPopFilename = "../runs-svn/run663/it.500/500.plans4plansScore0.xml.gz";

		Scenario sc = new ScenarioImpl();

		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(netFilename);

		Population pop = sc.getPopulation();
		new MatsimPopulationReader(sc).readFile(popFilename);

		PopWithSameScores pwss = new PopWithSameScores(net, pop,
				newPopFilename, 0);
		pwss.run(pop);
		pwss.writeEndPlans();
	}
}
