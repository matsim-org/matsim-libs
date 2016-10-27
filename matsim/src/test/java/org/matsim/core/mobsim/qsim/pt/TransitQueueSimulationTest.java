/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulationTest.java
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

package org.matsim.core.mobsim.qsim.pt;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ExternalMobimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine.TransitAgentTriesToTeleportException;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.utils.CreateVehiclesForSchedule;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author mrieser
 */
public class TransitQueueSimulationTest {

    /**
     * Ensure that for each departure an agent is created and departs
     */
    @Test
    public void testCreateAgents() {
        // setup: config
        final Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);
        config.qsim().setEndTime(8.0*3600);
        
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

        // setup: network
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
        Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
        Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
        setDefaultLinkAttributes(link1);
        Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
        setDefaultLinkAttributes(link2);
        network.addLink(link1);
        network.addLink(link2);

        // setup: vehicles
        Vehicles vehicles = scenario.getTransitVehicles();
        VehiclesFactory vb = vehicles.getFactory();
        VehicleType vehicleType = vb.createVehicleType(Id.create("transitVehicleType", VehicleType.class));
        VehicleCapacity capacity = vb.createVehicleCapacity();
        capacity.setSeats(Integer.valueOf(101));
        capacity.setStandingRoom(Integer.valueOf(0));
        vehicleType.setCapacity(capacity);
        
        vehicles.addVehicleType(vehicleType);
        
        vehicles.addVehicle(vb.createVehicle(Id.create("veh1", Vehicle.class), vehicleType));
        vehicles.addVehicle(vb.createVehicle(Id.create("veh2", Vehicle.class), vehicleType));
        vehicles.addVehicle(vb.createVehicle(Id.create("veh3", Vehicle.class), vehicleType));
        vehicles.addVehicle(vb.createVehicle(Id.create("veh4", Vehicle.class), vehicleType));
        vehicles.addVehicle(vb.createVehicle(Id.create("veh5", Vehicle.class), vehicleType));

        // setup: transit schedule
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory builder = schedule.getFactory();

        TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("stop1", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("stop2", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("stop3", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create("stop4", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        ArrayList<TransitRouteStop> stops = new ArrayList<>();
        stops.add(builder.createTransitRouteStop(stop1, 50, 60));
        stops.add(builder.createTransitRouteStop(stop2, 150, 160));
        stops.add(builder.createTransitRouteStop(stop3, 250, 260));
        stops.add(builder.createTransitRouteStop(stop4, 350, 360));
        schedule.addStopFacility(stop1);
        schedule.addStopFacility(stop2);
        schedule.addStopFacility(stop3);
        schedule.addStopFacility(stop4);
        stop1.setLinkId(link1.getId());
        stop2.setLinkId(link1.getId());
        stop3.setLinkId(link2.getId());
        stop4.setLinkId(link2.getId());

        NetworkRoute route = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
        ArrayList<Id<Link>> links = new ArrayList<>(0);
        route.setLinkIds(link1.getId(), links, link2.getId());

        { // line 1, 1 route, 2 departures
            TransitLine line = builder.createTransitLine(Id.create("1", TransitLine.class));
            TransitRoute tRoute = builder.createTransitRoute(Id.create(">", TransitRoute.class), route, stops, TransportMode.pt);
            Departure dep = builder.createDeparture(Id.create("dep1", Departure.class), 6.0*3600);
            dep.setVehicleId(Id.create("veh1", Vehicle.class));
            tRoute.addDeparture(dep);
            dep = builder.createDeparture(Id.create("dep2", Departure.class), 7.0*3600);
            dep.setVehicleId(Id.create("veh2", Vehicle.class));
            tRoute.addDeparture(dep);
            line.addRoute(tRoute);
            schedule.addTransitLine(line);
        }

        { // line 2, 3 routes, each 1 departure
            TransitLine line = builder.createTransitLine(Id.create("2", TransitLine.class));
            { // route 1
                TransitRoute tRoute = builder.createTransitRoute(Id.create("A", TransitRoute.class), route, stops, TransportMode.pt);
                Departure dep = builder.createDeparture(Id.create("dep3", Departure.class), 8.0*3600);
                dep.setVehicleId(Id.create("veh3", Vehicle.class));
                tRoute.addDeparture(dep);
                line.addRoute(tRoute);
            }
            { // route 2
                TransitRoute tRoute = builder.createTransitRoute(Id.create("B", TransitRoute.class), route, stops, TransportMode.pt);
                Departure dep = builder.createDeparture(Id.create("dep4", Departure.class), 8.5*3600);
                dep.setVehicleId(Id.create("veh4", Vehicle.class));
                tRoute.addDeparture(dep);
                line.addRoute(tRoute);
            }
            { // route 3
                TransitRoute tRoute = builder.createTransitRoute(Id.create("C", TransitRoute.class), route, stops, TransportMode.pt);
                Departure dep = builder.createDeparture(Id.create("dep5", Departure.class), 9.0*3600);
                dep.setVehicleId(Id.create("veh5", Vehicle.class));
                tRoute.addDeparture(dep);
                line.addRoute(tRoute);
            }
            schedule.addTransitLine(line);
        }

        scenario.getConfig().addModule( new ExternalMobimConfigGroup() );
        scenario.getConfig().qsim().setEndTime(1.0*3600); // prevent running the actual simulation

        QSim sim = QSimUtils.createDefaultQSim(scenario, EventsUtils.createEventsManager());
        sim.run();
        List<MobsimAgent> agents = new ArrayList<>(sim.getAgents());
        Collections.sort(agents, new Comparator<MobsimAgent>() {
            @Override
            public int compare(MobsimAgent mobsimAgent, MobsimAgent mobsimAgent1) {
                return Double.compare(mobsimAgent.getActivityEndTime(), mobsimAgent1.getActivityEndTime());
            }
        });
        assertEquals(5, agents.size());
        assertTrue(agents.get(0) instanceof TransitDriverAgent);
        assertEquals(6.0*3600, agents.get(0).getActivityEndTime(), MatsimTestCase.EPSILON);
        assertEquals(7.0*3600, agents.get(1).getActivityEndTime(), MatsimTestCase.EPSILON);
        assertEquals(8.0*3600, agents.get(2).getActivityEndTime(), MatsimTestCase.EPSILON);
        assertEquals(8.5*3600, agents.get(3).getActivityEndTime(), MatsimTestCase.EPSILON);
        assertEquals(9.0*3600, agents.get(4).getActivityEndTime(), MatsimTestCase.EPSILON);
    }

    /**
     * Tests that the simulation is adding an agent correctly to the transit stop
     */
    @Test
    public void testAddAgentToStop() {
        // setup: config
        final Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
        
        // setup: network
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
        Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
        network.addNode(node1);
        network.addNode(node2);
        Link link = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
        setDefaultLinkAttributes(link);
        network.addLink(link);

        // setup: transit schedule
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory builder = schedule.getFactory();
        TransitLine line = builder.createTransitLine(Id.create("1", TransitLine.class));

        TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("stop1", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        stop1.setLinkId(link.getId());
        TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("stop2", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        schedule.addStopFacility(stop1);
        schedule.addStopFacility(stop2);

        // setup: population
        Population population = scenario.getPopulation();
        PopulationFactory pb = population.getFactory();
        Person person = pb.createPerson(Id.create("1", Person.class));
        Plan plan = pb.createPlan();
        person.addPlan(plan);
        Activity homeAct = pb.createActivityFromLinkId("home", Id.create("1", Link.class));

        homeAct.setEndTime(7.0*3600 - 10.0);
        // as no transit line runs, make sure to stop the simulation manually.
        scenario.getConfig().qsim().setEndTime(7.0*3600);

        Leg leg = pb.createLeg(TransportMode.pt);
        leg.setRoute(new ExperimentalTransitRoute(stop1, line, null, stop2));
        Activity workAct = pb.createActivityFromLinkId("work", Id.create("2", Link.class));
        plan.addActivity(homeAct);
        plan.addLeg(leg);
        plan.addActivity(workAct);
        population.addPerson(person);

        // run simulation
        EventsManager events = EventsUtils.createEventsManager();
		QSim qSim1 = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine(events, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, events);
		qSim1.addMobsimEngine(teleportationEngine);
        QSim qSim = qSim1;
        AgentFactory agentFactory = new TransitAgentFactory(qSim);
        TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
        transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
        qSim.addDepartureHandler(transitEngine);
        qSim.addAgentSource(transitEngine);
        qSim.addMobsimEngine(transitEngine);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
        qSim.addAgentSource(agentSource);
        qSim.run();

        // check everything
        assertEquals(1, transitEngine.getAgentTracker().getAgentsAtStop(stop1.getId()).size());
    }

    /**
     * Tests that the simulation refuses to let an agent teleport herself by starting a transit
     * leg on a link where she isn't.
     *
     */
    @Test(expected = TransitAgentTriesToTeleportException.class)
    public void testAddAgentToStopWrongLink() {
        // setup: config
        final Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);
        
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
   
        // setup: network
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
        Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
        Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
        Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
        setDefaultLinkAttributes(link1);
        network.addLink(link1);
        setDefaultLinkAttributes(link2);
        network.addLink(link2);

        // setup: transit schedule
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory builder = schedule.getFactory();
        TransitLine line = builder.createTransitLine(Id.create("1", TransitLine.class));

        TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("stop1", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        stop1.setLinkId(link1.getId());
        TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("stop2", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        stop2.setLinkId(link2.getId());
        schedule.addStopFacility(stop1);
        schedule.addStopFacility(stop2);

        // setup: population
        Population population = scenario.getPopulation();
        PopulationFactory pb = population.getFactory();
        Person person = pb.createPerson(Id.create("1", Person.class));
        Plan plan = pb.createPlan();
        person.addPlan(plan);
        Activity homeAct = pb.createActivityFromLinkId("home", Id.create("2", Link.class));

        homeAct.setEndTime(7.0*3600 - 10.0);
        // as no transit line runs, make sure to stop the simulation manually.
        scenario.getConfig().qsim().setEndTime(7.0*3600);

        Leg leg = pb.createLeg(TransportMode.pt);
        leg.setRoute(new ExperimentalTransitRoute(stop1, line, null, stop2));
        Activity workAct = pb.createActivityFromLinkId("work", Id.create("1", Link.class));
        plan.addActivity(homeAct);
        plan.addLeg(leg);
        plan.addActivity(workAct);
        population.addPerson(person);

        // run simulation
        EventsManager events = EventsUtils.createEventsManager();
        QSim simulation = QSimUtils.createDefaultQSim(scenario, events);
        simulation.run();
    }

    /**
     * Tests that a vehicle's handleStop() method is correctly called, e.g.
     * it is re-called again when returning a delay > 0, and that is is correctly
     * called when the stop is located on the first link of the network route, on the last
     * link of the network route, or any intermediary link.
     */
    @Test
    public void testHandleStop() {
        // setup: config
        final Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);
        config.qsim().setEndTime(8.0*3600);
        
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

        // setup: network
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
        Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
        Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
        Node node4 = network.getFactory().createNode(Id.create("4", Node.class), new Coord((double) 3000, (double) 0));
        Node node5 = network.getFactory().createNode(Id.create("5", Node.class), new Coord((double) 4000, (double) 0));
        Node node6 = network.getFactory().createNode(Id.create("6", Node.class), new Coord((double) 5000, (double) 0));
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);
        network.addNode(node5);
        network.addNode(node6);
        Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
        Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
        Link link3 = network.getFactory().createLink(Id.create("3", Link.class), node3, node4);
        Link link4 = network.getFactory().createLink(Id.create("4", Link.class), node4, node5);
        Link link5 = network.getFactory().createLink(Id.create("5", Link.class), node5, node6);
        setDefaultLinkAttributes(link1);
        setDefaultLinkAttributes(link2);
        setDefaultLinkAttributes(link3);
        setDefaultLinkAttributes(link4);
        setDefaultLinkAttributes(link5);
        network.addLink(link1);
        network.addLink(link2);
        network.addLink(link3);
        network.addLink(link4);
        network.addLink(link5);

        // setup: transit schedule
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory builder = schedule.getFactory();
        TransitLine line = builder.createTransitLine(Id.create("1", TransitLine.class));
        // important: do NOT add the line to the schedule, or agents will be created twice!

        TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("stop1", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("stop2", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("stop3", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        TransitStopFacility stop4 = builder.createTransitStopFacility(Id.create("stop4", TransitStopFacility.class), new Coord((double) 0, (double) 0), false);
        ArrayList<TransitRouteStop> stops = new ArrayList<>();
        stops.add(builder.createTransitRouteStop(stop1, 50, 60));
        stops.add(builder.createTransitRouteStop(stop2, 150, 160));
        stops.add(builder.createTransitRouteStop(stop3, 250, 260));
        stops.add(builder.createTransitRouteStop(stop4, 350, 360));
        schedule.addStopFacility(stop1);
        schedule.addStopFacility(stop2);
        schedule.addStopFacility(stop3);
        schedule.addStopFacility(stop4);

        stop1.setLinkId(link1.getId()); // one stop on the first link of network route, as that one may be specially handled
        stop2.setLinkId(link3.getId()); // some stop in the middle of the network route
        stop3.setLinkId(link4.getId());
        stop4.setLinkId(link5.getId()); // one stop on the last link of the network route, as that one may be specially handled

        NetworkRoute route = new LinkNetworkRouteImpl(link1.getId(), link5.getId());
        ArrayList<Id<Link>> links = new ArrayList<>();
        Collections.addAll(links, link2.getId(), link3.getId(), link4.getId());
        route.setLinkIds(link1.getId(), links, link5.getId());

        TransitRoute tRoute = builder.createTransitRoute(Id.create(">", TransitRoute.class), route, stops, TransportMode.pt);
        Departure departure = builder.createDeparture(Id.create("dep1", Departure.class), 6.0*3600);
        tRoute.addDeparture(departure);
        line.addRoute(tRoute);

        // setup: population
        Population population = scenario.getPopulation();
        PopulationFactory pb = population.getFactory();
        Person person1 = pb.createPerson(Id.create("1", Person.class));
        Plan plan1 = pb.createPlan();
        person1.addPlan(plan1);
        Activity homeAct = pb.createActivityFromLinkId("home", Id.create("1", Link.class));
        homeAct.setEndTime(departure.getDepartureTime() - 60.0);
        Leg leg1 = pb.createLeg(TransportMode.pt);
        leg1.setRoute(new ExperimentalTransitRoute(stop1, line, tRoute, stop3));
        Activity workAct = pb.createActivityFromLinkId("work", Id.create("2", Link.class));
        plan1.addActivity(homeAct);
        plan1.addLeg(leg1);
        plan1.addActivity(workAct);
        population.addPerson(person1);

        Person person2 = pb.createPerson(Id.create("2", Person.class));
        Plan plan2 = pb.createPlan();
        person2.addPlan(plan2);
        Leg leg2 = pb.createLeg(TransportMode.pt);
        leg2.setRoute(new ExperimentalTransitRoute(stop3, line, tRoute, stop4));
        Activity homeActOnLink4 = pb.createActivityFromLinkId("home", Id.create("4", Link.class));
        homeActOnLink4.setEndTime(departure.getDepartureTime() - 60.0);
        plan2.addActivity(homeActOnLink4);
        plan2.addLeg(leg2);
        Activity workActOnLink5 = pb.createActivityFromLinkId("work", Id.create("5", Link.class));
        plan2.addActivity(workActOnLink5);
        population.addPerson(person2);


        // run simulation
        EventsManager events = EventsUtils.createEventsManager();
        TestHandleStopSimulation simulation = TestHandleStopSimulation.createTestHandleStopSimulation(scenario, events, line, tRoute, departure);
        simulation.run();

        // check everything
        List<SpyHandleStopData> spyData = simulation.driver.spyData;
        assertEquals(7, spyData.size());
        SpyHandleStopData data;

        data = spyData.get(0);
        assertEquals(stop1, data.stopFacility);
        assertTrue(data.returnedDelay > 0);
        double lastTime = data.time;
        double lastDelay = data.returnedDelay;

        data = spyData.get(1);
        assertEquals(stop1, data.stopFacility);
        assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);
        assertEquals(lastTime + lastDelay, data.time, MatsimTestCase.EPSILON);

        data = spyData.get(2);
        assertEquals(stop2, data.stopFacility);
        assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);

        data = spyData.get(3);
        assertEquals(stop3, data.stopFacility);
        assertTrue(data.returnedDelay > 0);
        lastTime = data.time;
        lastDelay = data.returnedDelay;

        data = spyData.get(4);
        assertEquals(stop3, data.stopFacility);
        assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);
        assertEquals(lastTime + lastDelay, data.time, MatsimTestCase.EPSILON);

        data = spyData.get(5);
        assertEquals(stop4, data.stopFacility);
        assertTrue(data.returnedDelay > 0);
        lastTime = data.time;
        lastDelay = data.returnedDelay;

        data = spyData.get(6);
        assertEquals(stop4, data.stopFacility);
        assertEquals(0.0, data.returnedDelay, MatsimTestCase.EPSILON);
        assertEquals(lastTime + lastDelay, data.time, MatsimTestCase.EPSILON);
    }

    private void setDefaultLinkAttributes(final Link link) {
        link.setLength(1000.0);
        link.setFreespeed(10.0);
        link.setCapacity(3600.0);
        link.setNumberOfLanes(1);
    }

    protected static class TestHandleStopSimulation {
        private SpyDriver driver = null;
        private final TransitLine line;
        private final TransitRoute route;
        private final Departure departure;
        private final QSim qSim;

        private TestHandleStopSimulation(final MutableScenario scenario, final EventsManager events, final TransitLine line, final TransitRoute route, final Departure departure) {
            this.line = line;
            this.route = route;
            this.departure = departure;
			QSim qSim2 = new QSim(scenario, events);
			ActivityEngine activityEngine = new ActivityEngine(events, qSim2.getAgentCounter());
			qSim2.addMobsimEngine(activityEngine);
			qSim2.addActivityHandler(activityEngine);
            QNetsimEngine netsimEngine = new QNetsimEngine(qSim2);
			qSim2.addMobsimEngine(netsimEngine);
			qSim2.addDepartureHandler(netsimEngine.getDepartureHandler());
			TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, events);
			qSim2.addMobsimEngine(teleportationEngine);
            QSim qSim1 = qSim2;
            AgentFactory agentFactory = new TransitAgentFactory(qSim1);
            final TransitQSimEngine transitEngine = new TransitQSimEngine(qSim1);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim1.addDepartureHandler(transitEngine);
            qSim1.addAgentSource(transitEngine);
            qSim1.addMobsimEngine(transitEngine);
            PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim1);
            qSim1.addAgentSource(agentSource);
            qSim = qSim1;
            qSim.addAgentSource(new AgentSource() {
                @Override
                public void insertAgentsIntoMobsim() {
                    TestHandleStopSimulation.this.driver = new SpyDriver(TestHandleStopSimulation.this.line, 
                    		TestHandleStopSimulation.this.route, TestHandleStopSimulation.this.departure, 
                    		transitEngine.getAgentTracker(), transitEngine);

                    VehicleType vehicleType = new VehicleTypeImpl(Id.create("transitVehicleType", VehicleType.class));
                    VehicleCapacity capacity = new VehicleCapacityImpl();
                    capacity.setSeats(101);
                    capacity.setStandingRoom(0);
                    vehicleType.setCapacity(capacity);

                    TransitQVehicle veh = new TransitQVehicle(new VehicleImpl(Id.create(TestHandleStopSimulation.this.driver.getId(), Vehicle.class), vehicleType));
                    veh.setDriver(TestHandleStopSimulation.this.driver);
                    veh.setStopHandler(new SimpleTransitStopHandler());
                    TestHandleStopSimulation.this.driver.setVehicle(veh);
                    TestHandleStopSimulation.this.departure.setVehicleId(veh.getVehicle().getId());
                    qSim.addParkedVehicle(veh, route.getRoute().getStartLinkId());
                    qSim.insertAgentIntoMobsim(TestHandleStopSimulation.this.driver); 
                }
            });

        }

        protected static TestHandleStopSimulation createTestHandleStopSimulation(final MutableScenario scenario, final EventsManager events,
                                                                                 final TransitLine line, final TransitRoute route, final Departure departure) {
            return new TestHandleStopSimulation(scenario, events, line, route, departure);
        }

        public SpyDriver getDriver() {
            return driver;
        }

        public void run() {
            qSim.run();
        }


    }

    protected static class SpyDriver extends TransitDriverAgentImpl {

        public final List<SpyHandleStopData> spyData = new ArrayList<>();

        public SpyDriver(final TransitLine line, final TransitRoute route, final Departure departure,
                         final TransitStopAgentTracker agentTracker, final TransitQSimEngine trEngine) {
            super(new SingletonUmlaufBuilderImpl(Collections.singleton(line)).build().get(0), TransportMode.car, agentTracker, trEngine.getInternalInterface());
        }

        @Override
        public double handleTransitStop(final TransitStopFacility stop, final double now) {
            double delay = super.handleTransitStop(stop, now);
            this.spyData.add(new SpyHandleStopData(stop, now, delay));
            return delay;
        }

    }

    private static class SpyHandleStopData {
        private static final Logger log = Logger.getLogger(TransitQueueSimulationTest.SpyHandleStopData.class);

        public final TransitStopFacility stopFacility;
        public final double time;
        public final double returnedDelay;

        protected SpyHandleStopData(final TransitStopFacility stopFacility, final double time, final double returnedDelay) {
            this.stopFacility = stopFacility;
            this.time = time;
            this.returnedDelay = returnedDelay;
            log.info("handle stop: " + stopFacility.getId() + " " + time + " " + returnedDelay);
        }
    }

    @Test
    public void testStartAndEndTime() {
        final Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
      
        // build simple network with 2 links
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(0.0, 0.0));
        Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000.0, 0.0));
        Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(2000.0, 0.0));
        network.addNode( node1);
        network.addNode( node2);
        network.addNode( node3);
        Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
        link1.setFreespeed(10.0);
        link1.setCapacity(2000.0);
        link1.setLength(1000.0);
        Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
        link2.setFreespeed(10.0);
        link2.setCapacity(2000.0);
        link2.setLength(1000.0);
        network.addLink(link1);
        network.addLink(link2);

        // build simple schedule with a single line
        double depTime = 7.0*3600;
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory sb = schedule.getFactory();
        TransitStopFacility stopFacility1 = sb.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 1000, (double) 0), false);
        TransitStopFacility stopFacility2 = sb.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 2000, (double) 0), false);
        schedule.addStopFacility(stopFacility1);
        schedule.addStopFacility(stopFacility2);
        stopFacility1.setLinkId(link1.getId());
        stopFacility2.setLinkId(link2.getId());
        TransitLine tLine = sb.createTransitLine(Id.create("1", TransitLine.class));
        NetworkRoute route = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
        TransitRouteStop stop1 = sb.createTransitRouteStop(stopFacility1, Time.UNDEFINED_TIME, 0.0);
        TransitRouteStop stop2 = sb.createTransitRouteStop(stopFacility2, 100.0, 100.0);
        List<TransitRouteStop> stops = new ArrayList<>(2);
        stops.add(stop1);
        stops.add(stop2);
        TransitRoute tRoute = sb.createTransitRoute(Id.create("1", TransitRoute.class), route, stops, "bus");
        Departure dep = sb.createDeparture(Id.create("1", Departure.class), depTime);
        tRoute.addDeparture(dep);
        tLine.addRoute(tRoute);
        schedule.addTransitLine(tLine);
        new CreateVehiclesForSchedule(schedule, scenario.getTransitVehicles()).run();

        // prepare test
        EventsManager events = EventsUtils.createEventsManager();
        FirstLastEventCollector collector = new FirstLastEventCollector();
        events.addHandler(collector);

        // first test without special settings
        QSim sim = QSimUtils.createDefaultQSim(scenario, events);
        sim.run();
        assertEquals(depTime, collector.firstEvent.getTime(), MatsimTestCase.EPSILON);
        assertEquals(depTime + 101.0, collector.lastEvent.getTime(), MatsimTestCase.EPSILON);
        collector.reset(0);

        // second test with special start/end times
        config.qsim().setStartTime(depTime + 20.0);
        config.qsim().setEndTime(depTime + 90.0);
        sim = QSimUtils.createDefaultQSim(scenario, events);
        sim.run();
        assertEquals(depTime + 20.0, collector.firstEvent.getTime(), MatsimTestCase.EPSILON);
        assertEquals(depTime + 90.0, collector.lastEvent.getTime(), MatsimTestCase.EPSILON);
    }

    /*package*/ final static class FirstLastEventCollector implements BasicEventHandler {
        public Event firstEvent = null;
        public Event lastEvent = null;

        @Override
        public void handleEvent(final Event event) {
            if (this.firstEvent == null) {
                this.firstEvent = event;
            }
            this.lastEvent = event;
        }

        @Override
        public void reset(final int iteration) {
            this.firstEvent = null;
            this.lastEvent = null;
        }
    }

    @Test
    public void testEvents() {
        final Config config = ConfigUtils.createConfig();
        config.transit().setUseTransit(true);

        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
     
        // build simple network with 2 links
        Network network = scenario.getNetwork();
        Node node1 = network.getFactory().createNode(Id.create("1", Node.class), new Coord(0.0, 0.0));
        Node node2 = network.getFactory().createNode(Id.create("2", Node.class), new Coord(1000.0, 0.0));
        Node node3 = network.getFactory().createNode(Id.create("3", Node.class), new Coord(2000.0, 0.0));
        network.addNode(  node1);
        network.addNode( node2);
        network.addNode(  node3 );
        Link link1 = network.getFactory().createLink(Id.create("1", Link.class), node1, node2);
        link1.setFreespeed(10.0);
        link1.setCapacity(2000.0);
        link1.setLength(1000.0);
        Link link2 = network.getFactory().createLink(Id.create("2", Link.class), node2, node3);
        link2.setFreespeed(10.0);
        link2.setCapacity(2000.0);
        link2.setLength(1000.0);
        network.addLink(link1);
        network.addLink(link2);

        // build simple schedule with a single line
        double depTime = 7.0*3600;
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory sb = schedule.getFactory();
        TransitStopFacility stopFacility1 = sb.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 1000, (double) 0), false);
        TransitStopFacility stopFacility2 = sb.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 2000, (double) 0), false);
        schedule.addStopFacility(stopFacility1);
        schedule.addStopFacility(stopFacility2);
        stopFacility1.setLinkId(link1.getId());
        stopFacility2.setLinkId(link2.getId());
        TransitLine tLine = sb.createTransitLine(Id.create("1", TransitLine.class));
        NetworkRoute route = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
        TransitRouteStop stop1 = sb.createTransitRouteStop(stopFacility1, Time.UNDEFINED_TIME, 0.0);
        TransitRouteStop stop2 = sb.createTransitRouteStop(stopFacility2, 100.0, 100.0);
        List<TransitRouteStop> stops = new ArrayList<>(2);
        stops.add(stop1);
        stops.add(stop2);
        TransitRoute tRoute = sb.createTransitRoute(Id.create("1", TransitRoute.class), route, stops, "bus");
        Departure dep = sb.createDeparture(Id.create("1", Departure.class), depTime);
        tRoute.addDeparture(dep);
        tLine.addRoute(tRoute);
        schedule.addTransitLine(tLine);
        new CreateVehiclesForSchedule(schedule, scenario.getTransitVehicles()).run();

        // build population with 1 person
        Population population = scenario.getPopulation();
        PopulationFactory pb = population.getFactory();
        Person person = pb.createPerson(Id.create("1", Person.class));
        Plan plan = pb.createPlan();
        Activity act1 = pb.createActivityFromLinkId("h", link1.getId());
        act1.setEndTime(depTime - 60.0);
        Leg leg1 = pb.createLeg(TransportMode.walk);
        Route route1 = new GenericRouteImpl(link1.getId(), link1.getId());
        route1.setTravelTime(10.0);
        route1.setDistance(10.0);
        leg1.setRoute(route1);
        Activity act2 = pb.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, link1.getId());
        act2.setEndTime(0.0);
        Leg leg2 = pb.createLeg(TransportMode.pt);
        Route route2 = new ExperimentalTransitRoute(stopFacility1, tLine, tRoute, stopFacility2);
        route2.setTravelTime(100.0);
        leg2.setRoute(route2);
        Activity act3 = pb.createActivityFromLinkId(PtConstants.TRANSIT_ACTIVITY_TYPE, link1.getId());
        act3.setEndTime(0.0);
        Leg leg3 = pb.createLeg(TransportMode.walk);
        Route route3 = new GenericRouteImpl(link2.getId(), link2.getId());
        route3.setTravelTime(10.0);
        route3.setDistance(10.0);
        leg3.setRoute(route3);
        Activity act4 = pb.createActivityFromLinkId("w", link2.getId());

        plan.addActivity(act1);
        plan.addLeg(leg1);
        plan.addActivity(act2);
        plan.addLeg(leg2);
        plan.addActivity(act3);
        plan.addLeg(leg3);
        plan.addActivity(act4);
        person.addPlan(plan);
        population.addPerson(person);

        // run sim
        EventsManager events = EventsUtils.createEventsManager();
        EventsCollector collector = new EventsCollector();
        events.addHandler(collector);
        QSimUtils.createDefaultQSim(scenario, events).run();
        List<Event> allEvents = collector.getEvents();

        for (Event event : allEvents) {
            System.out.println(event.toString());
        }


        assertEquals(30, allEvents.size());

        int idx = -1;
        assertTrue(allEvents.get(++idx) instanceof ActivityEndEvent);
        assertEquals("h", ((ActivityEndEvent) allEvents.get(0)).getActType());
        assertTrue(allEvents.get(++idx) instanceof PersonDepartureEvent);
        assertTrue(allEvents.get(++idx) instanceof TeleportationArrivalEvent);
        assertTrue(allEvents.get(++idx) instanceof PersonArrivalEvent);
        assertTrue(allEvents.get(++idx) instanceof ActivityStartEvent);
        assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityStartEvent) allEvents.get(idx)).getActType());
        assertTrue(allEvents.get(++idx) instanceof ActivityEndEvent); // zero activity duration, waiting at stop is considered as leg
        assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityEndEvent) allEvents.get(idx)).getActType());
        assertTrue(allEvents.get(++idx) instanceof PersonDepartureEvent);
        assertTrue(allEvents.get(++idx) instanceof AgentWaitingForPtEvent);
        assertTrue(allEvents.get(++idx) instanceof TransitDriverStartsEvent);
        assertTrue(allEvents.get(++idx) instanceof PersonDepartureEvent); // pt-driver
        assertTrue(allEvents.get(++idx) instanceof PersonEntersVehicleEvent); // pt-driver
        assertTrue(allEvents.get(++idx) instanceof VehicleEntersTrafficEvent); // pt-vehicle
        assertTrue(allEvents.get(++idx) instanceof VehicleArrivesAtFacilityEvent);
        assertTrue(allEvents.get(++idx) instanceof PersonEntersVehicleEvent);
        assertTrue(allEvents.get(++idx) instanceof VehicleDepartsAtFacilityEvent);
        assertTrue(allEvents.get(++idx) instanceof LinkLeaveEvent); // pt-vehicle
        assertTrue(allEvents.get(++idx) instanceof LinkEnterEvent); // pt-vehicle
        assertTrue(allEvents.get(++idx) instanceof VehicleArrivesAtFacilityEvent); // pt-vehicle
        assertTrue(allEvents.get(++idx) instanceof PersonLeavesVehicleEvent);
        assertTrue(allEvents.get(++idx) instanceof PersonArrivalEvent);
        assertTrue(allEvents.get(++idx) instanceof ActivityStartEvent);
        assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityStartEvent) allEvents.get(idx)).getActType());
        assertTrue(allEvents.get(++idx) instanceof ActivityEndEvent); // zero activity duration, waiting at stop is considered as leg
        assertEquals(PtConstants.TRANSIT_ACTIVITY_TYPE, ((ActivityEndEvent) allEvents.get(idx)).getActType());
        assertTrue(allEvents.get(++idx) instanceof PersonDepartureEvent); // walk
        assertTrue(allEvents.get(++idx) instanceof VehicleDepartsAtFacilityEvent);
        assertTrue(allEvents.get(++idx) instanceof VehicleLeavesTrafficEvent); // pt-driver
        assertTrue(allEvents.get(++idx) instanceof PersonLeavesVehicleEvent); // pt-driver
        assertTrue(allEvents.get(++idx) instanceof PersonArrivalEvent); // pt-driver
        assertTrue(allEvents.get(++idx) instanceof TeleportationArrivalEvent);
        assertTrue(allEvents.get(++idx) instanceof PersonArrivalEvent);
        assertTrue(allEvents.get(++idx) instanceof ActivityStartEvent);
        assertEquals("w", ((ActivityStartEvent) allEvents.get(idx)).getActType());
    }
}
