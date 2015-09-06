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

package org.matsim.contrib.analysis.filters.population;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class RouteLinkFilterTest extends MatsimTestCase {

	public void testRouteLinkFilter() {
		loadConfig(null); // used to set the default dtd-location
		Population population = getTestPopulation();

		TestAlgorithm tester = new TestAlgorithm();

		RouteLinkFilter linkFilter = new RouteLinkFilter(tester);
		linkFilter.addLink(Id.create(15, Link.class));

		SelectedPlanFilter selectedPlanFilter = new SelectedPlanFilter(linkFilter);
		selectedPlanFilter.run(population);
		assertEquals(3, population.getPersons().size());
		assertEquals(2, linkFilter.getCount());
	}

	private Population getTestPopulation() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile("test/scenarios/equil/network.xml");

		Link link1 = network.getLinks().get(Id.create(1, Link.class));
		Link link20 = network.getLinks().get(Id.create(20, Link.class));

		Population population = scenario.getPopulation();

		Person person;
		PlanImpl plan;
		LegImpl leg;
		NetworkRoute route;

		person = PersonImpl.createPerson(Id.create("1", Person.class));
		plan = PersonImpl.createAndAddPlan(person, true);
		ActivityImpl a = plan.createAndAddActivity("h", link1.getId());
		a.setEndTime(7.0 * 3600);
		leg = plan.createAndAddLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(link1.getId(), link20.getId());
		route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds("6 15"), link20.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", link20.getId());
		population.addPerson(person);

		person = PersonImpl.createPerson(Id.create("2", Person.class));
		plan = PersonImpl.createAndAddPlan(person, true);
		ActivityImpl a2 = plan.createAndAddActivity("h", link1.getId());
		a2.setEndTime(7.0 * 3600 + 5.0 * 60);
		leg = plan.createAndAddLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(link1.getId(), link20.getId());
		route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds("6 15"), link20.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", link20.getId());
		population.addPerson(person);

		person = PersonImpl.createPerson(Id.create("3", Person.class));
		plan = PersonImpl.createAndAddPlan(person, true);
		ActivityImpl a3 = plan.createAndAddActivity("h", link1.getId());
		a3.setEndTime(7.0 * 3600 + 10.0 * 60);
		leg = plan.createAndAddLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(link1.getId(), link20.getId());
		route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds("5 14"), link20.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", link20.getId());
		population.addPerson(person);

		return population;
	}

	/*package*/ static class TestAlgorithm implements PlanAlgorithm {

		@Override
		public void run(final Plan plan) {
			assertTrue("1".equals(plan.getPerson().getId().toString())
					|| "2".equals(plan.getPerson().getId().toString()));
		}

	}

}
