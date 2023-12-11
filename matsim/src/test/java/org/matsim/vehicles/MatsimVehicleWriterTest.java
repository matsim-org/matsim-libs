/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dgrether
 */
public class MatsimVehicleWriterTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private static final Logger log = LogManager.getLogger(MatsimVehicleWriterTest.class);

	private static final String TESTXML = "testVehicles_v1.xml";

	private Id<Vehicle> id23;
	private Id<Vehicle> id42;
	private Id<Vehicle> id42_23;

	@BeforeEach public void setUp() {

		id23 = Id.create("23", Vehicle.class);
		id42 = Id.create("42", Vehicle.class);

		// indeed this should be double blank in the middle but due to collapse this is
		// only one blank
		id42_23 = Id.create(" 42  23", Vehicle.class);
	}

	@Test
	void testWriter() throws FileNotFoundException, IOException {
		{
			String outfileName = utils.getOutputDirectory() + "testOutputVehicles.xml";

			// create empty vehicles container:
			Vehicles vehicles = VehicleUtils.createVehiclesContainer();

			// read, which will be v1:
			MatsimVehicleReader reader = new MatsimVehicleReader(vehicles);
			reader.readFile(utils.getPackageInputDirectory() + TESTXML);

			VehicleType vehType = vehicles.getVehicleTypes().get(Id.create("normal&Car", VehicleType.class));

			// write, which will be the newest fmt:
			MatsimVehicleWriter writer = new MatsimVehicleWriter(vehicles);
			writer.writeFile(outfileName);
			assertTrue(new File(outfileName).exists());
		}
		{
			// read, which will now be in the newest fmt:
			Vehicles vehicles = VehicleUtils.createVehiclesContainer();
			MatsimVehicleReader reader = new MatsimVehicleReader(vehicles);
			reader.readFile(utils.getOutputDirectory() + "testOutputVehicles.xml");

			VehicleType vehType = vehicles.getVehicleTypes().get(Id.create("normal&Car", VehicleType.class));

			// check it, check it, check it now!
			this.checkContent(vehicles);
		}
	}

	private void checkContent(Vehicles vehdef) {
		Map<Id<VehicleType>, VehicleType> vehicleTypes = vehdef.getVehicleTypes();
		Map<Id<Vehicle>, Vehicle> vehicles = vehdef.getVehicles();

		assertNotNull(vehicleTypes);
		assertEquals(2, vehicleTypes.size());
		VehicleType vehTypeDefaultCar = vehicleTypes.get(Id.create("normal&Car", VehicleType.class));
		assertNotNull(vehTypeDefaultCar);
		assertEquals(9.5, vehTypeDefaultCar.getLength(), MatsimTestUtils.EPSILON);
		assertEquals(3.0, vehTypeDefaultCar.getWidth(), MatsimTestUtils.EPSILON);
		assertEquals(42.0, vehTypeDefaultCar.getMaximumVelocity(), MatsimTestUtils.EPSILON);
		assertNotNull(vehTypeDefaultCar.getCapacity());
		assertEquals(Integer.valueOf(5), vehTypeDefaultCar.getCapacity().getSeats());
		assertEquals(Integer.valueOf(20), vehTypeDefaultCar.getCapacity().getStandingRoom());
		assertEquals(23.23, vehTypeDefaultCar.getCapacity().getVolumeInCubicMeters(), MatsimTestUtils.EPSILON);
		assertNotNull(vehTypeDefaultCar.getEngineInformation());
		assertEquals(EngineInformation.FuelType.diesel,
				VehicleUtils.getFuelType(vehTypeDefaultCar.getEngineInformation()));
		assertEquals(0.23, VehicleUtils.getFuelConsumption(vehTypeDefaultCar), MatsimTestUtils.EPSILON);
		assertEquals(23.23, VehicleUtils.getAccessTime(vehTypeDefaultCar), MatsimTestUtils.EPSILON);
		assertEquals(42.42, VehicleUtils.getEgressTime(vehTypeDefaultCar), MatsimTestUtils.EPSILON);
		assertEquals(VehicleType.DoorOperationMode.parallel, VehicleUtils.getDoorOperationMode(vehTypeDefaultCar));
		assertEquals(2.0, vehTypeDefaultCar.getPcuEquivalents(), 0);

		vehTypeDefaultCar = vehicleTypes.get(Id.create("defaultValue>Car", VehicleType.class));
		assertNotNull(vehTypeDefaultCar);
		assertEquals(7.5, vehTypeDefaultCar.getLength(), MatsimTestUtils.EPSILON);
		assertEquals(1.0, vehTypeDefaultCar.getWidth(), MatsimTestUtils.EPSILON);
		assertTrue(Double.isInfinite(vehTypeDefaultCar.getMaximumVelocity()));
		assertNotNull(vehTypeDefaultCar.getCapacity());
		assertNotNull(vehTypeDefaultCar.getCostInformation());
		assertNull(vehTypeDefaultCar.getCostInformation().getFixedCosts());
		assertNull(vehTypeDefaultCar.getCostInformation().getCostsPerMeter());
		assertNull(vehTypeDefaultCar.getCostInformation().getCostsPerSecond());
		assertEquals(VehicleType.DoorOperationMode.serial, VehicleUtils.getDoorOperationMode(vehTypeDefaultCar));
		assertEquals(1.0, vehTypeDefaultCar.getPcuEquivalents(), 0);

		assertNotNull(vehicles);
		assertEquals(3, vehicles.size());

		assertNotNull(vehicles.get(id23));
		assertEquals(id23, vehicles.get(id23).getId());
		assertEquals(Id.create("normal&Car", VehicleType.class), vehicles.get(id23).getType().getId());

		assertNotNull(vehicles.get(id42));
		assertEquals(id42, vehicles.get(id42).getId());
		assertEquals(Id.create("defaultValue>Car", VehicleType.class), vehicles.get(id42).getType().getId());

		assertNotNull(vehicles.get(id42_23));
		assertEquals(id42_23, vehicles.get(id42_23).getId());

	}

}
