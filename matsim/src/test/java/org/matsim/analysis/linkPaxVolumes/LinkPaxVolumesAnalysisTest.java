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

package org.matsim.analysis.linkPaxVolumes;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.linkpaxvolumes.LinkPaxVolumesAnalysis;
import org.matsim.analysis.linkpaxvolumes.LinkPaxVolumesWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.ParallelEventsManager;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LinkPaxVolumesAnalysisTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    /**
     * Test method for {@link LinkPaxVolumesAnalysis}.
     */

    @Test
    public void testLinkPaxVolumes() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);

        VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

        VehicleType vehiclesType1 = vehicleFactory.createVehicleType(Id.create("vehiclesType1", VehicleType.class));
        {
            VehicleCapacity capacity = vehiclesType1.getCapacity();
            capacity.setSeats(50);
            capacity.setStandingRoom(100);
            VehicleUtils.setDoorOperationMode(vehiclesType1, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
            VehicleUtils.setAccessTime(vehiclesType1, 1.0 / 3.0); // 1s per boarding agent, distributed on 3 doors
            VehicleUtils.setEgressTime(vehiclesType1, 1.0 / 3.0); // 1s per alighting ag ent, distributed on 3 doors
            scenario.getTransitVehicles().addVehicleType(vehiclesType1);
        }

        VehicleType vehiclesType2 = vehicleFactory.createVehicleType(Id.create("vehiclesType2", VehicleType.class));
        {
            VehicleCapacity capacity = vehiclesType2.getCapacity();
            capacity.setSeats(5);
            capacity.setStandingRoom(0);
            VehicleUtils.setDoorOperationMode(vehiclesType2, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
            VehicleUtils.setAccessTime(vehiclesType2, 1.0 / 3.0); // 1s per boarding agent, distributed on 3 doors
            VehicleUtils.setEgressTime(vehiclesType2, 1.0 / 3.0); // 1s per alighting ag ent, distributed on 3 doors
            scenario.getTransitVehicles().addVehicleType(vehiclesType2);
        }

        VehicleType transitVehicleType = vehicleFactory.createVehicleType(Id.create("transitVehicleType", VehicleType.class));
        {
            VehicleCapacity capacity = transitVehicleType.getCapacity();
            capacity.setSeats(50);
            capacity.setStandingRoom(100);
            VehicleUtils.setDoorOperationMode(transitVehicleType, VehicleType.DoorOperationMode.serial); // first finish boarding, then start alighting
            VehicleUtils.setAccessTime(transitVehicleType, 1.0 / 3.0); // 1s per boarding agent, distributed on 3 doors
            VehicleUtils.setEgressTime(transitVehicleType, 1.0 / 3.0); // 1s per alighting ag ent, distributed on 3 doors
            scenario.getTransitVehicles().addVehicleType(transitVehicleType);
        }


        Vehicle vehicle1 = vehicleFactory.createVehicle(Id.create("vehicle1", Vehicle.class), vehiclesType1);
        Vehicle vehicle2 = vehicleFactory.createVehicle(Id.create("vehicle2", Vehicle.class), vehiclesType2);
        Vehicle transitVehicle1 = vehicleFactory.createVehicle(Id.create("transitVehicle1", Vehicle.class), transitVehicleType);


        Vehicles vehicles;
        vehicles = scenario.getVehicles();

        vehicles.addVehicleType(vehiclesType1);
        vehicles.addVehicleType(vehiclesType2);
        vehicles.addVehicle(vehicle1);
        vehicles.addVehicle(vehicle2);

        Vehicles transitVehicles;
        transitVehicles = scenario.getTransitVehicles();
        transitVehicles.addVehicle(transitVehicle1);

        Id<Link> linkId1 = Id.createLinkId("link1");
        Id<Link> linkId1b = Id.createLinkId("link1b");
        Id<Link> linkId2 = Id.createLinkId("link2");
        Id<Link> linkId2b = Id.createLinkId("link2b");
        Id<Link> linkId3 = Id.createLinkId("link3");
        Id<Link> linkId3b = Id.createLinkId("link3b");
        Id<Link> linkId4 = Id.createLinkId("link4");
        Id<Link> linkId4b = Id.createLinkId("link4b");
        Id<Link> linkId5 = Id.createLinkId("link5");
        Id<Link> linkId5b = Id.createLinkId("link5b");

        Id<Person> person1 = Id.createPersonId("person1");
        Id<Person> person2 = Id.createPersonId("person2");
        Id<Person> person3 = Id.createPersonId("person3");
        Id<Person> person4 = Id.createPersonId("person4");
        Id<Person> person5 = Id.createPersonId("person5");
        Id<Person> person6 = Id.createPersonId("person6");
        Id<Person> person7 = Id.createPersonId("person7");
        Id<Person> person8 = Id.createPersonId("person8");
        Id<Person> person9 = Id.createPersonId("person9");
        Id<Person> person10 = Id.createPersonId("person10");
        Id<Person> person11 = Id.createPersonId("person11");
        Id<Person> person12 = Id.createPersonId("person12");
        Id<Person> person13 = Id.createPersonId("person13");
        Id<Person> person14 = Id.createPersonId("person14");

        String networkModeCar = TransportMode.car;
        String networkModePt = TransportMode.pt;


        Network network = scenario.getNetwork();
        /*
         * (1)
         * 	|
         * 	|
         * (2)------(5)
         * 	|		 |
         * 	|		 |
         * (3)------(4)
         */
        Node node1 = NetworkUtils.createAndAddNode(network, Id.create(1, Node.class), new Coord((double) 0, (double) 200));
        Node node2 = NetworkUtils.createAndAddNode(network, Id.create(2, Node.class), new Coord((double) 0, (double) 100));
        Node node3 = NetworkUtils.createAndAddNode(network, Id.create(3, Node.class), new Coord((double) 0, (double) 0));
        Node node4 = NetworkUtils.createAndAddNode(network, Id.create(4, Node.class), new Coord((double) 100, (double) 0));
        Node node5 = NetworkUtils.createAndAddNode(network, Id.create(5, Node.class), new Coord((double) 100, (double) 100));

        final Node fromNode1 = node1;
        final Node toNode1 = node2;
        final double freespeed1 = 2.7;
        final double capacity1 = 500.;
        final double numLanes1 = 1.;
        NetworkUtils.createAndAddLink(network, linkId1, fromNode1, toNode1, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode1b = node2;
        final Node toNode1b = node1;
        NetworkUtils.createAndAddLink(network, linkId1b, fromNode1b, toNode1b, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode2 = node2;
        final Node toNode2 = node3;
        NetworkUtils.createAndAddLink(network, linkId2, fromNode2, toNode2, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode2b = node3;
        final Node toNode2b = node2;
        NetworkUtils.createAndAddLink(network, linkId2b, fromNode2b, toNode2b, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode3 = node3;
        final Node toNode3 = node4;
        NetworkUtils.createAndAddLink(network, linkId3, fromNode3, toNode3, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode3b = node4;
        final Node toNode3b = node3;
        NetworkUtils.createAndAddLink(network, linkId3b, fromNode3b, toNode3b, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode4 = node4;
        final Node toNode4 = node5;
        NetworkUtils.createAndAddLink(network, linkId4, fromNode4, toNode4, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode4b = node4;
        final Node toNode4b = node5;
        NetworkUtils.createAndAddLink(network, linkId4b, fromNode4b, toNode4b, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode5 = node5;
        final Node toNode5 = node2;
        NetworkUtils.createAndAddLink(network, linkId5, fromNode5, toNode5, (double) 100, freespeed1, capacity1, numLanes1);
        final Node fromNode5b = node2;
        final Node toNode5b = node5;
        NetworkUtils.createAndAddLink(network, linkId5b, fromNode5b, toNode5b, (double) 100, freespeed1, capacity1, numLanes1);

        ParallelEventsManager events = new ParallelEventsManager(false);

        LinkPaxVolumesAnalysis linkPaxVolumes = new LinkPaxVolumesAnalysis(vehicles, transitVehicles);
        events.addHandler(linkPaxVolumes);

        events.initProcessing();
        //vehicle1
        events.processEvent(new PersonDepartureEvent(1, person1, linkId1, networkModeCar, networkModeCar));
        events.processEvent(new PersonEntersVehicleEvent(1.0, person1, vehicle1.getId()));
        events.processEvent(new VehicleEntersTrafficEvent(1.0, person1, linkId1, vehicle1.getId(), networkModeCar, 1.0));
        events.processEvent(new PersonEntersVehicleEvent(2.0, person2, vehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(2.0, person3, vehicle1.getId()));
        events.processEvent(new LinkLeaveEvent(4.0, vehicle1.getId(), linkId1));
        events.processEvent(new LinkEnterEvent(5.0, vehicle1.getId(), linkId2));
        events.processEvent(new PersonLeavesVehicleEvent(7.0, person2, vehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(7.0, person4, vehicle1.getId()));
        events.processEvent(new LinkLeaveEvent(9.0, vehicle1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(10.0, vehicle1.getId(), linkId3));
        events.processEvent(new PersonLeavesVehicleEvent(12.0, person3, vehicle1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(12.0, person4, vehicle1.getId()));
        events.processEvent(new VehicleLeavesTrafficEvent(14.0, person1, linkId3, vehicle1.getId(), networkModeCar, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(15.0, person1, vehicle1.getId()));
        events.processEvent(new PersonArrivalEvent(15.0, person1, linkId3, networkModeCar));
        //vehicle 2
        events.processEvent(new PersonDepartureEvent(1.0, person5, linkId3b, networkModeCar, networkModeCar));
        events.processEvent(new PersonEntersVehicleEvent(1.0, person5, vehicle2.getId()));
        events.processEvent(new VehicleEntersTrafficEvent(1.0, person5, linkId3b, vehicle2.getId(), networkModeCar, 1.0));
        events.processEvent(new PersonEntersVehicleEvent(2.0, person6, vehicle2.getId()));
        events.processEvent(new PersonEntersVehicleEvent(2.0, person7, vehicle2.getId()));
        events.processEvent(new LinkLeaveEvent(4.0, vehicle2.getId(), linkId3b));
        events.processEvent(new LinkEnterEvent(5.0, vehicle2.getId(), linkId2b));
        events.processEvent(new PersonLeavesVehicleEvent(7.0, person7, vehicle2.getId()));
        events.processEvent(new PersonEntersVehicleEvent(7.0, person8, vehicle2.getId()));
        events.processEvent(new LinkLeaveEvent(9.0, vehicle2.getId(), linkId2b));
        events.processEvent(new LinkEnterEvent(10.0, vehicle2.getId(), linkId5b));
        events.processEvent(new LinkLeaveEvent(11.0, vehicle2.getId(), linkId5b));
        events.processEvent(new LinkEnterEvent(12.0, vehicle2.getId(), linkId4b));
        events.processEvent(new LinkLeaveEvent(13.0, vehicle2.getId(), linkId4b));
        events.processEvent(new LinkEnterEvent(14.0, vehicle2.getId(), linkId3b));
        events.processEvent(new LinkLeaveEvent(15.0, vehicle2.getId(), linkId3b));
        events.processEvent(new LinkEnterEvent(16.0, vehicle2.getId(), linkId2b));
        events.processEvent(new LinkLeaveEvent(19.0, vehicle2.getId(), linkId2b));
        events.processEvent(new LinkEnterEvent(22.0, vehicle2.getId(), linkId5b));
        events.processEvent(new PersonLeavesVehicleEvent(22.0, person6, vehicle2.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(22.0, person8, vehicle2.getId()));
        events.processEvent(new VehicleLeavesTrafficEvent(24.0, person5, linkId5b, vehicle2.getId(), networkModeCar, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(25.0, person5, vehicle2.getId()));
        events.processEvent(new PersonArrivalEvent(25.0, person5, linkId5b, networkModeCar));
        //transitVehicle
        events.processEvent(new PersonDepartureEvent(3800.0, person9, linkId1, networkModePt, networkModePt));
        events.processEvent(new PersonEntersVehicleEvent(3801.0, person9, transitVehicle1.getId()));
        events.processEvent(new VehicleEntersTrafficEvent(3801.0, person9, linkId1, transitVehicle1.getId(), networkModePt, 1.0));
        events.processEvent(new PersonEntersVehicleEvent(3802.0, person10, transitVehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(3802.0, person11, transitVehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(3802.0, person12, transitVehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(3802.0, person13, transitVehicle1.getId()));
        events.processEvent(new LinkLeaveEvent(3804.0, transitVehicle1.getId(), linkId1));
        events.processEvent(new LinkEnterEvent(3805.0, transitVehicle1.getId(), linkId2));
        events.processEvent(new PersonLeavesVehicleEvent(3807.0, person10, transitVehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(3807.0, person14, transitVehicle1.getId()));
        events.processEvent(new LinkLeaveEvent(3809.0, transitVehicle1.getId(), linkId2));
        events.processEvent(new LinkEnterEvent(3810.0, transitVehicle1.getId(), linkId3));
        events.processEvent(new LinkLeaveEvent(3812.0, transitVehicle1.getId(), linkId3));
        events.processEvent(new LinkEnterEvent(3813.0, transitVehicle1.getId(), linkId4));
        events.processEvent(new PersonLeavesVehicleEvent(3815.0, person11, transitVehicle1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(3815.0, person12, transitVehicle1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(3815.0, person13, transitVehicle1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(3815.0, person14, transitVehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(3820.0, person1, transitVehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(3820.0, person2, transitVehicle1.getId()));
        events.processEvent(new PersonEntersVehicleEvent(3822.0, person3, transitVehicle1.getId()));
        events.processEvent(new LinkLeaveEvent(3823.0, transitVehicle1.getId(), linkId4));
        events.processEvent(new LinkEnterEvent(3824.0, transitVehicle1.getId(), linkId4b));
        events.processEvent(new LinkLeaveEvent(3826.0, transitVehicle1.getId(), linkId4b));
        events.processEvent(new LinkEnterEvent(3828.0, transitVehicle1.getId(), linkId3b));
        events.processEvent(new LinkLeaveEvent(3831.0, transitVehicle1.getId(), linkId3b));
        events.processEvent(new LinkEnterEvent(3833.0, transitVehicle1.getId(), linkId2b));
        events.processEvent(new LinkLeaveEvent(3836.0, transitVehicle1.getId(), linkId2b));
        events.processEvent(new LinkEnterEvent(3840.0, transitVehicle1.getId(), linkId1b));
        events.processEvent(new PersonLeavesVehicleEvent(3842.0, person1, transitVehicle1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(3842.0, person2, transitVehicle1.getId()));
        events.processEvent(new PersonLeavesVehicleEvent(3844.0, person3, transitVehicle1.getId()));
        events.processEvent(new VehicleLeavesTrafficEvent(3846.0, person1, linkId1b, transitVehicle1.getId(), networkModePt, 1.0));
        events.processEvent(new PersonLeavesVehicleEvent(3850.0, person1, transitVehicle1.getId()));
        events.processEvent(new PersonArrivalEvent(3850.0, person1, linkId1b, networkModePt));

        events.finishProcessing();


        LinkPaxVolumesWriter linkPaxVolumesWriter = new LinkPaxVolumesWriter(linkPaxVolumes, network, ";");

        linkPaxVolumesWriter.writeLinkVehicleAndPaxVolumesAllPerDayCsv(utils.getOutputDirectory() + "/LinkVehicleAndPaxVolumesAllPerDay.csv");

        String path = utils.getOutputDirectory() + "LinkVehicleAndPaxVolumesAllPerDay.csv";

        CSVFormat format = CSVFormat.newFormat(';').withFirstRecordAsHeader();

        try(FileReader allPerDayCsv = new FileReader(path);
            CSVParser parser = CSVParser.parse(allPerDayCsv, format)){
                List<CSVRecord> allDayRecordList = parser.getRecords();

                List<CSVRecord> allDayRecordListLink2 = allDayRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link2")).collect(Collectors.toList());
                Assert.assertEquals("Either no record or more than one record",1, allDayRecordListLink2.size());
                Assert.assertEquals("Wrong PassengerInclDriver on Link2",8, Double.parseDouble(allDayRecordListLink2.get(0).get(2)),MatsimTestUtils.EPSILON);

                List<CSVRecord> allDayRecordListLink2b = allDayRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link2b")).collect(Collectors.toList());
                Assert.assertEquals("Either no record or more than one record",1, allDayRecordListLink2b.size());
                Assert.assertEquals("Wrong amount of Vehicles on Link2b",3, Double.parseDouble(allDayRecordListLink2b.get(0).get(1)),MatsimTestUtils.EPSILON);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LinkPaxVolumesWriter linkVehicleAndPaxVolumesPerNetworkModePerHourCsv = new LinkPaxVolumesWriter(linkPaxVolumes, network, ";");

        linkVehicleAndPaxVolumesPerNetworkModePerHourCsv.writeLinkVehicleAndPaxVolumesPerNetworkModePerHourCsv(utils.getOutputDirectory() + "/LinkVehicleAndPaxVolumesPerNetworkModePerHour.csv");

        try(FileReader modePerHourCsv = new FileReader(utils.getOutputDirectory() + "/LinkVehicleAndPaxVolumesPerNetworkModePerHour.csv");
            CSVParser parser = CSVParser.parse(modePerHourCsv, format)){
            List<CSVRecord> modePerHourRecordList = parser.getRecords();

            List<CSVRecord> modePerHourRecordListLink2 = modePerHourRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link2") && record.get(parser.getHeaderMap().get("networkMode")).equals("car") && record.get(parser.getHeaderMap().get("hour")).equals("0")).collect(Collectors.toList());
            Assert.assertEquals("Either no record or more than one record",1, modePerHourRecordListLink2.size());
            Assert.assertEquals("Wrong PassengerInclDriver on Link2",3, Double.parseDouble(modePerHourRecordListLink2.get(0).get(4)),MatsimTestUtils.EPSILON);

            List<CSVRecord> modePerHourRecordListLink2b = modePerHourRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link2b") && record.get(parser.getHeaderMap().get("networkMode")).equals("car") && record.get(parser.getHeaderMap().get("hour")).equals("0")).collect(Collectors.toList());
            Assert.assertEquals("Either no record or more than one record",1, modePerHourRecordListLink2b.size());
            Assert.assertEquals("Wrong amount of Vehicles on Link2b",2, Double.parseDouble(modePerHourRecordListLink2b.get(0).get(3)),MatsimTestUtils.EPSILON);

            List<CSVRecord> modePerHourRecordListLink2Pt = modePerHourRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link2") && record.get(parser.getHeaderMap().get("networkMode")).equals("pt") && record.get(parser.getHeaderMap().get("hour")).equals("1")).collect(Collectors.toList());
            Assert.assertEquals("Either no record or more than one record",1, modePerHourRecordListLink2Pt.size());
            Assert.assertEquals("Wrong amount of Vehicles on Link2",5, Double.parseDouble(modePerHourRecordListLink2Pt.get(0).get(4)),MatsimTestUtils.EPSILON);

            List<CSVRecord> modePerHourRecordListLink4b = modePerHourRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link4b") && record.get(parser.getHeaderMap().get("networkMode")).equals("car") && record.get(parser.getHeaderMap().get("hour")).equals("10")).collect(Collectors.toList());
            Assert.assertEquals("Either no record or more than one record",1, modePerHourRecordListLink4b.size());
            Assert.assertEquals("Wrong amount of Vehicles on Link4b",0, Double.parseDouble(modePerHourRecordListLink4b.get(0).get(3)),MatsimTestUtils.EPSILON);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        LinkPaxVolumesWriter linkVehicleAndPaxVolumesPerVehicleTypePerHourCsv = new LinkPaxVolumesWriter(linkPaxVolumes, network, ";");

        linkVehicleAndPaxVolumesPerVehicleTypePerHourCsv.writeLinkVehicleAndPaxVolumesPerVehicleTypePerHourCsv(utils.getOutputDirectory() + "/LinkVehicleAndPaxVolumesPerVehicleTypePerHour.csv");

        try(FileReader vehicleTypePerHourCsv = new FileReader(utils.getOutputDirectory() + "/LinkVehicleAndPaxVolumesPerVehicleTypePerHour.csv");
            CSVParser parser = CSVParser.parse(vehicleTypePerHourCsv, format)){
            List<CSVRecord> vehicleTypePerHourRecordList = parser.getRecords();

            List<CSVRecord> vehicleTypePerHourRecordListLink2 = vehicleTypePerHourRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link2") && record.get(parser.getHeaderMap().get("vehicleType")).equals("vehiclesType1") && record.get(parser.getHeaderMap().get("hour")).equals("0")).collect(Collectors.toList());
            Assert.assertEquals("Either no record or more than one record",1, vehicleTypePerHourRecordListLink2.size());
            Assert.assertEquals("Wrong PassengerInclDriver on Link2",3, Double.parseDouble(vehicleTypePerHourRecordListLink2.get(0).get(4)),MatsimTestUtils.EPSILON);

            List<CSVRecord> vehicleTypePerHourRecordListLink2b = vehicleTypePerHourRecordList.stream().filter(record -> record.get(parser.getHeaderMap().get("link")).equals("link2b") && record.get(parser.getHeaderMap().get("vehicleType")).equals("transitVehicleType") && record.get(parser.getHeaderMap().get("hour")).equals("1")).collect(Collectors.toList());
            Assert.assertEquals("Either no record or more than one record",1, vehicleTypePerHourRecordListLink2b.size());
            Assert.assertEquals("Wrong amount of Vehicles on Link2b",1, Double.parseDouble(vehicleTypePerHourRecordListLink2b.get(0).get(3)),MatsimTestUtils.EPSILON);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
