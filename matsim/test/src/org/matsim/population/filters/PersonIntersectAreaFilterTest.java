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

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

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
		network.createNode("0", "0", "0", null);
		network.createNode("1", "10", "10", null);
		network.createNode("2", "90", "10", null);
		network.createNode("3", "10", "90", null);
		network.createNode("4", "90", "90", null);
		network.createNode("5", "100", "100", null);
		Link link0 = network.createLink("0", "0", "1",  "20", "20", "100", "1", null, null);
/*	Link link1=*/network.createLink("1", "1", "2", "100", "20", "100", "1", null, null);
		Link link2 = network.createLink("2", "2", "4", "100", "20", "100", "1", null, null);
/*	Link link3=*/network.createLink("3", "1", "3", "100", "20", "100", "1", null, null);
		Link link4 = network.createLink("4", "3", "4", "100", "20", "100", "1", null, null);
		Link link5 = network.createLink("5", "4", "5",  "20", "20", "100", "1", null, null);

		Gbl.createWorld().setNetworkLayer(network);

		// create a test person
		Person person = new PersonImpl(new IdImpl("1"));
		Plan plan = person.createPlan(true);

		Act act1 = plan.createAct("h", link0);
		act1.setEndTime(8.0*3600);

		Leg leg = plan.createLeg(BasicLeg.Mode.car);
		leg.setDepartureTime(8.0*3600);
		leg.setTravelTime(2.0*60);

		plan.createAct("w", link5);

		CarRoute route = new NodeCarRoute();
		leg.setRoute(route);

		// prepare route
		route.setNodes("1 2 4");

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
		leg.setMode(BasicLeg.Mode.walk);
		leg.setRoute(new NodeCarRoute()); // empty route

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
