/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.analysis.pt.stop2stop;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.events.TransitDriverStartsEventTest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PtStop2StopAnalysisTest {

    /**
     * TODO:
     * 1 test one line, one route, one departure, 3 stops (almost done)
     *
     * another test: complex: multiple lines (of different modes), events in chronological order but lines/routes/departures mixed up for at least 2 routes (not necessarily everything mixed up),
     * at least one line with multiple routes,
     * at least one route with multiple departures,
     * at least one route serving the same stop multiple times (identify using stopSequence),
     * at least one passenger using multiple lines one after the other,
     * at least one route with multiple links between two stops
     *
     * -> test stichprobenartig, nicht unbedingt an allen Halten fuer alle Linien,
     * am besten (nicht unbedingt fuer alle Linien, aber für mindestens eine) 0. Haltestelle, eine mittlere Haltestelle und die letzte Haltestelle
     * jede Linie / Route / usw. min. 2-3 Haltestellen
     *
     * vielleicht im simpleTest nur eine TransitLine, eine TransitRoute, eine Departure aber mit allen gemeinheiten (gleicher stop mehrfach bedient, passagier steigt ein, wieder aus, wieder ein und nochmal wieder aus)
     *
     * Dann diesen Test kopieren und die weiteren Lines/Routes/departures als Stoerfeuer zwischen die Events werfen und die gleichen Asserts nur fuer die eine besondere Linie am Ende + Asserts fuer eine oder zwei Haltestellen der train Line (-> andere vehicle capacity)
     *
     * helpful give expressive variable names
     *
     * Test method for {@link PtStop2StopAnalysis}.
     */
    @Test
    public void testPtStop2StopAnalysis() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

        VehicleType busVehicleType = vehicleFactory.createVehicleType( Id.create( "Bus_veh_type", VehicleType.class ) );
        {
            VehicleCapacity capacity = busVehicleType.getCapacity() ;
            capacity.setSeats( 50 );
            capacity.setStandingRoom( 100 );
            VehicleUtils.setDoorOperationMode(busVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
            VehicleUtils.setAccessTime(busVehicleType, 1.0 / 3.0); // 1s per boarding agent, distributed on 3 doors
            VehicleUtils.setEgressTime(busVehicleType, 1.0 / 3.0); // 1s per alighting agent, distributed on 3 doors
            scenario.getTransitVehicles().addVehicleType( busVehicleType );
        }

        VehicleType trainVehicleType = vehicleFactory.createVehicleType( Id.create( "Bus_veh_type", VehicleType.class ) );
        {
            VehicleCapacity capacity = trainVehicleType.getCapacity() ;
            capacity.setSeats( 500 );
            capacity.setStandingRoom( 500 );
            VehicleUtils.setDoorOperationMode(trainVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
            VehicleUtils.setAccessTime(trainVehicleType, 1.0 / 20.0); // 1s per boarding agent, distributed on 20 doors
            VehicleUtils.setEgressTime(trainVehicleType, 1.0 / 20.0); // 1s per alighting agent, distributed on 20 doors
            scenario.getTransitVehicles().addVehicleType( trainVehicleType );
        }

        Vehicle veh_bus1 = vehicleFactory.createVehicle(Id.create("pt_bus1", Vehicle.class), busVehicleType);
        Vehicle veh_bus_line1_route1_dep2 = vehicleFactory.createVehicle(Id.create("pt_bus2", Vehicle.class), busVehicleType);
        Vehicle veh_bus_line1_route2_dep1 = vehicleFactory.createVehicle(Id.create("pt_bus2", Vehicle.class), busVehicleType);
        Vehicle veh_bus_line2_route1_dep1 = vehicleFactory.createVehicle(Id.create("pt_bus2", Vehicle.class), busVehicleType);
        Vehicle veh_train1 = vehicleFactory.createVehicle(Id.create("pt_train1", Vehicle.class), trainVehicleType);
        scenario.getTransitVehicles().addVehicle(veh_bus1);
        scenario.getTransitVehicles().addVehicle(veh_bus_line1_route1_dep2);
        scenario.getTransitVehicles().addVehicle(veh_train1);
        Id<Person> driverId_bus1 = Id.createPersonId("driver_bus1");
        Id<Person> driverId_bus2 = Id.createPersonId("driver_bus2");
        Id<Person> driverId_train1 = Id.createPersonId("driver_train1");
        Id<TransitLine> transitLineId_bus1 = Id.create("bus_1", TransitLine.class);
        Id<TransitRoute> transitRouteId_bus1_route1 = Id.create("bus1_route1", TransitRoute.class);
        Id<Departure> departureId_bus1_route1_dep1 = Id.create("bus1_route1_dep1", Departure.class);
        // ...

        Id<TransitStopFacility> transitStopFacilityId1 = Id.create("stop1", TransitStopFacility.class);
        Id<TransitStopFacility> transitStopFacilityId2 = Id.create("stop2", TransitStopFacility.class);
        Id<TransitStopFacility> transitStopFacilityId3 = Id.create("stop3", TransitStopFacility.class);

        Id<Link> linkId1 = Id.createLinkId("link1");
        Id<Link> linkId2 = Id.createLinkId("link2");
        Id<Link> linkId3 = Id.createLinkId("link3");

        Id<Person> passenger1 = Id.createPersonId("passenger1");
        Id<Person> passenger2 = Id.createPersonId("passenger2");
        Id<Person> passenger3 = Id.createPersonId("passenger3");

        String networkMode_bus = TransportMode.car;

        PtStop2StopAnalysis ptStop2StopAnalysis = new PtStop2StopAnalysis(scenario.getTransitVehicles());

        ParallelEventsManager events = new ParallelEventsManager(false);
        events.addHandler(ptStop2StopAnalysis);

// for each Departure one TransitDriverStartsEvent
        events.processEvent(new TransitDriverStartsEvent(1.0, driverId_bus1, veh_bus1.getId(),
                transitLineId_bus1, transitRouteId_bus1_route1, departureId_bus1_route1_dep1));
        events.processEvent(new PersonDepartureEvent(1.0, driverId_bus1, linkId1, networkMode_bus));
        events.processEvent(new PersonEntersVehicleEvent(1.0, driverId_bus1, veh_bus1.getId()));
        events.processEvent(new VehicleEntersTrafficEvent(1.0, driverId_bus1, linkId1, veh_bus1.getId(), networkMode_bus, 1.0));
        events.processEvent(new VehicleArrivesAtFacilityEvent(1.0, veh_bus1.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new PersonEntersVehicleEvent(2.0, passenger1, veh_bus1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(2.0, passenger2, veh_bus1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(3.0, veh_bus1.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new LinkLeaveEvent(4.0, veh_bus1.getId(), linkId1));
        events.processEvent(new LinkEnterEvent(5.0, veh_bus1.getId(), linkId2));
        events.processEvent(new VehicleArrivesAtFacilityEvent(6.0, veh_bus1.getId(), transitStopFacilityId2, -1.0));
        events.processEvent(new PersonLeavesVehicleEvent(7.0, passenger1, veh_bus1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(7.0, passenger3, veh_bus1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(8.0, veh_bus1.getId(), transitStopFacilityId2, 1.0));
        events.processEvent(new LinkLeaveEvent(9.0, veh_bus1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(10.0, veh_bus1.getId(), linkId3));
        events.processEvent(new VehicleArrivesAtFacilityEvent(11.0, veh_bus1.getId(), transitStopFacilityId3, -1.0));
        events.processEvent(new PersonLeavesVehicleEvent(12.0, passenger2, veh_bus1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(12.0, passenger3, veh_bus1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(13.0, veh_bus1.getId(), transitStopFacilityId3, 1.0));
        events.processEvent(new VehicleLeavesTrafficEvent(14.0, driverId_bus1, linkId3, veh_bus1.getId(), networkMode_bus, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(15.0, driverId_bus1, veh_bus1.getId()));
        events.processEvent(new PersonArrivalEvent(15.0, driverId_bus1, linkId3, networkMode_bus));




        // das oben als einfachen test, dann weiteren Test mit mehreren Linien, TranistRoutes und Departures darunter separat als weiteren Test

        // fuer weitere Busse, zeitlich durchmischt. Mal ein Event fuer bus 1, dann eins fuer bus 2, dann wieder ein event an bus 1.
        // Events immer chronologisch, auch die Reihenfolge der Event Typen muss gleich bleiben, z.B. TransitDriverStartsEvent, dann PersonDepartureEvent, dann PersonEntersVehicleEvent, dann VehicleEntersTrafficEvent



        // Tests
        List<PtStop2StopAnalysis.Stop2StopEntry> line1_route1_dep1_stop1 = ptStop2StopAnalysis.getStop2StopEntriesByDeparture().stream().filter(entry -> entry.transitLineId.equals(transitLineId_bus1) && entry.transitRouteId.equals(transitRouteId_bus1_route1) && entry.departureId.equals(departureId_bus1_route1_dep1) && entry.stopId.equals(transitStopFacilityId1) && entry.stopSequence == 0).collect(Collectors.toList());
        Assert.assertEquals("Either no entry or more than entry for " + transitLineId_bus1 + ", " + transitRouteId_bus1_route1 + ", " + departureId_bus1_route1_dep1 + ", " + departureId_bus1_route1_dep1 + ", " + transitStopFacilityId1 + ", 0",1, line1_route1_dep1_stop1.size());
        Assert.assertNull("There should be no previous stop, but there was", line1_route1_dep1_stop1.get(0).stopPreviousId);
        Assert.assertEquals("Wrong arrivalTimeScheduled", 1.0, line1_route1_dep1_stop1.get(0).arrivalTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals("Wrong arrivalDelay", 0.0, line1_route1_dep1_stop1.get(0).arrivalDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(3.0, line1_route1_dep1_stop1.get(0).departureTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, line1_route1_dep1_stop1.get(0).departureDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, line1_route1_dep1_stop1.get(0).passengersAtArrival, MatsimTestUtils.EPSILON);
        Assert.assertEquals(veh_bus1.getType().getCapacity().getSeats() + veh_bus1.getType().getCapacity().getStandingRoom(), line1_route1_dep1_stop1.get(0).totalVehicleCapacity, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, line1_route1_dep1_stop1.get(0).passengersAlighting, MatsimTestUtils.EPSILON);
        Assert.assertEquals(2.0, line1_route1_dep1_stop1.get(0).passengersBoarding, MatsimTestUtils.EPSILON);
        List<Id<Link>> linkList = new ArrayList<>();
        linkList.add(linkId1);
        Assert.assertEquals("Wrong links", linkList, line1_route1_dep1_stop1.get(0).linkIdsSincePreviousStop);
    }

}