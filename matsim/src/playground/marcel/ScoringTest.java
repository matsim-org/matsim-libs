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
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.PlanScorer;

public class ScoringTest {

	private void test1() {

		PlanScorer scorer = new PlanScorer(new CharyparNagelScoringFunctionFactory());

		Gbl.createConfig(new String[] {"../mystudies/scoringtest/config.xml"});
		QueueNetworkLayer network = new QueueNetworkLayer();
		new MatsimNetworkReader(network).readFile("examples/equil/network.xml");
		Gbl.getWorld().setNetworkLayer(network);
		Plans population = new Plans(Plans.NO_STREAMING);
		new MatsimPlansReader(population).readFile("../mystudies/scoringtest/plans.xml");

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
