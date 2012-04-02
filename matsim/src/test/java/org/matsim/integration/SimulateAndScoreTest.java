/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,     *
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

package org.matsim.integration;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.ptproject.qsim.interfaces.Netsim;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

public class SimulateAndScoreTest extends MatsimTestCase {

    public void testRealPtScore() {
        Config config = ConfigUtils.createConfig();
        config.scenario().setUseTransit(true);
        config.scenario().setUseVehicles(true);
        Scenario scenario = ScenarioUtils.createScenario(config);
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(0, 0));
        Node node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(100, 0));
        Node node3 = network.getFactory().createNode(scenario.createId("3"), scenario.createCoord(1100, 0));
        Node node4 = network.getFactory().createNode(scenario.createId("4"), scenario.createCoord(1200, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
        Link link1 = network.getFactory().createLink(scenario.createId("1"), node1, node2);
		link1.setLength(100);
		link1.setFreespeed(100);
		link1.setCapacity(60000);
		link1.setNumberOfLanes(9);
        Link link2 = network.getFactory().createLink(scenario.createId("2"), node2, node3);
		link2.setLength(1000);
		link2.setFreespeed(100);
		link2.setCapacity(6000);
		link2.setNumberOfLanes(2);
        Link link3 = network.getFactory().createLink(scenario.createId("3"), node3, node4);
		link3.setLength(100);
		link3.setFreespeed(100);
		link3.setCapacity(60000);
		link3.setNumberOfLanes(9);
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);



        TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
        TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("1"), scenario.createCoord(100, 0), false);
        stop1.setLinkId(link1.getId());
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("2"), scenario.createCoord(1100, 0), false);
        stop2.setLinkId(link2.getId());
		TransitStopFacility stop3 = builder.createTransitStopFacility(scenario.createId("3"), scenario.createCoord(1200, 0), false);
        stop3.setLinkId(link3.getId());


        TransitLine line1 = builder.createTransitLine(scenario.createId("L1"));
        LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(link1.getId(), link3.getId());
        route.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
        TransitRoute route1 = builder.createTransitRoute(new IdImpl("R1"), route, Arrays.asList( builder.createTransitRouteStop(stop1,0,10), builder.createTransitRouteStop(stop2,0,20), builder.createTransitRouteStop(stop3,0,30)), TransportMode.car);
        line1.addRoute(route1);
        Departure d1 = builder.createDeparture(new IdImpl("D1"), 100);
        d1.setVehicleId(new IdImpl("V1"));
        route1.addDeparture(d1);

        Vehicles vehicles = ((ScenarioImpl) scenario).getVehicles();
        VehicleType vehicleType = vehicles.getFactory().createVehicleType(new IdImpl("VT1"));
        VehicleCapacity vehicleCapacity = vehicles.getFactory().createVehicleCapacity();
        vehicleCapacity.setSeats(30);
        vehicleCapacity.setStandingRoom(70);
        vehicleType.setCapacity(vehicleCapacity);
        Vehicle vehicle = vehicles.getFactory().createVehicle(new IdImpl("V1"), vehicleType);
        vehicles.getVehicles().put(vehicle.getId(), vehicle);

        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        transitSchedule.addStopFacility(stop1);
        transitSchedule.addStopFacility(stop2);
        transitSchedule.addStopFacility(stop3);
        transitSchedule.addTransitLine(line1);

        PopulationFactoryImpl populationFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
        Person person = populationFactory.createPerson(new IdImpl("0"));
		Plan plan = populationFactory.createPlan();
		Activity a1 = populationFactory.createActivityFromCoord("h", link1.getCoord());
        ((ActivityImpl) a1).setLinkId(link1.getId());

		a1.setEndTime(3);
        plan.addActivity(a1);

        Leg leg = populationFactory.createLeg(TransportMode.pt);
        plan.addLeg(leg);

        TransitRouterImplFactory router = new TransitRouterImplFactory(transitSchedule, new TransitRouterConfig(scenario.getConfig().planCalcScore(), scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
                scenario.getConfig().vspExperimental()));

        TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(network, config.travelTimeCalculator());
        PersonalizableTravelDisutility travelCostCalculator = new TravelCostCalculatorFactoryImpl().createTravelDisutility(travelTimeCalculator, config.planCalcScore());
        PlansCalcTransitRoute plansCalcRoute = new PlansCalcTransitRoute(config.plansCalcRoute(), network, travelCostCalculator, travelTimeCalculator,
					new DijkstraFactory(), ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).getModeRouteFactory(), config.transit(), router.createTransitRouter(), transitSchedule);



		Activity a2 = populationFactory.createActivityFromCoord("w", link3.getCoord());
        ((ActivityImpl) a2).setLinkId(link3.getId());
        plan.addActivity(a2);



        person.addPlan(plan);
        plansCalcRoute.run(plan);
		scenario.getPopulation().addPerson(person);

        EventsManager events = EventsUtils.createEventsManager();
        scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
        Netsim sim = new QSimFactory().createMobsim(scenario, events);
        PlanCalcScoreConfigGroup.ActivityParams h = new PlanCalcScoreConfigGroup.ActivityParams("h");
        h.setTypicalDuration(16 * 3600);
        PlanCalcScoreConfigGroup.ActivityParams w = new PlanCalcScoreConfigGroup.ActivityParams("w");
        w.setTypicalDuration(8 * 3600);
        PlanCalcScoreConfigGroup.ActivityParams transitActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
        scenario.getConfig().planCalcScore().setPerforming_utils_hr(0);
        scenario.getConfig().planCalcScore().setTraveling_utils_hr(0);
        scenario.getConfig().planCalcScore().setTravelingPt_utils_hr(0);
        scenario.getConfig().planCalcScore().setTravelingWalk_utils_hr(0);
        scenario.getConfig().planCalcScore().setMonetaryDistanceCostRateCar(10);
        scenario.getConfig().planCalcScore().setMonetaryDistanceCostRatePt(0);
        scenario.getConfig().planCalcScore().addActivityParams(h);
        scenario.getConfig().planCalcScore().addActivityParams(w);
        scenario.getConfig().planCalcScore().addActivityParams(transitActivityParams);
        EventsToScore scorer = new EventsToScore(scenario, new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getNetwork()));
        events.addHandler(scorer);
        EventsCollector handler = new EventsCollector();
        events.addHandler(handler);

        sim.run();
        scorer.finish();

        System.out.println(plan.getScore());
        for (Event event : handler.getEvents()) {
            System.out.println(event);
        }

    }

    public void testTeleportationScore() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(scenario.createId("1"), scenario.createCoord(0, 0));
        Node node2 = network.getFactory().createNode(scenario.createId("2"), scenario.createCoord(100, 0));
        Node node3 = network.getFactory().createNode(scenario.createId("3"), scenario.createCoord(1100, 0));
        Node node4 = network.getFactory().createNode(scenario.createId("4"), scenario.createCoord(1200, 0));
		network.addNode(node1);
		network.addNode(node2);
		network.addNode(node3);
		network.addNode(node4);
        Link link1 = network.getFactory().createLink(scenario.createId("1"), node1, node2);
        Link link2 = network.getFactory().createLink(scenario.createId("2"), node2, node3);
        Link link3 = network.getFactory().createLink(scenario.createId("3"), node3, node4);
		network.addLink(link1);
		network.addLink(link2);
		network.addLink(link3);
        PopulationFactoryImpl populationFactory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
        Person person = populationFactory.createPerson(new IdImpl("0"));
		Plan plan = populationFactory.createPlan();
		Activity a1 = populationFactory.createActivityFromLinkId("h", link1.getId());
		a1.setEndTime(6*3600);
        plan.addActivity(a1);

        Leg leg = populationFactory.createLeg(TransportMode.pt);
        Route ptRoute = populationFactory.createRoute(TransportMode.pt, link1.getId(), link3.getId());
        ptRoute.setDistance(1000);
        leg.setTravelTime(3600); // This is really wrong. It should be set on the route (but that doesn't have an effect).
		leg.setRoute(ptRoute);
        plan.addLeg(leg);

		Activity a2 = populationFactory.createActivityFromLinkId("w", link3.getId());
        plan.addActivity(a2);
        person.addPlan(plan);
		scenario.getPopulation().addPerson(person);

        EventsManager events = EventsUtils.createEventsManager();
        scenario.getConfig().addQSimConfigGroup(new QSimConfigGroup());
        Netsim sim = new QSimFactory().createMobsim(scenario, events);
        PlanCalcScoreConfigGroup.ActivityParams h = new PlanCalcScoreConfigGroup.ActivityParams("h");
        h.setTypicalDuration(16 * 3600);
        PlanCalcScoreConfigGroup.ActivityParams w = new PlanCalcScoreConfigGroup.ActivityParams("w");
        w.setTypicalDuration(8 * 3600);
        scenario.getConfig().planCalcScore().setPerforming_utils_hr(0);
        scenario.getConfig().planCalcScore().setTravelingPt_utils_hr(-1.00);
        scenario.getConfig().planCalcScore().setMonetaryDistanceCostRatePt(-0.001);
        scenario.getConfig().planCalcScore().addActivityParams(h);
        scenario.getConfig().planCalcScore().addActivityParams(w);
        EventsToScore scorer = new EventsToScore(scenario, new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getNetwork()));
        events.addHandler(scorer);
        EventsCollector handler = new EventsCollector();
        events.addHandler(handler);

        sim.run();
        scorer.finish();

        Double score = plan.getScore();
        assertEquals("Expecting -1.0 from travel time, -1.0 from travel distance.", -2.0, score, EPSILON);

    }

}
