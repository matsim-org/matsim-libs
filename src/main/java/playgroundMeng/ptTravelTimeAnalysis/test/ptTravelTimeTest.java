/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package playgroundMeng.ptTravelTimeAnalysis.test;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorParametersForPerson;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import playgroundMeng.ptTravelTimeAnalysis.FakeFacility;

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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Most of these tests were copied from org.matsim.pt.router.TransitRouterImplTest
 * and only minimally adapted to make them run with SwissRailRaptor.
 *
 * @author mrieser / SBB
 */
public class ptTravelTimeTest {

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
//        assertEquals(3, legs.size());
//        assertEquals(TransportMode.non_network_walk, legs.get(0).getMode());
//        assertEquals(TransportMode.pt, legs.get(1).getMode());
//        assertEquals(TransportMode.non_network_walk, legs.get(2).getMode());
//        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
//        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
//        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
//        assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
//        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
//        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
//        double actualTravelTime = 0.0;
//        double distance = 0.0;
//        for (Leg leg : legs) {
//            System.out.println(leg+" "+leg.getRoute().getDistance());
//            actualTravelTime += leg.getTravelTime();
//            distance += leg.getRoute().getDistance();
//        }
//        double expectedTravelTime = 29.0 * 60 + // agent takes the *:06 course, arriving in D at *:29
//                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / raptorParams.getBeelineWalkSpeed();
//        assertEquals(Math.ceil(expectedTravelTime), actualTravelTime, MatsimTestCase.EPSILON);
//        assertEquals(15434, Math.ceil(distance), MatsimTestCase.EPSILON);
        double actualTravelTime = 0.0;
	      double distance = 0.0;
	      for (Leg leg : legs) {
	          System.out.println(leg+" "+leg.getRoute().getDistance());
	          actualTravelTime += leg.getTravelTime();
	          distance += leg.getRoute().getDistance();
	      }
	      System.out.println(actualTravelTime);
    }
}