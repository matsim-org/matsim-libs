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

import gnu.trove.impl.sync.TSynchronizedShortObjectMap;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

import javax.measure.spi.SystemOfUnits;
import java.util.ArrayList;
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
    public void testPtStop2StopAnalysisSingle() {
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

        VehicleType trainVehicleType = vehicleFactory.createVehicleType( Id.create( "Train_veh_type", VehicleType.class ) );
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
        events.initProcessing();
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
//        events.processEvent(new PersonEntersVehicleEvent(9.0, passenger3, veh_bus1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(8.0, veh_bus1.getId(), transitStopFacilityId2, 1.0));
//        events.processEvent(new PersonEntersVehicleEvent(7.0, passenger3, veh_bus1.getId()));
        events.processEvent(new LinkLeaveEvent(9.0, veh_bus1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(10.0, veh_bus1.getId(), linkId3));
        events.processEvent(new VehicleArrivesAtFacilityEvent(11.0, veh_bus1.getId(), transitStopFacilityId3, -1.0));
        events.processEvent(new PersonLeavesVehicleEvent(12.0, passenger2, veh_bus1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(12.0, passenger3, veh_bus1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(13.0, veh_bus1.getId(), transitStopFacilityId3, 1.0));
        events.processEvent(new VehicleLeavesTrafficEvent(14.0, driverId_bus1, linkId3, veh_bus1.getId(), networkMode_bus, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(15.0, driverId_bus1, veh_bus1.getId()));
        events.processEvent(new PersonArrivalEvent(15.0, driverId_bus1, linkId3, networkMode_bus));
        events.finishProcessing();



        // das oben als einfachen test, dann weiteren Test mit mehreren Linien, TranistRoutes und Departures darunter separat als weiteren Test

        // fuer weitere Busse, zeitlich durchmischt. Mal ein Event fuer bus 1, dann eins fuer bus 2, dann wieder ein event an bus 1.
        // Events immer chronologisch, auch die Reihenfolge der Event Typen muss gleich bleiben, z.B. TransitDriverStartsEvent, dann PersonDepartureEvent, dann PersonEntersVehicleEvent, dann VehicleEntersTrafficEvent



        // Tests
        List<PtStop2StopAnalysis.Stop2StopEntry> line1_route1_dep1_stop1 = ptStop2StopAnalysis.getStop2StopEntriesByDeparture().stream()
                .filter(entry -> entry.transitLineId.equals(transitLineId_bus1) && entry.transitRouteId.equals(transitRouteId_bus1_route1)
                        && entry.departureId.equals(departureId_bus1_route1_dep1) && entry.stopId.equals(transitStopFacilityId1)
                        && entry.stopSequence == 0).collect(Collectors.toList());
        System.out.println(line1_route1_dep1_stop1.size());
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

        List<PtStop2StopAnalysis.Stop2StopEntry> line1_route1_dep1_stop3 = ptStop2StopAnalysis.getStop2StopEntriesByDeparture().stream()
                .filter(entry -> entry.transitLineId.equals(transitLineId_bus1) && entry.transitRouteId.equals(transitRouteId_bus1_route1)
                        && entry.departureId.equals(departureId_bus1_route1_dep1) && entry.stopId.equals(transitStopFacilityId3)
                        && entry.stopSequence == 2).collect(Collectors.toList());
        Assert.assertEquals("Either no entry or more than entry for " + transitLineId_bus1 + ", " + transitRouteId_bus1_route1 + ", " + departureId_bus1_route1_dep1 + ", " + departureId_bus1_route1_dep1 + ", " + transitStopFacilityId3 + ", 0",1, line1_route1_dep1_stop3.size());
        Assert.assertEquals("There is no previous stop", transitStopFacilityId2, line1_route1_dep1_stop3.get(0).stopPreviousId);
        Assert.assertEquals(12.0, line1_route1_dep1_stop3.get(0).departureTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals("Wrong arrivalTimeScheduled", 12.0, line1_route1_dep1_stop3.get(0).arrivalTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals("Wrong arrival delay", -1.0, line1_route1_dep1_stop3.get(0).arrivalDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals("Wrong departure delay", 1.0, line1_route1_dep1_stop3.get(0).departureDelay, MatsimTestUtils.EPSILON);
//        Assert.assertEquals("Wrong amount of passenger alighting", 1.0, line1_route1_dep1_stop3.get(0).passengersAlighting, MatsimTestUtils.EPSILON);
//        Assert.assertEquals("Wrong amount of passenger at Arrival", 1.0, line1_route1_dep1_stop3.get(0).passengersAtArrival, MatsimTestUtils.EPSILON);

        //   Assert.assertEquals("Either no entry or more than entry for " + transitLineId_bus1 + ", " + transitRouteId_bus1_route1 + ", " + departureId_bus1_route1_dep1 + ", " + departureId_bus1_route1_dep1 + ", " + transitStopFacilityId1 + ", 0",1, line1_route1_dep1_stop1.size());
     //   Assert.assertEquals("There is no previous stop", "stop2", line1_route1_dep1_stop3.get(2).stopPreviousId);

    }

/*    private void LinkEnterStopLeave(Vehicle veh_bus, Id<TransitStopFacility> transitStopFacilityId, Id<Link> linkId, Id<Person> passengerLeave, Id<Person> passengerEnter, ParallelEventsManager events) {
        events.processEvent(new LinkEnterEvent(5.0, veh_bus.getId(), linkId));
        events.processEvent(new VehicleArrivesAtFacilityEvent(6.0, veh_bus.getId(), transitStopFacilityId, -1.0));
        events.processEvent(new PersonLeavesVehicleEvent(7.0, passengerLeave, veh_bus.getId()));
        events.processEvent(new PersonEntersVehicleEvent(7.0, passengerEnter, veh_bus.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(8.0, veh_bus.getId(), transitStopFacilityId, 1.0));
        events.processEvent(new LinkLeaveEvent(9.0, veh_bus.getId(), linkId));
    }*/


//    -------------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    public void testPtStop2StopAnalysisMulti() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

        VehicleType busVehicleType = vehicleFactory.createVehicleType( Id.create( "Bus_veh_type", VehicleType.class ) );
        {
            VehicleCapacity capacity = busVehicleType.getCapacity() ;
            capacity.setSeats( 10 );
            capacity.setStandingRoom( 5 );
            VehicleUtils.setDoorOperationMode(busVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
            VehicleUtils.setAccessTime(busVehicleType, 1.0 / 3.0); // 1s per boarding agent, distributed on 3 doors
            VehicleUtils.setEgressTime(busVehicleType, 1.0 / 3.0); // 1s per alighting agent, distributed on 3 doors
            scenario.getTransitVehicles().addVehicleType( busVehicleType );
        }

        VehicleType trainVehicleType = vehicleFactory.createVehicleType( Id.create( "Train_veh_type", VehicleType.class ) );
        {
            VehicleCapacity capacity = trainVehicleType.getCapacity() ;
            capacity.setSeats( 500 );
            capacity.setStandingRoom( 500 );
            VehicleUtils.setDoorOperationMode(trainVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
            VehicleUtils.setAccessTime(trainVehicleType, 1.0 / 20.0); // 1s per boarding agent, distributed on 20 doors
            VehicleUtils.setEgressTime(trainVehicleType, 1.0 / 20.0); // 1s per alighting agent, distributed on 20 doors
            scenario.getTransitVehicles().addVehicleType( trainVehicleType );
        }

        Vehicle veh_bus1_dep1 = vehicleFactory.createVehicle(Id.create("pt_bus_1", Vehicle.class), busVehicleType);
        Vehicle veh_bus1_dep2 = vehicleFactory.createVehicle(Id.create("pt_bus_2", Vehicle.class), busVehicleType);
/*        Vehicle veh_bus1_line1_route2_dep1 = vehicleFactory.createVehicle(Id.create("pt_bus_4321", Vehicle.class), busVehicleType);
        Vehicle veh_bus2_line2_route1_dep1 = vehicleFactory.createVehicle(Id.create("pt_bus_156734", Vehicle.class), busVehicleType);
        Vehicle veh_bus3_line3_route1_dep1 = vehicleFactory.createVehicle(Id.create("pt_bus_1894", Vehicle.class), busVehicleType);*/
        Vehicle veh_train1_dep1 = vehicleFactory.createVehicle(Id.create("pt_train_1", Vehicle.class), trainVehicleType);
        scenario.getTransitVehicles().addVehicle(veh_bus1_dep1);
        scenario.getTransitVehicles().addVehicle(veh_bus1_dep2);
        scenario.getTransitVehicles().addVehicle(veh_train1_dep1);
//        scenario.getTransitVehicles().addVehicle(veh_bus2_line2_route1_dep1);
//        scenario.getTransitVehicles().addVehicle(veh_bus3_line3_route1_dep1);
        scenario.getTransitVehicles().addVehicle(veh_train1_dep1);
        Id<Person> driverId_bus1 = Id.createPersonId("driver_bus1");
        Id<Person> driverId_bus2 = Id.createPersonId("driver_bus2");
        Id<Person> driverId_3 = Id.createPersonId("driver_3");
        Id<Person> driverId_4 = Id.createPersonId("driver_4");
        Id<Person> driverId_train1 = Id.createPersonId("driver_train1");

        Id<TransitLine> transitLineId_bus1 = Id.create("bus_1", TransitLine.class);
        Id<TransitLine> transitLineId_train1 = Id.create("train_1", TransitLine.class);
        Id<TransitLine> transitLineId_bus2 = Id.create("bus_2", TransitLine.class);
        Id<TransitLine> transitLineId_bus3 = Id.create("bus_3", TransitLine.class);

        Id<TransitRoute> transitRouteId_bus1_route1 = Id.create("bus1_route1", TransitRoute.class);
        Id<TransitRoute> transitRouteId_train1_route1 = Id.create("train1_route1", TransitRoute.class);
        Id<TransitRoute> transitRouteId_bus1_route2 = Id.create("bus1_route2", TransitRoute.class);
        Id<TransitRoute> transitRouteId_bus2_route1 = Id.create("bus2_route1", TransitRoute.class);
        Id<TransitRoute> transitRouteId_bus3_route1 = Id.create("bus3_route1", TransitRoute.class);

        Id<Departure> departureId_bus1_dep1 = Id.create("bus1_dep1", Departure.class);
        Id<Departure> departureId_bus1_dep2 = Id.create("bus1_dep2", Departure.class);
        Id<Departure> departureId_train1_dep1 = Id.create("train1_dep1", Departure.class);
        Id<Departure> departureId_bus2_route1_dep1 = Id.create("bus2_route1_dep1", Departure.class);
        Id<Departure> departureId_bus3_route1_dep1 = Id.create("bus3_route1_dep1", Departure.class);

        Id<TransitStopFacility> transitStopFacilityId1 = Id.create("stop1", TransitStopFacility.class);
        Id<TransitStopFacility> transitStopFacilityId2 = Id.create("stop2", TransitStopFacility.class);
        Id<TransitStopFacility> transitStopFacilityId3 = Id.create("stop3", TransitStopFacility.class);
        Id<TransitStopFacility> transitStopFacilityId4 = Id.create("stop4", TransitStopFacility.class);
        Id<TransitStopFacility> transitStopFacilityId6 = Id.create("stop6", TransitStopFacility.class);


        Id<Link> linkId1 = Id.createLinkId("link1");
        Id<Link> linkId2 = Id.createLinkId("link2");
        Id<Link> linkId3 = Id.createLinkId("link3");
        Id<Link> linkId4 = Id.createLinkId("link4");
        Id<Link> linkId5 = Id.createLinkId("link5");
        Id<Link> linkId6 = Id.createLinkId("link6");
        Id<Link> linkId7 = Id.createLinkId("link7");
//        Id<Link> linkId8 = Id.createLinkId("link8");
//        Id<Link> linkId9 = Id.createLinkId("link9");



        Id<Person> passenger1B = Id.createPersonId("passenger1B");
        Id<Person> passenger2B = Id.createPersonId("passenger2B");
        Id<Person> passenger3B = Id.createPersonId("passenger3B");
        Id<Person> passenger4B = Id.createPersonId("passenger4B");
        Id<Person> passenger5B = Id.createPersonId("passenger5B");
        Id<Person> passenger6B = Id.createPersonId("passenger6B");
        Id<Person> passenger7B = Id.createPersonId("passenger7B");
        Id<Person> passenger8B = Id.createPersonId("passenger8B");

        Id<Person> passenger1T = Id.createPersonId("passenger1T");
        Id<Person> passenger2T = Id.createPersonId("passenger2T");
        Id<Person> passenger3T = Id.createPersonId("passenger3T");
        Id<Person> passenger4T = Id.createPersonId("passenger4T");
        Id<Person> passenger5T = Id.createPersonId("passenger5T");
        Id<Person> passenger6T = Id.createPersonId("passenger6T");
        Id<Person> passenger7T = Id.createPersonId("passenger7T");
        Id<Person> passenger8T = Id.createPersonId("passenger8T");
        Id<Person> passenger9T = Id.createPersonId("passenger9T");
        Id<Person> passenger10T = Id.createPersonId("passenger10T");

        String networkMode_bus = TransportMode.car;
        String networkMode_train = TransportMode.train;

        PtStop2StopAnalysis ptStop2StopAnalysis = new PtStop2StopAnalysis(scenario.getTransitVehicles());

        ParallelEventsManager events = new ParallelEventsManager(false);
        events.addHandler(ptStop2StopAnalysis);

// for each Departure one TransitDriverStartsEvent
        events.initProcessing();
        events.processEvent(new TransitDriverStartsEvent(1.0, driverId_bus1, veh_bus1_dep1.getId(),
                transitLineId_bus1, transitRouteId_bus1_route1, departureId_bus1_dep1));
        events.processEvent(new TransitDriverStartsEvent(1.0, driverId_train1, veh_train1_dep1.getId(),
                transitLineId_train1, transitRouteId_train1_route1, departureId_train1_dep1));

        events.processEvent(new PersonDepartureEvent(1.0, driverId_bus1, linkId1, networkMode_bus));
        events.processEvent(new PersonDepartureEvent(1.0, driverId_train1, linkId7, networkMode_train));

        events.processEvent(new PersonEntersVehicleEvent(1.0, driverId_bus1, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(1.0, driverId_train1, veh_train1_dep1.getId()));

        events.processEvent(new VehicleEntersTrafficEvent(1.0, driverId_bus1, linkId1, veh_bus1_dep1.getId(), networkMode_bus, 1.0));
        events.processEvent(new VehicleEntersTrafficEvent(1.0, driverId_train1, linkId7, veh_train1_dep1.getId(), networkMode_train, 1.0));
//      BUS1 :stopSequence == 0
        events.processEvent(new VehicleArrivesAtFacilityEvent(1.0, veh_bus1_dep1.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new PersonEntersVehicleEvent(2.0, passenger1B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(2.0, passenger2B, veh_bus1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(3.0, veh_bus1_dep1.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new LinkLeaveEvent(4.0, veh_bus1_dep1.getId(), linkId1));

//      TRAIN1 :stopSequence == 0
        events.processEvent(new LinkLeaveEvent(2.0, veh_train1_dep1.getId(), linkId7));
        events.processEvent(new LinkEnterEvent(3.0, veh_train1_dep1.getId(), linkId2));
        events.processEvent(new VehicleArrivesAtFacilityEvent(3.0, veh_train1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new PersonEntersVehicleEvent(4.0, passenger1T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(4.0, passenger2T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(4.0, passenger3T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(4.0, passenger4T, veh_train1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(5.0, veh_train1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new LinkLeaveEvent(5.0, veh_train1_dep1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(6.0, veh_train1_dep1.getId(), linkId5));
        events.processEvent(new LinkLeaveEvent(7.0, veh_train1_dep1.getId(), linkId5));

//      BUS1 - 1,2 :stopSequence == 1
        events.processEvent(new LinkEnterEvent(5.0, veh_bus1_dep1.getId(), linkId2));
        events.processEvent(new VehicleArrivesAtFacilityEvent(6.0, veh_bus1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new PersonEntersVehicleEvent(7.0, passenger3B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(7.0, passenger4B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(7.0, passenger1B, veh_bus1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(8.0, veh_bus1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new LinkLeaveEvent(9.0, veh_bus1_dep1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(10.0, veh_bus1_dep1.getId(), linkId3));
        events.processEvent(new LinkLeaveEvent(11.0, veh_bus1_dep1.getId(), linkId3));

//      TRAIN1 - 1,2,3,4 :stopSequence == 1
        events.processEvent(new LinkEnterEvent(8.0, veh_train1_dep1.getId(), linkId6));
        events.processEvent(new VehicleArrivesAtFacilityEvent(8.0, veh_train1_dep1.getId(), transitStopFacilityId6, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(9.0, passenger1T, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(9.0, passenger2T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(9.0, passenger5T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(9.0, passenger6T, veh_train1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(10.0, veh_train1_dep1.getId(), transitStopFacilityId6, 0.0));
        events.processEvent(new LinkLeaveEvent(11.0, veh_train1_dep1.getId(), linkId6));
        events.processEvent(new LinkEnterEvent(12.0, veh_train1_dep1.getId(), linkId7));
        events.processEvent(new LinkLeaveEvent(13.0, veh_train1_dep1.getId(), linkId7));

//      BUS2 :stopSequence == 0
        events.processEvent(new TransitDriverStartsEvent(11.0, driverId_bus2, veh_bus1_dep2.getId(),
                transitLineId_bus1, transitRouteId_bus1_route1, departureId_bus1_dep2));
        events.processEvent(new PersonDepartureEvent(11.0, driverId_bus2, linkId1, networkMode_bus));
        events.processEvent(new PersonEntersVehicleEvent(11.0, driverId_bus2, veh_bus1_dep2.getId()));
        events.processEvent(new VehicleEntersTrafficEvent(11.0, driverId_bus2, linkId1, veh_bus1_dep2.getId(), networkMode_bus, 1.0));
        events.processEvent(new VehicleArrivesAtFacilityEvent(11.0, veh_bus1_dep2.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new PersonEntersVehicleEvent(12.0, passenger5B, veh_bus1_dep2.getId()));
        events.processEvent(new PersonEntersVehicleEvent(12.0, passenger6B, veh_bus1_dep2.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(13.0, veh_bus1_dep2.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new LinkLeaveEvent(13.0, veh_bus1_dep2.getId(), linkId1));
//      BUS2 :stopSequence == 1
        events.processEvent(new LinkEnterEvent(14.0, veh_bus1_dep2.getId(), linkId2));
        events.processEvent(new VehicleArrivesAtFacilityEvent(14.0, veh_bus1_dep2.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(15.0, passenger6B, veh_bus1_dep2.getId()));
        events.processEvent(new PersonEntersVehicleEvent(15.0, passenger7B, veh_bus1_dep2.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(16.0, veh_bus1_dep2.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new LinkLeaveEvent(17.0, veh_bus1_dep2.getId(), linkId2));

//      BUS1 - 2,3,4 :stopSequence == 2
        events.processEvent(new LinkEnterEvent(12.0, veh_bus1_dep1.getId(), linkId4));
        events.processEvent(new VehicleArrivesAtFacilityEvent(13.0, veh_bus1_dep1.getId(), transitStopFacilityId4, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(14.0, passenger2B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(14.0, passenger3B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(14.0, passenger4B, veh_bus1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(15.0, veh_bus1_dep1.getId(), transitStopFacilityId4, 0.0));
        events.processEvent(new VehicleLeavesTrafficEvent(15.0, driverId_bus1, linkId4, veh_bus1_dep1.getId(), networkMode_bus, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(15.0, driverId_bus1, veh_bus1_dep1.getId()));
        events.processEvent(new PersonArrivalEvent(15.0, driverId_bus1, linkId4, networkMode_bus));

//      TRAIN1 - 3,4,5,6 :stopSequence == 2
        events.processEvent(new LinkEnterEvent(14.0, veh_train1_dep1.getId(), linkId2));
        events.processEvent(new VehicleArrivesAtFacilityEvent(14.0, veh_train1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(15.0, passenger5T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(15.0, passenger2T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(15.0, passenger1T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(16.0, passenger6B, veh_train1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(16.0, veh_train1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new LinkLeaveEvent(16.0, veh_train1_dep1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(17.0, veh_train1_dep1.getId(), linkId5));
        events.processEvent(new LinkLeaveEvent(18.0, veh_train1_dep1.getId(), linkId5));


//      BUS2 - 5,7 :stopSequence == 2
        events.processEvent(new LinkEnterEvent(18.0, veh_bus1_dep2.getId(), linkId3));
        events.processEvent(new LinkLeaveEvent(19.0, veh_bus1_dep2.getId(), linkId3));
        events.processEvent(new LinkEnterEvent(20.0, veh_bus1_dep2.getId(), linkId4));
        events.processEvent(new VehicleArrivesAtFacilityEvent(21.0, veh_bus1_dep2.getId(), transitStopFacilityId4, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(22.0, passenger5B, veh_bus1_dep2.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(22.0, passenger7B, veh_bus1_dep2.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(23.0, veh_bus1_dep2.getId(), transitStopFacilityId4, 0.0));
        events.processEvent(new VehicleLeavesTrafficEvent(23.0, driverId_bus2, linkId4, veh_bus1_dep2.getId(), networkMode_bus, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(23.0, driverId_bus2, veh_bus1_dep2.getId()));
        events.processEvent(new PersonArrivalEvent(23.0, driverId_bus2, linkId4, networkMode_bus));

//      BUS1 :stopSequence == 3
        events.processEvent(new TransitDriverStartsEvent(21.0, driverId_bus1, veh_bus1_dep1.getId(),
                transitLineId_bus1, transitRouteId_bus1_route1, departureId_bus1_dep1));
        events.processEvent(new PersonDepartureEvent(21.0, driverId_bus1, linkId4, networkMode_bus));
        events.processEvent(new PersonEntersVehicleEvent(22.0, driverId_bus1, veh_bus1_dep1.getId()));
        events.processEvent(new VehicleEntersTrafficEvent(22.0, driverId_bus1, linkId4, veh_bus1_dep1.getId(), networkMode_bus, 1.0));
        events.processEvent(new VehicleArrivesAtFacilityEvent(23.0, veh_bus1_dep1.getId(), transitStopFacilityId4, 0.0));
        events.processEvent(new PersonEntersVehicleEvent(24.0, passenger1B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(24.0, passenger2B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(24.0, passenger3B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(24.0, passenger4B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(24.0, passenger5B, veh_bus1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(25.0, veh_bus1_dep1.getId(), transitStopFacilityId4, 0.0));
        events.processEvent(new LinkLeaveEvent(26.0, veh_bus1_dep1.getId(), linkId4));
        events.processEvent(new LinkEnterEvent(26.0, veh_bus1_dep1.getId(), linkId3));
        events.processEvent(new LinkLeaveEvent(27.0, veh_bus1_dep1.getId(), linkId3));



//      TRAIN1 - 1,2,3,4,6,6B :stopSequence == 3
        events.processEvent(new LinkEnterEvent(19.0, veh_train1_dep1.getId(), linkId6));
        events.processEvent(new VehicleArrivesAtFacilityEvent(19.0, veh_train1_dep1.getId(), transitStopFacilityId6, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(20.0, passenger1T, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(20.0, passenger2T, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(20.0, passenger6B, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(20.0, passenger6T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(20.0, passenger5T, veh_train1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(21.0, veh_train1_dep1.getId(), transitStopFacilityId6, 0.0));
        events.processEvent(new LinkLeaveEvent(22.0, veh_train1_dep1.getId(), linkId6));
        events.processEvent(new LinkEnterEvent(23.0, veh_train1_dep1.getId(), linkId7));
        events.processEvent(new LinkLeaveEvent(24.0, veh_train1_dep1.getId(), linkId7));
//      TRAIN1 - 3,4,5 :stopSequence == 4
        events.processEvent(new LinkEnterEvent(24.0, veh_train1_dep1.getId(), linkId2));
        events.processEvent(new VehicleArrivesAtFacilityEvent(25.0, veh_train1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(25.0, passenger5T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(25.0, passenger2T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(25.0, passenger1T, veh_train1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(26.0, passenger8T, veh_train1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(26.0, veh_train1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new LinkLeaveEvent(27.0, veh_train1_dep1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(27.0, veh_train1_dep1.getId(), linkId5));
        events.processEvent(new LinkLeaveEvent(28.0, veh_train1_dep1.getId(), linkId5));

        // BUS1 1,2,3,4,5 :stopSequence == 4
        events.processEvent(new LinkEnterEvent(27.0, veh_bus1_dep1.getId(), linkId2));
        events.processEvent(new VehicleArrivesAtFacilityEvent(28.0, veh_bus1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(28.0, passenger1B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(28.0, passenger2B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(28.0, passenger6B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(28.0, passenger5T, veh_bus1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(29.0, veh_bus1_dep1.getId(), transitStopFacilityId2, 0.0));
        events.processEvent(new LinkLeaveEvent(30.0, veh_bus1_dep1.getId(), linkId2));



        //      TRAIN1 - 1,2,3,4,8 :stopSequence == 5
        events.processEvent(new LinkEnterEvent(28.0, veh_train1_dep1.getId(), linkId6));
        events.processEvent(new VehicleArrivesAtFacilityEvent(29.0, veh_train1_dep1.getId(), transitStopFacilityId6, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(30.0, passenger1T, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(30.0, passenger2T, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(30.0, passenger3T, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(30.0, passenger4T, veh_train1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(30.0, passenger8T, veh_train1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(31.0, veh_train1_dep1.getId(), transitStopFacilityId6, 0.0));
        events.processEvent(new LinkLeaveEvent(32.0, veh_train1_dep1.getId(), linkId6));
        events.processEvent(new LinkEnterEvent(33.0, veh_train1_dep1.getId(), linkId7));
        events.processEvent(new VehicleLeavesTrafficEvent(34.0, driverId_train1, linkId7, veh_train1_dep1.getId(), networkMode_train, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(34.0, driverId_train1, veh_train1_dep1.getId()));
        events.processEvent(new PersonArrivalEvent(33.0, driverId_train1, linkId7, networkMode_train));

        // BUS1 3,4,5,6,5T :stopSequence == 5
        events.processEvent(new LinkEnterEvent(30.0, veh_bus1_dep1.getId(), linkId1));
        events.processEvent(new VehicleArrivesAtFacilityEvent(31.0, veh_bus1_dep1.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new PersonLeavesVehicleEvent(31.0, passenger5B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(31.0, passenger3B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(31.0, passenger4B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(31.0, passenger6B, veh_bus1_dep1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(32.0, passenger5T, veh_bus1_dep1.getId()));
        events.processEvent(new VehicleDepartsAtFacilityEvent(32.0, veh_bus1_dep1.getId(), transitStopFacilityId1, 0.0));
        events.processEvent(new VehicleLeavesTrafficEvent(32.0, driverId_bus1, linkId1, veh_bus1_dep1.getId(), networkMode_bus, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(32.0, driverId_bus1, veh_bus1_dep1.getId()));
        events.processEvent(new PersonArrivalEvent(33.0, driverId_bus1, linkId1, networkMode_bus));
        events.finishProcessing();



        // das oben als einfachen test, dann weiteren Test mit mehreren Linien, TranistRoutes und Departures darunter separat als weiteren Test

        // fuer weitere Busse, zeitlich durchmischt. Mal ein Event fuer bus 1, dann eins fuer bus 2, dann wieder ein event an bus 1.
        // Events immer chronologisch, auch die Reihenfolge der Event Typen muss gleich bleiben, z.B. TransitDriverStartsEvent, dann PersonDepartureEvent, dann PersonEntersVehicleEvent, dann VehicleEntersTrafficEvent



        // Tests
        List<PtStop2StopAnalysis.Stop2StopEntry> bus1_dep1_stop1 = ptStop2StopAnalysis.getStop2StopEntriesByDeparture().stream().filter(entry -> entry.transitLineId.equals(transitLineId_bus1) && entry.transitRouteId.equals(transitRouteId_bus1_route1) && entry.departureId.equals(departureId_bus1_dep1) && entry.stopId.equals(transitStopFacilityId1) && entry.stopSequence == 0).collect(Collectors.toList());
        Assert.assertEquals("Either no entry or more than entry for " + transitLineId_bus1 + ", " + transitRouteId_bus1_route1 + ", " + departureId_bus1_dep1 + ", " + departureId_bus1_dep1 + ", " + transitStopFacilityId1 + ", 0",1, bus1_dep1_stop1.size());
        Assert.assertNull("There should be no previous stop, but there was", bus1_dep1_stop1.get(0).stopPreviousId);
        Assert.assertEquals("Wrong arrivalTimeScheduled", 1.0, bus1_dep1_stop1.get(0).arrivalTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals("Wrong arrivalDelay", 0.0, bus1_dep1_stop1.get(0).arrivalDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(3.0, bus1_dep1_stop1.get(0).departureTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop1.get(0).departureDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop1.get(0).passengersAtArrival, MatsimTestUtils.EPSILON);
        Assert.assertEquals(veh_bus1_dep1.getType().getCapacity().getSeats() + veh_bus1_dep1.getType().getCapacity().getStandingRoom(), bus1_dep1_stop1.get(0).totalVehicleCapacity, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop1.get(0).passengersAlighting, MatsimTestUtils.EPSILON);
        Assert.assertEquals(2.0, bus1_dep1_stop1.get(0).passengersBoarding, MatsimTestUtils.EPSILON);

        List<Id<Link>> linkList = new ArrayList<>();
        linkList.add(linkId1);
        Assert.assertEquals("Wrong links", linkList, bus1_dep1_stop1.get(0).linkIdsSincePreviousStop);

        List<PtStop2StopAnalysis.Stop2StopEntry> bus1_dep1_stop4 = ptStop2StopAnalysis.getStop2StopEntriesByDeparture().stream().filter(entry -> entry.transitLineId.equals(transitLineId_bus1) && entry.transitRouteId.equals(transitRouteId_bus1_route1) && entry.departureId.equals(departureId_bus1_dep1) && entry.stopId.equals(transitStopFacilityId1) && entry.stopSequence == 3).collect(Collectors.toList());
        Assert.assertEquals("Either no entry or more than entry for " + transitLineId_bus1 + ", " + transitRouteId_bus1_route1 + ", " + departureId_bus1_dep1 + ", " + departureId_bus1_dep1 + ", " + transitStopFacilityId1 + ", 0",1, bus1_dep1_stop1.size());
        Assert.assertNull("There should be no previous stop, but there was", bus1_dep1_stop4.get(0).stopPreviousId);
        Assert.assertEquals("Wrong arrivalTimeScheduled", 1.0, bus1_dep1_stop4.get(0).arrivalTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals("Wrong arrivalDelay", 0.0, bus1_dep1_stop4.get(0).arrivalDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(25.0, bus1_dep1_stop4.get(0).departureTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop4.get(0).departureDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop4.get(0).passengersAtArrival, MatsimTestUtils.EPSILON);
        Assert.assertEquals(veh_bus1_dep1.getType().getCapacity().getSeats() + veh_bus1_dep1.getType().getCapacity().getStandingRoom(), bus1_dep1_stop1.get(0).totalVehicleCapacity, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop4.get(0).passengersAlighting, MatsimTestUtils.EPSILON);
        Assert.assertEquals(5.0, bus1_dep1_stop4.get(0).passengersBoarding, MatsimTestUtils.EPSILON);

        List<PtStop2StopAnalysis.Stop2StopEntry> bus1_dep1_stop5 = ptStop2StopAnalysis.getStop2StopEntriesByDeparture().stream().filter(entry -> entry.transitLineId.equals(transitLineId_bus1) && entry.transitRouteId.equals(transitRouteId_bus1_route1) && entry.departureId.equals(departureId_bus1_dep1) && entry.stopId.equals(transitStopFacilityId1) && entry.stopSequence == 4).collect(Collectors.toList());
        Assert.assertEquals("Either no entry or more than entry for " + transitLineId_bus1 + ", " + transitRouteId_bus1_route1 + ", " + departureId_bus1_dep1 + ", " + departureId_bus1_dep1 + ", " + transitStopFacilityId1 + ", 0",1, bus1_dep1_stop1.size());
        Assert.assertEquals("Wrong arrivalTimeScheduled", 1.0, bus1_dep1_stop5.get(0).arrivalTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals("Wrong arrivalDelay", 0.0, bus1_dep1_stop5.get(0).arrivalDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(32.0, bus1_dep1_stop5.get(0).departureTimeScheduled, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop5.get(0).departureDelay, MatsimTestUtils.EPSILON);
        Assert.assertEquals(5.0, bus1_dep1_stop5.get(0).passengersAtArrival, MatsimTestUtils.EPSILON);
        Assert.assertEquals(veh_bus1_dep1.getType().getCapacity().getSeats() + veh_bus1_dep1.getType().getCapacity().getStandingRoom(), bus1_dep1_stop1.get(0).totalVehicleCapacity, MatsimTestUtils.EPSILON);
        Assert.assertEquals(5.0, bus1_dep1_stop5.get(0).passengersAlighting, MatsimTestUtils.EPSILON);
        Assert.assertEquals(0.0, bus1_dep1_stop5.get(0).passengersBoarding, MatsimTestUtils.EPSILON);

    }
}