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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

public class PlanImplTest extends MatsimTestCase {

	static private final Logger log = Logger.getLogger(PlanImplTest.class);

	/**
	 * @author mrieser
	 */
	public void testCreateLeg() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		try {
			plan.createAndAddLeg(TransportMode.car);
			fail("expected IllegalStateException when creating a leg in an empty plan.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		try {
			plan.createAndAddLeg(TransportMode.bike);
			fail("expected IllegalStateException.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createAndAddActivity("w", new CoordImpl(100, 200));
		plan.createAndAddLeg(TransportMode.bike);
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
	}

	/**
	 * @author mrieser
	 */
	public void testCreateAct() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
		// don't allow a second act directly after the first
		try {
			plan.createAndAddActivity("w", new CoordImpl(100, 200));
			fail("expected IllegalStateException.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createAndAddLeg(TransportMode.car);
		// but after a leg, it must be possible to add an additional act
		plan.createAndAddActivity("w", new CoordImpl(100, 200));
	}

	/**
	 * @author mrieser
	 */
	public void testInsertActLeg_Between() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		ActivityImpl homeAct = plan.createAndAddActivity("h", new CoordImpl(0, 0));
		LegImpl leg1 = plan.createAndAddLeg(TransportMode.car);
		ActivityImpl workAct = plan.createAndAddActivity("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new org.matsim.core.population.ActivityImpl("l", new CoordImpl(200, 100));
		LegImpl l = new org.matsim.core.population.LegImpl(TransportMode.car);
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
	public void testInsertActLeg_AtEnd() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		ActivityImpl homeAct = plan.createAndAddActivity("h", new CoordImpl(0, 0));
		LegImpl leg1 = plan.createAndAddLeg(TransportMode.car);
		ActivityImpl workAct = plan.createAndAddActivity("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new org.matsim.core.population.ActivityImpl("l", new CoordImpl(200, 100));
		LegImpl l = new org.matsim.core.population.LegImpl(TransportMode.car);
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
	public void testInsertActLeg_AtWrongPosition() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new org.matsim.core.population.ActivityImpl("l", new CoordImpl(200, 100));
		LegImpl l = new org.matsim.core.population.LegImpl(TransportMode.car);
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
	public void testInsertActLeg_AtStart() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new org.matsim.core.population.ActivityImpl("l", new CoordImpl(200, 100));
		LegImpl l = new org.matsim.core.population.LegImpl(TransportMode.car);
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
	public void testInsertActLeg_BehindEnd() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new CoordImpl(100, 200));

		// precondition
		assertEquals(3, plan.getPlanElements().size());

		// modification
		ActivityImpl a = new org.matsim.core.population.ActivityImpl("l", new CoordImpl(200, 100));
		LegImpl l = new org.matsim.core.population.LegImpl(TransportMode.car);
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

	public void testCopyPlan_NetworkRoute() {
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Node node3 = network.createAndAddNode(new IdImpl(3), new CoordImpl(2000, 0));
		Link link1 = network.createAndAddLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(new IdImpl(2), node2, node3, 1000.0, 100.0, 3600.0, 1.0);
		
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new CoordImpl(100, 200));
		RouteWRefs route = new NodeNetworkRouteImpl(link1, link2);
		route.setDistance(123.45);
		route.setTravelTime(98.76);
		leg.setRoute(route);
		
		PlanImpl plan2 = new PlanImpl(new PersonImpl(new IdImpl(2)));
		plan2.copyPlan(plan);
		
		assertEquals("person must not be copied.", new IdImpl(2), plan2.getPerson().getId());
		assertEquals("wrong number of plan elements.", plan.getPlanElements().size(), plan2.getPlanElements().size());
		RouteWRefs route2 = ((LegImpl) plan.getPlanElements().get(1)).getRoute();
		assertTrue(route2 instanceof NetworkRouteWRefs);
		assertEquals(123.45, route2.getDistance(), EPSILON);
		assertEquals(98.76, route2.getTravelTime(), EPSILON);
	}

	public void testCopyPlan_GenericRoute() {
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(1000, 0));
		Node node3 = network.createAndAddNode(new IdImpl(3), new CoordImpl(2000, 0));
		Link link1 = network.createAndAddLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(new IdImpl(2), node2, node3, 1000.0, 100.0, 3600.0, 1.0);
		
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createAndAddActivity("h", new CoordImpl(0, 0));
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("w", new CoordImpl(100, 200));
		RouteWRefs route = new GenericRouteImpl(link1, link2);
		route.setDistance(123.45);
		route.setTravelTime(98.76);
		leg.setRoute(route);
		
		PlanImpl plan2 = new PlanImpl(new PersonImpl(new IdImpl(2)));
		plan2.copyPlan(plan);
		
		assertEquals("person must not be copied.", new IdImpl(2), plan2.getPerson().getId());
		assertEquals("wrong number of plan elements.", plan.getPlanElements().size(), plan2.getPlanElements().size());
		RouteWRefs route2 = ((LegImpl) plan.getPlanElements().get(1)).getRoute();
		assertTrue(route2 instanceof GenericRoute);
		assertEquals(123.45, route2.getDistance(), EPSILON);
		assertEquals(98.76, route2.getTravelTime(), EPSILON);
	}
	
	/**
	 * @author meisterk
	 */
	public void testRemoveActivity() {

		PlanImpl testee = new PlanImpl(new PersonImpl(new IdImpl(1)));
		testee.createAndAddActivity("h", new CoordImpl(0, 0));
		LegImpl leg = testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("w", new CoordImpl(100, 200));
		leg = testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("h", new CoordImpl(0, 0));

		testee.removeActivity(3);
		assertEquals(5, testee.getPlanElements().size());
		
		testee.removeActivity(4);
		assertEquals(3, testee.getPlanElements().size());
		
	}

	/**
	 * @author meisterk
	 */
	public void testRemoveLeg() {

		PlanImpl testee = new PlanImpl(new PersonImpl(new IdImpl(1)));
		testee.createAndAddActivity("h", new CoordImpl(0, 0));
		LegImpl leg = testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("w", new CoordImpl(100, 200));
		leg = testee.createAndAddLeg(TransportMode.car);
		testee.createAndAddActivity("h", new CoordImpl(0, 0));

		testee.removeLeg(4);
		assertEquals(5, testee.getPlanElements().size());
		
		testee.removeLeg(3);
		assertEquals(3, testee.getPlanElements().size());

	}
	
}
