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

package playground.mrieser.pt.router;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;


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

	public void testDirectWalkCheaper() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		TransitRouter router = new TransitRouter(f.schedule, config);
		Coord fromCoord = f.scenario.createCoord(4000, 3000);
		Coord toCoord = f.scenario.createCoord(8000, 3000);
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

	public void testSingleLine_DifferentWaitingTime() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig();
		TransitRouter router = new TransitRouter(f.schedule, config);
		Coord fromCoord = f.scenario.createCoord(4000, 5002);
		Coord toCoord = f.scenario.createCoord(8000, 5002);

		double inVehicleTime = 7.0*60; // travel time from A to B
		for (int min = 0; min < 30; min += 3) {
			List<Leg> legs = router.calcRoute(fromCoord, toCoord, 5.0*3600 + min*60);
			assertEquals(3, legs.size()); // walk-pt-walk
			double actualTravelTime = 0.0;
			for (Leg leg : legs) {
				actualTravelTime += leg.getTravelTime();
			}
			double waitingTime = ((46 - min) % 20) * 60; // departures at *:06 and *:26 and *:46
			assertEquals("expected different waiting time at 05:"+min, waitingTime, actualTravelTime - inVehicleTime, MatsimTestCase.EPSILON);
		}
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
		config.beelineWalkSpeed = 0.1; // something very slow, so the agent does not walk over night
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
	 */
	public void testDoubleWalk() {
		WalkFixture f = new WalkFixture();
		f.routerConfig.marginalUtilityOfTravelTimeTransit = -1.0 / 3600.0;
		TransitRouter router = new TransitRouter(f.schedule, f.routerConfig);
		List<Leg> legs = router.calcRoute(f.coord1, f.coord7, 990);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.stop1.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getAccessStopId());
		assertEquals(f.stop2.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getEgressStopId());
		assertEquals(f.stop1.getLinkId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getStartLinkId());
		assertEquals(f.stop2.getLinkId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getEndLinkId());
		assertEquals(TransportMode.walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(f.stop2.getLinkId(), legs.get(2).getRoute().getStartLinkId());
		assertEquals(f.stop6.getLinkId(), legs.get(2).getRoute().getEndLinkId());
		assertEquals(f.stop6.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getAccessStopId());
		assertEquals(f.stop7.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getEgressStopId());
		assertEquals(f.stop6.getLinkId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getStartLinkId());
		assertEquals(f.stop7.getLinkId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getEndLinkId());
		assertEquals(TransportMode.walk, legs.get(4).getMode());
	}

	/**
	 * Tests that if only a single transfer-/walk-link is found, the router correctly only returns
	 * on walk leg from start to end.
	 */
	public void testSingleWalkOnly() {
		WalkFixture f = new WalkFixture();
		f.routerConfig.searchRadius = 0.8 * CoordUtils.calcDistance(f.coord2, f.coord4);
		f.routerConfig.extensionRadius = 0.0;

		TransitRouter router = new TransitRouter(f.schedule, f.routerConfig);
		List<Leg> legs = router.calcRoute(f.coord2, f.coord4, 990);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
	}

	/**
	 * Tests that if only exactly two transfer-/walk-link are found, the router correctly only returns
	 * on walk leg from start to end. Differs from {@link #testSingleWalkOnly()} in that it tests for
	 * the correct internal working when more than one walk links are returned.
	 */
	public void testDoubleWalkOnly() {
		WalkFixture f = new WalkFixture();
		f.routerConfig.searchRadius = 0.8 * CoordUtils.calcDistance(f.coord2, f.coord4);
		f.routerConfig.extensionRadius = 0.0;

		TransitRouter router = new TransitRouter(f.schedule, f.routerConfig);
		List<Leg> legs = router.calcRoute(f.coord2, f.coord6, 990);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
	}

	/**
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
	 *
	 * @author mrieser
	 */
	private static class WalkFixture {

		/*package*/ final ScenarioImpl scenario;
		/*package*/ final TransitSchedule schedule;
		/*package*/ final TransitRouterConfig routerConfig;

		final Coord coord1;
		final Coord coord2;
		final Coord coord3;
		final Coord coord4;
		final Coord coord5;
		final Coord coord6;
		final Coord coord7;

		final TransitStopFacility stop1;
		final TransitStopFacility stop2;
		final TransitStopFacility stop3;
		final TransitStopFacility stop4;
		final TransitStopFacility stop5;
		final TransitStopFacility stop6;
		final TransitStopFacility stop7;

		/*package*/ WalkFixture() {
			this.scenario = new ScenarioImpl();
			this.scenario.getConfig().scenario().setUseTransit(true);
			this.routerConfig = new TransitRouterConfig();
			this.routerConfig.searchRadius = 500.0;
			this.routerConfig.beelineWalkConnectionDistance = 100.0;
			this.routerConfig.beelineWalkSpeed = 10.0; // so the agents can walk the distance in 10 seconds

			double x = 0;
			this.coord1 = this.scenario.createCoord(x, 0);
			x += 1000;
			this.coord2 = this.scenario.createCoord(x, 0);
			x += (this.routerConfig.beelineWalkConnectionDistance * 0.75);
			this.coord3 = this.scenario.createCoord(x, -1000);
			this.coord4 = this.scenario.createCoord(x, 0);
			this.coord5 = this.scenario.createCoord(x, 1000);
			x += (this.routerConfig.beelineWalkConnectionDistance * 0.75);
			this.coord6 = this.scenario.createCoord(x, 0);
			x += 1000;
			this.coord7 = this.scenario.createCoord(x, 0);

			// network
			NetworkImpl network = this.scenario.getNetwork();
			NodeImpl node1 = network.getFactory().createNode(this.scenario.createId("1"), this.coord1);
			NodeImpl node2 = network.getFactory().createNode(this.scenario.createId("2"), this.coord2);
			NodeImpl node3 = network.getFactory().createNode(this.scenario.createId("3"), this.coord3);
			NodeImpl node4 = network.getFactory().createNode(this.scenario.createId("4"), this.coord4);
			NodeImpl node5 = network.getFactory().createNode(this.scenario.createId("5"), this.coord5);
			NodeImpl node6 = network.getFactory().createNode(this.scenario.createId("6"), this.coord6);
			NodeImpl node7 = network.getFactory().createNode(this.scenario.createId("7"), this.coord7);
			network.getNodes().put(node1.getId(), node1);
			network.getNodes().put(node2.getId(), node2);
			network.getNodes().put(node3.getId(), node3);
			network.getNodes().put(node4.getId(), node4);
			network.getNodes().put(node5.getId(), node5);
			network.getNodes().put(node6.getId(), node6);
			network.getNodes().put(node7.getId(), node7);
			LinkImpl link1 = network.getFactory().createLink(this.scenario.createId("1"), node1.getId(), node2.getId());
			LinkImpl link2 = network.getFactory().createLink(this.scenario.createId("2"), node3.getId(), node4.getId());
			LinkImpl link3 = network.getFactory().createLink(this.scenario.createId("3"), node4.getId(), node5.getId());
			LinkImpl link4 = network.getFactory().createLink(this.scenario.createId("4"), node6.getId(), node7.getId());
			network.getLinks().put(link1.getId(), link1);
			network.getLinks().put(link2.getId(), link2);
			network.getLinks().put(link3.getId(), link3);
			network.getLinks().put(link4.getId(), link4);

			// schedule
			this.schedule = this.scenario.getTransitSchedule();
			TransitScheduleFactory sb = this.schedule.getFactory();

			this.stop1 = sb.createTransitStopFacility(this.scenario.createId("1"), this.coord1, false);
			this.stop2 = sb.createTransitStopFacility(this.scenario.createId("2"), this.coord2, false);
			this.stop3 = sb.createTransitStopFacility(this.scenario.createId("3"), this.coord3, false);
			this.stop4 = sb.createTransitStopFacility(this.scenario.createId("4"), this.coord4, false);
			this.stop5 = sb.createTransitStopFacility(this.scenario.createId("5"), this.coord5, false);
			this.stop6 = sb.createTransitStopFacility(this.scenario.createId("6"), this.coord6, false);
			this.stop7 = sb.createTransitStopFacility(this.scenario.createId("7"), this.coord7, false);
			this.stop1.setLink(link1);
			this.stop2.setLink(link1);
			this.stop3.setLink(link2);
			this.stop4.setLink(link2);
			this.stop5.setLink(link3);
			this.stop6.setLink(link4);
			this.stop7.setLink(link4);

			{ // line 1
				TransitLine tLine = sb.createTransitLine(this.scenario.createId("1"));
				{
					NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(link1, link1);
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
					stops.add(sb.createTransitRouteStop(this.stop1, 0, 0));
					stops.add(sb.createTransitRouteStop(this.stop2, 50, 50));
					TransitRoute tRoute = sb.createTransitRoute(this.scenario.createId("1a"), netRoute, stops, TransportMode.bus);
					tRoute.addDeparture(sb.createDeparture(this.scenario.createId("1a1"), 1000));
					tLine.addRoute(tRoute);
				}
				this.schedule.addTransitLine(tLine);
			}

			{ // line 2
				TransitLine tLine = sb.createTransitLine(this.scenario.createId("2"));
				{
					NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(link2, link3);
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(3);
					stops.add(sb.createTransitRouteStop(this.stop3, 0, 0));
					stops.add(sb.createTransitRouteStop(this.stop4, 50, 50));
					stops.add(sb.createTransitRouteStop(this.stop5, 100, 100));
					TransitRoute tRoute = sb.createTransitRoute(this.scenario.createId("2a"), netRoute, stops, TransportMode.bus);
					tRoute.addDeparture(sb.createDeparture(this.scenario.createId("2a1"), 1000));
					tLine.addRoute(tRoute);
				}
				this.schedule.addTransitLine(tLine);
			}

			{ // line 3
				TransitLine tLine = sb.createTransitLine(this.scenario.createId("3"));
				{
					NetworkRouteWRefs netRoute = new LinkNetworkRouteImpl(link4, link4);
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
					stops.add(sb.createTransitRouteStop(this.stop6, 0, 0));
					stops.add(sb.createTransitRouteStop(this.stop7, 50, 50));
					TransitRoute tRoute = sb.createTransitRoute(this.scenario.createId("3a"), netRoute, stops, TransportMode.train);
					tRoute.addDeparture(sb.createDeparture(this.scenario.createId("3a1"), 1070));
					tLine.addRoute(tRoute);
				}
				this.schedule.addTransitLine(tLine);
			}
		}

	}

}
