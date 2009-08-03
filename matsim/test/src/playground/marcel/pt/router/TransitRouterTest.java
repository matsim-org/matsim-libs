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

import java.util.List;

import junit.framework.TestCase;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.events.Events;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.marcel.pt.demo.ScenarioPlayer;
import playground.marcel.pt.routes.ExperimentalTransitRoute;

public class TransitRouterTest extends TestCase {

	public void visualizeFixture() {
		Fixture f = new Fixture();
		f.init();
		ScenarioPlayer.play(f.scenario, new Events());
	}

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

	private void printLegs(final List<Leg> legs) {
		System.out.println("---");

		for (Leg leg : legs) {
			System.out.print(leg.getMode());
			System.out.print("  ");
			System.out.print(leg.getTravelTime());
			System.out.print("  ");
			if (leg.getRoute() instanceof ExperimentalTransitRoute) {
				ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
				System.out.print(route.getRouteDescription());
			}
			System.out.println();
		}
	}

	public static void main(final String[] args) {
		new TransitRouterTest().visualizeFixture();
	}

}
