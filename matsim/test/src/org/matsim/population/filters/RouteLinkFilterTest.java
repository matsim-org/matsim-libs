/* *********************************************************************** *
 * project: org.matsim.*
 * RouteLinkFilterTest.java.java
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

package org.matsim.population.filters;

import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.filters.RouteLinkFilter;
import org.matsim.population.filters.SelectedPlanFilter;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.World;

public class RouteLinkFilterTest extends MatsimTestCase {

	public void testRouteLinkFilter() {
		loadConfig(null); // used to set the default dtd-location
		Population population = getTestPopulation(Gbl.createWorld());

		TestAlgorithm tester = new TestAlgorithm();

		RouteLinkFilter linkFilter = new RouteLinkFilter(tester);
		linkFilter.addLink(new IdImpl(15));

		SelectedPlanFilter selectedPlanFilter = new SelectedPlanFilter(linkFilter);
		selectedPlanFilter.run(population);
		assertEquals(3, population.getPersons().size());
		assertEquals(2, linkFilter.getCount());
	}

	private Population getTestPopulation(final
			World world) {
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("test/scenarios/equil/network.xml");
		world.setNetworkLayer(network);

		Population population = new Population(Population.NO_STREAMING);

		Person person;
		Plan plan;
		Leg leg;
		Route route;
		try {
			person = new Person(new IdImpl("1"));
			plan = person.createPlan(true);
			plan.createAct("h", (String)null, null, "1", null, "07:00:00", null, null);
			leg = plan.createLeg("car", "07:00:00", null, null);
			route = new Route();
			route.setRoute("2 7 12");
			leg.setRoute(route);
			plan.createAct("w", (String)null, null, "20", "08:00:00", null, null, null);
			population.addPerson(person);

			person = new Person(new IdImpl("2"));
			plan = person.createPlan(true);
			plan.createAct("h", (String)null, null, "1", null, "07:05:00", null, null);
			leg = plan.createLeg("car", "07:05:00", null, null);
			route = new Route();
			route.setRoute("2 7 12");
			leg.setRoute(route);
			plan.createAct("w", (String)null, null, "20", "08:05:00", null, null, null);
			population.addPerson(person);

			person = new Person(new IdImpl("3"));
			plan = person.createPlan(true);
			plan.createAct("h", (String)null, null, "1", null, "07:10:00", null, null);
			leg = plan.createLeg("car", "07:10:00", null, null);
			route = new Route();
			route.setRoute("2 6 12");
			leg.setRoute(route);
			plan.createAct("w", (String)null, null, "20", "08:10:00", null, null, null);
			population.addPerson(person);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return population;
	}

	/*package*/ static class TestAlgorithm implements PlanAlgorithm {

		public void run(final Plan plan) {
			assertTrue("1".equals(plan.getPerson().getId().toString())
					|| "2".equals(plan.getPerson().getId().toString()));
		}

	}
}
