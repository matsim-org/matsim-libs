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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class RouteLinkFilterTest extends MatsimTestCase {

	public void testRouteLinkFilter() {
		loadConfig(null); // used to set the default dtd-location
		PopulationImpl population = getTestPopulation();

		TestAlgorithm tester = new TestAlgorithm();

		RouteLinkFilter linkFilter = new RouteLinkFilter(tester);
		linkFilter.addLink(new IdImpl(15));

		SelectedPlanFilter selectedPlanFilter = new SelectedPlanFilter(linkFilter);
		selectedPlanFilter.run(population);
		assertEquals(3, population.getPersons().size());
		assertEquals(2, linkFilter.getCount());
	}

	private PopulationImpl getTestPopulation() {
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("test/scenarios/equil/network.xml");

		LinkImpl link1 = network.getLink(new IdImpl(1));
		LinkImpl link20 = network.getLink(new IdImpl(20));

		PopulationImpl population = new PopulationImpl();

		PersonImpl person;
		PlanImpl plan;
		LegImpl leg;
		NetworkRouteWRefs route;

		person = new PersonImpl(new IdImpl("1"));
		plan = person.createAndAddPlan(true);
		ActivityImpl a = plan.createAndAddActivity("h", link1);
		a.setEndTime(7.0 * 3600);
		leg = plan.createAndAddLeg(TransportMode.car);
		route = (NetworkRouteWRefs) network.getFactory().createRoute(TransportMode.car, link1, link20);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 7 12"), link20);
		leg.setRoute(route);
		plan.createAndAddActivity("w", link20);
		population.getPersons().put(person.getId(), person);

		person = new PersonImpl(new IdImpl("2"));
		plan = person.createAndAddPlan(true);
		ActivityImpl a2 = plan.createAndAddActivity("h", link1);
		a2.setEndTime(7.0 * 3600 + 5.0 * 60);
		leg = plan.createAndAddLeg(TransportMode.car);
		route = (NetworkRouteWRefs) network.getFactory().createRoute(TransportMode.car, link1, link20);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 7 12"), link20);
		leg.setRoute(route);
		plan.createAndAddActivity("w", link20);
		population.getPersons().put(person.getId(), person);

		person = new PersonImpl(new IdImpl("3"));
		plan = person.createAndAddPlan(true);
		ActivityImpl a3 = plan.createAndAddActivity("h", link1);
		a3.setEndTime(7.0 * 3600 + 10.0 * 60);
		leg = plan.createAndAddLeg(TransportMode.car);
		route = (NetworkRouteWRefs) network.getFactory().createRoute(TransportMode.car, link1, link20);
		route.setNodes(link1, NetworkUtils.getNodes(network, "2 6 12"), link20);
		leg.setRoute(route);
		plan.createAndAddActivity("w", link20);
		population.getPersons().put(person.getId(), person);

		return population;
	}

	/*package*/ static class TestAlgorithm implements PlanAlgorithm {

		public void run(final PlanImpl plan) {
			assertTrue("1".equals(plan.getPerson().getId().toString())
					|| "2".equals(plan.getPerson().getId().toString()));
		}

	}


}
