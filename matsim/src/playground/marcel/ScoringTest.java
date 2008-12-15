/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringTest.java.java
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

package playground.marcel;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.PlanScorer;

public class ScoringTest {

	private void test1() {

		PlanScorer scorer = new PlanScorer(new CharyparNagelScoringFunctionFactory());

		Gbl.createConfig(new String[] {"../mystudies/scoringtest/config.xml"});
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("examples/equil/network.xml");
		Gbl.createWorld().setNetworkLayer(network);
		Gbl.getWorld().complete();
		Population population = new Population(Population.NO_STREAMING);
		new MatsimPopulationReader(population).readFile("../mystudies/scoringtest/plans.xml");

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getPlans().get(0);
			System.out.println(scorer.getScore(plan));
		}

	}

	public static void main(final String[] args) {
		ScoringTest tester = new ScoringTest();
		tester.test1();
	}

}
