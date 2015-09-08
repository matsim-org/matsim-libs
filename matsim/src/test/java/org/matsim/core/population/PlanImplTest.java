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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

public class PlanImplTest {

	static private final Logger log = Logger.getLogger(PlanImplTest.class);

	/**
	 * @author mrieser
	 */
	@Test
	public void testCreateAndAddActAndLeg() {
		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		try {
			plan.createAndAddLeg(TransportMode.car);
			fail("expected IllegalStateException when creating a leg in an empty plan.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createAndAddActivity("h", new Coord((double) 0, (double) 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));
		plan.createAndAddLeg(TransportMode.bike);
		plan.createAndAddActivity("h", new Coord((double) 0, (double) 0));
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testInsertActLeg_Between() {
		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		ActivityImpl homeAct = plan.createAndAddActivity("h", new Coord(0, 0));
		Leg leg1 = plan.createAndAddLeg(TransportMode.car);
		ActivityImpl workAct = plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new ActivityImpl("l", new Coord((double) 200, (double) 100));
		Leg l = new LegImpl(TransportMode.car);
		plan.insertLegAct(1, l, a);

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
	public void testInsertActLeg_AtEnd() {
		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		ActivityImpl homeAct = plan.createAndAddActivity("h", new Coord(0, 0));
		Leg leg1 = plan.createAndAddLeg(TransportMode.car);
		ActivityImpl workAct = plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new ActivityImpl("l", new Coord((double) 200, (double) 100));
		Leg l = new LegImpl(TransportMode.car);
		plan.insertLegAct(3, l, a);

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
	public void testInsertActLeg_AtWrongPosition() {
		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("h", new Coord(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new ActivityImpl("l", new Coord((double) 200, (double) 100));
		Leg l = new LegImpl(TransportMode.car);
		try {
			plan.insertLegAct(2, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testInsertActLeg_AtStart() {
		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("h", new Coord(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new ActivityImpl("l", new Coord((double) 200, (double) 100));
		Leg l = new LegImpl(TransportMode.car);
		try {
			plan.insertLegAct(0, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}
	}


	/**
	 * @author mrieser
	 */
	@Test
	public void testInsertActLeg_BehindEnd() {
		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("h", new Coord(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new ActivityImpl("l", new Coord((double) 200, (double) 100));
		Leg l = new LegImpl(TransportMode.car);
		try {
			plan.insertLegAct(4, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}

		try {
			plan.insertLegAct(5, l, a);
			fail("expected Exception because of wrong act/leg-index.");
		} catch (IllegalArgumentException e) {
			log.debug("catched expected exception.", e);
		}

	}

	@Test
	public void testCopyPlan_NetworkRoute() {
		NetworkImpl network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 2000, (double) 0));
		Link link1 = network.createAndAddLink(Id.create(1, Link.class), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(Id.create(2, Link.class), node2, node3, 1000.0, 100.0, 3600.0, 1.0);

		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("h", new Coord(0, 0));
		Leg leg = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));
		Route route = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
		route.setTravelTime(98.76);
		leg.setRoute(route);

		PlanImpl plan2 = new PlanImpl(PersonImpl.createPerson(Id.create(2, Person.class)));
		plan2.copyFrom(plan);

		assertEquals("person must not be copied.", Id.create(2, Person.class), plan2.getPerson().getId());
		assertEquals("wrong number of plan elements.", plan.getPlanElements().size(), plan2.getPlanElements().size());
		Route route2 = ((Leg) plan.getPlanElements().get(1)).getRoute();
		assertTrue(route2 instanceof NetworkRoute);
		assertEquals(98.76, route2.getTravelTime(), 1e-8);
	}

	@Test
	public void testCopyPlan_GenericRoute() {
		NetworkImpl network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 2000, (double) 0));
		Link link1 = network.createAndAddLink(Id.create(1, Link.class), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(Id.create(2, Link.class), node2, node3, 1000.0, 100.0, 3600.0, 1.0);

		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		plan.createAndAddActivity("h", new Coord(0, 0));
		Leg leg = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new Coord((double) 100, (double) 200));
		Route route = new GenericRouteImpl(link1.getId(), link2.getId());
		route.setTravelTime(98.76);
		leg.setRoute(route);

		PlanImpl plan2 = new PlanImpl(PersonImpl.createPerson(Id.create(2, Person.class)));
		plan2.copyFrom(plan);

		assertEquals("person must not be copied.", Id.create(2, Person.class), plan2.getPerson().getId());
		assertEquals("wrong number of plan elements.", plan.getPlanElements().size(), plan2.getPlanElements().size());
		Route route2 = ((Leg) plan.getPlanElements().get(1)).getRoute();
		assertTrue(route2 instanceof GenericRoute);
		assertEquals(98.76, route2.getTravelTime(), 1e-8);
	}

	/**
	 * @author meisterk
	 */
	@Test
	public void testRemoveActivity() {

		PlanImpl testee = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		testee.createAndAddActivity("h", new Coord(0, 0));
		testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("w", new Coord((double) 100, (double) 200));
		testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("h", new Coord((double) 0, (double) 0));

		testee.removeActivity(3);
		assertEquals(5, testee.getPlanElements().size());

		testee.removeActivity(4);
		assertEquals(3, testee.getPlanElements().size());
	}

	/**
	 * @author meisterk
	 */
	@Test
	public void testRemoveLeg() {
		PlanImpl testee = new PlanImpl(PersonImpl.createPerson(Id.create(1, Person.class)));
		testee.createAndAddActivity("h", new Coord(0, 0));
		testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("w", new Coord((double) 100, (double) 200));
		testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("h", new Coord((double) 0, (double) 0));

		testee.removeLeg(4);
		assertEquals(5, testee.getPlanElements().size());

		testee.removeLeg(3);
		assertEquals(3, testee.getPlanElements().size());
	}

	@Test
	public void addMultipleLegs() {
		Plan p = new PlanImpl();
		p.addActivity(new ActivityImpl("h"));
		p.addLeg(new LegImpl(TransportMode.walk));
		p.addLeg(new LegImpl(TransportMode.pt));
		p.addLeg(new LegImpl(TransportMode.walk));
		p.addActivity(new ActivityImpl("w"));

		Assert.assertEquals(5, p.getPlanElements().size());
		Assert.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assert.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(2) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(3) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(4) instanceof Activity);
	}

	@Test
	public void addMultipleActs() {
		Plan p = new PlanImpl();
		p.addActivity(new ActivityImpl("h"));
		p.addLeg(new LegImpl(TransportMode.walk));
		p.addActivity(new ActivityImpl("w"));
		p.addActivity(new ActivityImpl("l"));

		Assert.assertEquals(4, p.getPlanElements().size());
		Assert.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assert.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(2) instanceof Activity);
		Assert.assertTrue(p.getPlanElements().get(3) instanceof Activity);
	}

	@Test
	public void createAndAddMultipleLegs() {
		PlanImpl p = new PlanImpl();
		p.createAndAddActivity("h");
		p.createAndAddLeg(TransportMode.walk);
		p.createAndAddLeg(TransportMode.pt);
		p.createAndAddLeg(TransportMode.walk);
		p.createAndAddActivity("w");

		Assert.assertEquals(5, p.getPlanElements().size());
		Assert.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assert.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(2) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(3) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(4) instanceof Activity);
	}

	@Test
	public void createAndAddMultipleActs() {
		PlanImpl p = new PlanImpl();
		p.createAndAddActivity("h");
		p.createAndAddLeg(TransportMode.walk);
		p.createAndAddActivity("w");
		p.createAndAddActivity("l");

		Assert.assertEquals(4, p.getPlanElements().size());
		Assert.assertTrue(p.getPlanElements().get(0) instanceof Activity);
		Assert.assertTrue(p.getPlanElements().get(1) instanceof Leg);
		Assert.assertTrue(p.getPlanElements().get(2) instanceof Activity);
		Assert.assertTrue(p.getPlanElements().get(3) instanceof Activity);
	}

}
