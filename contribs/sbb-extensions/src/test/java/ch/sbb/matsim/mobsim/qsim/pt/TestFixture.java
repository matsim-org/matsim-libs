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
package ch.sbb.matsim.mobsim.qsim.pt;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleType.DoorOperationMode;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 * Creates a test fixture with a simple TransitSchedule consisting of 1 TransitLine with 1 TransitRoute with 1 Departure and 5 Stops
 *
 * @author mrieser / SBB
 */
class TestFixture {

    Config config;
    SBBTransitConfigGroup sbbConfig;
    Scenario scenario;

    TransitStopFacility stopA;
    TransitStopFacility stopB;
    TransitStopFacility stopC;
    TransitStopFacility stopD;
    TransitStopFacility stopE;

    TransitLine line1;
    TransitRoute route1;

    TestFixture() {
        this.config = ConfigUtils.createConfig();
        this.config.transit().setUseTransit(true);
        this.config.qsim().setEndTime(24 * 3600);
        this.sbbConfig = ConfigUtils.addOrGetModule(this.config, SBBTransitConfigGroup.class);
        this.sbbConfig.setDeterministicServiceModes(Collections.singleton("train"));
        this.scenario = ScenarioUtils.createScenario(this.config);

        Network network = this.scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        Node node1 = nf.createNode(Id.create(1, Node.class), new Coord(10000, 0));
        Node node2 = nf.createNode(Id.create(2, Node.class), new Coord(15000, 0));
        Node node3 = nf.createNode(Id.create(3, Node.class), new Coord(25000, 0));
        Node node4 = nf.createNode(Id.create(4, Node.class), new Coord(35000, 0));
        Node node5 = nf.createNode(Id.create(5, Node.class), new Coord(40000, 0));

        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);
        network.addNode(node5);

        Link link1 = createLink(nf, 1, 7500, node1, node2);
        Link link2 = createLink(nf, 2, 1200, node2, node3);
        Link link3 = createLink(nf, 3, 1200, node3, node4);
        Link link4 = createLink(nf, 4, 6500, node4, node5);

        network.addLink(link1);
        network.addLink(link2);
        network.addLink(link3);
        network.addLink(link4);

        TransitSchedule schedule = this.scenario.getTransitSchedule();
        Vehicles vehicles = this.scenario.getTransitVehicles();
        TransitScheduleFactory f = schedule.getFactory();
        VehiclesFactory vf = vehicles.getFactory();

        VehicleType vehType1 = vf.createVehicleType(Id.create("some_train", VehicleType.class));
        VehicleCapacity vehCapacity = vehType1.getCapacity();
        vehCapacity.setSeats(300);
        vehCapacity.setStandingRoom(150);
        vehicles.addVehicleType(vehType1);
        VehicleUtils.setDoorOperationMode(vehType1, DoorOperationMode.serial);
        VehicleUtils.setAccessTime(vehType1, 2); // 1 person takes 2 seconds to board
        VehicleUtils.setEgressTime(vehType1, 2);
        Vehicle veh1 = vf.createVehicle(Id.create("train1", Vehicle.class), vehType1);
        vehicles.addVehicle(veh1);

        this.stopA = f.createTransitStopFacility(Id.create("A", TransitStopFacility.class), node1.getCoord(), false);
        this.stopB = f.createTransitStopFacility(Id.create("B", TransitStopFacility.class), node2.getCoord(), false);
        this.stopC = f.createTransitStopFacility(Id.create("C", TransitStopFacility.class), node3.getCoord(), false);
        this.stopD = f.createTransitStopFacility(Id.create("D", TransitStopFacility.class), node4.getCoord(), false);
        this.stopE = f.createTransitStopFacility(Id.create("E", TransitStopFacility.class), node5.getCoord(), false);

        this.stopA.setLinkId(link1.getId());
        this.stopB.setLinkId(link1.getId());
        this.stopC.setLinkId(link2.getId());
        this.stopD.setLinkId(link3.getId());
        this.stopE.setLinkId(link4.getId());

        schedule.addStopFacility(this.stopA);
        schedule.addStopFacility(this.stopB);
        schedule.addStopFacility(this.stopC);
        schedule.addStopFacility(this.stopD);
        schedule.addStopFacility(this.stopE);

        this.line1 = f.createTransitLine(Id.create("1", TransitLine.class));

        List<Id<Link>> linkIdList = new ArrayList<>();
        linkIdList.add(link2.getId());
        linkIdList.add(link3.getId());
        NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), linkIdList, link4.getId());

        List<TransitRouteStop> stops = new ArrayList<>(5);
        stops.add(f.createTransitRouteStopBuilder(this.stopA).departureOffset(0.0).build());
        stops.add(f.createTransitRouteStop(this.stopB, 100, 120.0));
        stops.add(f.createTransitRouteStopBuilder(this.stopC).departureOffset(300.).build());
        stops.add(f.createTransitRouteStop(this.stopD, 570, 600.0));
        stops.add(f.createTransitRouteStopBuilder(this.stopE).arrivalOffset(720.).build());

        this.route1 = f.createTransitRoute(Id.create("A2E", TransitRoute.class), networkRoute, stops, "train");

        Departure departure1 = f.createDeparture(Id.create(1, Departure.class), 30000.0);
        departure1.setVehicleId(veh1.getId());
        this.route1.addDeparture(departure1);

        this.line1.addRoute(this.route1);
        schedule.addTransitLine(this.line1);
    }

    private Link createLink(NetworkFactory nf, int id, double length, Node fromNode, Node toNode) {
        Link link = nf.createLink(Id.create(id, Link.class), fromNode, toNode);
        link.setAllowedModes(Collections.singleton("train"));
        link.setLength(length);
        link.setFreespeed(33.3);
        link.setCapacity(1000);
        link.setNumberOfLanes(1);
        return link;
    }

    void addSingleTransitDemand() {
        Population population = this.scenario.getPopulation();
        PopulationFactory pf = population.getFactory();
        Person person = pf.createPerson(Id.create(1, Person.class));
        Plan plan = pf.createPlan();
        Activity act1 = pf.createActivityFromLinkId("home", Id.create(1, Link.class));
        act1.setEndTime(29500);
        Leg leg = pf.createLeg("pt");
        Route route = new DefaultTransitPassengerRoute(this.stopB, this.line1, this.route1, this.stopD);
        leg.setRoute(route);
        Activity act2 = pf.createActivityFromLinkId("work", Id.create(3, Link.class));

        plan.addActivity(act1);
        plan.addLeg(leg);
        plan.addActivity(act2);
        person.addPlan(plan);
        population.addPerson(person);
    }

    void addTripleTransitDemand() {
        Population population = this.scenario.getPopulation();
        PopulationFactory pf = population.getFactory();
        for (int i = 1; i <= 3; i++) {
            Person person = pf.createPerson(Id.create(i, Person.class));
            Plan plan = pf.createPlan();
            Activity act1 = pf.createActivityFromLinkId("home", Id.create(1, Link.class));
            act1.setEndTime(29500);
            Leg leg = pf.createLeg("pt");
            Route route = new DefaultTransitPassengerRoute(this.stopB, this.line1, this.route1, this.stopD);
            leg.setRoute(route);
            Activity act2 = pf.createActivityFromLinkId("work", Id.create(3, Link.class));

            plan.addActivity(act1);
            plan.addLeg(leg);
            plan.addActivity(act2);
            person.addPlan(plan);
            population.addPerson(person);
        }
    }

    void addSesselbahn(boolean removeExistingLines, boolean malformed) {
        if (removeExistingLines) {
            removeExistingLines();
        }

        TransitSchedule schedule = this.scenario.getTransitSchedule();
        TransitScheduleFactory f = schedule.getFactory();

        TransitLine line = f.createTransitLine(Id.create("SB", TransitLine.class));
        NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(this.stopB.getLinkId(), Collections.emptyList(), this.stopC.getLinkId());
        List<TransitRouteStop> stops = new ArrayList<>();
        stops.add(f.createTransitRouteStopBuilder(this.stopB).departureOffset(0).build());
        stops.add(f.createTransitRouteStopBuilder(this.stopC).arrivalOffset(malformed ? 0 : 120).build());
        TransitRoute route = f.createTransitRoute(Id.create("SB1", TransitRoute.class), netRoute, stops, "train");
        Departure dep1 = f.createDeparture(Id.create("SB1_1", Departure.class), 35000);
        dep1.setVehicleId(Id.create("train1", Vehicle.class));
        route.addDeparture(dep1);
        line.addRoute(route);
        schedule.addTransitLine(line);
    }

    void addLoopyRoute(boolean removeExistingLines) {
        if (removeExistingLines) {
            removeExistingLines();
        }

        Network n = this.scenario.getNetwork();
        NetworkFactory nf = n.getFactory();

        Node node2 = n.getNodes().get(Id.create(2, Node.class));
        Node node3 = n.getNodes().get(Id.create(3, Node.class));

        Link link1 = n.getLinks().get(Id.create(1, Link.class));
        Link link2 = n.getLinks().get(Id.create(2, Link.class));
        Link link3 = n.getLinks().get(Id.create(3, Link.class));

        Link loopLink2 = createLink(nf, -2, 0, node2, node2);
        Link loopLink3 = createLink(nf, -3, 0, node3, node3);
        n.addLink(loopLink2);
        n.addLink(loopLink3);

        TransitSchedule schedule = this.scenario.getTransitSchedule();
        TransitScheduleFactory f = schedule.getFactory();

        TransitStopFacility stopBLoop = f.createTransitStopFacility(Id.create("BLoop", TransitStopFacility.class), node2.getCoord(), false);
        TransitStopFacility stopCLoop = f.createTransitStopFacility(Id.create("CLoop", TransitStopFacility.class), node3.getCoord(), false);

        stopBLoop.setLinkId(loopLink2.getId());
        stopCLoop.setLinkId(loopLink3.getId());

        schedule.addStopFacility(stopBLoop);
        schedule.addStopFacility(stopCLoop);

        TransitLine line = f.createTransitLine(Id.create("Loopy", TransitLine.class));

        List<Id<Link>> linkIdList = new ArrayList<>();
        linkIdList.add(loopLink2.getId());
        linkIdList.add(link2.getId());
        linkIdList.add(loopLink3.getId());
        NetworkRoute networkRoute = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), linkIdList, link3.getId());

        List<TransitRouteStop> stops = new ArrayList<>(5);
        stops.add(f.createTransitRouteStopBuilder(this.stopA).departureOffset(0.0).build());
        stops.add(f.createTransitRouteStop(stopBLoop, 100, 120.0));
        stops.add(f.createTransitRouteStopBuilder(stopCLoop).departureOffset(300.).build());
        stops.add(f.createTransitRouteStopBuilder(this.stopD).arrivalOffset(570.).build());

        TransitRoute route = f.createTransitRoute(Id.create("A2D", TransitRoute.class), networkRoute, stops, "train");

        Vehicle veh1 = this.scenario.getTransitVehicles().getVehicles().get(Id.create("train1", Vehicle.class));

        Departure departure1 = f.createDeparture(Id.create(1, Departure.class), 30000.0);
        departure1.setVehicleId(veh1.getId());
        route.addDeparture(departure1);

        line.addRoute(route);
        schedule.addTransitLine(line);
    }

    private void removeExistingLines() {
        TransitSchedule schedule = this.scenario.getTransitSchedule();
        List<TransitLine> lines = new ArrayList<>(schedule.getTransitLines().values());
        for (TransitLine line : lines) {
            schedule.removeTransitLine(line);
        }
    }

    void delayDepartureAtFirstStop() {
        this.line1.removeRoute(this.route1);

        Id<TransitRoute> routeId = this.route1.getId();
        NetworkRoute netRoute = this.route1.getRoute();
        List<TransitRouteStop> stops = new ArrayList<>(this.route1.getStops());
        String mode = this.route1.getTransportMode();
        Collection<Departure> departures = this.route1.getDepartures().values();

        TransitScheduleFactory f = this.scenario.getTransitSchedule().getFactory();

        TransitRouteStop oldStop = stops.get(0);
        TransitRouteStop stop = f.createTransitRouteStopBuilder(oldStop.getStopFacility()).departureOffset(20.0).build();
        stops.set(0, stop);

        this.route1 = f.createTransitRoute(routeId, netRoute, stops, mode);
        for (Departure dep : departures) {
            this.route1.addDeparture(dep);
        }
        this.line1.addRoute(this.route1);
    }
}
