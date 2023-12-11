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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

public class RouteLinkFilterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testRouteLinkFilter() {
		// used to set the default dtd-location
		utils.loadConfig((String)null);
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
		new MatsimNetworkReader(scenario.getNetwork()).parse(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml"));

		Link link1 = network.getLinks().get(Id.create(1, Link.class));
		Link link20 = network.getLinks().get(Id.create(20, Link.class));

		Population population = scenario.getPopulation();

		Person person;
		Plan plan;
		Leg leg;
		NetworkRoute route;

		person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		plan = PersonUtils.createAndAddPlan(person, true);
		Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
		a.setEndTime(7.0 * 3600);
		leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		route = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link20.getId());
		route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds("6 15"), link20.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link20.getId());
		population.addPerson(person);

		person = PopulationUtils.getFactory().createPerson(Id.create("2", Person.class));
		plan = PersonUtils.createAndAddPlan(person, true);
		Activity a2 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
		a2.setEndTime(7.0 * 3600 + 5.0 * 60);
		leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		route = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link20.getId());
		route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds("6 15"), link20.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link20.getId());
		population.addPerson(person);

		person = PopulationUtils.getFactory().createPerson(Id.create("3", Person.class));
		plan = PersonUtils.createAndAddPlan(person, true);
		Activity a3 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
		a3.setEndTime(7.0 * 3600 + 10.0 * 60);
		leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		route = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link20.getId());
		route.setLinkIds(link1.getId(), NetworkUtils.getLinkIds("5 14"), link20.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link20.getId());
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
