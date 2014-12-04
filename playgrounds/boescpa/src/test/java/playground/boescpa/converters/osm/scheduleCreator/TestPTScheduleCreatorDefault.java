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
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * The default implementation of PTStationCreator (using the Swiss-HAFAS-Schedule).
 *
 * @author boescpa
 */
public class TestPTScheduleCreatorDefault {

    PTScheduleCreatorDefault scheduleCreator = null;
    TransitSchedule schedule = null;

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Before
    public void prepareTests() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        scenario.getConfig().scenario().setUseTransit(true);
        schedule = scenario.getTransitSchedule();
        scheduleCreator = new PTScheduleCreatorDefault(schedule);
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
    public void testReadLines() {
        scheduleCreator.readStops(utils.getClassInputDirectory()+"BFKOORD_GEO");
        scheduleCreator.readLines(utils.getClassInputDirectory()+"FPLAN");
        // TODO-boescpa Finish tests...
    }
}
