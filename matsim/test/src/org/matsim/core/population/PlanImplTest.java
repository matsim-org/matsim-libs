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
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.GenericRoute;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NodeNetworkRoute;
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
			plan.createLeg(TransportMode.car);
			fail("expected IllegalStateException when creating a leg in an empty plan.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createActivity("h", new CoordImpl(0, 0));
		plan.createLeg(TransportMode.car);
		try {
			plan.createLeg(TransportMode.bike);
			fail("expected IllegalStateException.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createActivity("w", new CoordImpl(100, 200));
		plan.createLeg(TransportMode.bike);
		plan.createActivity("h", new CoordImpl(0, 0));
	}

	/**
	 * @author mrieser
	 */
	public void testCreateAct() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createActivity("h", new CoordImpl(0, 0));
		// don't allow a second act directly after the first
		try {
			plan.createActivity("w", new CoordImpl(100, 200));
			fail("expected IllegalStateException.");
		} catch (IllegalStateException e) {
			log.debug("catched expected exception.", e);
		}
		plan.createLeg(TransportMode.car);
		// but after a leg, it must be possible to add an additional act
		plan.createActivity("w", new CoordImpl(100, 200));
	}

	/**
	 * @author mrieser
	 */
	public void testInsertActLeg_Between() {
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		ActivityImpl homeAct = plan.createActivity("h", new CoordImpl(0, 0));
		LegImpl leg1 = plan.createLeg(TransportMode.car);
		ActivityImpl workAct = plan.createActivity("w", new CoordImpl(100, 200));

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
		ActivityImpl homeAct = plan.createActivity("h", new CoordImpl(0, 0));
		LegImpl leg1 = plan.createLeg(TransportMode.car);
		ActivityImpl workAct = plan.createActivity("w", new CoordImpl(100, 200));

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
		plan.createActivity("h", new CoordImpl(0, 0));
		plan.createLeg(TransportMode.car);
		plan.createActivity("w", new CoordImpl(100, 200));

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
		plan.createActivity("h", new CoordImpl(0, 0));
		plan.createLeg(TransportMode.car);
		plan.createActivity("w", new CoordImpl(100, 200));

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
		plan.createActivity("h", new CoordImpl(0, 0));
		plan.createLeg(TransportMode.car);
		plan.createActivity("w", new CoordImpl(100, 200));

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
		NodeImpl node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		NodeImpl node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createNode(new IdImpl(3), new CoordImpl(2000, 0));
		LinkImpl link1 = network.createLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		LinkImpl link2 = network.createLink(new IdImpl(2), node2, node3, 1000.0, 100.0, 3600.0, 1.0);
		
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createActivity("h", new CoordImpl(0, 0));
		LegImpl leg = plan.createLeg(TransportMode.car);
		plan.createActivity("w", new CoordImpl(100, 200));
		Route route = new NodeNetworkRoute(link1, link2);
		route.setDistance(123.45);
		route.setTravelTime(98.76);
		leg.setRoute(route);
		
		PlanImpl plan2 = new PlanImpl(new PersonImpl(new IdImpl(2)));
		plan2.copyPlan(plan);
		
		assertEquals("person must not be copied.", new IdImpl(2), plan2.getPerson().getId());
		assertEquals("wrong number of plan elements.", plan.getPlanElements().size(), plan2.getPlanElements().size());
		Route route2 = ((LegImpl) plan.getPlanElements().get(1)).getRoute();
		assertTrue(route2 instanceof NetworkRoute);
		assertEquals(123.45, route2.getDistance(), EPSILON);
		assertEquals(98.76, route2.getTravelTime(), EPSILON);
	}

	public void testCopyPlan_GenericRoute() {
		NetworkLayer network = new NetworkLayer();
		NodeImpl node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		NodeImpl node2 = network.createNode(new IdImpl(2), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createNode(new IdImpl(3), new CoordImpl(2000, 0));
		LinkImpl link1 = network.createLink(new IdImpl(1), node1, node2, 1000.0, 100.0, 3600.0, 1.0);
		LinkImpl link2 = network.createLink(new IdImpl(2), node2, node3, 1000.0, 100.0, 3600.0, 1.0);
		
		PlanImpl plan = new PlanImpl(new PersonImpl(new IdImpl(1)));
		plan.createActivity("h", new CoordImpl(0, 0));
		LegImpl leg = plan.createLeg(TransportMode.car);
		plan.createActivity("w", new CoordImpl(100, 200));
		Route route = new GenericRouteImpl(link1, link2);
		route.setDistance(123.45);
		route.setTravelTime(98.76);
		leg.setRoute(route);
		
		PlanImpl plan2 = new PlanImpl(new PersonImpl(new IdImpl(2)));
		plan2.copyPlan(plan);
		
		assertEquals("person must not be copied.", new IdImpl(2), plan2.getPerson().getId());
		assertEquals("wrong number of plan elements.", plan.getPlanElements().size(), plan2.getPlanElements().size());
		Route route2 = ((LegImpl) plan.getPlanElements().get(1)).getRoute();
		assertTrue(route2 instanceof GenericRoute);
		assertEquals(123.45, route2.getDistance(), EPSILON);
		assertEquals(98.76, route2.getTravelTime(), EPSILON);
	}
	
}
