/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.replanning.selectors;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.utils.geometry.CoordImpl;

/**
 * Tests for {@link PathSizeLogitSelector}.
 *
 * @author laemmel
 */
public class PathSizeLogitSelectorTest extends AbstractPlanSelectorTest {

	private final static Logger log = Logger.getLogger(RandomPlanSelectorTest.class);

	@Override
	public void setUp() throws Exception {
		super.setUp();
		Config config = loadConfig(null); // required for planCalcScore.beta to be defined
		config.charyparNagelScoring().setBrainExpBeta(2.0);
		config.charyparNagelScoring().setPathSizeLogitBeta(2.0);
	}

	@Override
	protected PlanSelector getPlanSelector() {
		return new PathSizeLogitSelector();
	}

	@Override
	public void testNegativeScore() {
		NetworkLayer network = createNetwork();
		PlanSelector selector = getPlanSelector();

		Link l6 = network.getLink("6");
		Link l7 = network.getLink("7");

		Node n1 = network.getNode("1");
		Node n2 = network.getNode("2");
		Node n3 = network.getNode("3");
		Node n4 = network.getNode("4");

		// test with only one plan...
		Person person = new PersonImpl(new IdImpl(1));
		Plan p1 = new org.matsim.population.PlanImpl(person);
		Act a = new org.matsim.population.ActImpl("h", l6);
		Act b = new org.matsim.population.ActImpl("w", l7);
		Leg leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);

		CarRoute r = new NodeCarRoute(l6, l7);
		ArrayList<Node> srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p1.addAct(a);
		p1.addLeg(leg);
		p1.addAct(b);
		p1.setScore(-10);
		person.addPlan(p1);

		assertNotNull(selector.selectPlan(person));

		// ... test with multiple plans that all have negative score
		a = new org.matsim.population.ActImpl("h", l6);
		b = new org.matsim.population.ActImpl("w", l7);
		leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		Plan p2 = new org.matsim.population.PlanImpl(person);
		r = new NodeCarRoute(l6, l7);
		srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n2);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p2.addAct(a);
		p2.addLeg(leg);
		p2.addAct(b);
		p2.setScore(-10);
		person.addPlan(p2);

		a = new org.matsim.population.ActImpl("h", l6);
		b = new org.matsim.population.ActImpl("w", l7);
		leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		Plan p3 = new org.matsim.population.PlanImpl(person);
		r = new NodeCarRoute(l6, l7);
		srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n2);
		srcRoute.add(n4);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p3.addAct(a);
		p3.addLeg(leg);
		p3.addAct(b);
		p3.setScore(-10);
		person.addPlan(p3);
		assertNotNull(selector.selectPlan(person));
		
		// ... and test with multiple plans where the sum of all scores stays negative
		p3.setScore(15);
		assertNotNull(selector.selectPlan(person));
		
		// test with only one plan, but with NEGATIVE_INFINITY...
		person = new PersonImpl(new IdImpl(1));
		p1 = new org.matsim.population.PlanImpl(person);
		a = new org.matsim.population.ActImpl("h", l6);
		b = new org.matsim.population.ActImpl("w", l7);
		leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		r = new NodeCarRoute(l6, l7);
		srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p1.addAct(a);
		p1.addLeg(leg);
		p1.addAct(b);
		p1.setScore(Double.NEGATIVE_INFINITY);
		person.addPlan(p1);		
		assertNotNull(selector.selectPlan(person));
		
	}

	@Override
	public void testZeroScore() {
		NetworkLayer network = createNetwork();
		PlanSelector selector = getPlanSelector();
		Link l6 = network.getLink("6");
		Link l7 = network.getLink("7");
		
		Node n1 = network.getNode("1");
		Node n3 = network.getNode("3");
		
		Person person = new PersonImpl(new IdImpl(1));
		Plan p1 = new org.matsim.population.PlanImpl(person);
		Act a = new org.matsim.population.ActImpl("h", l6);
		Act b = new org.matsim.population.ActImpl("w", l7);
		Leg leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		
		CarRoute r = new NodeCarRoute(l6, l7);
		ArrayList<Node> srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p1.addAct(a);
		p1.addLeg(leg);
		p1.addAct(b);
		p1.setScore(0);
		person.addPlan(p1);
		
		assertNotNull(selector.selectPlan(person));
	}

	public void testPathSizeLogitSelector() {
		NetworkLayer network = createNetwork();

		Link l6 = network.getLink("6");
		Link l7 = network.getLink("7");
		
		Node n1 = network.getNode("1");
		Node n2 = network.getNode("2");
		Node n3 = network.getNode("3");
		Node n4 = network.getNode("4");

		Person person = new PersonImpl(new IdImpl(1));
		Plan p1 = new org.matsim.population.PlanImpl(person);
		Act a = new org.matsim.population.ActImpl("h", l6);
		Act b = new org.matsim.population.ActImpl("w", l7);
		Leg leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		CarRoute r = new NodeCarRoute(l6, l7);
		ArrayList<Node> srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p1.addAct(a);
		p1.addLeg(leg);
		p1.addAct(b);
		p1.setScore(-10);
		person.addPlan(p1);
	
		a = new org.matsim.population.ActImpl("h", l6);
		b = new org.matsim.population.ActImpl("w", l7);
		leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		Plan p2 = new org.matsim.population.PlanImpl(person);
		r = new NodeCarRoute(l6, l7);
		srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n2);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p2.addAct(a);
		p2.addLeg(leg);
		p2.addAct(b);
		p2.setScore(-10);
		person.addPlan(p2);
		
		a = new org.matsim.population.ActImpl("h", l6);
		b = new org.matsim.population.ActImpl("w", l7);
		leg = new org.matsim.population.LegImpl(BasicLeg.Mode.car);
		leg.setDepartureTime(0.0);
		leg.setTravelTime(10.0);
		leg.setArrivalTime(10.0);
		Plan p3 = new org.matsim.population.PlanImpl(person);
		r = new NodeCarRoute(l6, l7);
		srcRoute = new ArrayList<Node>();
		srcRoute.add(n1);
		srcRoute.add(n2);
		srcRoute.add(n4);
		srcRoute.add(n3);
		r.setNodes(l6, srcRoute, l7);
		leg.setRoute(r);
		p3.addAct(a);
		p3.addLeg(leg);
		p3.addAct(b);
		p3.setScore(-10);
		person.addPlan(p3);

		PathSizeLogitSelector selector = new PathSizeLogitSelector();
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

	private NetworkLayer createNetwork() {
		//we use a simple "red bus / blue bus paradox" network
		// Sketch of the network
		// Note: Node (4) is necessary since MATSim still operates on node based routes and without this 
		// additional node (4) the route (1)-(2)-(3) would be ambiguous.
		// Since activities are performed on links we need to additional links: (6)-(1) and (3)-(5). 
		//            
		//             (6)
		//              |
		//             (1)
		//             / \
		//            /   \
		//           /     |
		//          |      |
		//          |      |
		//          |      |
		//          |      |
		//          |    (2)
		//           \   /\
		//            \ /(4)
		//             \|/
		//             (3)
		//              |
		//             (5)
		
		NetworkLayer network = new NetworkLayer();
		Node n1 = network.createNode(new IdImpl(1), new CoordImpl(0,10));
		Node n2 = network.createNode(new IdImpl(2), new CoordImpl(3,2));
		Node n3 = network.createNode(new IdImpl(3), new CoordImpl(0,0));
		Node n4 = network.createNode(new IdImpl(4), new CoordImpl(4,1));
		Node n5 = network.createNode(new IdImpl(5), new CoordImpl(0,-1));
		Node n6 = network.createNode(new IdImpl(6), new CoordImpl(0,11));
		network.createLink(new IdImpl(1), n1, n3, 10, 1, 10, 1);
		network.createLink(new IdImpl(2), n1, n2, 8, 1, 10, 1);
		network.createLink(new IdImpl(3), n2, n3, 2, 1, 10, 1);
		network.createLink(new IdImpl(4), n2, n4, 1, 1, 10, 1);
		network.createLink(new IdImpl(5), n4, n3, 1, 1, 10, 1);
		network.createLink(new IdImpl(6), n6, n1, 1, 1, 10, 1);
		network.createLink(new IdImpl(7), n3, n5, 1, 1, 10, 1);

		return network;
	}

}
