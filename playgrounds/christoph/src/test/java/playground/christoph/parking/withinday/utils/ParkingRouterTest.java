/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingRouterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.parking.withinday.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;

public class ParkingRouterTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(ParkingRouterTest.class);
	
//	public static void main(String[] args) {
//		
//		Config config = ConfigUtils.createConfig();
//		Scenario scenario = ScenarioUtils.createScenario(config);
//		
//		createNetwork(scenario);
//		testAdaptStartOfRoute(scenario, 3);
//		testAdaptEndOfRoute(scenario, 3);
//		testAdaptStartAndEndOfRoute(scenario, 2);
//		testAdaptStartOfShortRoute(scenario, 3);
//		testAdaptEndOfShortRoute(scenario, 3);
//		testAdaptStartAndEndOfShortRoute(scenario, 3);
//	}
	
	public void testAdaptStartOfRoute() {
		
		log.info("testAdaptStartOfRoute");
		
		Scenario scenario = createScenario();
		TestTimeCostFactory testTimeCostFactory = new TestTimeCostFactory();
		TestTimeCost testTimeCost = testTimeCostFactory.createTravelDisutility(null, null);
		ParkingRouter parkingRouter;
		NetworkRoute route;
		
		Id startLinkId = scenario.createId("l5");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 1);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(5, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 2);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(4, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 3);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(3, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 10);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());

		// now make the links more expensive
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 10);
		testTimeCost.setData(scenario.createId("l9"), 10.0, 5.0);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(3, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l9"), 10.0, 5.0);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(3, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l8"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l9"), 10.0, 5.0);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(4, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l7"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l8"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l9"), 10.0, 5.0);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(5, route.getLinkIds().size());
		
		// now l6 .. l9 have all the same costs, therefore l9 should be chosen again
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l6"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l7"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l8"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l9"), 10.0, 5.0);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());
		
//		log.info(route.getStartLinkId());
//		for(Id linkId : route.getLinkIds()) log.info(linkId);
//		log.info(route.getEndLinkId());
	}
	
	public void testAdaptEndOfRoute() {
		
		log.info("testAdaptEndOfRoute");
		
		Scenario scenario = createScenario();
		TestTimeCostFactory testTimeCostFactory = new TestTimeCostFactory();
		TestTimeCost testTimeCost = testTimeCostFactory.createTravelDisutility(null, null);
		ParkingRouter parkingRouter;
		NetworkRoute route;
		
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
				
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 1);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		log.info(route.getStartLinkId());
		for(Id linkId : route.getLinkIds()) log.info(linkId);
		log.info(route.getEndLinkId());
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(5, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 2);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(4, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 3);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(3, route.getLinkIds().size());

		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 10);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());

		// now make the links more expensive
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 10);
		testTimeCost.setData(scenario.createId("l11"), 10.0, 5.0);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(3, route.getLinkIds().size());

		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l11"), 10.0, 5.0);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(3, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l11"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l12"), 10.0, 5.0);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(4, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l11"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l12"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l13"), 10.0, 5.0);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(5, route.getLinkIds().size());
		
		// now l11 .. l14 have all the same costs, therefore l11 should be chosen again
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		testTimeCost.setData(scenario.createId("l11"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l12"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l13"), 10.0, 5.0);
		testTimeCost.setData(scenario.createId("l14"), 10.0, 5.0);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());
	}
	
	public void testAdaptStartAndEndOfRoute() {
		
		log.info("testAdaptStartAndEndOfRoute");
		
		Scenario scenario = createScenario();
		TestTimeCostFactory testTimeCostFactory = new TestTimeCostFactory();
		ParkingRouter parkingRouter;
		NetworkRoute route;
		
		Id startLinkId = scenario.createId("l5");
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 1);
		parkingRouter.adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle, TransportMode.car);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(7, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 2);
		parkingRouter.adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle, TransportMode.car);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(5, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 3);
		parkingRouter.adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle, TransportMode.car);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());
		
		route = createRoute(scenario);
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 10);
		parkingRouter.adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle, TransportMode.car);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());
	}
	
	/*
	 * Test for route that starts and ends on the same link and is not a round-trip.
	 */
	public void testAdaptStartOfShortRoute() {
		
		log.info("testAdaptStartOfShortRoute");
		
		Scenario scenario = createScenario();
		TestTimeCostFactory testTimeCostFactory = new TestTimeCostFactory();
		ParkingRouter parkingRouter;
		NetworkRoute route;
		
		Id startLinkId = scenario.createId("l5");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;
				
		route = new LinkNetworkRouteImpl(scenario.createId("l2"), scenario.createId("l2"));
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		parkingRouter.adaptStartOfCarRoute(route, startLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(1, route.getLinkIds().size());
	}
	
	public void testAdaptEndOfShortRoute() {
		
		log.info("testAdaptEndOfShortRoute");
		
		Scenario scenario = createScenario();
		TestTimeCostFactory testTimeCostFactory = new TestTimeCostFactory();
		ParkingRouter parkingRouter;
		NetworkRoute route;
		
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;

		route = new LinkNetworkRouteImpl(scenario.createId("l2"), scenario.createId("l2"));
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 4);
		parkingRouter.adaptEndOfCarRoute(route, endLinkId, time, person, vehicle);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(1, route.getLinkIds().size());
	}
	
	public void testAdaptStartAndEndOfShortRoute() {
		
		log.info("testAdaptStartAndEndOfShortRoute");
		
		Scenario scenario = createScenario();
		TestTimeCostFactory testTimeCostFactory = new TestTimeCostFactory();
		ParkingRouter parkingRouter;
		NetworkRoute route;
		
		
		Id startLinkId = scenario.createId("l5");
		Id endLinkId = scenario.createId("l10");
		double time = 100.0;
		Person person = null;
		Vehicle vehicle = null;

		route = new LinkNetworkRouteImpl(scenario.createId("l2"), scenario.createId("l2"));
		parkingRouter = createParkingRouter(scenario, testTimeCostFactory, 3);
		parkingRouter.adaptStartAndEndOfRoute(route, startLinkId, endLinkId, time, person, vehicle, TransportMode.car);
		assertEquals(true, checkRouteValidity(scenario, route));
		assertEquals(2, route.getLinkIds().size());
	}
	
	private boolean checkRouteValidity(Scenario scenario, NetworkRoute route) {
		
		// for short routes
		if (route.getStartLinkId().equals(route.getEndLinkId())) {
			return route.getLinkIds().size() == 0;
		}
		
		List<Id> linkIds = new ArrayList<Id>();
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		
		for (int i = 0; i < linkIds.size() - 1; i++) {
			Id fromId = linkIds.get(i);
			Id toId = linkIds.get(i + 1);
			
			Link fromLink = scenario.getNetwork().getLinks().get(fromId);
			Link toLink = scenario.getNetwork().getLinks().get(toId);
			
			if (!fromLink.getToNode().getId().equals(toLink.getFromNode().getId())) return false;
		}
		
		return true;
	}
	
	private ParkingRouter createParkingRouter(Scenario scenario, TestTimeCostFactory testTimeCostFactory, int nodesToCheck) {
		
		TestTimeCost testTimeCost = testTimeCostFactory.createTravelDisutility(null, null);
		
		// by default: 10s travel time, 1 monetary unit travel cost
		testTimeCost.setData(scenario.createId("l0"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l1"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l2"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l3"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l4"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l5"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l6"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l7"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l8"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l9"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l10"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l11"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l12"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l13"), 10.0, 1.0);
		testTimeCost.setData(scenario.createId("l14"), 10.0, 1.0);
		
		TravelTime carTravelTime = testTimeCost;
		TravelTime walkTravelTime = null;
		
		TripRouter tripRouter = null;
		ParkingRouter parkingRouter = new ParkingRouter(scenario, carTravelTime, walkTravelTime, testTimeCostFactory, tripRouter, nodesToCheck);
				
		return parkingRouter;
	}
	
	private Scenario createScenario() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);
		
		return scenario;
	}
	
	/**
	 * Creates a simple network to be used in tests.
	 * Links are uni-directional, from left to right and bottom to up.
	 * I.e. routes like n7-n6-n2-n1 are not valid since n2->n1 is not possible.
	 *
	 * <pre>
	 *                                           (n9)
	 *                                            |
	 *                                            |
	 *                                            |
	 *                                           l10
	 *                                            |
	 *                                            |
	 *                                            | 
	 *                                -----------(n8)        
	 *                               /         /  |  \
	 *                              /         /   |   \
	 *                             /         /    |    \
	 *                            l11      l12   l13   l14
	 *                           /         /      |      \
	 *                          /         /       |       \
	 *                         /         /        |        \
	 *   (n0)--l0--(n1)--l1--(n2)--l2--(n3)--l3--(n4)--l4--(n5)
	 *     \        |        /         /
	 *      \       |       /         /
	 *       \      |      /         /
	 *        l6    l7    l8        l9
	 *         \    |    /         /
	 *          \   |   /         /
	 *           \  |  /         /
	 *             (n6)-----------
	 *              |
	 *              |
	 *              |
	 *              l5
	 *              |
	 *              |
	 *              |
	 *             (n7)
	 * </pre>
	 *
	 * @param scenario
	 */
	private void createNetwork(Scenario scenario) {
		
		NetworkFactory networkFactory = scenario.getNetwork().getFactory();
		
		Node n0 = networkFactory.createNode(scenario.createId("n0"), scenario.createCoord(0.0, 0.0));
		Node n1 = networkFactory.createNode(scenario.createId("n1"), scenario.createCoord(1000.0, 0.0));
		Node n2 = networkFactory.createNode(scenario.createId("n2"), scenario.createCoord(2000.0, 0.0));
		Node n3 = networkFactory.createNode(scenario.createId("n3"), scenario.createCoord(3000.0, 0.0));
		Node n4 = networkFactory.createNode(scenario.createId("n4"), scenario.createCoord(4000.0, 0.0));
		Node n5 = networkFactory.createNode(scenario.createId("n5"), scenario.createCoord(5000.0, 0.0));
		Node n6 = networkFactory.createNode(scenario.createId("n6"), scenario.createCoord(1000.0, -1000.0));
		Node n7 = networkFactory.createNode(scenario.createId("n7"), scenario.createCoord(1000.0, -2000.0));
		Node n8 = networkFactory.createNode(scenario.createId("n8"), scenario.createCoord(4000.0, 1000.0));
		Node n9 = networkFactory.createNode(scenario.createId("n9"), scenario.createCoord(4000.0, 2000.0));
		
		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		scenario.getNetwork().addNode(n4);
		scenario.getNetwork().addNode(n5);
		scenario.getNetwork().addNode(n6);
		scenario.getNetwork().addNode(n7);
		scenario.getNetwork().addNode(n8);
		scenario.getNetwork().addNode(n9);

		Link l0 = networkFactory.createLink(scenario.createId("l0"), n0, n1);
		l0.setLength(1000.0);
		l0.setFreespeed(10.0);
		
		Link l1 = networkFactory.createLink(scenario.createId("l1"), n1, n2);
		l1.setLength(1000.0);
		l1.setFreespeed(10.0);
		
		Link l2 = networkFactory.createLink(scenario.createId("l2"), n2, n3);
		l2.setLength(1000.0);
		l2.setFreespeed(10.0);
		
		Link l3 = networkFactory.createLink(scenario.createId("l3"), n3, n4);
		l3.setLength(1000.0);
		l3.setFreespeed(10.0);

		Link l4 = networkFactory.createLink(scenario.createId("l4"), n4, n5);
		l4.setLength(1000.0);
		l4.setFreespeed(10.0);

		Link l5 = networkFactory.createLink(scenario.createId("l5"), n7, n6);
		l5.setLength(1000.0);
		l5.setFreespeed(10.0);
		
		Link l6 = networkFactory.createLink(scenario.createId("l6"), n6, n0);
		l6.setLength(1415.0);
		l6.setFreespeed(10.0);

		Link l7 = networkFactory.createLink(scenario.createId("l7"), n6, n1);
		l7.setLength(1000.0);
		l7.setFreespeed(10.0);

		Link l8 = networkFactory.createLink(scenario.createId("l8"), n6, n2);
		l8.setLength(1415.0);
		l8.setFreespeed(10.0);

		Link l9 = networkFactory.createLink(scenario.createId("l9"), n6, n3);
		l9.setLength(2237.0);
		l9.setFreespeed(10.0);

		Link l10 = networkFactory.createLink(scenario.createId("l10"), n8, n9);
		l10.setLength(1000.0);
		l10.setFreespeed(10.0);

		Link l11 = networkFactory.createLink(scenario.createId("l11"), n2, n8);
		l11.setLength(2237.0);
		l11.setFreespeed(10.0);

		Link l12 = networkFactory.createLink(scenario.createId("l12"), n3, n8);
		l12.setLength(1415.0);
		l12.setFreespeed(10.0);

		Link l13 = networkFactory.createLink(scenario.createId("l13"), n4, n8);
		l13.setLength(1000.0);
		l13.setFreespeed(10.0);

		Link l14 = networkFactory.createLink(scenario.createId("l14"), n5, n8);
		l14.setLength(1415.0);
		l14.setFreespeed(10.0);
		
		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		scenario.getNetwork().addLink(l4);
		scenario.getNetwork().addLink(l5);
		scenario.getNetwork().addLink(l6);
		scenario.getNetwork().addLink(l7);
		scenario.getNetwork().addLink(l8);
		scenario.getNetwork().addLink(l9);
		scenario.getNetwork().addLink(l10);
		scenario.getNetwork().addLink(l11);
		scenario.getNetwork().addLink(l12);
		scenario.getNetwork().addLink(l13);
		scenario.getNetwork().addLink(l14);
	}
	
	private NetworkRoute createRoute(Scenario scenario) {
		
		NetworkRoute route = new LinkNetworkRouteImpl(scenario.createId("l0"), scenario.createId("l4"));
		List<Id<Link>> routeLinkIds = new ArrayList<Id<Link>>();
		routeLinkIds.add(scenario.createId("l1"));
		routeLinkIds.add(scenario.createId("l2"));
		routeLinkIds.add(scenario.createId("l3"));
		route.setLinkIds(scenario.createId("l0"), routeLinkIds, scenario.createId("l4"));
		
		return route;
	}
	
	/*package*/ static class TestTimeCostFactory implements TravelDisutilityFactory {

		private TestTimeCost testTimeCost = new TestTimeCost();
		
		@Override
		public TestTimeCost createTravelDisutility(
				TravelTime timeCalculator,
				PlanCalcScoreConfigGroup cnScoringGroup) {
			return testTimeCost;
		}
		
	}
	
	/*package*/ static class TestTimeCost implements TravelTime, TravelDisutility {

		private final Map<Id, Double> travelTimes = new HashMap<Id, Double>();
		private final Map<Id, Double> travelCosts = new HashMap<Id, Double>();

		public void setData(final Id id, final double travelTime, final double travelCost) {
			this.travelTimes.put(id, Double.valueOf(travelTime));
			this.travelCosts.put(id, Double.valueOf(travelCost));
		}

		@Override
		public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
			return this.travelTimes.get(link.getId()).doubleValue();
		}

		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			return this.travelCosts.get(link.getId()).doubleValue();
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 0;
		}

	}
}