
/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAverageTripLengthTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.analysis;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

	public class CalcAverageTripLengthTest {

	 @Test
	 void testWithRoute() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		NetworkFactory nf = network.getFactory();
		Node n1, n2, n3, n4, n5;
		network.addNode(n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0)));
		network.addNode(n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 50, (double) 0)));
		network.addNode(n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 100, (double) 0)));
		network.addNode(n4 = nf.createNode(Id.create("4", Node.class), new Coord((double) 200, (double) 0)));
		network.addNode(n5 = nf.createNode(Id.create("5", Node.class), new Coord((double) 400, (double) 0)));
		Link l1 = nf.createLink(Id.create("1", Link.class), n1, n2);
		Link l2 = nf.createLink(Id.create("2", Link.class), n2, n3);
		Link l3 = nf.createLink(Id.create("3", Link.class), n3, n4);
		Link l4 = nf.createLink(Id.create("4", Link.class), n4, n5);
		l1.setLength(50);
		l2.setLength(100);
		l3.setLength(200);
		l4.setLength(400);
		network.addLink(l1);
		network.addLink(l2);
		network.addLink(l3);
		network.addLink(l4);

		PopulationFactory pf = population.getFactory();

		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromLinkId("h", l1.getId());
		Leg leg = pf.createLeg(TransportMode.car);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(l1.getId(), l3.getId());
		ArrayList<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(l2.getId());
		route.setLinkIds(l1.getId(), linkIds, l3.getId());
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromLinkId("w", l3.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);

		// test simple route, startLink should not be included, endLink should
		CalcAverageTripLength catl = new CalcAverageTripLength(network);
		Assertions.assertEquals(0.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
		catl.run(plan);
		Assertions.assertEquals(300.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);

		// extend route by one link, test again
		linkIds.add(l3.getId());
		route.setLinkIds(l1.getId(), linkIds, l4.getId());
		((Activity) act2).setLinkId(l4.getId());

		catl = new CalcAverageTripLength(network);
		catl.run(plan);
		Assertions.assertEquals(700.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);

		// don't reset catl, modify route, test average
		linkIds.remove(1);
		route.setLinkIds(l1.getId(), linkIds, l3.getId());
		((Activity) act2).setLinkId(l3.getId());

		catl.run(plan);
		Assertions.assertEquals(500.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
	}

	 @Test
	 void testWithRoute_OneLinkRoute() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		NetworkFactory nf = network.getFactory();
		Node n1, n2, n3;
		network.addNode(n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0)));
		network.addNode(n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 50, (double) 0)));
		network.addNode(n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 100, (double) 0)));
		Link l1 = nf.createLink(Id.create("1", Link.class), n1, n2);
		Link l2 = nf.createLink(Id.create("2", Link.class), n2, n3);
		l1.setLength(50);
		l2.setLength(100);
		network.addLink(l1);
		network.addLink(l2);

		PopulationFactory pf = population.getFactory();

		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromLinkId("h", l1.getId());
		Leg leg = pf.createLeg(TransportMode.car);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(l1.getId(), l2.getId());
		route.setLinkIds(l1.getId(), new ArrayList<Id<Link>>(0), l2.getId());
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromLinkId("w", l2.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);

		// test simple route, startLink should not be included, endLink should be
		CalcAverageTripLength catl = new CalcAverageTripLength(network);
		catl.run(plan);
		Assertions.assertEquals(100.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
	}

	 @Test
	 void testWithRoute_StartEndOnSameLink() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		Population population = scenario.getPopulation();

		NetworkFactory nf = network.getFactory();
		Node n1, n2;
		network.addNode(n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0)));
		network.addNode(n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 50, (double) 0)));
		Link l1 = nf.createLink(Id.create("1", Link.class), n1, n2);
		l1.setLength(50);
		network.addLink(l1);

		PopulationFactory pf = population.getFactory();

		Plan plan = pf.createPlan();
		Activity act1 = pf.createActivityFromLinkId("h", l1.getId());
		Leg leg = pf.createLeg(TransportMode.car);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(l1.getId(), l1.getId());
		route.setLinkIds(l1.getId(), new ArrayList<Id<Link>>(0), l1.getId());
		leg.setRoute(route);
		Activity act2 = pf.createActivityFromLinkId("w", l1.getId());
		plan.addActivity(act1);
		plan.addLeg(leg);
		plan.addActivity(act2);

		// test simple route, none of the links should be included, as there is no real traffic
		CalcAverageTripLength catl = new CalcAverageTripLength(network);
		catl.run(plan);
		Assertions.assertEquals(0.0, catl.getAverageTripLength(), MatsimTestUtils.EPSILON);
	}

}
