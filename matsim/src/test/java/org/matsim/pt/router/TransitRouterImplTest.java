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

package org.matsim.pt.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class TransitRouterImplTest {

	@Test
	public void testSingleLine() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		Coord fromCoord = new Coord((double) 3800, (double) 5100);
		Coord toCoord = new Coord((double) 16100, (double) 5050);
		List<Leg> legs = router.calcRoute(fromCoord, toCoord, 5.0*3600, null);
		assertEquals(3, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
		assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 29.0 * 60 + // agent takes the *:06 course, arriving in D at *:29
				CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / config.getBeelineWalkSpeed();
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	@Test
	public void testFromToSameStop() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		Coord fromCoord = new Coord((double) 3800, (double) 5100);
		Coord toCoord = new Coord((double) 4100, (double) 5050);
		List<Leg> legs = router.calcRoute(fromCoord, toCoord, 5.0*3600, null);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / config.getBeelineWalkSpeed();
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	@Test
	public void testDirectWalkCheaper() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		Coord fromCoord = new Coord((double) 4000, (double) 3000);
		Coord toCoord = new Coord((double) 8000, (double) 3000);
		List<Leg> legs = router.calcRoute(fromCoord, toCoord, 5.0*3600, null);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / config.getBeelineWalkSpeed();
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	@Test
	public void testSingleLine_DifferentWaitingTime() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		Coord fromCoord = new Coord((double) 4000, (double) 5002);
		Coord toCoord = new Coord((double) 8000, (double) 5002);

		double inVehicleTime = 7.0*60; // travel time from A to B
		for (int min = 0; min < 30; min += 3) {
			List<Leg> legs = router.calcRoute(fromCoord, toCoord, 5.0*3600 + min*60, null);
			assertEquals(3, legs.size()); // walk-pt-walk
			double actualTravelTime = 0.0;
			for (Leg leg : legs) {
				actualTravelTime += leg.getTravelTime();
			}
			double waitingTime = ((46 - min) % 20) * 60; // departures at *:06 and *:26 and *:46
			assertEquals("expected different waiting time at 05:"+min, waitingTime, actualTravelTime - inVehicleTime, MatsimTestCase.EPSILON);
		}
	}

	@Test
	public void testLineChange() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		Coord toCoord = new Coord((double) 16100, (double) 10050);
		List<Leg> legs = router.calcRoute(new Coord((double) 3800, (double) 5100), toCoord, 6.0*3600, null);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(TransportMode.transit_walk, legs.get(4).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
		assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
		assertTrue("expected TransitRoute in leg.", legs.get(3).getRoute() instanceof ExperimentalTransitRoute);
		ptRoute = (ExperimentalTransitRoute) legs.get(3).getRoute();
		assertEquals(Id.create("18", TransitStopFacility.class), ptRoute.getAccessStopId());
		assertEquals(Id.create("19", TransitStopFacility.class), ptRoute.getEgressStopId());
		assertEquals(f.greenLine.getId(), ptRoute.getLineId());
		assertEquals(Id.create("green clockwise", TransitRoute.class), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 31.0 * 60 + // agent takes the *:06 course, arriving in C at *:18, departing at *:21, arriving in K at*:31
				CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("19", TransitStopFacility.class)).getCoord(), toCoord) / config.getBeelineWalkSpeed();
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	@Test
	public void testFasterAlternative() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		Coord toCoord = new Coord((double) 28100, (double) 4950);
		List<Leg> legs = router.calcRoute(new Coord((double) 3800, (double) 5100), toCoord, 5.0*3600 + 40.0*60, null);
		assertEquals(4, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.pt, legs.get(2).getMode());
		assertEquals(TransportMode.transit_walk, legs.get(3).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
		assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
		assertTrue("expected TransitRoute in leg.", legs.get(2).getRoute() instanceof ExperimentalTransitRoute);
		ptRoute = (ExperimentalTransitRoute) legs.get(2).getRoute();
		assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getAccessStopId());
		assertEquals(Id.create("12", TransitStopFacility.class), ptRoute.getEgressStopId());
		assertEquals(f.redLine.getId(), ptRoute.getLineId());
		assertEquals(Id.create("red C > G", TransitRoute.class), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 29.0 * 60 + // agent takes the *:46 course, arriving in C at *:58, departing at *:00, arriving in G at*:09
				CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("12", TransitStopFacility.class)).getCoord(), toCoord) / config.getBeelineWalkSpeed();
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	@Test
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
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		config.setUtilityOfLineSwitch_utl(0);
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		List<Leg> legs = router.calcRoute(new Coord((double) 11900, (double) 5100), new Coord((double) 24100, (double) 4950), 6.0*3600 - 5.0*60, null);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.redLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getLineId());
		assertEquals(TransportMode.transit_walk, legs.get(4).getMode());

		config.setUtilityOfLineSwitch_utl(300.0 * config.getMarginalUtilityOfTravelTimePt_utl_s()); // corresponds to 5 minutes transit travel time
		legs = router.calcRoute(new Coord((double) 11900, (double) 5100), new Coord((double) 24100, (double) 4950), 6.0*3600 - 5.0*60, null);
		assertEquals(3, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
	}

	@Test
	public void testTransferTime() {
		/* idea: travel from C to F
		 * If starting at the right time, one could take the red line to G and travel back with blue to F.
		 * If one doesn't want to switch lines, one could take the blue line from C to F directly.
		 * Using the red line (dep *:00, change at G *:09/*:12) results in an arrival time of *:19,
		 * using the blue line only (dep *:02) results in an arrival time of *:23.
		 * For the line switch at G, 3 minutes are available. If the additional "savety" time is larger than
		 * that, the direct connection should be taken.
		 */
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		config.setUtilityOfLineSwitch_utl(0);
		assertEquals(0, config.getAdditionalTransferTime(), 1e-8);
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		List<Leg> legs = router.calcRoute(new Coord((double) 11900, (double) 5100), new Coord((double) 24100, (double) 4950), 6.0*3600 - 5.0*60, null);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.redLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getLineId());
		assertEquals(TransportMode.transit_walk, legs.get(4).getMode());

		config.setAdditionalTransferTime(3.0*60); // 3 mins already enough, as there is a small distance to walk anyway which adds some time
		legs = router.calcRoute(new Coord((double) 11900, (double) 5100), new Coord((double) 24100, (double) 4950), 6.0*3600 - 5.0*60, null);
		assertEquals(3, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
	}

	@Test
	public void testAfterMidnight() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental());
		config.setBeelineWalkSpeed(0.1); // something very slow, so the agent does not walk over night
		TransitRouterImpl router = new TransitRouterImpl(config, f.schedule);
		Coord toCoord = new Coord((double) 16100, (double) 5050);
		List<Leg> legs = router.calcRoute(new Coord((double) 3800, (double) 5100), toCoord, 25.0*3600, null);
		assertEquals(3, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
		assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
		double actualTravelTime = 0.0;
		for (Leg leg : legs) {
			actualTravelTime += leg.getTravelTime();
		}
		double expectedTravelTime = 4*3600 + 29.0 * 60 + // arrival at 05:29 at D
				CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / config.getBeelineWalkSpeed();
		assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
	}

	@Test
	public void testCoordFarAway() {
		Fixture f = new Fixture();
		f.init();
		TransitRouterImpl router = new TransitRouterImpl( new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
				f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
				f.scenario.getConfig().vspExperimental() ), f.schedule ) ;
		double x = +42000;
		double x1 = -2000;
		List<Leg> legs = router.calcRoute(new Coord(x1, (double) 0), new Coord(x, (double) 0), 5.5*3600, null); // should map to stops A and I
		assertEquals(3, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
		assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
		ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
		assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
		assertEquals(Id.create("16", TransitStopFacility.class), ptRoute.getEgressStopId());
		assertEquals(f.blueLine.getId(), ptRoute.getLineId());
		assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
	}

	/**
	 * In rare cases, Dijkstra may choose to go along two walk links to get from one location to another.
	 * Test, that still only one walk leg with the correct start and end points/links is returned.
	 */
	@Test
	public void testDoubleWalk() {
		WalkFixture f = new WalkFixture();
		f.routerConfig.setMarginalUtilityOfTravelTimePt_utl_s(-1.0 / 3600.0 - 6.0/3600.0);
		f.routerConfig.setUtilityOfLineSwitch_utl(0.2); // must be relatively low in this example, otherwise it's cheaper to walk the whole distance...
		TransitRouterImpl router = new TransitRouterImpl(f.routerConfig, f.schedule);
		List<Leg> legs = router.calcRoute(f.coord1, f.coord7, 990, null);
		assertEquals(5, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
		assertEquals(TransportMode.pt, legs.get(1).getMode());
		assertEquals(f.stop1.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getAccessStopId());
		assertEquals(f.stop2.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getEgressStopId());
		assertEquals(f.stop1.getLinkId(), legs.get(1).getRoute().getStartLinkId());
		assertEquals(f.stop2.getLinkId(), legs.get(1).getRoute().getEndLinkId());
		assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
		assertEquals(TransportMode.pt, legs.get(3).getMode());
		assertEquals(f.stop2.getLinkId(), legs.get(2).getRoute().getStartLinkId());
		assertEquals(f.stop6.getLinkId(), legs.get(2).getRoute().getEndLinkId());
		assertEquals(f.stop6.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getAccessStopId());
		assertEquals(f.stop7.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getEgressStopId());
		assertEquals(f.stop6.getLinkId(), legs.get(3).getRoute().getStartLinkId());
		assertEquals(f.stop7.getLinkId(), legs.get(3).getRoute().getEndLinkId());
		assertEquals(TransportMode.transit_walk, legs.get(4).getMode());
	}

	/**
	 * Tests that if only a single transfer-/walk-link is found, the router correctly only returns
	 * on walk leg from start to end.
	 */
	@Test
	public void testSingleWalkOnly() {
		WalkFixture f = new WalkFixture();
		f.routerConfig.setSearchRadius(0.8 * CoordUtils.calcEuclideanDistance(f.coord2, f.coord4));
		f.routerConfig.setExtensionRadius(0.0);

		TransitRouterImpl router = new TransitRouterImpl(f.routerConfig, f.schedule);
		List<Leg> legs = router.calcRoute(f.coord2, f.coord4, 990, null);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
	}

	/**
	 * Tests that if only exactly two transfer-/walk-link are found, the router correctly only returns
	 * on walk leg from start to end. Differs from {@link #testSingleWalkOnly()} in that it tests for
	 * the correct internal working when more than one walk links are returned.
	 */
	@Test
	public void testDoubleWalkOnly() {
		WalkFixture f = new WalkFixture();
		f.routerConfig.setSearchRadius(0.8 * CoordUtils.calcEuclideanDistance(f.coord2, f.coord4));
		f.routerConfig.setExtensionRadius(0.0);

		TransitRouterImpl router = new TransitRouterImpl(f.routerConfig, f.schedule);
		List<Leg> legs = router.calcRoute(f.coord2, f.coord6, 990, null);
		assertEquals(1, legs.size());
		assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
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

		/*package*/ final MutableScenario scenario;
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
			this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.scenario.getConfig().transit().setUseTransit(true);
			this.routerConfig = new TransitRouterConfig(this.scenario.getConfig().planCalcScore(),
					this.scenario.getConfig().plansCalcRoute(), this.scenario.getConfig().transitRouter(),
					this.scenario.getConfig().vspExperimental());
			this.routerConfig.setSearchRadius(500.0);
			this.routerConfig.setBeelineWalkConnectionDistance(100.0);
			this.routerConfig.setBeelineWalkSpeed(10.0); // so the agents can walk the distance in 10 seconds

			double x = 0;
			this.coord1 = new Coord(x, (double) 0);
			x += 1000;
			this.coord2 = new Coord(x, (double) 0);
			x += (this.routerConfig.getBeelineWalkConnectionDistance() * 0.75);
			double y = -1000;
			this.coord3 = new Coord(x, y);
			this.coord4 = new Coord(x, (double) 0);
			this.coord5 = new Coord(x, (double) 1000);
			x += (this.routerConfig.getBeelineWalkConnectionDistance() * 0.75);
			this.coord6 = new Coord(x, (double) 0);
			x += 1000;
			this.coord7 = new Coord(x, (double) 0);

			// network
			NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
			NodeImpl node1 = network.getFactory().createNode(Id.create("1", Node.class), this.coord1);
			NodeImpl node2 = network.getFactory().createNode(Id.create("2", Node.class), this.coord2);
			NodeImpl node3 = network.getFactory().createNode(Id.create("3", Node.class), this.coord3);
			NodeImpl node4 = network.getFactory().createNode(Id.create("4", Node.class), this.coord4);
			NodeImpl node5 = network.getFactory().createNode(Id.create("5", Node.class), this.coord5);
			NodeImpl node6 = network.getFactory().createNode(Id.create("6", Node.class), this.coord6);
			NodeImpl node7 = network.getFactory().createNode(Id.create("7", Node.class), this.coord7);
			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);
			network.addNode(node6);
			network.addNode(node7);
			Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
			Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node3, node4);
			Link link3 = network.getFactory().createLink(Id.create("3", Link.class), node4, node5);
			Link link4 = network.getFactory().createLink(Id.create("4", Link.class), node6, node7);
			network.addLink(link1);
			network.addLink(link2);
			network.addLink(link3);
			network.addLink(link4);

			// schedule
			this.schedule = this.scenario.getTransitSchedule();
			TransitScheduleFactory sb = this.schedule.getFactory();

			this.stop1 = sb.createTransitStopFacility(Id.create("1", TransitStopFacility.class), this.coord1, false);
			this.stop2 = sb.createTransitStopFacility(Id.create("2", TransitStopFacility.class), this.coord2, false);
			this.stop3 = sb.createTransitStopFacility(Id.create("3", TransitStopFacility.class), this.coord3, false);
			this.stop4 = sb.createTransitStopFacility(Id.create("4", TransitStopFacility.class), this.coord4, false);
			this.stop5 = sb.createTransitStopFacility(Id.create("5", TransitStopFacility.class), this.coord5, false);
			this.stop6 = sb.createTransitStopFacility(Id.create("6", TransitStopFacility.class), this.coord6, false);
			this.stop7 = sb.createTransitStopFacility(Id.create("7", TransitStopFacility.class), this.coord7, false);
			this.stop1.setLinkId(link1.getId());
			this.stop2.setLinkId(link1.getId());
			this.stop3.setLinkId(link2.getId());
			this.stop4.setLinkId(link2.getId());
			this.stop5.setLinkId(link3.getId());
			this.stop6.setLinkId(link4.getId());
			this.stop7.setLinkId(link4.getId());

			{ // line 1
				TransitLine tLine = sb.createTransitLine(Id.create("1", TransitLine.class));
				{
					NetworkRoute netRoute = new LinkNetworkRouteImpl(link1.getId(), link1.getId());
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
					stops.add(sb.createTransitRouteStop(this.stop1, 0, 0));
					stops.add(sb.createTransitRouteStop(this.stop2, 50, 50));
					TransitRoute tRoute = sb.createTransitRoute(Id.create("1a", TransitRoute.class), netRoute, stops, "bus");
					tRoute.addDeparture(sb.createDeparture(Id.create("1a1", Departure.class), 1000));
					tLine.addRoute(tRoute);
				}
				this.schedule.addTransitLine(tLine);
			}

			{ // line 2
				TransitLine tLine = sb.createTransitLine(Id.create("2", TransitLine.class));
				{
					NetworkRoute netRoute = new LinkNetworkRouteImpl(link2.getId(), link3.getId());
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(3);
					stops.add(sb.createTransitRouteStop(this.stop3, 0, 0));
					stops.add(sb.createTransitRouteStop(this.stop4, 50, 50));
					stops.add(sb.createTransitRouteStop(this.stop5, 100, 100));
					TransitRoute tRoute = sb.createTransitRoute(Id.create("2a", TransitRoute.class), netRoute, stops, "bus");
					tRoute.addDeparture(sb.createDeparture(Id.create("2a1", Departure.class), 1000));
					tLine.addRoute(tRoute);
				}
				this.schedule.addTransitLine(tLine);
			}

			{ // line 3
				TransitLine tLine = sb.createTransitLine(Id.create("3", TransitLine.class));
				{
					NetworkRoute netRoute = new LinkNetworkRouteImpl(link4.getId(), link4.getId());
					List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>(2);
					stops.add(sb.createTransitRouteStop(this.stop6, 0, 0));
					stops.add(sb.createTransitRouteStop(this.stop7, 50, 50));
					TransitRoute tRoute = sb.createTransitRoute(Id.create("3a", TransitRoute.class), netRoute, stops, "train");
					tRoute.addDeparture(sb.createDeparture(Id.create("3a1", Departure.class), 1070));
					tLine.addRoute(tRoute);
				}
				this.schedule.addTransitLine(tLine);
			}
		}

	}

}
