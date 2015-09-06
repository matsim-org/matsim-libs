/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareInitialRoutesTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.christoph.controler;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author cdobler
 */
public class PrepareInitialRoutesTest {

	@Rule 
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	/*
	 * The person has already a valid plan. It should NOT be adapted.
	 */
	@Test
	public void testPersonWithAlreadyPreparedPlan() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		
		Person person01 = createPerson(scenario, "p01");
		scenario.getPopulation().addPerson(person01);
		
		new PrepareInitialRoutes(scenario).run();
		
		Leg leg = (Leg) person01.getSelectedPlan().getPlanElements().get(1);
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		
		Assert.assertEquals(3, route.getLinkIds().size());
	}
	
	/*
	 * The person has a plan without a route. It should be adapted.
	 */
	@Test
	public void testPersonWithUnpreparedPlan() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		
		Person person01 = createPerson(scenario, "p01");
		scenario.getPopulation().addPerson(person01);

		// delete route - a new dummy route should be created
		Leg leg01 = (Leg) person01.getSelectedPlan().getPlanElements().get(1);
		leg01.setRoute(null);
		
		new PrepareInitialRoutes(scenario).run();
		
		// leg has been replaced by the TripRouter, therefore update reference
		leg01 = (Leg) person01.getSelectedPlan().getPlanElements().get(1);
		NetworkRoute route = (NetworkRoute) leg01.getRoute();
		
		Assert.assertEquals(1, route.getLinkIds().size());
	}
	
	/*
	 * One person with a valid plan, one without.
	 */
	@Test
	public void testPersonMixed() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		
		Person person01 = createPerson(scenario, "p01");
		Person person02 = createPerson(scenario, "p02");
		scenario.getPopulation().addPerson(person01);
		scenario.getPopulation().addPerson(person02);

		Leg leg01 = (Leg) person01.getSelectedPlan().getPlanElements().get(1);
		leg01.setRoute(null);
		
		new PrepareInitialRoutes(scenario).run();
		
		// leg has been replaced by the TripRouter, therefore update reference
		leg01 = (Leg) person01.getSelectedPlan().getPlanElements().get(1);
		NetworkRoute route01 = (NetworkRoute) leg01.getRoute();
		
		Assert.assertEquals(1, route01.getLinkIds().size());
		
		Leg leg02 = (Leg) person02.getSelectedPlan().getPlanElements().get(1);
		NetworkRoute route02 = (NetworkRoute) leg02.getRoute();
		Assert.assertEquals(3, route02.getLinkIds().size());
	}
	
	/*
	 * Network looks like:

	 * n0----n1----n2----n3----n4----n5
	 *    l0    l1    l2    l3    l4
	 */
	private void createNetwork(Scenario scenario) {
		
		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		Node node0 = networkFactory.createNode(Id.create("n0", Node.class), new Coord(0.0, 0.0));
		Node node1 = networkFactory.createNode(Id.create("n1", Node.class), new Coord(1.0, 0.0));
		Node node2 = networkFactory.createNode(Id.create("n2", Node.class), new Coord(2.0, 0.0));
		Node node3 = networkFactory.createNode(Id.create("n3", Node.class), new Coord(3.0, 0.0));
		Node node4 = networkFactory.createNode(Id.create("n4", Node.class), new Coord(4.0, 0.0));
		Node node5 = networkFactory.createNode(Id.create("n5", Node.class), new Coord(5.0, 0.0));
		
		Link link0 = networkFactory.createLink(Id.create("l0", Link.class), node0, node1);
		Link link1 = networkFactory.createLink(Id.create("l1", Link.class), node1, node2);
		Link link2 = networkFactory.createLink(Id.create("l2", Link.class), node2, node3);
		Link link3 = networkFactory.createLink(Id.create("l3", Link.class), node3, node4);
		Link link4 = networkFactory.createLink(Id.create("l4", Link.class), node4, node5);
		
		link0.setLength(1000.0);
		link1.setLength(1000.0);
		link2.setLength(1000.0);
		link3.setLength(1000.0);
		link4.setLength(1000.0);

		network.addNode(node0);
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
		network.addNode(node5);
		network.addLink(link0);
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
		network.addLink(link4);
	}
	
	/*
	 * Create a person without route.
	 */
	private Person createPerson(Scenario scenario, String id) {
		
		PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(Id.create(id, Person.class));
		
		person.setAge(20);
		person.setSex("m");

		Activity from = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("l0", Link.class));
		Leg leg = scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		Activity to = scenario.getPopulation().getFactory().createActivityFromLinkId("home", Id.create("l4", Link.class));

		from.setEndTime(8*3600);
		leg.setDepartureTime(8*3600);
		
		RouteFactory routeFactory = new LinkNetworkRouteFactory();
		Id startLinkId = Id.create("l0", Link.class);
		Id endLinkId = Id.create("l4", Link.class);
		NetworkRoute route = (NetworkRoute) routeFactory.createRoute(startLinkId, endLinkId);
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(Id.create("l1", Link.class));
		linkIds.add(Id.create("l2", Link.class));
		linkIds.add(Id.create("l3", Link.class));
		route.setLinkIds(startLinkId, linkIds, endLinkId);
		leg.setRoute(route);
		
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(from);
		plan.addLeg(leg);
		plan.addActivity(to);
		
		person.addPlan(plan);
		
		return person;
	}
}