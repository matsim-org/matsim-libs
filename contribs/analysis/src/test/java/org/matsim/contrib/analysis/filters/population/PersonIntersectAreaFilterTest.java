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

package org.matsim.contrib.analysis.filters.population;

import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
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
		NetworkImpl network = NetworkImpl.createNetwork();
		Node node0 = network.createAndAddNode(Id.create("0", Node.class), new Coord((double) 0, (double) 0));
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord((double) 10, (double) 10));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord((double) 90, (double) 10));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord((double) 10, (double) 90));
		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord((double) 90, (double) 90));
		Node node5 = network.createAndAddNode(Id.create("5", Node.class), new Coord((double) 100, (double) 100));
		Link link0 = network.createAndAddLink(Id.create("0", Link.class), node0, node1, 20, 20, 100, 1);
/*	Link link1=*/network.createAndAddLink(Id.create("1", Link.class), node1, node2, 100, 20, 100, 1);
		Link link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node4, 100, 20, 100, 1);
/*	Link link3=*/network.createAndAddLink(Id.create("3", Link.class), node1, node3, 100, 20, 100, 1);
		Link link4 = network.createAndAddLink(Id.create("4", Link.class), node3, node4, 100, 20, 100, 1);
		Link link5 = network.createAndAddLink(Id.create("5", Link.class), node4, node5, 20, 20, 100, 1);

		// create a test person
		Person person = PersonImpl.createPerson(Id.create("1", Person.class));
		PlanImpl plan = PersonUtils.createAndAddPlan(person, true);

		ActivityImpl act1 = plan.createAndAddActivity("h", link0.getId());
		act1.setEndTime(8.0*3600);

		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		leg.setDepartureTime(8.0*3600);
		leg.setTravelTime(2.0*60);

		plan.createAndAddActivity("w", link5.getId());

		NetworkRoute route = new LinkNetworkRouteImpl(link0.getId(), link5.getId());
		leg.setRoute(route);

		// prepare route
		route.setLinkIds(link0.getId(), NetworkUtils.getLinkIds("1 2"), link5.getId());

		// prepare area of interest
		HashMap<Id<Link>, Link> aoi = new HashMap<>();

		// prepare filter
		PersonIntersectAreaFilter filter = null;

		// test route through aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertTrue("test route through aoi", filter.judge(person));

		// test departure link
		aoi.clear();
		aoi.put(link0.getId(), link0);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertTrue("test departure link as aoi", filter.judge(person));

		// test arrival link
		aoi.clear();
		aoi.put(link5.getId(), link5);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertTrue("test arrival link as aoi", filter.judge(person));

		// test route outside aoi
		aoi.clear();
		aoi.put(link4.getId(), link4);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertFalse("test route outside aoi", filter.judge(person));

		// prepare bee-line tests
		leg.setMode(TransportMode.walk);
		leg.setRoute(new LinkNetworkRouteImpl(link0.getId(), link5.getId())); // empty route

		// test bee-line without alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertFalse("test bee-line without alternative aoi", filter.judge(person));

		// test bee-line with too small alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		filter.setAlternativeAOI(new Coord((double) 100, (double) 0), 20.0);
		assertFalse("test bee-line with too small alternative aoi", filter.judge(person));

		// test bee-line with big enough alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		filter.setAlternativeAOI(new Coord((double) 100, (double) 0), 80.0);
		assertTrue("test bee-line with big enough alternative aoi", filter.judge(person));

	}

}
