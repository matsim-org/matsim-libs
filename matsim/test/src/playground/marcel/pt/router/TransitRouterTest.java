/* *********************************************************************** *
 * project: org.matsim.*
 * NewTransitRouteTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.router;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.routes.ExperimentalTransitRoute;

public class TransitRouterTest extends TestCase {

	public void testSingleLine() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		TransitRouter router = new TransitRouter(f.schedule, config);
		Coord fromCoord = f.scenario.createCoord(3800, 5100);
		Coord toCoord = f.scenario.createCoord(16100, 5050);
		List<Leg> legs = router.calcRoute(fromCoord, toCoord, 5.0*3600);
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(f.scenario.createId("0"), ptRoute.getAccessStopId());
		assertEquals(f.scenario.createId("6"), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(f.scenario.createId("blue A > I"), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 29.0 * 60 + // agent takes the *:06 course, arriving in D at *:29
				CoordUtils.calcDistance(f.schedule.getFacilities().get(f.scenario.createId("6")).getCoord(), toCoord) / config.beelineWalkSpeed;
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	public void testFromToSameStop() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		TransitRouter router = new TransitRouter(f.schedule, config);
		Coord fromCoord = f.scenario.createCoord(3800, 5100);
		Coord toCoord = f.scenario.createCoord(4100, 5050);
		List<Leg> legs = router.calcRoute(fromCoord, toCoord, 5.0*3600);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = CoordUtils.calcDistance(fromCoord, toCoord) / config.beelineWalkSpeed;
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	public void xtestSingleLine_DifferentWaitingTime() {
		Fixture f = new Fixture();
		f.init();
		fail("not yet implemented.");
	}

	public void testLineChange() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		TransitRouter router = new TransitRouter(f.schedule, config);
		Coord toCoord = f.scenario.createCoord(16100, 10050);
		List<Leg> legs = router.calcRoute(f.scenario.createCoord(3800, 5100), toCoord, 6.0*3600);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(TransportMode.walk, legs.get(4).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(f.scenario.createId("0"), ptRoute.getAccessStopId());
		assertEquals(f.scenario.createId("4"), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(f.scenario.createId("blue A > I"), ptRoute.getRouteId());
		assertTrue("expected TransitRoute in leg.", legs.get(3).getRoute() instanceof ExperimentalTransitRoute);
		ptRoute = (ExperimentalTransitRoute) legs.get(3).getRoute();
		assertEquals(f.scenario.createId("18"), ptRoute.getAccessStopId());
		assertEquals(f.scenario.createId("19"), ptRoute.getEgressStopId());
		assertEquals(f.greenLine.getId(), ptRoute.getLineId());
		assertEquals(f.scenario.createId("green clockwise"), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 31.0 * 60 + // agent takes the *:06 course, arriving in C at *:18, departing at *:21, arriving in K at*:31
				CoordUtils.calcDistance(f.schedule.getFacilities().get(f.scenario.createId("19")).getCoord(), toCoord) / config.beelineWalkSpeed;
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	public void testFasterAlternative() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		TransitRouter router = new TransitRouter(f.schedule, config);
		Coord toCoord = f.scenario.createCoord(28100, 4950);
		List<Leg> legs = router.calcRoute(f.scenario.createCoord(3800, 5100), toCoord, 5.0*3600 + 40.0*60);
		assertEquals(4, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.pt, legs.get(2).getMode());
		assertEquals(TransportMode.walk, legs.get(3).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(f.scenario.createId("0"), ptRoute.getAccessStopId());
		assertEquals(f.scenario.createId("4"), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(f.scenario.createId("blue A > I"), ptRoute.getRouteId());
		assertTrue("expected TransitRoute in leg.", legs.get(2).getRoute() instanceof ExperimentalTransitRoute);
		ptRoute = (ExperimentalTransitRoute) legs.get(2).getRoute();
		assertEquals(f.scenario.createId("4"), ptRoute.getAccessStopId());
		assertEquals(f.scenario.createId("12"), ptRoute.getEgressStopId());
		assertEquals(f.redLine.getId(), ptRoute.getLineId());
		assertEquals(f.scenario.createId("red C > G"), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 29.0 * 60 + // agent takes the *:46 course, arriving in C at *:58, departing at *:00, arriving in G at*:09
				CoordUtils.calcDistance(f.schedule.getFacilities().get(f.scenario.createId("12")).getCoord(), toCoord) / config.beelineWalkSpeed;
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	public void testTransferWeights() {
		/* idea: travel from C to F
		 * If starting at the right time, one could take the red line to G and travel back with blue to F.
		 * If one doesn't want to switch lines, one could take the blue line from C to F directly.
		 * Using the red line (dep *:00, change at G *:09/*:12) results in an arrival time of *:19,
		 * using the blue line only (dep *:02) results in an arrival time of *:23. So the line switch
		 * cost must be larger than 4 minutes to have an effect.
		 */
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		config.costLineSwitch = 0;
		TransitRouter router = new TransitRouter(f.schedule, config);
		List<Leg> legs = router.calcRoute(f.scenario.createCoord(11900, 5100), f.scenario.createCoord(24100, 4950), 6.0*3600 - 5.0*60);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.redLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getLineId());
		assertEquals(TransportMode.walk, legs.get(4).getMode());

		config.costLineSwitch = 300.0 * -config.marginalUtilityOfTravelTimeTransit; // corresponds to 5 minutes transit travel time
		legs = router.calcRoute(f.scenario.createCoord(11900, 5100), f.scenario.createCoord(24100, 4950), 6.0*3600 - 5.0*60);
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
	}

	public void testAfterMidnight() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		TransitRouter router = new TransitRouter(f.schedule, config);
		Coord toCoord = f.scenario.createCoord(16100, 5050);
		List<Leg> legs = router.calcRoute(f.scenario.createCoord(3800, 5100), toCoord, 25.0*3600);
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(f.scenario.createId("0"), ptRoute.getAccessStopId());
		assertEquals(f.scenario.createId("6"), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(f.scenario.createId("blue A > I"), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 4*3600 + 29.0 * 60 + // arrival at 05:29 at D
				CoordUtils.calcDistance(f.schedule.getFacilities().get(f.scenario.createId("6")).getCoord(), toCoord) / config.beelineWalkSpeed;
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	public void testCoordFarAway() {
		Fixture f = new Fixture();
		f.init();
		TransitRouter router = new TransitRouter(f.schedule);
		List<Leg> legs = router.calcRoute(f.scenario.createCoord(-2000, 0), f.scenario.createCoord(+42000, 0), 5.5*3600); // should map to stops A and I
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(f.scenario.createId("0"), ptRoute.getAccessStopId());
		assertEquals(f.scenario.createId("16"), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(f.scenario.createId("blue A > I"), ptRoute.getRouteId());
	}
	
	/**
	 * In rare cases, Dijkstra may choose to go along two walk links to get from one location to another.
	 * Test, that still only one walk leg with the correct start and end points/links is returned.
	 * 
	 * Generates the following network for testing:
	 * <pre>
	 *                (5)
	 *                 |
	 *                 3
	 *                 |
	 * (1)---1---(2)  (4)  (6)---4---(7)
	 *                 |
	 *                 2
	 *                 |
	 *                (3)
	 * </pre>
	 * Each link represents a transit line. Between the stops (2) and (4) and also
	 * between (4) and (6) agents must walk.
	 */
	public void testDoubleWalk() {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseTransit(true);
		TransitRouterConfig routerConfig = new TransitRouterConfig();
		routerConfig.searchRadius = 500.0;
		routerConfig.beelineWalkConnectionDistance = 100.0;
		routerConfig.beelineWalkSpeed = 10.0; // so the agents can walk the distance in 10 seconds
		
		double x = 0;
		Coord coord1 = scenario.createCoord(x, 0);
		x += 1000;
		Coord coord2 = scenario.createCoord(x, 0);
		x += (routerConfig.beelineWalkConnectionDistance * 0.75);
		Coord coord3 = scenario.createCoord(x, -1000);
		Coord coord4 = scenario.createCoord(x, 0);
		Coord coord5 = scenario.createCoord(x, 1000);
		x += (routerConfig.beelineWalkConnectionDistance * 0.75);
		Coord coord6 = scenario.createCoord(x, 0);
		x += 1000;
		Coord coord7 = scenario.createCoord(x, 0);
		
		// network
		NetworkImpl network = scenario.getNetwork();
		NodeImpl node1 = (NodeImpl) network.getBuilder().createNode(scenario.createId("1"));
		node1.setCoord(coord1);
		NodeImpl node2 = (NodeImpl) network.getBuilder().createNode(scenario.createId("2"));
		node2.setCoord(coord2);
		NodeImpl node3 = (NodeImpl) network.getBuilder().createNode(scenario.createId("3"));
		node3.setCoord(coord3);
		NodeImpl node4 = (NodeImpl) network.getBuilder().createNode(scenario.createId("4"));
		node4.setCoord(coord4);
		NodeImpl node5 = (NodeImpl) network.getBuilder().createNode(scenario.createId("5"));
		node5.setCoord(coord5);
		NodeImpl node6 = (NodeImpl) network.getBuilder().createNode(scenario.createId("6"));
		node6.setCoord(coord6);
		NodeImpl node7 = (NodeImpl) network.getBuilder().createNode(scenario.createId("7"));
		node7.setCoord(coord7);
		network.getNodes().put(node1.getId(), node1);
		network.getNodes().put(node2.getId(), node2);
		network.getNodes().put(node3.getId(), node3);
		network.getNodes().put(node4.getId(), node4);
		network.getNodes().put(node5.getId(), node5);
		network.getNodes().put(node6.getId(), node6);
		network.getNodes().put(node7.getId(), node7);
		LinkImpl link1 = (LinkImpl) network.getBuilder().createLink(scenario.createId("1"), node1.getId(), node2.getId());
		LinkImpl link2 = (LinkImpl) network.getBuilder().createLink(scenario.createId("2"), node3.getId(), node4.getId());
		LinkImpl link3 = (LinkImpl) network.getBuilder().createLink(scenario.createId("3"), node4.getId(), node5.getId());
		LinkImpl link4 = (LinkImpl) network.getBuilder().createLink(scenario.createId("4"), node6.getId(), node7.getId());
		network.getLinks().put(link1.getId(), link1);
		network.getLinks().put(link2.getId(), link2);
		network.getLinks().put(link3.getId(), link3);
		network.getLinks().put(link4.getId(), link4);
		
		// schedule
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleBuilder sb = schedule.getBuilder();
		
		TransitStopFacility stop1 = sb.createTransitStopFacility(scenario.createId("1"), coord1, false);
		TransitStopFacility stop2 = sb.createTransitStopFacility(scenario.createId("2"), coord2, false);
		TransitStopFacility stop3 = sb.createTransitStopFacility(scenario.createId("3"), coord3, false);
		TransitStopFacility stop4 = sb.createTransitStopFacility(scenario.createId("4"), coord4, false);
		TransitStopFacility stop5 = sb.createTransitStopFacility(scenario.createId("5"), coord5, false);
		TransitStopFacility stop6 = sb.createTransitStopFacility(scenario.createId("6"), coord6, false);
		TransitStopFacility stop7 = sb.createTransitStopFacility(scenario.createId("7"), coord7, false);
		stop1.setLink(link1);
		stop2.setLink(link1);
		stop3.setLink(link2);
		stop4.setLink(link2);
		stop5.setLink(link3);
		stop6.setLink(link4);
		stop7.setLink(link4);
		
		{ // line 1
			TransitLine tLine = sb.createTransitLine(scenario.createId("1"));
			{
				NetworkRoute netRoute = new LinkNetworkRoute(link1, link1);
				List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
				stops.add(sb.createTransitRouteStop(stop1, 0, 0));
				stops.add(sb.createTransitRouteStop(stop2, 100, 100));
				TransitRoute tRoute = sb.createTransitRoute(scenario.createId("1a"), netRoute, stops, TransportMode.bus);
				tRoute.addDeparture(sb.createDeparture(scenario.createId("1a1"), 1000));
				tLine.addRoute(tRoute);
			}
			schedule.addTransitLine(tLine);
		}

		{ // line 2
			TransitLine tLine = sb.createTransitLine(scenario.createId("2"));
			{
				NetworkRoute netRoute = new LinkNetworkRoute(link2, link3);
				List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(3);
				stops.add(sb.createTransitRouteStop(stop3, 0, 0));
				stops.add(sb.createTransitRouteStop(stop4, 100, 100));
				stops.add(sb.createTransitRouteStop(stop5, 200, 200));
				TransitRoute tRoute = sb.createTransitRoute(scenario.createId("2a"), netRoute, stops, TransportMode.bus);
				tRoute.addDeparture(sb.createDeparture(scenario.createId("2a1"), 1000));
				tLine.addRoute(tRoute);
			}
			schedule.addTransitLine(tLine);
		}
		
		{ // line 3
			TransitLine tLine = sb.createTransitLine(scenario.createId("3"));
			{
				NetworkRoute netRoute = new LinkNetworkRoute(link4, link4);
				List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
				stops.add(sb.createTransitRouteStop(stop6, 0, 0));
				stops.add(sb.createTransitRouteStop(stop7, 100, 100));
				TransitRoute tRoute = sb.createTransitRoute(scenario.createId("3a"), netRoute, stops, TransportMode.train);
				tRoute.addDeparture(sb.createDeparture(scenario.createId("3a1"), 1020));
				tLine.addRoute(tRoute);
			}
			schedule.addTransitLine(tLine);
		}
		
		// test
		TransitRouter router = new TransitRouter(schedule, routerConfig);
		List<Leg> legs = router.calcRoute(coord1, coord7, 990);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(stop1.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getAccessStopId());
		assertEquals(stop2.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getEgressStopId());
		assertEquals(stop1.getLinkId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getStartLinkId());
		assertEquals(stop2.getLinkId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getEndLinkId());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(stop2.getLinkId(), legs.get(2).getRoute().getStartLinkId());
		assertEquals(stop6.getLinkId(), legs.get(2).getRoute().getEndLinkId());
		assertEquals(stop6.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getAccessStopId());
		assertEquals(stop7.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getEgressStopId());
		assertEquals(stop6.getLinkId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getStartLinkId());
		assertEquals(stop7.getLinkId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getEndLinkId());
		assertEquals(TransportMode.walk, legs.get(4).getMode());
	}

}
