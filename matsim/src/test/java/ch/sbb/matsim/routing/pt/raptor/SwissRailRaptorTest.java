/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.routing.pt.raptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
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
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

/**
 * Most of these tests were copied from org.matsim.pt.router.TransitRouterImplTest
 * and only minimally adapted to make them run with SwissRailRaptor.
 *
 * @author mrieser / SBB
 */
public class SwissRailRaptorTest {

    private SwissRailRaptor createTransitRouter(TransitSchedule schedule, Config config, Network network) {
        SwissRailRaptorData data = SwissRailRaptorData.create(schedule, RaptorUtils.createStaticConfig(config), network);
        DefaultRaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, new DefaultRaptorIntermodalAccessEgress(), null);
        SwissRailRaptor raptor = new SwissRailRaptor(data, new DefaultRaptorParametersForPerson(config), new LeastCostRaptorRouteSelector(), stopFinder);
        return raptor;
    }

    @Test
    public void testSingleLine() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        double distance = 0.0;
        for (Leg leg : legs) {
            System.out.println(leg+" "+leg.getRoute().getDistance());
            actualTravelTime += leg.getTravelTime();
            distance += leg.getRoute().getDistance();
        }
        double expectedTravelTime = 29.0 * 60 + // agent takes the *:06 course, arriving in D at *:29
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestCase.EPSILON);
        assertEquals(15434, Math.ceil(distance), MatsimTestCase.EPSILON);
    }

    @Test
    public void testSingleLine_linkIds() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        Id<Link> fromLinkId = Id.create("ffrroomm", Link.class);
        Id<Link> toLinkId = Id.create("ttoo", Link.class);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord, fromLinkId), new FakeFacility(toCoord, toLinkId), 5.0*3600, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(fromLinkId, legs.get(0).getRoute().getStartLinkId());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        assertEquals(toLinkId, legs.get(2).getRoute().getEndLinkId());
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
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestCase.EPSILON);
    }

    @Test
    public void testWalkDurations() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());

        double expectedAccessWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("0", TransitStopFacility.class)).getCoord(), fromCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedAccessWalkTime), legs.get(0).getTravelTime(), MatsimTestUtils.EPSILON);
        double expectedEgressWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedEgressWalkTime), legs.get(2).getTravelTime(), MatsimTestUtils.EPSILON);
    }

    @Test
    public void testWalkDurations_range() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        SwissRailRaptor router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        double depTime = 5.0 * 3600;
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());

        double expectedAccessWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("0", TransitStopFacility.class)).getCoord(), fromCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedAccessWalkTime), legs.get(0).getTravelTime(), MatsimTestUtils.EPSILON);
        double expectedEgressWalkTime = CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedEgressWalkTime), legs.get(2).getTravelTime(), MatsimTestUtils.EPSILON);
    }


    /**
     * The fromFacility and toFacility are both closest to TransitStopFacility I. The expectation is that the Swiss Rail
     * Raptor will return null (TripRouter / FallbackRouter will create a transit_walk between the fromFacility and 
     * toFacility) instead of routing the agent to make a major detour by walking the triangle from the fromFacility to 
     * the transitStopFacility and then to the toFacility, without once using pt.
     */

    @Test
    public void testFromToSameStop() {
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(4100, 5050);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null);
        
        Assert.assertNull("The router should not find a route and return null, but did return something else.", legs);
    }



    // now the pt router should always try to return a pt route no matter whether a direct walk would be faster
    // adjusted the test - gl aug'19
    @Test
    public void testDirectWalkCheaper() {
        Fixture f = new Fixture();
        f.init();
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(4000, 3000);
        Coord toCoord = new Coord(8000, 3000);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null);
        
        assertEquals(1, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(4000*1.3, legs.get(0).getRoute().getDistance(), 0.0);
        double actualTravelTime = legs.get(0).getTravelTime();
        double expectedTravelTime = CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
    }
    
    @Test
    public void testDirectWalkFactor() {
        Fixture f = new Fixture();
        f.init();
        f.config.transitRouter().setDirectWalkFactor(100.0);
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(4000, 3000);
        Coord toCoord = new Coord(8000, 3000);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null);
        
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        // check individual legs
        Coord ptStop0 = f.schedule.getFacilities().get(Id.create("0", TransitStopFacility.class)).getCoord();
        double expectedAccessWalkTravelTime = CoordUtils.calcEuclideanDistance(fromCoord, ptStop0) / raptorParams.getBeelineWalkSpeed();
        assertEquals(expectedAccessWalkTravelTime, legs.get(0).getTravelTime(), 1.0);
        assertEquals((5002-3000)*1.3, legs.get(0).getRoute().getDistance(), 0.0);

        double expectedPtTravelTime = 6.0*3600 + 6.0*60 - (5.0*3600 + expectedAccessWalkTravelTime) + 7*60; // next departure blue line is at 6.0*3600 + 6.0*60, 7*60 travel time
        assertEquals(expectedPtTravelTime, legs.get(1).getTravelTime(), 1.0);
        
        Coord ptStop2 = f.schedule.getFacilities().get(Id.create("2", TransitStopFacility.class)).getCoord();
        double expectedEgressWalkTravelTime = CoordUtils.calcEuclideanDistance(ptStop2, toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(expectedEgressWalkTravelTime, legs.get(2).getTravelTime(), 1.0);
        assertEquals((5002-3000)*1.3, legs.get(2).getRoute().getDistance(), 0.0);
    }

    @Test
    public void testSingleLine_DifferentWaitingTime() {
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(4000, 5002);
        Coord toCoord = new Coord(8000, 5002);

        double inVehicleTime = 7.0*60; // travel time from A to B
        for (int min = 0; min < 30; min += 3) {
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600 + min*60, null);
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
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord toCoord = new Coord(16100, 10050);
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord(3800, 5100)), new FakeFacility(toCoord), 6.0*3600, null);
        assertEquals(5, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        assertEquals(TransportMode.pt, legs.get(3).getMode());
        assertEquals(TransportMode.walk, legs.get(4).getMode());
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
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("19", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestCase.EPSILON);
    }

    @Test
    public void testFasterAlternative() {
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
        List<Leg> legs = router.calcRoute(new FakeFacility( new Coord(3800, 5100)), new FakeFacility(toCoord), 5.0*3600 + 40.0*60, null);
        assertEquals("wrong number of legs", 4, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.pt, legs.get(2).getMode());
        assertEquals(TransportMode.walk, legs.get(3).getMode());
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
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("12", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestCase.EPSILON);
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
        f.config.planCalcScore().setUtilityOfLineSwitch(0);
        RaptorParameters raptorParams = RaptorUtils.createParameters(f.config);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null);
        assertEquals("wrong number of legs", 5, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.redLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        assertEquals(TransportMode.pt, legs.get(3).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getLineId());
        assertEquals(TransportMode.walk, legs.get(4).getMode());

        Config config = ConfigUtils.createConfig();
        double transferUtility = 300.0 * raptorParams.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt); // corresponds to 5 minutes transit travel time
        config.planCalcScore().setUtilityOfLineSwitch(transferUtility);
        raptorParams = RaptorUtils.createParameters(config);
        Assert.assertEquals(-transferUtility, raptorParams.getTransferPenaltyFixCostPerTransfer(), 0.0);
        router = createTransitRouter(f.schedule, config, f.network);
        legs = router.calcRoute(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
    }

    @Test
    public void testTransferTime() {
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
        f.config.planCalcScore().setUtilityOfLineSwitch(0);
        f.config.transitRouter().setAdditionalTransferTime(0);
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null);
        assertEquals("wrong number of legs",5, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.redLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        assertEquals(TransportMode.pt, legs.get(3).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getLineId());
        assertEquals(TransportMode.walk, legs.get(4).getMode());

        Config config = ConfigUtils.createConfig();
        config.transitRouter().setAdditionalTransferTime(3*60 + 1);
        router = createTransitRouter(f.schedule, config, f.network); // this is necessary to update the router for any change in config.
        legs = router.calcRoute(new FakeFacility(new Coord(11900, 5100)), new FakeFacility(new Coord(24100, 4950)), 6.0*3600 - 5.0*60, null);
        assertEquals("wrong number of legs",3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
    }


    @Test
    public void testAfterMidnight() {
        // in contrast to the default PT router, SwissRailRaptor will not automatically
        // repeat the schedule after 24 hours, so any agent departing late will have to walk if there
        // is no late service in the schedule.
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 25.0*3600, null);
        
        Assert.assertNull("The router should not find a route and return null, but did return something else.", legs);
    }

    @Test
    public void testCoordFarAway() {
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        double x = +42000;
        double x1 = -2000;
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord(x1, 0)), new FakeFacility(new Coord(x, 0)), 5.5*3600, null); // should map to stops A and I
        assertEquals(3, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
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
    public void testSingleWalkOnly() {
        WalkFixture f = new WalkFixture();
        f.scenario.getConfig().transitRouter().setSearchRadius(0.8 * CoordUtils.calcEuclideanDistance(f.coord2, f.coord4));
        f.scenario.getConfig().transitRouter().setExtensionRadius(0.0);

        TransitRouter router = createTransitRouter(f.schedule, f.scenario.getConfig(), f.scenario.getNetwork());
        List<Leg> legs = router.calcRoute(new FakeFacility(f.coord2), new FakeFacility(f.coord4), 990, null);
        Assert.assertNull("The router should not find a route and return null, but did return something else.", legs);
    }


    /**
     * Tests that if only exactly two transfer-/walk-link are found, the router correctly only returns
     * null (which will be replaced by the TripRouter and FallbackRouter with one walk leg from start 
     * to end). Differs from {@link #testSingleWalkOnly()} in that it tests for the correct internal 
     * working when more than one walk links are returned.
     */
    @Test
    public void testDoubleWalkOnly() {
        WalkFixture f = new WalkFixture();
        f.scenario.getConfig().transitRouter().setSearchRadius(0.8 * CoordUtils.calcEuclideanDistance(f.coord2, f.coord4));
        f.scenario.getConfig().transitRouter().setExtensionRadius(0.0);

        TransitRouter router = createTransitRouter(f.schedule, f.scenario.getConfig(), f.scenario.getNetwork());
        List<Leg> legs = router.calcRoute(new FakeFacility(f.coord2), new FakeFacility(f.coord6), 990, null);
        
        Assert.assertNull("The router should not find a route and return null, but did return something else.", legs);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLongTransferTime_withTransitRouterWrapper() {
        // 5 minutes additional transfer time
        {
            TransferFixture f = new TransferFixture(5 * 60.0);
            TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
            Coord fromCoord = f.fromFacility.getCoord();
            Coord toCoord = f.toFacility.getCoord();
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 7.0*3600 + 50*60, null);
            double legDuration = calcTripDuration(new ArrayList<>(legs));
            Assert.assertEquals(5, legs.size());
            Assert.assertEquals(100, legs.get(0).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
            Assert.assertEquals(800, legs.get(1).getTravelTime(), 0.0);	// 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
            Assert.assertEquals(300, legs.get(2).getTravelTime(), 0.0);	// 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s; arrival at 08:10:00
            Assert.assertEquals(600, legs.get(3).getTravelTime(), 0.0);	// 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
            Assert.assertEquals(100, legs.get(4).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s
            Assert.assertEquals(1900.0, legDuration, 0.0);

            RoutingModule walkRoutingModule = DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, f.scenario,
                    f.config.plansCalcRoute().getModeRoutingParams().get(TransportMode.walk));

            TransitRouterWrapper wrapper = new TransitRouterWrapper(
                    router,
                    f.schedule,
                    f.scenario.getNetwork(), // use a walk router in case no PT path is found
                    walkRoutingModule);

            List<PlanElement> planElements = (List<PlanElement>) wrapper.calcRoute(f.fromFacility, f.toFacility, 7.0*3600 + 50*60, null);
            double tripDuration = calcTripDuration(planElements);
            Assert.assertEquals(9, planElements.size());
            Assert.assertEquals(100, ((Leg) planElements.get(0)).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
            Assert.assertEquals(800, ((Leg) planElements.get(2)).getTravelTime(), 0.0);	// 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
            Assert.assertEquals(300, ((Leg) planElements.get(4)).getTravelTime(), 0.0);	// 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s; arrival at 08:10:00
            Assert.assertEquals(600, ((Leg) planElements.get(6)).getTravelTime(), 0.0);	// 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
            Assert.assertEquals(100, ((Leg) planElements.get(8)).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s
            Assert.assertEquals(1900.0, tripDuration, 0.0);
        }

        // 65 minutes additional transfer time - miss one departure
        {
            TransferFixture f = new TransferFixture(65 * 60.0);
            TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
            Coord fromCoord = f.fromFacility.getCoord();
            Coord toCoord = f.toFacility.getCoord();
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 7.0*3600 + 50*60, null);
            double legDuration = calcTripDuration(new ArrayList<>(legs));
            Assert.assertEquals(5, legs.size());
            Assert.assertEquals(100, legs.get(0).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
            Assert.assertEquals(800, legs.get(1).getTravelTime(), 0.0);	// 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
            Assert.assertEquals(3900, legs.get(2).getTravelTime(), 0.0);	// 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s; arrival at 08:10:00
            Assert.assertEquals(600, legs.get(3).getTravelTime(), 0.0);	// 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
            Assert.assertEquals(100, legs.get(4).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s
            Assert.assertEquals(5500.0, legDuration, 0.0);

            RoutingModule walkRoutingModule = DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, f.scenario,
                    f.config.plansCalcRoute().getModeRoutingParams().get(TransportMode.walk));

            TransitRouterWrapper wrapper = new TransitRouterWrapper(
                    router,
                    f.schedule,
                    f.scenario.getNetwork(), // use a walk router in case no PT path is found
                    walkRoutingModule);

            List<PlanElement> planElements = (List<PlanElement>) wrapper.calcRoute(f.fromFacility, f.toFacility, 7.0*3600 + 50*60, null);
            double tripDuration = calcTripDuration(planElements);
            Assert.assertEquals(9, planElements.size());
            Assert.assertEquals(100, ((Leg) planElements.get(0)).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s; arrival at 07:51:40
            Assert.assertEquals(800, ((Leg) planElements.get(2)).getTravelTime(), 0.0);	// 8m 20s waiting for pt departure and 5m pt travel time -> 500s + 300s = 800s; arrival at 08:05:00
            Assert.assertEquals(3900, ((Leg) planElements.get(4)).getTravelTime(), 0.0);	// 0.004km with 1m/s walk speed, but minimal waiting time -> max(4s, 300s) = 300s; arrival at 08:10:00
            Assert.assertEquals(600, ((Leg) planElements.get(6)).getTravelTime(), 0.0);	// 5m 00s waiting for pt departure and 5m pt travel time -> 300s + 300s = 600s; arrival at 08:15:00
            Assert.assertEquals(100, ((Leg) planElements.get(8)).getTravelTime(), 0.0);	// 0.1km with 1m/s walk speed -> 100s
            Assert.assertEquals(5500.0, tripDuration, 0.0);
        }

        // 600 minutes additional transfer time - miss all departures
        {
            TransferFixture f = new TransferFixture(600 * 60.0);
            TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
            Coord fromCoord = f.fromFacility.getCoord();
            Coord toCoord = f.toFacility.getCoord();
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 7.0*3600 + 50*60, null);
            Assert.assertNull("The router should not find a route and return null, but did return something else.", legs);

            RoutingModule walkRoutingModule = DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, f.scenario,
                    f.config.plansCalcRoute().getModeRoutingParams().get(TransportMode.walk));

            TransitRouterWrapper routingModule = new TransitRouterWrapper(
                    router,
                    f.schedule,
                    f.scenario.getNetwork(), // use a walk router in case no PT path is found
                    walkRoutingModule);

            TransitRouterWrapper wrapper = new TransitRouterWrapper(router, f.schedule, f.scenario.getNetwork(), routingModule);
            List<PlanElement> planElements = (List<PlanElement>) wrapper.calcRoute(f.fromFacility, f.toFacility, 7.0*3600 + 50*60, null);
            Assert.assertNull("The router should not find a route and return null, but did return something else.", planElements);
        }
    }

    private static double calcTripDuration(List<PlanElement> planElements) {
        double duration = 0.0;
        for (PlanElement pe : planElements) {
            if (pe instanceof Activity) {
                Activity act = (Activity)pe;
                double startTime = act.getStartTime();
                if (!Time.isUndefinedTime(startTime) && !act.isEndTimeUndefined()) {
                    double endTime = act.getEndTime();
                    duration += (endTime - startTime);
                }
            } else if (pe instanceof Leg) {
                Leg leg = (Leg) pe;
                duration += leg.getTravelTime();
            }
        }
        return duration;
    }

    @Test
    public void testNightBus() {
        // test a special case where a direct connection only runs at a late time, when typically
        // no other services run anymore.
        NightBusFixture f = new NightBusFixture();

        TransitRouter router = createTransitRouter(f.schedule, f.config, f.scenario.getNetwork());
        Coord fromCoord = new Coord(5010, 1010);
        Coord toCoord = new Coord(5010, 5010);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 8.0*3600-2*60, null);
        assertEquals(5, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.pt, legs.get(2).getMode());
        assertEquals(TransportMode.pt, legs.get(3).getMode());
        assertEquals(TransportMode.walk, legs.get(4).getMode());
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(f.stop0.getId(), ptRoute.getAccessStopId());
        assertEquals(f.stop1.getId(), ptRoute.getEgressStopId());
        assertEquals(f.lineId0, ptRoute.getLineId());
        assertTrue("expected TransitRoute in leg.", legs.get(2).getRoute() instanceof ExperimentalTransitRoute);
        ptRoute = (ExperimentalTransitRoute) legs.get(2).getRoute();
        assertEquals(f.stop1.getId(), ptRoute.getAccessStopId());
        assertEquals(f.stop2.getId(), ptRoute.getEgressStopId());
        assertEquals(f.lineId1, ptRoute.getLineId());
        assertTrue("expected TransitRoute in leg.", legs.get(3).getRoute() instanceof ExperimentalTransitRoute);
        ptRoute = (ExperimentalTransitRoute) legs.get(3).getRoute();
        assertEquals(f.stop2.getId(), ptRoute.getAccessStopId());
        assertEquals(f.stop3.getId(), ptRoute.getEgressStopId());
        assertEquals(f.lineId3, ptRoute.getLineId());
    }

    @Test
    public void testCircularLine() {
        Fixture f = new Fixture();
        f.init();

        SwissRailRaptor raptor = createTransitRouter(f.schedule, f.config, f.network);

        Coord fromCoord = new Coord(16000, 100); // stop N
        Coord toCoord = new Coord(24000, 9950); // stop L
        double depTime = 5.0 * 3600 + 50 * 60;
        List<Leg> legs = raptor.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime,null);

        for (Leg leg : legs) {
            System.out.println(leg);
        }

        Assert.assertEquals(4, legs.size());
        Assert.assertEquals(TransportMode.walk, legs.get(0).getMode());
        Assert.assertEquals(TransportMode.pt, legs.get(1).getMode());
        Assert.assertEquals(TransportMode.pt, legs.get(2).getMode());
        Assert.assertEquals(TransportMode.walk, legs.get(3).getMode());

        Assert.assertEquals(f.greenLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        Assert.assertEquals(Id.create(23, TransitStopFacility.class), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getAccessStopId());
        Assert.assertEquals(f.greenLine.getId(), ((ExperimentalTransitRoute) legs.get(2).getRoute()).getLineId());
        Assert.assertEquals(Id.create(20, TransitStopFacility.class), ((ExperimentalTransitRoute) legs.get(2).getRoute()).getEgressStopId());
    }

    @Test
    public void testRangeQuery() {
        Fixture f = new Fixture();
        f.init();
        SwissRailRaptor raptor = createTransitRouter(f.schedule, f.config, f.network);

        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(28100, 4950);
        double depTime = 5.0 * 3600 + 50 * 60;
        List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 600, depTime, depTime + 3600, null);

        for (int i = 0; i < routes.size(); i++) {
            RaptorRoute route = routes.get(i);
            System.out.println(i + "  depTime = " + Time.writeTime(route.getDepartureTime()) + "  arrTime = " + Time.writeTime(route.getDepartureTime() + route.getTravelTime()) + "  # transfers = " + route.getNumberOfTransfers() + "  costs = " + route.getTotalCosts());
        }

        Assert.assertEquals(6, routes.size());

        assertRaptorRoute(routes.get(0), "05:40:12", "06:30:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(1), "06:00:12", "06:50:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(2), "06:20:12", "07:10:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(3), "06:40:12", "07:30:56", 0, 10.1466666);
        assertRaptorRoute(routes.get(4), "05:40:12", "06:11:56", 1, 7.3466666);
        assertRaptorRoute(routes.get(5), "06:40:12", "07:11:56", 1, 7.3466666);
    }

    private void assertRaptorRoute(RaptorRoute route, String depTime, String arrTime, int expectedTransfers, double expectedCost) {
        Assert.assertEquals("wrong number of transfers", expectedTransfers, route.getNumberOfTransfers());
        Assert.assertEquals("wrong departure time", Time.parseTime(depTime), route.getDepartureTime(), 0.99);
        Assert.assertEquals("wrong arrival time", Time.parseTime(arrTime), route.getDepartureTime() + route.getTravelTime(), 0.99);
        Assert.assertEquals("wrong cost", expectedCost, route.getTotalCosts(), 1e-5);
    }

    /** test for https://github.com/SchweizerischeBundesbahnen/matsim-sbb-extensions/issues/1
     *
     * If there are StopFacilities in the transit schedule, that are not part of any route, the Router crashes with a NPE in SwissRailRaptorData at line 213, because toRouteStopIndices == null.
     */
    @Test
    public void testUnusedTransitStop() {
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
        List<Leg> legs = raptor.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime, null);
        // this test mostly checks that there are no Exceptions.
        Assert.assertEquals(3, legs.size());
    }

    @Test
    public void testTravelTimeDependentTransferCosts() {
        TravelTimeDependentTransferFixture f = new TravelTimeDependentTransferFixture();

        { // test default 0 + 0 * tt
            Config config = prepareConfig(0, 0); // sets beeline walk speed to 1m/s
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(20000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null);
            Assert.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 35*60; // 35 minutes: 15 Blue, 5 Transfer, 15 Red
            double expectedAccessEgressTime = 2 * 100; // 2 * 100 meters at 1m/s beeline walk speed (beeline factor included)
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt);

            Assert.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }

        { // test 2 + 0 * tt
            Config config = prepareConfig(2, 0);
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(20000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null);
            Assert.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 35*60; // 35 minutes: 15 Blue, 5 Transfer, 15 Red
            double expectedAccessEgressTime = 2 * 100;  // 2 * 100 meters at 1m/s
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt) + 2;

            Assert.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }

        { // test 2 + 9 * tt[h]
            Config config = prepareConfig(2, 9);
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(20000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null);
            Assert.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 35*60; // 35 minutes: 15 Blue, 5 Transfer, 15 Red
            double expectedAccessEgressTime = 2 * 100;  // 2 * 100 meters at 1m/s
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt) + 2 + 0.0025 * expectedTravelTime;

            Assert.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }

        { // test 2 + 9 * tt[h], longer trip
            Config config = prepareConfig(2, 9);
            SwissRailRaptor raptor = createTransitRouter(f.schedule, config, f.network);

            Coord fromCoord = new Coord(0, 100);
            Coord toCoord = new Coord(40000, 100);
            double depTime = 6.0 * 3600;
            List<RaptorRoute> routes = raptor.calcRoutes(new FakeFacility(fromCoord), new FakeFacility(toCoord), depTime - 300, depTime, depTime + 300, null);
            Assert.assertEquals(1, routes.size());
            RaptorRoute route1 = routes.get(0);

            RaptorParameters params = RaptorUtils.createParameters(config);

            double expectedTravelTime = 65*60; // 65 minutes: 15 Blue, 5 Transfer, 45 Red
            double expectedAccessEgressTime = 2 * 100;  // 2 * 100 meters at 1m/s
            double expectedCost = (expectedTravelTime + expectedAccessEgressTime) * -params.getMarginalUtilityOfTravelTime_utl_s(TransportMode.pt) + 2 + 0.0025 * expectedTravelTime;

            Assert.assertEquals(expectedCost, route1.getTotalCosts(), 1e-7);
        }
    }

    private Config prepareConfig(double transferFixedCost, double transferRelativeCostFactor) {
        SwissRailRaptorConfigGroup srrConfig = new SwissRailRaptorConfigGroup();
        Config config = ConfigUtils.createConfig(srrConfig);
        config.transitRouter().setDirectWalkFactor(1.0);

        double beelineDistanceFactor = config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
        PlansCalcRouteConfigGroup.ModeRoutingParams walkParameters = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.walk);
        walkParameters.setTeleportedModeSpeed(beelineDistanceFactor); // set it such that the beelineWalkSpeed is exactly 1
        config.plansCalcRoute().addParameterSet(walkParameters);

        config.planCalcScore().setUtilityOfLineSwitch(-transferFixedCost);
        srrConfig.setTransferPenaltyBaseCost(transferFixedCost);
        srrConfig.setTransferPenaltyCostPerTravelTimeHour(transferRelativeCostFactor);

        return config;
    }

    @Test
    public void testModeMapping() {
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
        f.config.planCalcScore().addModeParams(railParams);

        ModeParams roadParams = new ModeParams("road");
        roadParams.setMarginalUtilityOfTraveling(-6.0);
        f.config.planCalcScore().addModeParams(roadParams);

        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord toCoord = new Coord(16100, 10050);
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord(3800, 5100)), new FakeFacility(toCoord), 6.0*3600, null);
        assertEquals(5, legs.size());
        assertEquals(TransportMode.walk, legs.get(0).getMode());
        assertEquals("rail", legs.get(1).getMode());
        assertEquals(TransportMode.walk, legs.get(2).getMode());
        assertEquals("road", legs.get(3).getMode());
        assertEquals(TransportMode.walk, legs.get(4).getMode());
    }

    @Test
    public void testModeMappingCosts() {
        Fixture f = new Fixture();
        f.init();

        Coord fromCoord = new Coord(12000, 5050); // C
        Coord toCoord = new Coord(28000, 5050); // G
        { // test default, from C to G the red line is the fastest/cheapest

            TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0 * 3600 - 5 * 60, null);
            assertEquals(3, legs.size());
            assertEquals(TransportMode.walk, legs.get(0).getMode());
            assertEquals("pt", legs.get(1).getMode());
            assertEquals(TransportMode.walk, legs.get(2).getMode());

            ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
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
            config.planCalcScore().addModeParams(railParams);

            ModeParams roadParams = new ModeParams("road");
            roadParams.setMarginalUtilityOfTraveling(-6.0);
            config.planCalcScore().addModeParams(roadParams);
        }

        { // test with similar costs, the red line should still be cheaper
            TransitRouter router = createTransitRouter(f.schedule, config, f.network);
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0 * 3600 - 5 * 60, null);
            assertEquals(3, legs.size());
            assertEquals(TransportMode.walk, legs.get(0).getMode());
            assertEquals("rail", legs.get(1).getMode());
            assertEquals(TransportMode.walk, legs.get(2).getMode());

            ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
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
            config.planCalcScore().addModeParams(roadParams);

            TransitRouter router = createTransitRouter(f.schedule, config, f.network);
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 6.0 * 3600 - 5 * 60, null);
            assertEquals(3, legs.size());
            assertEquals(TransportMode.walk, legs.get(0).getMode());
            assertEquals("road", legs.get(1).getMode());
            assertEquals(TransportMode.walk, legs.get(2).getMode());

            ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
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
    public void testDepartureAfterLastBus(){
        Fixture f = new Fixture();
        f.init();
        TransitRouter router = createTransitRouter(f.schedule, f.config, f.network);
        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(8100, 5050);

        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 11.0*3600, null);

        Assert.assertNull("The router should not find a route and return null, but did return something else.", legs);
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
            this.coord1 = new Coord(x, (double) 0);
            x += 1000;
            this.coord2 = new Coord(x, (double) 0);
            x += (this.staticConfig.getBeelineWalkConnectionDistance() * 0.75);
            double y = -1000;
            this.coord3 = new Coord(x, y);
            this.coord4 = new Coord(x, (double) 0);
            this.coord5 = new Coord(x, (double) 1000);
            x += (this.staticConfig.getBeelineWalkConnectionDistance() * 0.75);
            this.coord6 = new Coord(x, (double) 0);
            x += 1000;
            this.coord7 = new Coord(x, (double) 0);

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

            double beelineDistanceFactor = this.config.plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor();
            PlansCalcRouteConfigGroup.ModeRoutingParams walkParameters = new PlansCalcRouteConfigGroup.ModeRoutingParams(TransportMode.walk);
            walkParameters.setTeleportedModeSpeed(beelineDistanceFactor); // set it such that the beelineWalkSpeed is exactly 1
            this.config.plansCalcRoute().addParameterSet(walkParameters);

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
                stops.add(sb.createTransitRouteStop(this.stop0, Time.getUndefinedTime(), 0.0));
                stops.add(sb.createTransitRouteStop(this.stop1, 5*60.0, Time.getUndefinedTime()));
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
                stops.add(sb.createTransitRouteStop(this.stop2, Time.getUndefinedTime(), 0.0));
                stops.add(sb.createTransitRouteStop(this.stop3, 5*60.0, Time.getUndefinedTime()));
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
                stops.add(sb.createTransitRouteStop(this.stop0, Time.getUndefinedTime(), 0.0));
                stops.add(sb.createTransitRouteStop(this.stop1, 5*60.0, Time.getUndefinedTime()));
                TransitRoute route = sb.createTransitRoute(Id.create("0to1", TransitRoute.class), netRoute, stops, "train");
                line0.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l0 d0", Departure.class), 8.0*3600));
            }
            { // line 1
                TransitLine line1 = sb.createTransitLine(this.lineId1);
                this.schedule.addTransitLine(line1);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(linkId10, linkId20);
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStop(this.stop1, Time.getUndefinedTime(), 0.0));
                stops.add(sb.createTransitRouteStop(this.stop2, 5*60.0, Time.getUndefinedTime()));
                TransitRoute route = sb.createTransitRoute(Id.create("1to2", TransitRoute.class), netRoute, stops, "train");
                line1.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l1 d0", Departure.class), 8.0*3600 + 10*60));
            }
            { // line 2
                TransitLine line2 = sb.createTransitLine(this.lineId2);
                this.schedule.addTransitLine(line2);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(linkId0, linkId30);
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStop(this.stop0, Time.getUndefinedTime(), 0.0));
                stops.add(sb.createTransitRouteStop(this.stop3, 5*60.0, Time.getUndefinedTime()));
                TransitRoute route = sb.createTransitRoute(Id.create("0to3", TransitRoute.class), netRoute, stops, "train");
                line2.addRoute(route);

                route.addDeparture(sb.createDeparture(Id.create("l2 d0", Departure.class), 23.0*3600));
            }
            { // line 3
                TransitLine line3 = sb.createTransitLine(this.lineId3);
                this.schedule.addTransitLine(line3);
                NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(linkId20, linkId40);
                List<TransitRouteStop> stops = new ArrayList<>();
                stops.add(sb.createTransitRouteStop(this.stop2, Time.getUndefinedTime(), 0.0));
                stops.add(sb.createTransitRouteStop(this.stop3, Time.getUndefinedTime(), 5*60.0));
                stops.add(sb.createTransitRouteStop(this.stop4, 10*60.0, Time.getUndefinedTime()));
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
            TransitRouteStop rStop0 = f.createTransitRouteStop(this.stops[0], Time.getUndefinedTime(), 0);
            TransitRouteStop rStop1 = f.createTransitRouteStop(this.stops[1], 900, Time.getUndefinedTime());
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
            TransitRouteStop rStop2 = f.createTransitRouteStop(this.stops[2], Time.getUndefinedTime(), 0);
            TransitRouteStop rStop3 = f.createTransitRouteStop(this.stops[3], 900, 930);
            TransitRouteStop rStop4 = f.createTransitRouteStop(this.stops[4], 1800, 1830);
            TransitRouteStop rStop5 = f.createTransitRouteStop(this.stops[5], 2700, Time.getUndefinedTime());
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