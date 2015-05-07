/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.osm.scheduleCreator.hafasCreator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.assertEquals;

/**
 * @author boescpa
 */
public class TestStopReader {

	private StopReader reader;
	private TransitSchedule schedule;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		scenario.getConfig().scenario().setUseVehicles(true);
		this.schedule = scenario.getTransitSchedule();
		this.reader = new StopReader(TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus"));
	}

	@Test
	public void testReadStops() {
		reader.createTransitStops(this.schedule, utils.getClassInputDirectory());
		Id facility1Id = Id.create(1234567, TransitStopFacility.class);
		Id facility2Id = Id.create(1234568, TransitStopFacility.class);
		assertEquals("Correct Reading of ID of HAFAS-Stopfacilities.",
				Id.create(1234567, TransitStopFacility.class).toString(),
				schedule.getFacilities().get(facility1Id).getId().toString());
		assertEquals("Correct Reading of Coordinates.", 1250450.0, schedule.getFacilities().get(facility2Id).getCoord().getY(), 0.0);
		assertEquals("Correct Reading of Names.", "Test Bahnhof 1", schedule.getFacilities().get(facility1Id).getName());
	}
}
