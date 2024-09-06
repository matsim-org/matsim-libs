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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Some tests for org.matsim.population.filters.PersonIntersectAreaFilter.
 *
 * @author mrieser
 */
public class PersonIntersectAreaFilterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testFilter() throws Exception {
		/* create a simple network where agents can drive from the lower left
		 * to the upper right */
		Network network = NetworkUtils.createNetwork();
        Node node0 = NetworkUtils.createAndAddNode(network, Id.create("0", Node.class), new Coord((double) 0, (double) 0));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 10, (double) 10));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 90, (double) 10));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 10, (double) 90));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 90, (double) 90));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord((double) 100, (double) 100));
		final Node fromNode = node0;
		final Node toNode = node1;
		Link link0 = NetworkUtils.createAndAddLink(network,Id.create("0", Link.class), fromNode, toNode, (double) 20, (double) 20, (double) 100, (double) 1 );
		final Node fromNode1 = node1;
		final Node toNode1 = node2;
/*	Link link1=*/NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode1, toNode1, (double) 100, (double) 20, (double) 100, (double) 1 );
final Node fromNode2 = node2;
final Node toNode2 = node4;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode2, toNode2, (double) 100, (double) 20, (double) 100, (double) 1 );
		final Node fromNode3 = node1;
		final Node toNode3 = node3;
/*	Link link3=*/NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode3, toNode3, (double) 100, (double) 20, (double) 100, (double) 1 );
final Node fromNode4 = node3;
final Node toNode4 = node4;
		Link link4 = NetworkUtils.createAndAddLink(network,Id.create("4", Link.class), fromNode4, toNode4, (double) 100, (double) 20, (double) 100, (double) 1 );
		final Node fromNode5 = node4;
		final Node toNode5 = node5;
		Link link5 = NetworkUtils.createAndAddLink(network,Id.create("5", Link.class), fromNode5, toNode5, (double) 20, (double) 20, (double) 100, (double) 1 );

		// create a test person
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);

		Activity act1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link0.getId());
		act1.setEndTime(8.0*3600);

		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		leg.setDepartureTime(8.0*3600);
		leg.setTravelTime(2.0*60);

		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link5.getId());

		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(link0.getId(), link5.getId());
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
		assertTrue(filter.judge(person), "test route through aoi");

		// test departure link
		aoi.clear();
		aoi.put(link0.getId(), link0);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertTrue(filter.judge(person), "test departure link as aoi");

		// test arrival link
		aoi.clear();
		aoi.put(link5.getId(), link5);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertTrue(filter.judge(person), "test arrival link as aoi");

		// test route outside aoi
		aoi.clear();
		aoi.put(link4.getId(), link4);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertFalse(filter.judge(person), "test route outside aoi");

		// prepare bee-line tests
		leg.setMode(TransportMode.walk);
		leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(link0.getId(), link5.getId())); // empty route

		// test bee-line without alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		assertFalse(filter.judge(person), "test bee-line without alternative aoi");

		// test bee-line with too small alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		filter.setAlternativeAOI(new Coord((double) 100, (double) 0), 20.0);
		assertFalse(filter.judge(person), "test bee-line with too small alternative aoi");

		// test bee-line with big enough alternative aoi
		aoi.clear();
		aoi.put(link2.getId(), link2);
		filter = new PersonIntersectAreaFilter(null, aoi, network);
		filter.setAlternativeAOI(new Coord((double) 100, (double) 0), 80.0);
		assertTrue(filter.judge(person), "test bee-line with big enough alternative aoi");

	}

}
