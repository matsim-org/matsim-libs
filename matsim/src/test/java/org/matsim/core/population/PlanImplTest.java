/* *********************************************************************** *
 * project: org.matsim.*
 * PlanTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.population;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

public class PlanImplTest {

	static private final Logger log = LogManager.getLogger(PlanImplTest.class);

	/**
	 * @author mrieser
	 */
	@Test
	void testCreateAndAddActAndLeg() {
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		try {
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			fail("expected IllegalStateException when creating a leg in an empty plan.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));
		PopulationUtils.createAndAddLeg( plan, TransportMode.bike );
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testInsertActLeg_Between() {
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		Activity homeAct = PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		Leg leg1 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		Activity workAct = PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		Activity a = PopulationUtils.createActivityFromCoord("l", new Coord(200, 100));
		Leg l = PopulationUtils.createLeg(TransportMode.car);
		final Leg leg = l;
		final Activity act = a;
		PopulationUtils.insertLegAct(plan, 1, leg, act);

		// test
		assertEquals(5, plan.getPlanElements().size());
		assertEquals(homeAct, plan.getPlanElements().get(0));
		assertEquals(l, plan.getPlanElements().get(1));
		assertEquals(a, plan.getPlanElements().get(2));
		assertEquals(leg1, plan.getPlanElements().get(3));
		assertEquals(workAct, plan.getPlanElements().get(4));
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testInsertActLeg_AtEnd() {
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		Activity homeAct = PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		Leg leg1 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		Activity workAct = PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		Activity a = PopulationUtils.createActivityFromCoord("l", new Coord(200, 100));
		Leg l = PopulationUtils.createLeg(TransportMode.car);
		final Leg leg = l;
		final Activity act = a;
		PopulationUtils.insertLegAct(plan, 3, leg, act);

		// test
		assertEquals(5, plan.getPlanElements().size());
		assertEquals(homeAct, plan.getPlanElements().get(0));
		assertEquals(leg1, plan.getPlanElements().get(1));
		assertEquals(workAct, plan.getPlanElements().get(2));
		assertEquals(l, plan.getPlanElements().get(3));
		assertEquals(a, plan.getPlanElements().get(4));
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testInsertActLeg_AtWrongPosition() {
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		Activity a = PopulationUtils.createActivityFromCoord("l", new Coord(200, 100));
		Leg l = PopulationUtils.createLeg(TransportMode.car);
		try {
			final Leg leg = l;
			final Activity act = a;
			PopulationUtils.insertLegAct(plan, 2, leg, act);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testInsertActLeg_AtStart() {
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		Activity a = PopulationUtils.createActivityFromCoord("l", new Coord(200, 100));
		Leg l = PopulationUtils.createLeg(TransportMode.car);
		try {
			final Leg leg = l;
			final Activity act = a;
			PopulationUtils.insertLegAct(plan, 0, leg, act);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}
	}


	/**
	 * @author mrieser
	 */
	@Test
	void testInsertActLeg_BehindEnd() {
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		Activity a = PopulationUtils.createActivityFromCoord("l", new Coord(200, 100));
		Leg l = PopulationUtils.createLeg(TransportMode.car);
		try {
			final Leg leg = l;
			final Activity act = a;
			PopulationUtils.insertLegAct(plan, 4, leg, act);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}

		try {
			final Leg leg = l;
			final Activity act = a;
			PopulationUtils.insertLegAct(plan, 5, leg, act);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}

	}

	@Test
	void testCopyPlan_NetworkRoute() {
		Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord(1000, 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord(2000, 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 100.0, 3600.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, 1000.0, 100.0, 3600.0, 1.0 );

		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));
		Route route = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link2.getId());
		route.setTravelTime(98.76);
		leg.setRoute(route);

		Plan plan2 = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(2, Person.class)));
		PopulationUtils.copyFromTo(plan, plan2);

		assertEquals(Id.create(2, Person.class), plan2.getPerson().getId(), "person must not be copied.");
		assertEquals(plan.getPlanElements().size(), plan2.getPlanElements().size(), "wrong number of plan elements.");
		Route route2 = ((Leg) plan.getPlanElements().get(1)).getRoute();
		assertTrue(route2 instanceof NetworkRoute);
		assertEquals(98.76, route2.getTravelTime().seconds(), 1e-8);
	}

	@Test
	void testCopyPlan_GenericRoute() {
        Network network = NetworkUtils.createNetwork();
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord(1000, 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord(2000, 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 100.0, 3600.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), fromNode1, toNode1, 1000.0, 100.0, 3600.0, 1.0 );

		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord(100, 200));
		Route route = RouteUtils.createGenericRouteImpl(link1.getId(), link2.getId());
		route.setTravelTime(98.76);
		leg.setRoute(route);

		Plan plan2 = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(2, Person.class)));
		PopulationUtils.copyFromTo(plan, plan2);

		assertEquals(Id.create(2, Person.class), plan2.getPerson().getId(), "person must not be copied.");
		assertEquals(plan.getPlanElements().size(), plan2.getPlanElements().size(), "wrong number of plan elements.");
		Route route2 = ((Leg) plan.getPlanElements().get(1)).getRoute();
//		assertTrue(route2 instanceof GenericRouteImpl);
		assertEquals(98.76, route2.getTravelTime().seconds(), 1e-8);
	}

	/**
	 * @author meisterk
	 */
	@Test
	void testRemoveActivity() {

		Plan testee = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(testee, "h", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( testee, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(testee, "w", new Coord(100, 200));
		PopulationUtils.createAndAddLeg( testee, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(testee, "h", new Coord(0, 0));

		PopulationUtils.removeActivity(testee, 3);
		assertEquals(5, testee.getPlanElements().size());

		PopulationUtils.removeActivity(testee, 4);
		assertEquals(3, testee.getPlanElements().size());
	}

	/**
	 * @author meisterk
	 */
	@Test
	void testRemoveLeg() {
		Plan testee = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));
		PopulationUtils.createAndAddActivityFromCoord(testee, "h", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( testee, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(testee, "w", new Coord(100, 200));
		PopulationUtils.createAndAddLeg( testee, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(testee, "h", new Coord(0, 0));

		PopulationUtils.removeLeg(testee, 4);
		assertEquals(5, testee.getPlanElements().size());

		PopulationUtils.removeLeg(testee, 3);
		assertEquals(3, testee.getPlanElements().size());
	}

	@Test
	void addMultipleLegs() {
		Plan p = PopulationUtils.createPlan();
		p.addActivity(new ActivityImpl("h"));
		p.addLeg(PopulationUtils.createLeg(TransportMode.walk));
		p.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		p.addLeg(PopulationUtils.createLeg(TransportMode.walk));
		p.addActivity(new ActivityImpl("w"));

		Assertions.assertEquals(5, p.getPlanElements().size());
		Assertions.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assertions.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(2) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(3) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(4) instanceof Activity);
	}

	@Test
	void addMultipleActs() {
		Plan p = PopulationUtils.createPlan();
		p.addActivity(new ActivityImpl("h"));
		p.addLeg(PopulationUtils.createLeg(TransportMode.walk));
		p.addActivity(new ActivityImpl("w"));
		p.addActivity(new ActivityImpl("l"));

		Assertions.assertEquals(4, p.getPlanElements().size());
		Assertions.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assertions.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(2) instanceof Activity);
		Assertions.assertTrue(p.getPlanElements().get(3) instanceof Activity);
	}

	@Test
	void createAndAddMultipleLegs() {
		Plan p = PopulationUtils.createPlan();
		PopulationUtils.createAndAddActivity(p, "h");
		PopulationUtils.createAndAddLeg( p, TransportMode.walk );
		PopulationUtils.createAndAddLeg( p, TransportMode.pt );
		PopulationUtils.createAndAddLeg( p, TransportMode.walk );
		PopulationUtils.createAndAddActivity(p, "w");

		Assertions.assertEquals(5, p.getPlanElements().size());
		Assertions.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assertions.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(2) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(3) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(4) instanceof Activity);
	}

	@Test
	void createAndAddMultipleActs() {
		Plan p = PopulationUtils.createPlan();
		PopulationUtils.createAndAddActivity(p, "h");
		PopulationUtils.createAndAddLeg( p, TransportMode.walk );
		PopulationUtils.createAndAddActivity(p, "w");
		PopulationUtils.createAndAddActivity(p, "l");

		Assertions.assertEquals(4, p.getPlanElements().size());
		Assertions.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assertions.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assertions.assertTrue(p.getPlanElements().get(2) instanceof Activity);
		Assertions.assertTrue(p.getPlanElements().get(3) instanceof Activity);
	}

}
