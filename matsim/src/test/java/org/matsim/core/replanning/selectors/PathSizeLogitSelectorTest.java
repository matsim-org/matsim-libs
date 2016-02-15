/* ***********************************************, ************************ *
 * project: org.matsim.*1, Person.class
 * BestPlanSelectorTest.java
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

package org.matsim.core.replanning.selectors;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

/**
 * Tests for {@link PathSizeLogitSelector}.
 *
 * @author laemmel
 */
public class PathSizeLogitSelectorTest extends AbstractPlanSelectorTest {

	private final static Logger log = Logger.getLogger(RandomPlanSelectorTest.class);

	private Network network = null;
	private Config config = null;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.config = loadConfig(null); // required for planCalcScore.beta to be defined
		config.planCalcScore().setBrainExpBeta(2.0);
		config.planCalcScore().setPathSizeLogitBeta(2.0);
		this.network = null;
	}

	@Override
	protected void tearDown() throws Exception {
		this.network = null;
		this.config = null;
		super.tearDown();
	}

	@Override
	protected PlanSelector getPlanSelector() {
		return new PathSizeLogitSelector(config.planCalcScore(), createNetwork());
	}

	@Override
	public void testNegativeScore() {
		this.network = createNetwork();
		PlanSelector selector = getPlanSelector();

		Link l1 = network.getLinks().get(Id.create("1", Link.class));
		Link l2 = network.getLinks().get(Id.create("2", Link.class));
		Link l3 = network.getLinks().get(Id.create("3", Link.class));
		Link l4 = network.getLinks().get(Id.create("4", Link.class));
		Link l5 = network.getLinks().get(Id.create("5", Link.class));
		Link l6 = network.getLinks().get(Id.create("6", Link.class));
		Link l7 = network.getLinks().get(Id.create("7", Link.class));

		// test with only one plan...
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl p1 = new PlanImpl(person);
		Activity a = new ActivityImpl("h", l6.getId());
		Activity b = new ActivityImpl("w", l7.getId());
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);

		NetworkRoute r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		ArrayList<Id<Link>> srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l1.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p1.addActivity(a);
		p1.addLeg(leg);
		p1.addActivity(b);
		p1.setScore(-10.0);
		person.addPlan(p1);

		assertNotNull(selector.selectPlan(person));

		// ... test with multiple plans that all have negative score
		a = new org.matsim.core.population.ActivityImpl("h", l6.getId());
		b = new org.matsim.core.population.ActivityImpl("w", l7.getId());
		leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		PlanImpl p2 = new org.matsim.core.population.PlanImpl(person);
		r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l2.getId());
		srcRoute.add(l3.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p2.addActivity(a);
		p2.addLeg(leg);
		p2.addActivity(b);
		p2.setScore(-10.0);
		person.addPlan(p2);

		a = new org.matsim.core.population.ActivityImpl("h", l6.getId());
		b = new org.matsim.core.population.ActivityImpl("w", l7.getId());
		leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		PlanImpl p3 = new org.matsim.core.population.PlanImpl(person);
		r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l2.getId());
		srcRoute.add(l4.getId());
		srcRoute.add(l5.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p3.addActivity(a);
		p3.addLeg(leg);
		p3.addActivity(b);
		p3.setScore(-10.0);
		person.addPlan(p3);
		assertNotNull(selector.selectPlan(person));

		// ... and test with multiple plans where the sum of all scores stays negative
		p3.setScore(15.0);
		assertNotNull(selector.selectPlan(person));

		// test with only one plan, but with NEGATIVE_INFINITY...
		person = PopulationUtils.createPerson(Id.create(1, Person.class));
		p1 = new org.matsim.core.population.PlanImpl(person);
		a = new org.matsim.core.population.ActivityImpl("h", l6.getId());
		b = new org.matsim.core.population.ActivityImpl("w", l7.getId());
		leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l1.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p1.addActivity(a);
		p1.addLeg(leg);
		p1.addActivity(b);
		p1.setScore(Double.NEGATIVE_INFINITY);
		person.addPlan(p1);
		assertNotNull(selector.selectPlan(person));

	}

	@Override
	public void testZeroScore() {
		this.network = createNetwork();
		PlanSelector selector = getPlanSelector();
		Link l1 = network.getLinks().get(Id.create("1", Link.class));
		Link l6 = network.getLinks().get(Id.create("6", Link.class));
		Link l7 = network.getLinks().get(Id.create("7", Link.class));

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl p1 = new org.matsim.core.population.PlanImpl(person);
		ActivityImpl a = new org.matsim.core.population.ActivityImpl("h", l6.getId());
		ActivityImpl b = new org.matsim.core.population.ActivityImpl("w", l7.getId());
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);

		NetworkRoute r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		ArrayList<Id<Link>> srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l1.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p1.addActivity(a);
		p1.addLeg(leg);
		p1.addActivity(b);
		p1.setScore(0.0);
		person.addPlan(p1);

		assertNotNull(selector.selectPlan(person));
	}

	public void testPathSizeLogitSelector() {
		this.network = createNetwork();

		Link l1 = network.getLinks().get(Id.create("1", Link.class));
		Link l2 = network.getLinks().get(Id.create("2", Link.class));
		Link l3 = network.getLinks().get(Id.create("3", Link.class));
		Link l4 = network.getLinks().get(Id.create("4", Link.class));
		Link l5 = network.getLinks().get(Id.create("5", Link.class));
		Link l6 = network.getLinks().get(Id.create("6", Link.class));
		Link l7 = network.getLinks().get(Id.create("7", Link.class));

		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl p1 = new org.matsim.core.population.PlanImpl(person);
		ActivityImpl a = new org.matsim.core.population.ActivityImpl("h", l6.getId());
		ActivityImpl b = new org.matsim.core.population.ActivityImpl("w", l7.getId());
		LegImpl leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		NetworkRoute r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		ArrayList<Id<Link>> srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l1.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p1.addActivity(a);
		p1.addLeg(leg);
		p1.addActivity(b);
		p1.setScore(-10.0);
		person.addPlan(p1);

		a = new org.matsim.core.population.ActivityImpl("h", l6.getId());
		b = new org.matsim.core.population.ActivityImpl("w", l7.getId());
		leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		PlanImpl p2 = new org.matsim.core.population.PlanImpl(person);
		r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l2.getId());
		srcRoute.add(l3.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p2.addActivity(a);
		p2.addLeg(leg);
		p2.addActivity(b);
		p2.setScore(-10.0);
		person.addPlan(p2);

		a = new org.matsim.core.population.ActivityImpl("h", l6.getId());
		b = new org.matsim.core.population.ActivityImpl("w", l7.getId());
		leg = new org.matsim.core.population.LegImpl(TransportMode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		PlanImpl p3 = new org.matsim.core.population.PlanImpl(person);
		r = new LinkNetworkRouteImpl(l6.getId(), l7.getId());
		srcRoute = new ArrayList<Id<Link>>();
		srcRoute.add(l2.getId());
		srcRoute.add(l4.getId());
		srcRoute.add(l5.getId());
		r.setLinkIds(l6.getId(), srcRoute, l7.getId());
		r.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(r, network));
		leg.setRoute(r);
		p3.addActivity(a);
		p3.addLeg(leg);
		p3.addActivity(b);
		p3.setScore(-10.0);
		person.addPlan(p3);

		PathSizeLogitSelector selector = new PathSizeLogitSelector(this.config.planCalcScore(), network);
		int cnt1 = 0;
		int cnt2 = 0;
		int cnt3 = 0;

		for (int i = 0; i < 10000; i++) {
			Plan plan = selector.selectPlan(person);
			if (plan == p1) cnt1++;
			if (plan == p2) cnt2++;
			if (plan == p3) cnt3++;
		}

		log.info("Plan 1 was returned " + cnt1 + " times.");
		log.info("Plan 2 was returned " + cnt2 + " times.");
		log.info("Plan 3 was returned " + cnt3 + " times.");

		assertEquals(5732, cnt1);
		assertEquals(2136, cnt2);
		assertEquals(2132, cnt3);
	}

	private NetworkImpl createNetwork() {
		//we use a simple "red bus / blue bus paradox" network
		// Sketch of the network
		// Note: Node (4) is necessary since MATSim still operates on node based routes and without this
		// additional node (4) the route (1)-(2)-(3) would be ambiguous.
		// Since activities are performed on links we need to additional links: (6)-(1) and (3)-(5).
		//
		//             (6)
		//              |6
		//             (1)
		//             / \
		//            /   \
		//           /     |
		//          |      |
		//          |      2
		//          1      |
		//          |      |
		//          |    (2)
		//           \   /\4
		//            \ 3(4)
		//             \|/5
		//             (3)
		//              |7
		//             (5)

		NetworkImpl network = NetworkImpl.createNetwork();
		Node n1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 10));
		Node n2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 3, (double) 2));
		Node n3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 0, (double) 0));
		Node n4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 4, (double) 1));
		final double y = -1;
		Node n5 = network.createAndAddNode(Id.create(5, Node.class), new Coord((double) 0, y));
		Node n6 = network.createAndAddNode(Id.create(6, Node.class), new Coord((double) 0, (double) 11));
		network.createAndAddLink(Id.create(1, Link.class), n1, n3, 10, 1, 10, 1);
		network.createAndAddLink(Id.create(2, Link.class), n1, n2, 8, 1, 10, 1);
		network.createAndAddLink(Id.create(3, Link.class), n2, n3, 2, 1, 10, 1);
		network.createAndAddLink(Id.create(4, Link.class), n2, n4, 1, 1, 10, 1);
		network.createAndAddLink(Id.create(5, Link.class), n4, n3, 1, 1, 10, 1);
		network.createAndAddLink(Id.create(6, Link.class), n6, n1, 1, 1, 10, 1);
		network.createAndAddLink(Id.create(7, Link.class), n3, n5, 1, 1, 10, 1);

		return network;
	}

}
