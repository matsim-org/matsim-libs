/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.scheduleCreator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import static org.junit.Assert.assertEquals;

/**
 * The default implementation of PTStationCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
public class TestPTScheduleCreatorDefault {

    PTScheduleCreatorDefault scheduleCreator = null;
    TransitSchedule schedule = null;
    Vehicles vehicles = null;

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Before
    public void prepareTests() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().scenario().setUseTransit(true);
        scenario.getConfig().scenario().setUseVehicles(true);
        schedule = scenario.getTransitSchedule();
        vehicles = scenario.getVehicles();
        scheduleCreator = new PTScheduleCreatorDefault(schedule, vehicles);
    }

    @Test
    public void testReadStops() {
        scheduleCreator.readStops(utils.getClassInputDirectory()+"BFKOORD_GEO");
        Id facility1Id = Id.create(1234567, TransitStopFacility.class);
        Id facility2Id = Id.create(1234568, TransitStopFacility.class);
        assertEquals("Correct Reading of ID of HAFAS-Stopfacilities.",
                Id.create(1234567, TransitStopFacility.class).toString(),
                schedule.getFacilities().get(facility1Id).getId().toString());
        assertEquals("Correct Reading of Coordinates.", 250450.4833905114, schedule.getFacilities().get(facility2Id).getCoord().getY(), 0.0);
        assertEquals("Correct Reading of Names.", "Test Bahnhof 1", schedule.getFacilities().get(facility1Id).getName());
    }

    @Test
    public void testReadVehicles() {
        scheduleCreator.readVehicles(utils.getClassInputDirectory() + "VehicleData.csv");
        assertEquals("Correct reading of vehicles.",
                scheduleCreator.vehicles.getVehicleTypes().get(Id.create("R", VehicleType.class)).getLength(),
                2.1, 0.0);
        assertEquals("Correct reading of vehicles.",
                scheduleCreator.vehicles.getVehicleTypes().get(Id.create("BUS", VehicleType.class)).getAccessTime(),
                1.3, 0.0);
        assertEquals("Correct reading of vehicles.",
                scheduleCreator.vehicles.getVehicleTypes().get(Id.create("BUS", VehicleType.class)).getEgressTime(),
                1.4, 0.0);
    }

    @Test
    public void testReadLines() {
        System.out.println("*** Test Read Lines ***");
        scheduleCreator.readVehicles(utils.getClassInputDirectory() + "VehicleData.csv");
        scheduleCreator.readStops(utils.getClassInputDirectory() + "BFKOORD_GEO");
        scheduleCreator.readLines(utils.getClassInputDirectory() + "FPLAN");
        new TransitScheduleWriter(schedule).writeFile(utils.getOutputDirectory() + "ScheduleTest.xml");
        new VehicleWriterV1(vehicles).writeFile(utils.getOutputDirectory() + "VehicleTest.xml");
        scheduleCreator.printVehiclesUndefined();
    }

    @Test
    public void testVehiclesNotAvailable() {
        System.out.println("*** Test Vehicles Not Available ***");
        scheduleCreator.readVehicles(utils.getClassInputDirectory() + "VehicleData2.csv");
        scheduleCreator.readStops(utils.getClassInputDirectory() + "BFKOORD_GEO");
        scheduleCreator.readLines(utils.getClassInputDirectory() + "FPLAN");
        scheduleCreator.printVehiclesUndefined();
    }
}
