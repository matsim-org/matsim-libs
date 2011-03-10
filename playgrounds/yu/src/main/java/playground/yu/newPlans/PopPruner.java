/* *********************************************************************** *
 * project: org.matsim.*
 * PopPruner.java
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
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

/**
 * deletes the spilth {@code org.matsim.api.core.v01.population.Plan}s, which is
 * more than the maxPlansPerAgent (default 4)
 * 
 * @author yu
 * 
 */
public class PopPruner extends NewPopulation {
	private final int maxPlansPerAgent;
	private final WorstPlanForRemovalSelector worstPlanSelector;

	public PopPruner(Network net, Population population, String filename,
			int maxPlansPerAgent) {
		super(net, population, filename);
		this.maxPlansPerAgent = maxPlansPerAgent;
		this.worstPlanSelector = new WorstPlanForRemovalSelector();
	}

	public PopPruner(Network net, Population population, String filename) {
		this(net, population, filename, 4/* default maxPlansPerAgent */);
	}

	@Override
	public void run(Person person) {
		int size = person.getPlans().size();
		while (size > this.maxPlansPerAgent) {
			person.getPlans().remove(this.worstPlanSelector.selectPlan(person));
			size = person.getPlans().size();
		}
		this.pw.writePerson(person);
	}

	public static void main(String args[]) {
		System.out.println("----->>>>>----->>>>>");

		String netFilename = "../../matsim/examples/equil/network.xml";
		String oldPopFilename = "../../matsim/output/equil_tta/output_plans.xml.gz";
		String newPopFilename = "../../matsim/output/equil_tta/output_plans4.xml.gz";

		System.out.println("net\t:\t" + netFilename);

		Scenario s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		NetworkImpl net = (NetworkImpl) s.getNetwork();
		new MatsimNetworkReader(s).readFile(netFilename);

		Population pop = s.getPopulation();
		new MatsimPopulationReader(s).readFile(oldPopFilename);

		PopPruner pp = new PopPruner(net, pop, newPopFilename);
		pp.run(pop);
		pp.writeEndPlans();
	}
}
