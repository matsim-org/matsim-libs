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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author boescpa
 */
public class TestFPLANReader {

	private TransitSchedule schedule;
	private Vehicles vehicles;
	private Map<String, String> operators;
	Set<Integer> bitfeldNummern;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void prepareTests() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().transit().setUseTransit(true);
		this.schedule = scenario.getTransitSchedule();
		this.vehicles = scenario.getTransitVehicles();
		StopReader stopReader = new StopReader(TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus"));
		stopReader.createTransitStops(this.schedule, utils.getClassInputDirectory());
		operators = OperatorReader.readOperators(utils.getClassInputDirectory() + "HAFAS/BETRIEB_DE");
		bitfeldNummern = new HashSet<>();
		bitfeldNummern.add(0);
	}

	@Test
	public void testReadLines() {
		System.out.println("*** Test Read Lines ***");
		VehicleTypesReader.readVehicles(this.vehicles, utils.getClassInputDirectory() + "VehicleData.csv");
		Map<String, Integer> vehiclesUndefined =
				FPLANReader.readLines(this.schedule, this.vehicles, bitfeldNummern, operators, utils.getClassInputDirectory() + "HAFAS/FPLAN");
		new TransitScheduleWriter(schedule).writeFile(utils.getOutputDirectory() + "ScheduleTest.xml");
		new VehicleWriterV1(vehicles).writeFile(utils.getOutputDirectory() + "VehicleTest.xml");
		// print vehicles undefined
		for (String vehicleUndefined : vehiclesUndefined.keySet()) {
			System.out.println("Undefined vehicle " + vehicleUndefined + " occured in " + vehiclesUndefined.get(vehicleUndefined) + " routes.");
		}
	}

	@Test
	public void testVehiclesNotAvailable() {
		System.out.println("*** Test Vehicles Not Available ***");
		VehicleTypesReader.readVehicles(this.vehicles, utils.getClassInputDirectory() + "VehicleData2.csv");
		Map<String, Integer> vehiclesUndefined =
				FPLANReader.readLines(this.schedule, this.vehicles, bitfeldNummern, operators, utils.getClassInputDirectory() + "HAFAS/FPLAN");
		// print vehicles undefined
		for (String vehicleUndefined : vehiclesUndefined.keySet()) {
			System.out.println("Undefined vehicle " + vehicleUndefined + " occured in " + vehiclesUndefined.get(vehicleUndefined) + " routes.");
		}
	}
}
