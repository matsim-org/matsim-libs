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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.events.Events;

import playground.marcel.pt.demo.ScenarioPlayer;
import playground.marcel.pt.routes.ExperimentalTransitRoute;

public class NewTransitRouterTest extends TestCase {

	public void visualizeFixture() {
		Fixture f = new Fixture();
		f.init();
		ScenarioPlayer.play(f.scenario, new Events());
	}

	public void testSingleLine() {
		Fixture f = new Fixture();
		f.init();
		TransitRouter router = new TransitRouter(f.schedule);
		List<Leg> legs = router.calcRoute2(f.scenario.createCoord(3800, 5100), f.scenario.createCoord(16100, 5050), 5.0*3600);
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
		// TODO [MR] need to check travel times
	}

	public void testFromToSameStop() {
		Fixture f = new Fixture();
		f.init();
		TransitRouter router = new TransitRouter(f.schedule);
		List<Leg> legs = router.calcRoute2(f.scenario.createCoord(3800, 5100), f.scenario.createCoord(4100, 5050), 5.0*3600);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.walk, legs.get(0).getMode());
		// TODO [MR] need to check travel time
	}

	public void xtestSingleLine_DifferentWaitingTime() {
		Fixture f = new Fixture();
		f.init();
		fail("not yet implemented.");
	}

	public void testLineChange() {
		Fixture f = new Fixture();
		f.init();
		TransitRouter router = new TransitRouter(f.schedule);
		List<Leg> legs = router.calcRoute2(f.scenario.createCoord(3800, 5100), f.scenario.createCoord(16100, 10050), 6.0*3600);
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
		// TODO [MR] need to check travel times
	}

	public void testFasterAlternative() {
		Fixture f = new Fixture();
		f.init();
		TransitRouter router = new TransitRouter(f.schedule);
		List<Leg> legs = router.calcRoute2(f.scenario.createCoord(3800, 5100), f.scenario.createCoord(28100, 4950), 5.0*3600 + 45.0*60);
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
		// TODO [MR] need to check travel times
	}

	public void xtestTransferWeights() {
		// idea: C to F could either be blue-line (C-D-E-F) or red/blue lines (C-G-F)
		Fixture f = new Fixture();
		f.init();
		fail("not yet implemented.");
	}
	
	public void xtestAfterMidnight() {
		Fixture f = new Fixture();
		f.init();
		fail("not yet implemented.");
	}

	public void xtestCoordFarAway() {
		fail("not yet implemented.");
	}
	
	public static void main(final String[] args) {
		new NewTransitRouterTest().visualizeFixture();
	}
}
