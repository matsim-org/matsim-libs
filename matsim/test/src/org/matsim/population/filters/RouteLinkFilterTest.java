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
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.population.algorithms.PlanAlgorithm;
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

		Link link1 = network.getLink(new IdImpl(1));
		Link link20 = network.getLink(new IdImpl(20));
		
		Population population = new Population(Population.NO_STREAMING);

		Person person;
		Plan plan;
		Leg leg;
		Route route;

		person = new PersonImpl(new IdImpl("1"));
		plan = person.createPlan(true);
		Act a = plan.createAct("h", link1);
		a.setEndTime(7.0 * 3600);
		leg = plan.createLeg(Mode.car);
		route = new Route();
		route.setRoute("2 7 12");
		leg.setRoute(route);
		plan.createAct("w", link20);
		population.addPerson(person);

		person = new PersonImpl(new IdImpl("2"));
		plan = person.createPlan(true);
		Act a2 = plan.createAct("h", link1);
		a2.setEndTime(7.0 * 3600 + 5.0 * 60);
		leg = plan.createLeg(Mode.car);
		route = new Route();
		route.setRoute("2 7 12");
		leg.setRoute(route);
		plan.createAct("w", link20);
		population.addPerson(person);

		person = new PersonImpl(new IdImpl("3"));
		plan = person.createPlan(true);
		Act a3 = plan.createAct("h", link1);
		a3.setEndTime(7.0 * 3600 + 10.0 * 60);
		leg = plan.createLeg(Mode.car);
		route = new Route();
		route.setRoute("2 6 12");
		leg.setRoute(route);
		plan.createAct("w", link20);
		population.addPerson(person);

		return population;
	}

	/*package*/ static class TestAlgorithm implements PlanAlgorithm {

		public void run(final Plan plan) {
			assertTrue("1".equals(plan.getPerson().getId().toString())
					|| "2".equals(plan.getPerson().getId().toString()));
		}

	}
}
