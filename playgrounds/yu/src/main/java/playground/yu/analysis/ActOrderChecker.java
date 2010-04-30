/* *********************************************************************** *
 * project: org.matsim.*
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.yu.utils.io.SimpleWriter;

/**
 * @author yu
 *
 */
public class ActOrderChecker extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	// ------------------------------------------------------------------------
	public static class ActOder {
		public static String getActOder(final Plan plan) {
			StringBuffer acts = new StringBuffer();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					acts.append(((Activity) pe).getType());
				}
			}
			return acts.toString();
		}
	}

	// --------------------------------------------------------------------------
	private Id personId;
	private final Map<Id, String> actsMap = new HashMap<Id, String>();

	public Map<Id, String> getActsMap() {
		return this.actsMap;
	}

	@Override
	public void run(final Person person) {
		this.personId = person.getId();
		run(person.getSelectedPlan());
	}

	public void run(final Plan plan) {
		this.actsMap.put(this.personId, ActOder.getActOder(plan));
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(final String[] args) throws IOException {
		Gbl.startMeasurement();

		final String netFilename = args[0];
		final String plansFilenameA = args[1];
		final String plansFilenameB = args[2];
		final String outputFilename = args[3];

//		Gbl.createConfig(null);

		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		Population populationA = scenario.getPopulation();
		new MatsimPopulationReader(scenario).readFile(plansFilenameA);
		ActOrderChecker aocA = new ActOrderChecker();
		aocA.run(populationA);

		ScenarioImpl scenarioB = new ScenarioImpl();
		scenarioB.setNetwork(scenario.getNetwork());
		Population populationB = scenarioB.getPopulation();
		ActOrderChecker aocB = new ActOrderChecker();
		new MatsimPopulationReader(scenarioB).readFile(plansFilenameB);
		aocB.run(populationB);

		SimpleWriter writer = new SimpleWriter(outputFilename);
		writer.writeln("personId\toriginal ActChain\tcurrent ActChain");
		Map<Id, String> actsA = aocA.getActsMap();
		Map<Id, String> actsB = aocB.getActsMap();
		int c = 0, changed = 0;
		for (Entry<Id, String> personEntry : actsA.entrySet()) {
			c++;
			String personId = personEntry.getKey().toString();
			String actChainA = personEntry.getValue();
			String actChainB = actsB.get(personEntry.getKey());
			if (!actChainA.equals(actChainB)) {
				writer.writeln(personId + "\t" + actChainA + "\t"
						+ actChainB);
				changed++;
			}
			writer.writeln(personId + "\t" + actChainA + "\t" + actChainB);
			writer.writeln("agents :\t" + c + "\tchanged :\t" + changed);
			writer.close();
		}

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}
}
