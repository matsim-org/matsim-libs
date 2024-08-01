/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationWriterHandlerImplV4Test.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.population.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.testcases.MatsimTestUtils;

public class PopulationWriterHandlerImplV4Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testWriteGenericRoute() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(utils.loadConfig((String)null));
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile("test/scenarios/equil/network.xml");
		Link link1 = network.getLinks().get(Id.create(1, Link.class));
		Link link2 = network.getLinks().get(Id.create(2, Link.class));

		Scenario tmpScenario = new ScenarioBuilder(ConfigUtils.createConfig()).setNetwork(network).build() ;
		Population pop = tmpScenario.getPopulation();
		PopulationFactory pb = pop.getFactory();
		Person person = pb.createPerson(Id.create(1, Person.class));
		Plan plan = (Plan) pb.createPlan();
		plan.setPerson(person);
		plan.addActivity(pb.createActivityFromLinkId("h", link1.getId()));
		Leg leg = pb.createLeg("undefined");
		Route route = RouteUtils.createGenericRouteImpl(link1.getId(), link2.getId());
		route.setTravelTime(123);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(pb.createActivityFromLinkId("h", Id.create(1, Link.class)));
		person.addPlan(plan);
		pop.addPerson(person);

		String filename = utils.getOutputDirectory() + "population.xml";
		new PopulationWriter(pop, network).writeV4(filename);

		Population pop2 = scenario.getPopulation();
		new PopulationReader(scenario).readFile(filename);
		Person person2 = pop2.getPersons().get(Id.create(1, Person.class));
		Leg leg2 = (Leg) person2.getPlans().get(0).getPlanElements().get(1);
		Route route2 = leg2.getRoute();
		assertEquals(123, route2.getTravelTime().seconds(), MatsimTestUtils.EPSILON); // if this succeeds, we know that writing/reading the data works
	}

}
