/* *********************************************************************** *
 * project: org.matsim.*
 * PersonIntersectAreaFilterTest.java
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

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * Some tests for org.matsim.population.filters.PersonIntersectAreaFilter.
 *
 * @author mrieser
 */
public class PersonIntersectAreaFilterTest extends MatsimTestCase {

	public void testFilter() throws Exception {
		/* create a simple network where agents can drive from the lower left
		 * to the upper right */
		NetworkLayer network = new NetworkLayer();
		Node node0 = network.createNode(new IdImpl("0"), new CoordImpl(0, 0));
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(10, 10));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(90, 10));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(10, 90));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(90, 90));
		Node node5 = network.createNode(new IdImpl("5"), new CoordImpl(100, 100));
		Link link0 = network.createLink(new IdImpl("0"), node0, node1, 20, 20, 100, 1);
/*	Link link1=*/network.createLink(new IdImpl("1"), node1, node2, 100, 20, 100, 1);
		Link link2 = network.createLink(new IdImpl("2"), node2, node4, 100, 20, 100, 1);
/*	Link link3=*/network.createLink(new IdImpl("3"), node1, node3, 100, 20, 100, 1);
		Link link4 = network.createLink(new IdImpl("4"), node3, node4, 100, 20, 100, 1);
		Link link5 = network.createLink(new IdImpl("5"), node4, node5, 20, 20, 100, 1);

		// create a test person
		Person person = new PersonImpl(new IdImpl("1"));
		Plan plan = person.createPlan(true);

		Activity act1 = plan.createActivity("h", link0);
		act1.setEndTime(8.0*3600);

		Leg leg = plan.createLeg(TransportMode.car);
		leg.setDepartureTime(8.0*3600);
		leg.setTravelTime(2.0*60);

		plan.createActivity("w", link5);

		NetworkRoute route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, link0, link5);
		leg.setRoute(route);

		// prepare route
		route.setNodes(link0, NetworkUtils.getNodes(network, "1 2 4"), link5);

		// prepare area of interest
		HashMap<Id, Link> aoi = new HashMap<Id, Link>();

		// prepare filter
		PersonIntersectAreaFilter filter = null;

		// test route through aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi);
		assertTrue("test route through aoi", filter.judge(person));

		// test departure link
		aoi.clear();
		aoi.put(link0.getId(), link0);
		filter = new PersonIntersectAreaFilter(null, aoi);
		assertTrue("test departure link as aoi", filter.judge(person));

		// test arrival link
		aoi.clear();
		aoi.put(link5.getId(), link5);
		filter = new PersonIntersectAreaFilter(null, aoi);
		assertTrue("test arrival link as aoi", filter.judge(person));

		// test route outside aoi
		aoi.clear();
		aoi.put(link4.getId(), link4);
		filter = new PersonIntersectAreaFilter(null, aoi);
		assertFalse("test route outside aoi", filter.judge(person));

		// prepare bee-line tests
		leg.setMode(TransportMode.walk);
		leg.setRoute(network.getFactory().createRoute(TransportMode.car, link0, link5)); // empty route // TODO should be switched to WalkRoute once that exists...

		// test bee-line without alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi);
		assertFalse("test bee-line without alternative aoi", filter.judge(person));

		// test bee-line with too small alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi);
		filter.setAlternativeAOI(new CoordImpl(100, 0), 20.0);
		assertFalse("test bee-line with too small alternative aoi", filter.judge(person));

		// test bee-line with big enough alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi);
		filter.setAlternativeAOI(new CoordImpl(100, 0), 80.0);
		assertTrue("test bee-line with big enough alternative aoi", filter.judge(person));

	}

}
