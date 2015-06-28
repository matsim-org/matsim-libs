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
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import static org.junit.Assert.assertEquals;

/**
 * @author boescpa
 */
public class TestVehicleTypesReader {

	private Vehicles vehicles;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().scenario().setUseTransit(true);
		this.vehicles = scenario.getTransitVehicles();
	}

	@Test
	public void testReadStops() {
		VehicleTypesReader.readVehicles(this.vehicles, utils.getClassInputDirectory() + "/VehicleData.csv");
		assertEquals("Correct reading of vehicles.",
				this.vehicles.getVehicleTypes().get(Id.create("R", VehicleType.class)).getLength(),2.1, 0.0);
		assertEquals("Correct reading of vehicles.",
				this.vehicles.getVehicleTypes().get(Id.create("BUS", VehicleType.class)).getAccessTime(),1.3, 0.0);
		assertEquals("Correct reading of vehicles.",
				this.vehicles.getVehicleTypes().get(Id.create("BUS", VehicleType.class)).getEgressTime(),1.4, 0.0);
	}

}
