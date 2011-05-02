/* *********************************************************************** *
 * project: org.matsim.*
 * PopComplementer.java
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * complements the {@code Population}, that each has less than maxPlansPerAgents
 * {@code Plan}s, so that every person has maxPlansPerAgents {@code Plan}s
 *
 * @author yu
 *
 */
public class PopComplementer extends NewPopulation {
	private final int maxPlansPerAgent;

	/**
	 * @param network
	 * @param population
	 * @param filename
	 */
	public PopComplementer(Network network, Population population,
			String filename, int maxPlansPerAgent) {
		super(network, population, filename);
		this.maxPlansPerAgent = maxPlansPerAgent;
	}

	@Override
	public void run(Person person) {
		int size = person.getPlans().size();
		while (size < maxPlansPerAgent) {
			person.addPlan(new RandomPlanSelector().selectPlan(person));
			size = person.getPlans().size();
		}
		pw.writePerson(person);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilename = "../../matsim/examples/equil/network.xml";
		String oldPopFilename = "test/input/10.plans.xml.gz";
		String newPopFilename = "test/input/10.plansFull.xml.gz";

		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl net = (NetworkImpl) s.getNetwork();
		new MatsimNetworkReader(s).readFile(netFilename);

		Population pop = s.getPopulation();
		new MatsimPopulationReader(s).readFile(oldPopFilename);

		PopComplementer pp = new PopComplementer(net, pop, newPopFilename, 4);
		pp.run(pop);
		pp.writeEndPlans();
	}
}
