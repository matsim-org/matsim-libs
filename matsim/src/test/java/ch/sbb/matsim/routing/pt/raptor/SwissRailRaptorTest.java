/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor.Builder;
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Most of these tests were copied from org.matsim.pt.router.TransitRouterImplTest
 * and only minimally adapted to make them run with SwissRailRaptor.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptorTest {

    private SwissRailRaptor createTransitRouter(TransitSchedule schedule, Config config, Network network) {
        SwissRailRaptorData data = SwissRailRaptorData.create(schedule, null, RaptorUtils.createStaticConfig(config), network, null);
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(data, config).build();
        return raptor;
    }

	@Test
	void testSingleLine() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null));
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertTrue(((Leg)legs.get(1)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        double distance = 0.0;
        for (PlanElement leg : legs) {
            System.out.println(leg + " " + ((Leg)leg).getRoute().getDistance());
            actualTravelTime += ((Leg)leg).getTravelTime().seconds();
            distance += ((Leg)leg).getRoute().getDistance();
        }
        double expectedTravelTime = 29.0 * 60 + // agent takes the *:06 course, arriving in D at *:29
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestUtils.EPSILON);
        assertEquals(15434, Math.ceil(distance), MatsimTestUtils.EPSILON);
    }

	@Test
	void testSingleLine_linkIds() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        Id<Link> fromLinkId = Id.create("ffrroomm", Link.class);
        Id<Link> toLinkId = Id.create("ttoo", Link.class);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord, fromLinkId), new FakeFacility(toCoord, toLinkId), 5.0*3600, null));
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(fromLinkId, ((Leg)legs.get(0)).getRoute().getStartLinkId());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertEquals(toLinkId, ((Leg)legs.get(2)).getRoute().getEndLinkId());
        assertTrue(((Leg)legs.get(1)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        for (PlanElement leg : legs) {
			actualTravelTime += ((Leg)leg).getTravelTime().seconds();
        }
        double expectedTravelTime = 29.0 * 60 + // agent takes the *:06 course, arriving in D at *:29
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestUtils.EPSILON);
    }

	@Test
	void testWalkDurations() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null));
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

        double expectedAccessWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("0", TransitStopFacility.class)).getCoord(), fromCoord) / raptorParams.getBeelineWalkSpeed();
		assertEquals(Math.ceil(expectedAccessWalkTime), ((Leg)legs.get(0)).getTravelTime().seconds(), MatsimTestUtils.EPSILON);
        double expectedEgressWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
		assertEquals(Math.ceil(expectedEgressWalkTime), ((Leg)legs.get(2)).getTravelTime().seconds(), MatsimTestUtils.EPSILON);
    }


	@Test
	void testStationAccessEgressTimes() {
		Fixture f = new Fixture();
		f.init();
		RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
		f.schedule.getFacilities().values().forEach(facility-> TransitScheduleUtils.setSymmetricStopAccessEgressTime(facility,120.0));
		TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
		Coord fromCoord = new Coord(3800, 5100);
		Coord toCoord = new Coord(16100, 5050);
		List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null));
		assertEquals(3, legs.size());
		assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
		assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
		assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

		double expectedAccessWalkTime = 120.0 + CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("0", TransitStopFacility.class)).getCoord(), fromCoord) / raptorParams.getBeelineWalkSpeed();
		assertEquals(Math.ceil(expectedAccessWalkTime), ((Leg)legs.get(0)).getTravelTime().seconds(), MatsimTestUtils.EPSILON);
		double expectedEgressWalkTime = 120.0 + CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
		assertEquals(Math.ceil(expectedEgressWalkTime), ((Leg)legs.get(2)).getTravelTime().seconds(), MatsimTestUtils.EPSILON);
	}

	@Test
	void testWalkDurations_range() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        SwissRailRaptor router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        double depTime = 5.0 * 3600;
        List<? extends PlanElement> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null, new AttributesImpl());
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

        double expectedAccessWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("0", TransitStopFacility.class)).getCoord(), fromCoord) / raptorParams.getBeelineWalkSpeed();
		assertEquals(Math.ceil(expectedAccessWalkTime), ((Leg)legs.get(0)).getTravelTime().seconds(), MatsimTestUtils.EPSILON);
        double expectedEgressWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
		assertEquals(Math.ceil(expectedEgressWalkTime), ((Leg)legs.get(2)).getTravelTime().seconds(), MatsimTestUtils.EPSILON);
    }


	/**
	* The fromFacility and toFacility are both closest to TransitStopFacility I. The expectation is that the Swiss Rail
	* Raptor will return null (TripRouter / FallbackRouter will create a transit_walk between the fromFacility and
	* toFacility) instead of routing the agent to make a major detour by walking the triangle from the fromFacility to
	* the transitStopFacility and then to the toFacility, without once using pt.
	*/

	@Test
	void testFromToSameStop() {
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(4100, 5050);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null));

        Assertions.assertNull(legs, "The router should not find a route and return null, but did return something else.");
    }


	// now the pt router should always try to return a pt route no matter whether a direct walk would be faster
	// adjusted the test - gl aug'19
	@Test
	void testDirectWalkCheaper() {
        Fixture f = new Fixture();
        f.init();
		SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(f.config,SwissRailRaptorConfigGroup.class);
		srrConfig.setIntermodalLegOnlyHandling(SwissRailRaptorConfigGroup.IntermodalLegOnlyHandling.avoid);
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);

        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(4000, 3000);
        Coord toCoord = new Coord(8000, 3000);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null));

        assertEquals(1, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(4000*1.3, ((Leg)legs.get(0)).getRoute().getDistance(), 0.0);
		double actualTravelTime = ((Leg)legs.get(0)).getTravelTime().seconds();
        double expectedTravelTime = CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(expectedTravelTime, actualTravelTime, MatsimTestUtils.EPSILON);
    }

	@Test
	void testDirectWalkFactor() {
        Fixture f = new Fixture();
        f.init();
        f.config.transitRouter().setDirectWalkFactor(100.0);
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(4000, 3000);
        Coord toCoord = new Coord(8000, 3000);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null));

        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        // check individual legs
        Coord ptStop0 = f.schedule.getFacilities().get(Id.create("0", TransitStopFacility.class)).getCoord();
        double expectedAccessWalkTravelTime = CoordUtils.calcEuclideanDistance(fromCoord, ptStop0) / raptorParams.getBeelineWalkSpeed();
		assertEquals(expectedAccessWalkTravelTime, ((Leg)legs.get(0)).getTravelTime().seconds(), 1.0);
        assertEquals((5002-3000)*1.3, ((Leg)legs.get(0)).getRoute().getDistance(), 0.0);

        double expectedPtTravelTime = 6.0*3600 + 6.0*60 - (5.0*3600 + expectedAccessWalkTravelTime) + 7*60; // next departure blue line is at 6.0*3600 + 6.0*60, 7*60 travel time
		assertEquals(expectedPtTravelTime, ((Leg)legs.get(1)).getTravelTime().seconds(), 1.0);

        Coord ptStop2 = f.schedule.getFacilities().get(Id.create("2", TransitStopFacility.class)).getCoord();
        double expectedEgressWalkTravelTime = CoordUtils.calcEuclideanDistance(ptStop2, toCoord) / raptorParams.getBeelineWalkSpeed();
		assertEquals(expectedEgressWalkTravelTime, ((Leg)legs.get(2)).getTravelTime().seconds(), 1.0);
        assertEquals((5002-3000)*1.3, ((Leg)legs.get(2)).getRoute().getDistance(), 0.0);
    }

	@Test
	void testSingleLine_DifferentWaitingTime() {
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(4000, 5002);
        Coord toCoord = new Coord(8000, 5002);

        double inVehicleTime = 7.0*60; // travel time from A to B
        for (int min = 0; min < 30; min += 3) {
            List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600 + min*60, null));
            assertEquals(3, legs.size()); // walk-pt-walk
            double actualTravelTime = 0.0;
            for (PlanElement leg : legs) {
				actualTravelTime += ((Leg)leg).getTravelTime().seconds();
            }
            double waitingTime = ((46 - min) % 20) * 60; // departures at *:06 and *:26 and *:46
            assertEquals(waitingTime, actualTravelTime - inVehicleTime, MatsimTestUtils.EPSILON, "expected different waiting time at 05:"+min);
        }
    }

	@Test
	void testLineChange() {
        Fixture f = new Fixture();
        f.init();
        ConfigUtils.addOrGetModule(f.config, SwissRailRaptorConfigGroup.class).setTransferWalkMargin(0);
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord toCoord = new Coord(16100, 10050);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(new Coord(3800, 5100)), new FakeFacility(toCoord), 6.0*3600, null));
        assertEquals(5, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());
        assertTrue(((Leg)legs.get(1)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        assertTrue(((Leg)legs.get(3)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        ptRoute = (TransitPassengerRoute) ((Leg)legs.get(3)).getRoute();
        assertEquals(Id.create("18", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("19", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.greenLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("green clockwise", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        for (PlanElement leg : legs) {
            actualTravelTime += ((Leg)leg).getTravelTime().seconds();
            System.out.println(((Leg)leg).getTravelTime().seconds());
        }
        double expectedTravelTime = 31.0 * 60 + // agent takes the *:06 course, arriving in C at *:18, departing at *:21, arriving in K at*:31
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("19", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestUtils.EPSILON);
    }
	@Test
	void testLineChangeWithDifferentModeChangeUtils() {
        Fixture f = new Fixture();
        f.init();
		f.blueLine.getRoutes().values().forEach(transitRoute -> transitRoute.setTransportMode(TransportMode.ship));
		SwissRailRaptorConfigGroup swissRailRaptorConfigGroup = ConfigUtils.addOrGetModule(f.config, SwissRailRaptorConfigGroup.class);
		SwissRailRaptorConfigGroup.ModeToModeTransferPenalty trainToShip = new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty("train",TransportMode.ship,1000000.0);
		SwissRailRaptorConfigGroup.ModeToModeTransferPenalty shipToTrain = new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty(TransportMode.ship,"train",1000000.0);
		swissRailRaptorConfigGroup.addModeToModeTransferPenalty(trainToShip);
		swissRailRaptorConfigGroup.addModeToModeTransferPenalty(shipToTrain);
		swissRailRaptorConfigGroup.setTransferWalkMargin(0);
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
		Coord fromCoord = new Coord(3800, 5100);
		Coord toCoord = new Coord(16100, 10050);
		List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0*3600, null));
		//changing from train to ship is so expensive that direct walk is cheaper
		assertNull(legs);
	}
	
	@Test
	void testLineChangeWithDifferentTravelTimeUtils() {
        Fixture f = new Fixture();
        f.init();
		f.blueLine.getRoutes().values().forEach(transitRoute -> transitRoute.setTransportMode("bus"));
		SwissRailRaptorConfigGroup swissRailRaptorConfigGroup = ConfigUtils.addOrGetModule(f.config, SwissRailRaptorConfigGroup.class);
		swissRailRaptorConfigGroup.setTransferWalkMargin(0);
		RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        SwissRailRaptorData data = SwissRailRaptorData.create(f.schedule, null, RaptorUtils.createStaticConfig(f.config), f.network, null);
        TransitRouter router = new SwissRailRaptor.Builder(data, f.config).with(new RaptorParametersForPerson() {	
			@Override
			public RaptorParameters getRaptorParameters(Person person) {
				return raptorParams;
			}
		}).build();
        
        // from C to G (see Fixture), competing between red line (express) and blue line (regular)
		Coord fromCoord = new Coord(12000, 5000);
		Coord toCoord = new Coord(28000, 5000);
		
		// default case
		List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0*3600 - 60.0, null));
		assertEquals(3, legs.size());
		assertEquals("red", ((TransitPassengerRoute) ((Leg) legs.get(1)).getRoute()).getLineId().toString());
		
		// routing by transport mode, same costs, choose red (train) again
		raptorParams.setUseTransportModeUtilities(true);
		raptorParams.setMarginalUtilityOfTravelTime_utl_s("train", -1e-3);
		raptorParams.setMarginalUtilityOfTravelTime_utl_s("bus", -1e-3);
		legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0*3600 - 60.0, null));
		assertEquals(3, legs.size());
		assertEquals("red", ((TransitPassengerRoute) ((Leg) legs.get(1)).getRoute()).getLineId().toString());

		// routing by transport mode, train is quicker, but more costly now, so choose blue (bus)
		raptorParams.setUseTransportModeUtilities(true);
		raptorParams.setMarginalUtilityOfTravelTime_utl_s("train", -1e-2);
		raptorParams.setMarginalUtilityOfTravelTime_utl_s("bus", -1e-3);
		legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0*3600 - 60.0, null));
		assertEquals(3, legs.size());
		assertEquals("blue", ((TransitPassengerRoute) ((Leg) legs.get(1)).getRoute()).getLineId().toString());
	}

	@Test
	void testFasterAlternative() {
        /* idea: travel from A to G
         * One could just take the blue line and travel from A to G (dep *:46, arrival *:28),
         * or one could first travel from A to C (dep *:46, arr *:58), and then take the red line
         * from C to G (dep *:00, arr *:09), but this requires an additional transfer (but
         * at the same StopFacility, so there should not be a transit_walk-leg).
         */
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord toCoord = new Coord(28100, 4950);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility( new Coord(3800, 5100)), new FakeFacility(toCoord), 5.0*3600 + 40.0*60, null));
        assertEquals(5, legs.size(), "wrong number of legs");
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());
        assertTrue(((Leg)legs.get(1)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        assertTrue(((Leg)legs.get(3)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        ptRoute = (TransitPassengerRoute) ((Leg)legs.get(3)).getRoute();
        assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("12", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.redLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("red C > G", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        for (PlanElement leg : legs) {
			actualTravelTime += ((Leg)leg).getTravelTime().seconds();
        }
        double expectedTravelTime = 29.0 * 60 + // agent takes the *:46 course, arriving in C at *:58, departing at *:00, arriving in G at*:09
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("12", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestUtils.EPSILON);
    }


	@Test
	void testTransferWeights() {
        /* idea: travel from C to F
         * If starting at the right time, one could take the red line to G and travel back with blue to F.
         * If one doesn't want to switch lines, one could take the blue line from C to F directly.
         * Using the red line (dep *:00, change at G *:09/*:12) results in an arrival time of *:19,
         * using the blue line only (dep *:02) results in an arrival time of *:23. So the line switch
         * cost must be larger than 4 minutes to have an effect.
         */
        Fixture f = new Fixture();
        f.init();
        f.config.scoring().setUtilityOfLineSwitch(0);
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null));
        assertEquals(5, legs.size(), "wrong number of legs");
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(f.redLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(1)).getRoute()).getLineId());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
        assertEquals(f.blueLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(3)).getRoute()).getLineId());
        assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());

        Config config = ConfigUtils.createConfig();
        double transferUtility = 300.0 * raptorParams.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt); // corresponds to 5 minutes transit travel time
        config.scoring().setUtilityOfLineSwitch(transferUtility);
        raptorParams = RaptorUtils.createParameters(config);
        Assertions.assertEquals(-transferUtility, raptorParams.getTransferPenaltyFixCostPerTransfer(), 0.0);
        router = createTransitRouter(f.schedule, config, f.network);
        legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null));
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(f.blueLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(1)).getRoute()).getLineId());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
    }

	@Test
	void testTransferTime() {
        /* idea: travel from C to F
         * If starting at the right time, one could take the red line to G and travel back with blue to F.
         * If one doesn't want to switch lines, one could take the blue line from C to F directly.
         * Using the red line (dep *:00, change at G *:09/*:12) results in an arrival time of *:19,
         * using the blue line only (dep *:02) results in an arrival time of *:23.
         * For the line switch at G, 3 minutes are available. If the minimum transfer time is larger than
         * that, the direct connection should be taken.
         */
        Fixture f = new Fixture();
        f.init();
        f.config.scoring().setUtilityOfLineSwitch(0);
        f.config.transitRouter().setAdditionalTransferTime(0);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null));
        assertEquals(5, legs.size(), "wrong number of legs");
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(f.redLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(1)).getRoute()).getLineId());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
        assertEquals(f.blueLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(3)).getRoute()).getLineId());
        assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());

        Config config = ConfigUtils.createConfig();
        config.transitRouter().setAdditionalTransferTime(3*60 + 1);
        router = createTransitRouter(f.schedule, config, f.network); // this is necessary to update the router for any change in config.
        legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null));
        assertEquals(3, legs.size(), "wrong number of legs");
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(f.blueLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(1)).getRoute()).getLineId());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
    }


	@Test
	void testAfterMidnight() {
        // in contrast to the default PT router, SwissRailRaptor will not automatically
        // repeat the schedule after 24 hours, so any agent departing late will have to walk if there
        // is no late service in the schedule.
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 25.0*3600, null));

        Assertions.assertNull(legs, "The router should not find a route and return null, but did return something else.");
    }

	@Test
	void testCoordFarAway() {
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        double x = 42000;
        double x1 = -2000;
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(new Coord(x1, 0)), new FakeFacility(new Coord(x, 0)), 5.5*3600, null)); // should map to stops A and I
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertTrue(((Leg)legs.get(1)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("16", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
    }

	/**
	* Tests that if only a single transfer-/walk-link is found, the router correctly only returns
	* null (TripRouter/FallbackRouter will create a walk leg from start to end).
	*/
	@Test
	void testSingleWalkOnly() {
        WalkFixture f = new WalkFixture();
        f.scenario.getConfig().transitRouter().setSearchRadius(0.8 * CoordUtils.calcEuclideanDistance(f.coord2, f.coord4));
        f.scenario.getConfig().transitRouter().setExtensionRadius(0.0);

        TransitRouter router = createTransitRouter(f.schedule, f.scenario.getConfig(), f.scenario.getNetwork());
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(f.coord2), new FakeFacility(f.coord4), 990, null));
        Assertions.assertNull(legs, "The router should not find a route and return null, but did return something else.");
    }


	/**
	* Tests that if only exactly two transfer-/walk-link are found, the router correctly only returns
	* null (which will be replaced by the TripRouter and FallbackRouter with one walk leg from start
	* to end). Differs from {@link #testSingleWalkOnly()} in that it tests for the correct internal
	* working when more than one walk links are returned.
	*/
	@Test
	void testDoubleWalkOnly() {
        WalkFixture f = new WalkFixture();
        f.scenario.getConfig().transitRouter().setSearchRadius(0.8 * CoordUtils.calcEuclideanDistance(f.coord2, f.coord4));
        f.scenario.getConfig().transitRouter().setExtensionRadius(0.0);

        TransitRouter router = createTransitRouter(f.schedule, f.scenario.getConfig(), f.scenario.getNetwork());
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(f.coord2), new FakeFacility(f.coord6), 990, null));

        Assertions.assertNull(legs, "The router should not find a route and return null, but did return something else.");
    }

	@SuppressWarnings("unchecked")
	@Test
	void testLongTransferTime_withTransitRouterWrapper() {
        // 5 minutes additional transfer time
        {
            TransferFixture f = new TransferFixture(5 * 60.0);
            TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
            Coord fromCoord = f.fromFacility.getCoord();
            Coord toCoord = f.toFacility.getCoord();
            List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 7.0 * 3600 + 50 * 60, null));
            double legDuration = calcTripDuration(new ArrayList<>(legs));
            Assertions.assertEquals(5, legs.size());
            Assertions.assertEquals(100, ((Leg)legs.get(0)).getTravelTime().seconds(), 0.0);    // 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
            Assertions.assertEquals(800, ((Leg)legs.get(1)).getTravelTime().seconds(), 0.0);    // 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
            Assertions.assertEquals(295, ((Leg)legs.get(2)).getTravelTime().seconds(),
                    0.0);    // 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s -5 seconds margin; arrival at 08:10:00
            Assertions.assertEquals(600, ((Leg)legs.get(3)).getTravelTime().seconds(), 0.0);    // 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
            Assertions.assertEquals(100, ((Leg)legs.get(4)).getTravelTime().seconds(), 0.0);    // 0.1km with 1m/s walk speed -> 100s
            Assertions.assertEquals(1895.0, legDuration, 0.0);

            RoutingModule walkRoutingModule = DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, f.scenario,
                    f.config.routing().getModeRoutingParams().get(TransportMode.walk));

            TransitRouterWrapper wrapper = new TransitRouterWrapper(
                    router,
                    f.schedule,
                    f.scenario.getNetwork(), // use a walk router in case no PT path is found
                    walkRoutingModule);

            List<PlanElement> planElements = (List<PlanElement>) wrapper.calcRoute(DefaultRoutingRequest.withoutAttributes(f.fromFacility, f.toFacility, 7.0 * 3600 + 50 * 60, null));
            double tripDuration = calcTripDuration(planElements);
            Assertions.assertEquals(9, planElements.size());
            Assertions.assertEquals(100, ((Leg) planElements.get(0)).getTravelTime().seconds(), 0.0);    // 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
            Assertions.assertEquals(800, ((Leg) planElements.get(2)).getTravelTime().seconds(), 0.0);    // 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
            Assertions.assertEquals(295, ((Leg) planElements.get(4)).getTravelTime().seconds(),
                    0.0);    // 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s; arrival at 08:10:00
            Assertions.assertEquals(600, ((Leg) planElements.get(6)).getTravelTime().seconds(), 0.0);    // 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
            Assertions.assertEquals(100, ((Leg) planElements.get(8)).getTravelTime().seconds(), 0.0);    // 0.1km with 1m/s walk speed -> 100s
            Assertions.assertEquals(1895.0, tripDuration, 0.0);
        }

        // 65 minutes additional transfer time - miss one departure
        {
            TransferFixture f = new TransferFixture(65 * 60.0);
            ConfigUtils.addOrGetModule(f.config, SwissRailRaptorConfigGroup.class).setTransferWalkMargin(0);
            TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
            Coord fromCoord = f.fromFacility.getCoord();
            Coord toCoord = f.toFacility.getCoord();
            List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 7.0*3600 + 50*60, null));
            double legDuration = calcTripDuration(new ArrayList<>(legs));
            Assertions.assertEquals(5, legs.size());
			Assertions.assertEquals(100, ((Leg)legs.get(0)).getTravelTime().seconds(), 0.0);	// 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
			Assertions.assertEquals(800, ((Leg)legs.get(1)).getTravelTime().seconds(), 0.0);	// 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
			Assertions.assertEquals(3900, ((Leg)legs.get(2)).getTravelTime().seconds(), 0.0);	// 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s; arrival at 08:10:00
			Assertions.assertEquals(600, ((Leg)legs.get(3)).getTravelTime().seconds(), 0.0);	// 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
			Assertions.assertEquals(100, ((Leg)legs.get(4)).getTravelTime().seconds(), 0.0);	// 0.1km with 1m/s walk speed -> 100s
            Assertions.assertEquals(5500.0, legDuration, 0.0);

            RoutingModule walkRoutingModule = DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, f.scenario,
                    f.config.routing().getModeRoutingParams().get(TransportMode.walk));

            TransitRouterWrapper wrapper = new TransitRouterWrapper(
                    router,
                    f.schedule,
                    f.scenario.getNetwork(), // use a walk router in case no PT path is found
                    walkRoutingModule);

            List<PlanElement> planElements = (List<PlanElement>) wrapper.calcRoute(DefaultRoutingRequest.withoutAttributes(f.fromFacility, f.toFacility, 7.0*3600 + 50*60, null));
            double tripDuration = calcTripDuration(planElements);
            Assertions.assertEquals(9, planElements.size());
			Assertions.assertEquals(100, ((Leg)planElements.get(0)).getTravelTime().seconds(), 0.0);	// 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
			Assertions.assertEquals(800, ((Leg)planElements.get(2)).getTravelTime().seconds(), 0.0);	// 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
			Assertions.assertEquals(3900, ((Leg)planElements.get(4)).getTravelTime().seconds(), 0.0);	// 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s; arrival at 08:10:00
			Assertions.assertEquals(600, ((Leg)planElements.get(6)).getTravelTime().seconds(), 0.0);	// 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
			Assertions.assertEquals(100, ((Leg)planElements.get(8)).getTravelTime().seconds(), 0.0);	// 0.1km with 1m/s walk speed -> 100s
            Assertions.assertEquals(5500.0, tripDuration, 0.0);
        }

        // 600 minutes additional transfer time - miss all departures
        {
            TransferFixture f = new TransferFixture(600 * 60.0);
            TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
            Coord fromCoord = f.fromFacility.getCoord();
            Coord toCoord = f.toFacility.getCoord();
            List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 7.0*3600 + 50*60, null));
            Assertions.assertNull(legs, "The router should not find a route and return null, but did return something else.");

            RoutingModule walkRoutingModule = DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, f.scenario,
                    f.config.routing().getModeRoutingParams().get(TransportMode.walk));

            TransitRouterWrapper routingModule = new TransitRouterWrapper(
                    router,
                    f.schedule,
                    f.scenario.getNetwork(), // use a walk router in case no PT path is found
                    walkRoutingModule);

            TransitRouterWrapper wrapper = new TransitRouterWrapper(router, f.schedule, f.scenario.getNetwork(), routingModule);
            List<PlanElement> planElements = (List<PlanElement>) wrapper.calcRoute(DefaultRoutingRequest.withoutAttributes(f.fromFacility, f.toFacility, 7.0*3600 + 50*60, null));
            Assertions.assertNull(planElements, "The router should not find a route and return null, but did return something else.");
        }
    }

    private static double calcTripDuration(List<PlanElement> planElements) {
        double duration = 0.0;
        for (PlanElement pe : planElements) {
            if (pe instanceof Activity act) {
				if (act.getStartTime().isDefined() && act.getEndTime().isDefined()) {
                    double startTime = act.getStartTime().seconds();
					double endTime = act.getEndTime().seconds();
                    duration += (endTime - startTime);
                }
            } else if (pe instanceof Leg leg) {
				duration += leg.getTravelTime().seconds();
            }
        }
        return duration;
    }

	@Test
	void testNightBus() {
        // test a special case where a direct connection only runs at a late time, when typically
        // no other services run anymore.
        NightBusFixture f = new NightBusFixture();

        TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
        Coord fromCoord = new Coord(5010, 1010);
        Coord toCoord = new Coord(5010, 5010);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 8.0*3600-2*60, null));
        assertEquals(7, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());
        assertEquals(TransportMode.pt, ((Leg)legs.get(5)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(6)).getMode());
        assertTrue(((Leg)legs.get(1)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
        assertEquals(f.stop0.getId(), ptRoute.getAccessStopId());
        assertEquals(f.stop1.getId(), ptRoute.getEgressStopId());
        assertEquals(f.lineId0, ptRoute.getLineId());
        assertTrue(((Leg)legs.get(3)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        ptRoute = (TransitPassengerRoute) ((Leg)legs.get(3)).getRoute();
        assertEquals(f.stop1.getId(), ptRoute.getAccessStopId());
        assertEquals(f.stop2.getId(), ptRoute.getEgressStopId());
        assertEquals(f.lineId1, ptRoute.getLineId());
        assertTrue(((Leg)legs.get(5)).getRoute() instanceof TransitPassengerRoute, "expected TransitRoute in leg.");
        ptRoute = (TransitPassengerRoute) ((Leg)legs.get(5)).getRoute();
        assertEquals(f.stop2.getId(), ptRoute.getAccessStopId());
        assertEquals(f.stop3.getId(), ptRoute.getEgressStopId());
        assertEquals(f.lineId3, ptRoute.getLineId());
    }

	@Test
	void testCircularLine() {
        Fixture f = new Fixture();
        f.init();

        SwissRailRaptor raptor = createTransitRouter(f.schedule, f.config, f.network);

        Coord fromCoord = new Coord(16000, 100); // stop N
        Coord toCoord = new Coord(24000, 9950); // stop L
        double depTime = 5.0 * 3600 + 50 * 60;
        List<? extends PlanElement> legs = raptor.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime,null));

        for (PlanElement leg : legs) {
            System.out.println(leg);
        }

        Assertions.assertEquals(5, legs.size());
        Assertions.assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        Assertions.assertEquals(TransportMode.pt, ((Leg)legs.get(1)).getMode());
        Assertions.assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        Assertions.assertEquals(TransportMode.pt, ((Leg)legs.get(3)).getMode());
        Assertions.assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());

        Assertions.assertEquals(f.greenLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(1)).getRoute()).getLineId());
        Assertions.assertEquals(Id.create(23, TransitStopFacility.class), ((TransitPassengerRoute) ((Leg)legs.get(1)).getRoute()).getAccessStopId());
        Assertions.assertEquals(f.greenLine.getId(), ((TransitPassengerRoute) ((Leg)legs.get(3)).getRoute()).getLineId());
        Assertions.assertEquals(Id.create(20, TransitStopFacility.class), ((TransitPassengerRoute) ((Leg)legs.get(3)).getRoute()).getEgressStopId());
    }

	@Test
	void testRangeQuery() {
        Fixture f = new Fixture();
        f.init();
        SwissRailRaptor raptor = createTransitRouter(f.schedule, f.config, f.network);

        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(28100, 4950);
        double depTime = 5.0 * 3600 + 50 * 60;
        List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 600, depTime, depTime + 3600, null, new AttributesImpl());

        for (int i = 0; i < routes.size(); i++) {
            RaptorRoute route = routes.get(i);
            System.out.println(i + "  depTime = " + Time.writeTime(route.getDepartureTime()) + "  arrTime = " + Time.writeTime(route.getDepartureTime() + route.getTravelTime()) + "  # transfers = " + route.getNumberOfTransfers() + "  costs = " + route.getTotalCosts());
        }

        Assertions.assertEquals(6, routes.size());

        assertRaptorRoute(routes.get(0), "05:40:12", "06:30:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(1), "06:00:12", "06:50:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(2), "06:20:12", "07:10:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(3), "06:40:12", "07:30:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(4), "05:40:12", "06:11:56", 1, 7.3466666);
        assertRaptorRoute(routes.get(5), "06:40:12", "07:11:56", 1, 7.3466666);
    }

    private void assertRaptorRoute(RaptorRoute route, String depTime, String arrTime, int expectedTransfers, double expectedCost) {
        Assertions.assertEquals(expectedTransfers, route.getNumberOfTransfers(), "wrong number of transfers");
        Assertions.assertEquals(Time.parseTime(depTime), route.getDepartureTime(), 0.99, "wrong departure time");
        Assertions.assertEquals(Time.parseTime(arrTime), route.getDepartureTime() + route.getTravelTime(), 0.99, "wrong arrival time");
        Assertions.assertEquals(expectedCost, route.getTotalCosts(), 1e-5, "wrong cost");
    }

	/** test for https://github.com/SchweizerischeBundesbahnen/matsim-sbb-extensions/issues/1
	*
	* If there are StopFacilities in the transit schedule, that are not part of any route, the Router crashes with a NPE in SwissRailRaptorData at line 213, because toRouteStopIndices == null.
	*/
	@Test
	void testUnusedTransitStop() {
        Fixture f = new Fixture();
        f.init();

        // add some unused transit stops:
        // - one close to the start coordinate, so it gets selected as start stop
        TransitStopFacility fooStop = f.schedule.getFactory().createTransitStopFacility(Id.create("foo", TransitStopFacility.class), new Coord(3900, 4900), true);
        f.schedule.addStopFacility(fooStop);
        // - one close to another stop as a potential transfer
        TransitStopFacility barStop = f.schedule.getFactory().createTransitStopFacility(Id.create("bar", TransitStopFacility.class), new Coord(12010, 4990), true);
        f.schedule.addStopFacility(barStop);
        // - one close to the end coordinate as a potential arrival stop
        TransitStopFacility bazStop = f.schedule.getFactory().createTransitStopFacility(Id.create("baz", TransitStopFacility.class), new Coord(28010, 4990), true);
        f.schedule.addStopFacility(bazStop);


        SwissRailRaptor raptor = createTransitRouter(f.schedule, f.config, f.network);

        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(28100, 4950);
        double depTime = 5.0 * 3600 + 50 * 60;
        List<? extends PlanElement> legs = raptor.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime, null));
        // this test mostly checks that there are no Exceptions.
        Assertions.assertEquals(3, legs.size());
    }

	@Test
	void testTravelTimeDependentTransferCosts() {
        TravelTimeDependentTransferFixture f = new TravelTimeDependentTransferFixture();

        { // test default 0 + 0 * tt
            Config config = prepareConfig(0, 0); // sets beeline walk speed to 1m/s
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(20000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null, new AttributesImpl());
            Assertions.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 35*60; // 35 minutes: 15 Blue, 5 Transfer, 15 Red
            double expectedAccessEgressTime = 2 * 100; // 2 * 100 meters at 1m/s beeline walk speed (beeline factor included)
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt);

            Assertions.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }

        { // test 2 + 0 * tt
            Config config = prepareConfig(2, 0);
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(20000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null, new AttributesImpl());
            Assertions.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 35*60; // 35 minutes: 15 Blue, 5 Transfer, 15 Red
            double expectedAccessEgressTime = 2 * 100;  // 2 * 100 meters at 1m/s
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt) + 2;

            Assertions.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }

        { // test 2 + 9 * tt[h]
            Config config = prepareConfig(2, 9);
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(20000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null, new AttributesImpl());
            Assertions.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 35*60; // 35 minutes: 15 Blue, 5 Transfer, 15 Red
            double expectedAccessEgressTime = 2 * 100;  // 2 * 100 meters at 1m/s
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt) + 2 + 0.0025 * expectedTravelTime;

            Assertions.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }

        { // test 2 + 9 * tt[h], longer trip
            Config config = prepareConfig(2, 9);
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(40000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null, new AttributesImpl());
            Assertions.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 65*60; // 65 minutes: 15 Blue, 5 Transfer, 45 Red
            double expectedAccessEgressTime = 2 * 100;  // 2 * 100 meters at 1m/s
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt) + 2 + 0.0025 * expectedTravelTime;

            Assertions.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }
    }

	@Test
	void testCustomTransferCostCalculator() {
        TransferFixture f = new TransferFixture(60.0);

        int[] transferCount = new int[] { 0 };

        SwissRailRaptorData data = SwissRailRaptorData.create(f.schedule, null, RaptorUtils.createStaticConfig(f.config), f.scenario.getNetwork(), null);
        SwissRailRaptor router = new Builder(data, f.config).with((RaptorTransferCostCalculator) (currentPE, transfer, staticConfig, raptorParams, totalTravelTime, totalTransferCount, existingTransferCosts, currentTime) -> {

                transferCount[0]++;
                Transfer t = transfer.get();

                assertEquals(f.stop1, t.getFromStop());
                assertEquals(f.stop2, t.getToStop());
                assertEquals(Id.create("0to1", TransitLine.class), t.getFromTransitLine().getId());
                assertEquals(Id.create("2to3", TransitLine.class), t.getToTransitLine().getId());
                assertEquals(Id.create("0to1", TransitRoute.class), t.getFromTransitRoute().getId());
                assertEquals(Id.create("2to3", TransitRoute.class), t.getToTransitRoute().getId());

                return 0.5;

        }).build();

        Coord fromCoord = f.fromFacility.getCoord();
        Coord toCoord = f.toFacility.getCoord();
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 7.0*3600 + 50*60, null));
        for (PlanElement leg : legs) {
            System.out.println(leg);
        }
        Assertions.assertTrue(transferCount[0] > 0, "TransferCost function must have been called at least once.");
    }

    private Config prepareConfig(double transferFixedCost, double transferRelativeCostFactor) {
        SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
        Config config = ConfigUtils.createConfig(srrConfig);
        config.transitRouter().setDirectWalkFactor(1.0);

        double beelineDistanceFactor = config.routing().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
        RoutingConfigGroup.TeleportedModeParams walkParameters = new RoutingConfigGroup.TeleportedModeParams(TransportMode.walk);
        walkParameters.setTeleportedModeSpeed(beelineDistanceFactor); // set it such that the beelineWalkSpeed is exactly 1
        config.routing().addParameterSet(walkParameters);

        config.scoring().setUtilityOfLineSwitch(-transferFixedCost);
        srrConfig.setTransferPenaltyBaseCost(transferFixedCost);
        srrConfig.setTransferPenaltyCostPerTravelTimeHour(transferRelativeCostFactor);

        return config;
    }

	@Test
	void testModeMapping() {
        Fixture f = new Fixture();
        f.init();
        for (TransitRoute route : f.blueLine.getRoutes().values()) {
            route.setTransportMode("tram");
        }
        for (TransitRoute route : f.redLine.getRoutes().values()) {
            route.setTransportMode("train");
        }
        for (TransitRoute route : f.greenLine.getRoutes().values()) {
            route.setTransportMode("bus");
        }

        SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(f.config, SwissRailRaptorConfigGroup.class);
        srrConfig.setUseModeMappingForPassengers(true);
        srrConfig.addModeMappingForPassengers(new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet("tram", "rail"));
        srrConfig.addModeMappingForPassengers(new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet("train", "rail"));
        srrConfig.addModeMappingForPassengers(new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet("bus", "road"));

        ModeParams railParams = new ModeParams("rail");
        railParams.setMarginalUtilityOfTraveling(-6.0);
        f.config.scoring().addModeParams(railParams);

        ModeParams roadParams = new ModeParams("road");
        roadParams.setMarginalUtilityOfTraveling(-6.0);
        f.config.scoring().addModeParams(roadParams);

        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord toCoord = new Coord(16100, 10050);
        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(new Coord(3800, 5100)), new FakeFacility(toCoord), 6.0*3600, null));
        assertEquals(5, legs.size());
        assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
        assertEquals("rail", ((Leg)legs.get(1)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());
        assertEquals("road", ((Leg)legs.get(3)).getMode());
        assertEquals(TransportMode.walk, ((Leg)legs.get(4)).getMode());
    }

	@Test
	void testModeMappingCosts() {
        Fixture f = new Fixture();
        f.init();

        Coord fromCoord = new Coord(12000, 5050); // C
        Coord toCoord = new Coord(28000, 5050); // G
        { // test default, from C to G the red line is the fastest/cheapest

            TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
            List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0 * 3600 - 5 * 60, null));
            assertEquals(3, legs.size());
            assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
            assertEquals("pt", ((Leg)legs.get(1)).getMode());
            assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

            TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
            assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getAccessStopId());
            assertEquals(Id.create("12", TransitStopFacility.class), ptRoute.getEgressStopId());
            assertEquals(f.redLine.getId(), ptRoute.getLineId());
            assertEquals(Id.create("red C > G", TransitRoute.class), ptRoute.getRouteId());
        }

        // set different modes and add mode mapping

        for (TransitRoute route : f.blueLine.getRoutes().values()) {
            route.setTransportMode("tram");
        }
        for (TransitRoute route : f.redLine.getRoutes().values()) {
            route.setTransportMode("train");
        }
        for (TransitRoute route : f.greenLine.getRoutes().values()) {
            route.setTransportMode("bus");
        }

        Config config = ConfigUtils.createConfig();
        {
            SwissRailRaptorConfigGroup srrConfig = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
            srrConfig.setUseModeMappingForPassengers(true);
            srrConfig.addModeMappingForPassengers(new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet("tram", "rail"));
            srrConfig.addModeMappingForPassengers(new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet("train", "rail"));
            srrConfig.addModeMappingForPassengers(new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet("bus", "road"));

            ModeParams railParams = new ModeParams("rail");
            railParams.setMarginalUtilityOfTraveling(-6.0);
            config.scoring().addModeParams(railParams);

            ModeParams roadParams = new ModeParams("road");
            roadParams.setMarginalUtilityOfTraveling(-6.0);
            config.scoring().addModeParams(roadParams);
        }

        { // test with similar costs, the red line should still be cheaper
            TransitRouter router = createTransitRouter(f.schedule, config, f.network);
            List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0 * 3600 - 5 * 60, null));
            assertEquals(3, legs.size());
            assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
            assertEquals("rail", ((Leg)legs.get(1)).getMode());
            assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

            TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
            assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getAccessStopId());
            assertEquals(Id.create("12", TransitStopFacility.class), ptRoute.getEgressStopId());
            assertEquals(f.redLine.getId(), ptRoute.getLineId());
            assertEquals(Id.create("red C > G", TransitRoute.class), ptRoute.getRouteId());
        }

        { // make bus cheaper, thus the green line should be used
            // rail has cost 6/hour, opportunity cost are 6/hour, total cost 12/hour
            // red line takes 9 minutes --> 12 / 60 * 9 = cost 1.8
            // green takes 30 minutes from C to G, must be cheaper than 1.8
            // and green departs 1min later which adds waiting-time cost ((6 + opportunity 6)/60 = 0.2)
            // thus in-vehicle-cost must be cheaper than 1.6 (for 30 minutes) --> 3.2/hour
            // --> max total cost 3.2, subtract opportunity cost --> max cost -2.8 (so you would actually get money to ride the bus)
            // (the access/egress legs to red are each 2 meters shorter than to green line, which adds a little additional penalty for the green line, about 0.02)
            ModeParams roadParams = new ModeParams("road");
            roadParams.setMarginalUtilityOfTraveling(2.83);
            config.scoring().addModeParams(roadParams);

            TransitRouter router = createTransitRouter(f.schedule, config, f.network);
            List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0 * 3600 - 5 * 60, null));
            assertEquals(3, legs.size());
            assertEquals(TransportMode.walk, ((Leg)legs.get(0)).getMode());
            assertEquals("road", ((Leg)legs.get(1)).getMode());
            assertEquals(TransportMode.walk, ((Leg)legs.get(2)).getMode());

            TransitPassengerRoute ptRoute = (TransitPassengerRoute) ((Leg)legs.get(1)).getRoute();
            assertEquals(Id.create("18", TransitStopFacility.class), ptRoute.getAccessStopId());
            assertEquals(Id.create("21", TransitStopFacility.class), ptRoute.getEgressStopId());
            assertEquals(f.greenLine.getId(), ptRoute.getLineId());
            assertEquals(Id.create("green clockwise", TransitRoute.class), ptRoute.getRouteId());
        }
    }

	/**
	* Tests what happens if there is a transit service available, but the agent's departure time (11 AM) is after the last transit
	* departure time (9:46 AM). The expectation is that the router returns null and TripRouter/FallbackRouter will create a direct
	* walk leg to get from the fromFacility to the toFacility.
	*/
	@Test
	void testDepartureAfterLastBus(){
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(8100, 5050);

        List<? extends PlanElement> legs = router.calcRoute(DefaultRoutingRequest.withoutAttributes(new FakeFacility(fromCoord), new FakeFacility(toCoord), 11.0*3600, null));

        Assertions.assertNull(legs, "The router should not find a route and return null, but did return something else.");
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

        final MutableScenario scenario;
        final TransitSchedule schedule;
        final RaptorStaticConfig staticConfig;

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
            this.staticConfig = RaptorUtils.createStaticConfig(this.scenario.getConfig());
            this.staticConfig.setBeelineWalkConnectionDistance(100.0);
            this.staticConfig.setBeelineWalkSpeed(10.0); // so the agents can walk the distance in 10 seconds

            double x = 0;
            this.coord1 = new Coord(x, 0);
            x += 1000;
            this.coord2 = new Coord(x, 0);
            x += (this.staticConfig.getBeelineWalkConnectionDistance() * 0.75);
            double y = -1000;
            this.coord3 = new Coord(x, y);
            this.coord4 = new Coord(x, 0);
            this.coord5 = new Coord(x, 1000);
            x += (this.staticConfig.getBeelineWalkConnectionDistance() * 0.75);
            this.coord6 = new Coord(x, 0);
            x += 1000;
            this.coord7 = new Coord(x, 0);

            // network
            Network network = this.scenario.getNetwork();
            Node node1 = network.getFactory().createNode(Id.create("1", Node.class), this.coord1);
            Node node2 = network.getFactory().createNode(Id.create("2", Node.class), this.coord2);
            Node node3 = network.getFactory().createNode(Id.create("3", Node.class), this.coord3);
            Node node4 = network.getFactory().createNode(Id.create("4", Node.class), this.coord4);
            Node node5 = network.getFactory().createNode(Id.create("5", Node.class), this.coord5);
            Node node6 = network.getFactory().createNode(Id.create("6", Node.class), this.coord6);
            Node node7 = network.getFactory().createNode(Id.create("7", Node.class), this.coord7);
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
            this.schedule.addStopFacility(this.stop1);
            this.schedule.addStopFacility(this.stop2);
            this.schedule.addStopFacility(this.stop3);
            this.schedule.addStopFacility(this.stop4);
            this.schedule.addStopFacility(this.stop5);
            this.schedule.addStopFacility(this.stop6);
            this.schedule.addStopFacility(this.stop7);

            { // line 1
                TransitLine tLine = sb.createTransitLine(Id.create("1", TransitLine.class));
                {
                    NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link1.getId());
                    List<TransitRouteStop> stops = new ArrayList<>(2);
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
                    NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(link2.getId(), link3.getId());
                    List<TransitRouteStop> stops = new ArrayList<>(3);
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
                    NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(link4.getId(), link4.getId());
                    List<TransitRouteStop> stops = new ArrayList<>(2);
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

    /**
     * Generates the following network for testing:
     * <pre>
     *  (n) node
     *  [s] stop facilities
     *   l  link
     *
     *  [0]       [1]
     *  (0)---0---(1)---1---(2)
     *            [2]       [3]
     *
     * </pre>
     *
     * Simple setup with one line from 0 to 1 and one from 2 to 3.
     *
     * Departures are every 5 minutes. PT travel time from (1) to (2) and from (2) to (1) is one hour.
     * A short cut is realized via an entry in the transfer matrix (5 minutes).
     *
     * @author cdobler
     */
    private static class TransferFixture {

        /*package*/ final Config config;
        /*package*/ final Scenario scenario;
        /*package*/ final TransitSchedule schedule;

        final TransitStopFacility stop0;
        final TransitStopFacility stop1;
        final TransitStopFacility stop2;
        final TransitStopFacility stop3;

        final ActivityFacility fromFacility;
        final ActivityFacility toFacility;

        /*package*/ TransferFixture(double additionalTransferTime) {
            this.config = ConfigUtils.createConfig();
            this.config.transitRouter().setAdditionalTransferTime(additionalTransferTime);
            this.scenario = ScenarioUtils.createScenario(this.config);
            this.config.transit().setUseTransit(true);
            this.config.transitRouter().setSearchRadius(500.0);
            this.config.transitRouter().setMaxBeelineWalkConnectionDistance(100.0);

            double beelineDistanceFactor = this.config.routing().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
            RoutingConfigGroup.TeleportedModeParams walkParameters = new RoutingConfigGroup.TeleportedModeParams(TransportMode.walk);
            walkParameters.setTeleportedModeSpeed(beelineDistanceFactor); // set it such that the beelineWalkSpeed is exactly 1
            this.config.routing().addParameterSet(walkParameters);

            // network
            Network network = this.scenario.getNetwork();

            Node node0 = network.getFactory().createNode(Id.create("0", Node.class), new Coord(0, 1000));
            Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(25000, 1000));
            Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(50000, 1000));
            network.addNode(node0);
            network.addNode(node1);
            network.addNode(node2);

            Link link0 = network.getFactory().createLink(Id.create("0", Link.class), node0, node1);
            Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
            network.addLink(link0);
            network.addLink(link1);

            // facilities
            ActivityFacilities facilities = this.scenario.getActivityFacilities();

            this.fromFacility = facilities.getFactory().createActivityFacility(Id.create("fromFacility", ActivityFacility.class), new Coord(0, 1102));
            this.toFacility = facilities.getFactory().createActivityFacility(Id.create("toFacility", ActivityFacility.class), new Coord(50000, 898));
            facilities.addActivityFacility(this.fromFacility);
            facilities.addActivityFacility(this.toFacility);

            // schedule
            this.schedule = this.scenario.getTransitSchedule();
            TransitScheduleFactory sb = this.schedule.getFactory();

            this.stop0 = sb.createTransitStopFacility(Id.create("0", TransitStopFacility.class), new Coord(0, 1002), false);
            this.stop1 = sb.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord(25000,  1002), false);
            this.stop2 = sb.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord(25000, 998), false);
            this.stop3 = sb.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord(50000, 998), false);
            this.schedule.addStopFacility(this.stop0);
            this.schedule.addStopFacility(this.stop1);
            this.schedule.addStopFacility(this.stop2);
            this.schedule.addStopFacility(this.stop3);
            this.stop0.setLinkId(link0.getId());
            this.stop1.setLinkId(link0.getId());
            this.stop2.setLinkId(link1.getId());
            this.stop3.setLinkId(link1.getId());

            // route from 0 to 1
            {
                TransitLine line0to1 = sb.createTransitLine(Id.create("0to1", TransitLine.class));
                this.schedule.addTransitLine(line0to1);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(link0.getId(), link0.getId());
                List<Id<Link>> routeLinks = new ArrayList<>();
                netRoute.setLinkIds(link0.getId(), routeLinks, link0.getId());
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStopBuilder(this.stop0).departureOffset(0.0).build());
                stops.add(sb.createTransitRouteStopBuilder(this.stop1).arrivalOffset(5*60.0).build());
                TransitRoute route = sb.createTransitRoute(Id.create("0to1", TransitRoute.class), netRoute, stops, "train");
                line0to1.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l0to1 d0", Departure.class), 8.0*3600));
                route.addDeparture(sb.createDeparture(Id.create("l0to1 d1", Departure.class), 9.0*3600));
                route.addDeparture(sb.createDeparture(Id.create("l0to1 d2", Departure.class), 10.0*3600));
                route.addDeparture(sb.createDeparture(Id.create("l0to1 d3", Departure.class), 11.0*3600));
                route.addDeparture(sb.createDeparture(Id.create("l0to1 d4", Departure.class), 12.0*3600));
            }

            // route from 2 to 3
            {
                TransitLine line2to3 = sb.createTransitLine(Id.create("2to3", TransitLine.class));
                this.schedule.addTransitLine(line2to3);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link1.getId());
                List<Id<Link>> routeLinks = new ArrayList<>();
                netRoute.setLinkIds(link1.getId(), routeLinks, link1.getId());
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStopBuilder(this.stop2).departureOffset(0.0).build());
                stops.add(sb.createTransitRouteStopBuilder(this.stop3).arrivalOffset(5*60.0).build());
                TransitRoute route = sb.createTransitRoute(Id.create("2to3", TransitRoute.class), netRoute, stops, "train");
                line2to3.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l2to3 d0", Departure.class), 8.0*3600 + 15 * 60));
                route.addDeparture(sb.createDeparture(Id.create("l2to3 d1", Departure.class), 9.0*3600 + 15 * 60));
                route.addDeparture(sb.createDeparture(Id.create("l2to3 d2", Departure.class), 10.0*3600 + 15 * 60));
                route.addDeparture(sb.createDeparture(Id.create("l2to3 d3", Departure.class), 11.0*3600 + 15 * 60));
                route.addDeparture(sb.createDeparture(Id.create("l2to3 d4", Departure.class), 12.0*3600 + 15 * 60));
            }
        }
    }


    /**
     * Generates the following network for testing:
     * <pre>
     *  [s] stop facilities
     *   l  lines
     *
     *  [2]---3---[3]---3---[4]
     *   |         |
     *   1         2
     *   |         |
     *  [1]---0---[0]
     *
     * </pre>
     *
     * 5 stop facilities and 4 lines:
     * - line 0 from Stop 0 to Stop 1
     * - line 1 from Stop 1 to Stop 2
     * - line 2 from Stop 0 to Stop 3
     * - line 3 from Stop 2 via Stop 3 to Stop 4
     *
     * travel times between stops are always 5 minutes.
     *
     * Lines 0, 1, 3 depart regularly during the day. Line 2 runs only in the night, when
     * the others don't run anymore.
     *
     * When searching a route from [0] to [4], stop [3] is reached with fewer transfers but later.
     * Raptor might have a special case when stop [3] is reached the first time after any departure at this stop,
     * so let's test it that it's handled correctly.
     */
    private static class NightBusFixture {

        /*package*/ final Config config;
        /*package*/ final Scenario scenario;
        /*package*/ final TransitSchedule schedule;
        /*package*/ final RaptorParameters routerConfig;

        final TransitStopFacility stop0;
        final TransitStopFacility stop1;
        final TransitStopFacility stop2;
        final TransitStopFacility stop3;
        final TransitStopFacility stop4;

        Id<TransitLine> lineId0 = Id.create(0, TransitLine.class);
        Id<TransitLine> lineId1 = Id.create(1, TransitLine.class);
        Id<TransitLine> lineId2 = Id.create(2, TransitLine.class);
        Id<TransitLine> lineId3 = Id.create(3, TransitLine.class);

        private NightBusFixture() {
            this.config = ConfigUtils.createConfig();
            this.scenario = ScenarioUtils.createScenario(this.config);
            this.routerConfig = RaptorUtils.createParameters(this.scenario.getConfig());

            // schedule
            this.schedule = this.scenario.getTransitSchedule();
            TransitScheduleFactory sb = this.schedule.getFactory();

            Id<Link> linkId0 = Id.create(0, Link.class);
            Id<Link> linkId10 = Id.create(10, Link.class);
            Id<Link> linkId20 = Id.create(20, Link.class);
            Id<Link> linkId30 = Id.create(30, Link.class);
            Id<Link> linkId40 = Id.create(40, Link.class);

            Network network = this.scenario.getNetwork();
            NetworkFactory nf = network.getFactory();

            Node nodeA = nf.createNode(Id.create("A", Node.class), new Coord(5000, 1000));
            Node nodeB = nf.createNode(Id.create("B", Node.class), new Coord(9000, 5000));
            network.addNode(nodeA);
            network.addNode(nodeB);
            Link link0 = nf.createLink(linkId0, nodeA, nodeB);
            Link link10 = nf.createLink(linkId10, nodeA, nodeB);
            Link link20 = nf.createLink(linkId20, nodeA, nodeB);
            Link link30 = nf.createLink(linkId30, nodeA, nodeB);
            Link link40 = nf.createLink(linkId40, nodeA, nodeB);
            network.addLink(link0);
            network.addLink(link10);
            network.addLink(link20);
            network.addLink(link30);
            network.addLink(link40);

            this.stop0 = sb.createTransitStopFacility(Id.create("0", TransitStopFacility.class), new Coord(5000, 1000), false);
            this.stop1 = sb.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord(1000, 1000), false);
            this.stop2 = sb.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord(1000, 5000), false);
            this.stop3 = sb.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord(5000, 5000), false);
            this.stop4 = sb.createTransitStopFacility(Id.create("4", TransitStopFacility.class), new Coord(9000, 5000), false);
            this.schedule.addStopFacility(this.stop0);
            this.schedule.addStopFacility(this.stop1);
            this.schedule.addStopFacility(this.stop2);
            this.schedule.addStopFacility(this.stop3);
            this.schedule.addStopFacility(this.stop4);
            this.stop0.setLinkId(linkId0);
            this.stop1.setLinkId(linkId10);
            this.stop2.setLinkId(linkId20);
            this.stop3.setLinkId(linkId30);
            this.stop4.setLinkId(linkId40);

            { // line 0
                TransitLine line0 = sb.createTransitLine(this.lineId0);
                this.schedule.addTransitLine(line0);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(linkId0, linkId10);
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStopBuilder(this.stop0).departureOffset(0.0).build());
                stops.add(sb.createTransitRouteStopBuilder(this.stop1).arrivalOffset(5*60.0).build());
                TransitRoute route = sb.createTransitRoute(Id.create("0to1", TransitRoute.class), netRoute, stops, "train");
                line0.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l0 d0", Departure.class), 8.0*3600));
            }
            { // line 1
                TransitLine line1 = sb.createTransitLine(this.lineId1);
                this.schedule.addTransitLine(line1);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(linkId10, linkId20);
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStopBuilder(this.stop1).departureOffset(0.0).build());
                stops.add(sb.createTransitRouteStopBuilder(this.stop2).arrivalOffset(5*60.0).build());
                TransitRoute route = sb.createTransitRoute(Id.create("1to2", TransitRoute.class), netRoute, stops, "train");
                line1.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l1 d0", Departure.class), 8.0*3600 + 10*60));
            }
            { // line 2
                TransitLine line2 = sb.createTransitLine(this.lineId2);
                this.schedule.addTransitLine(line2);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(linkId0, linkId30);
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStopBuilder(this.stop0).departureOffset(0.0).build());
                stops.add(sb.createTransitRouteStopBuilder(this.stop3).arrivalOffset(5*60.0).build());
                TransitRoute route = sb.createTransitRoute(Id.create("0to3", TransitRoute.class), netRoute, stops, "train");
                line2.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l2 d0", Departure.class), 23.0*3600));
            }
            { // line 3
                TransitLine line3 = sb.createTransitLine(this.lineId3);
                this.schedule.addTransitLine(line3);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(linkId20, linkId40);
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStopBuilder(this.stop2).departureOffset(0.0).build());
                stops.add(sb.createTransitRouteStopBuilder(this.stop3).departureOffset(5*60.0).build());
                stops.add(sb.createTransitRouteStopBuilder(this.stop4).arrivalOffset(10*60.0).build());
                TransitRoute route = sb.createTransitRoute(Id.create("2to4", TransitRoute.class), netRoute, stops, "train");
                line3.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l3 d0", Departure.class), 8.0*3600 + 20*60));
            }
        }
    }

    /**
     * Generates the following network for testing:
     * <pre>
     * (n) Node
     * [s] Stop Facility
     *  l  Link
     *
     *
     * (0)---0---(1) (2)---1---(3)---2---(4)---3---(5)
     * [0]       [1] [2]       [3]       [4]       [5]
     *
     * There are two transit lines: the Blue line from 0 to 1, and the Red line from 2 to 5.
     * Travel times between stops are 15 minutes, Each line departs every 20 minutes between
     * 6 and 8 o'clock.
     * </pre>
     */
    private static class TravelTimeDependentTransferFixture {

        private final Config config;
        private final Network network;
        private final TransitSchedule schedule;
        private final TransitStopFacility[] stops = new TransitStopFacility[6];

        TravelTimeDependentTransferFixture() {
            Id<Link> linkId0 = Id.create(0, Link.class);
            Id<Link> linkId1 = Id.create(1, Link.class);
            Id<Link> linkId2 = Id.create(2, Link.class);
            Id<Link> linkId3 = Id.create(3, Link.class);

            this.config = ConfigUtils.createConfig();
            Scenario scenario = ScenarioUtils.createScenario(config);

            this.network = scenario.getNetwork();
            NetworkFactory nf = this.network.getFactory();

            Node[] nodes = new Node[6];
            nodes[0] = nf.createNode(Id.create(0, Node.class), new Coord(0, 0));
            nodes[1] = nf.createNode(Id.create(1, Node.class), new Coord(10000, 0));
            nodes[2] = nf.createNode(Id.create(2, Node.class), new Coord(10000, 0));
            nodes[3] = nf.createNode(Id.create(3, Node.class), new Coord(20000, 0));
            nodes[4] = nf.createNode(Id.create(4, Node.class), new Coord(30000, 0));
            nodes[5] = nf.createNode(Id.create(5, Node.class), new Coord(40000, 0));
            for (Node node : nodes) {
                this.network.addNode(node);
            }
            Link link0 = nf.createLink(linkId0, nodes[0], nodes[1]);
            Link link1 = nf.createLink(linkId1, nodes[2], nodes[3]);
            Link link2 = nf.createLink(linkId2, nodes[3], nodes[4]);
            Link link3 = nf.createLink(linkId3, nodes[4], nodes[5]);

            this.network.addLink(link0);
            this.network.addLink(link1);
            this.network.addLink(link2);
            this.network.addLink(link3);

            this.schedule = scenario.getTransitSchedule();
            TransitScheduleFactory f = schedule.getFactory();

            this.stops[0] = f.createTransitStopFacility(Id.create(0, TransitStopFacility.class), nodes[0].getCoord(), false);
            this.stops[1] = f.createTransitStopFacility(Id.create(1, TransitStopFacility.class), nodes[1].getCoord(), false);
            this.stops[2] = f.createTransitStopFacility(Id.create(2, TransitStopFacility.class), nodes[2].getCoord(), false);
            this.stops[3] = f.createTransitStopFacility(Id.create(3, TransitStopFacility.class), nodes[3].getCoord(), false);
            this.stops[4] = f.createTransitStopFacility(Id.create(4, TransitStopFacility.class), nodes[4].getCoord(), false);
            this.stops[5] = f.createTransitStopFacility(Id.create(5, TransitStopFacility.class), nodes[5].getCoord(), false);
            this.stops[0].setLinkId(linkId0);
            this.stops[1].setLinkId(linkId0);
            this.stops[2].setLinkId(linkId1);
            this.stops[3].setLinkId(linkId1);
            this.stops[4].setLinkId(linkId2);
            this.stops[5].setLinkId(linkId3);

            for (TransitStopFacility stop : this.stops) {
                schedule.addStopFacility(stop);
            }

            TransitLine blueLine = f.createTransitLine(Id.create("Blue", TransitLine.class));
            NetworkRoute blueNetRoute = RouteUtils.createLinkNetworkRouteImpl(linkId0, linkId0);
            List<TransitRouteStop> blueStops = new ArrayList<>();
            TransitRouteStop rStop0 = f.createTransitRouteStopBuilder(this.stops[0]).departureOffset(0).build();
            TransitRouteStop rStop1 = f.createTransitRouteStopBuilder(this.stops[1]).arrivalOffset(900).build();
            blueStops.add(rStop0);
            blueStops.add(rStop1);
            TransitRoute blueRoute = f.createTransitRoute(Id.create("Blue", TransitRoute.class), blueNetRoute, blueStops, "train");
            for (int i = 0; i < 7; i++) {
                blueRoute.addDeparture(f.createDeparture(Id.create("blue" + i, Departure.class), 6*3600 + i * 1200));
            }
            blueLine.addRoute(blueRoute);
            schedule.addTransitLine(blueLine);

            TransitLine redLine = f.createTransitLine(Id.create("Red", TransitLine.class));
            NetworkRoute redNetRoute = RouteUtils.createLinkNetworkRouteImpl(linkId1, Collections.singletonList(linkId2), linkId3);
            List<TransitRouteStop> redStops = new ArrayList<>();
            TransitRouteStop rStop2 = f.createTransitRouteStopBuilder(this.stops[2]).departureOffset(0).build();
            TransitRouteStop rStop3 = f.createTransitRouteStop(this.stops[3], 900, 930);
            TransitRouteStop rStop4 = f.createTransitRouteStop(this.stops[4], 1800, 1830);
            TransitRouteStop rStop5 = f.createTransitRouteStopBuilder(this.stops[5]).arrivalOffset(2700).build();
            redStops.add(rStop2);
            redStops.add(rStop3);
            redStops.add(rStop4);
            redStops.add(rStop5);
            TransitRoute redRoute = f.createTransitRoute(Id.create("Red", TransitRoute.class), redNetRoute, redStops, "train");
            for (int i = 0; i < 7; i++) {
                redRoute.addDeparture(f.createDeparture(Id.create("red" + i, Departure.class), 6*3600 + i * 1200));
            }
            redLine.addRoute(redRoute);
            schedule.addTransitLine(redLine);
        }
    }
}
