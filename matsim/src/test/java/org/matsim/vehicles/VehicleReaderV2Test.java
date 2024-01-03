/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dgrether
 */
public class VehicleReaderV2Test {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final String TESTXML2 = "testVehicles_v2.xml";

	private Id<Vehicle> id23;
	private Id<Vehicle> id42;
	private Id<Vehicle> id42_23;

	private Map<Id<VehicleType>, VehicleType> vehicleTypes;
	private Map<Id<Vehicle>, Vehicle> vehicles;

	@BeforeEach public void setUp() {

		Vehicles veh = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader = new MatsimVehicleReader(veh);
		reader.readFile(utils.getPackageInputDirectory() + TESTXML2);

		vehicleTypes = veh.getVehicleTypes();
		vehicles = veh.getVehicles();

		id23 = Id.create("23", Vehicle.class);
		id42 = Id.create("42", Vehicle.class);

		// indeed this should be double blank in the middle but due to collapse this is
		// only one blank
		id42_23 = Id.create(" 42  23", Vehicle.class);
	}

	@Test
	void test_NumberOfVehicleTypeisReadCorrectly() {
		assertNotNull(vehicleTypes);
		assertEquals(3, vehicleTypes.size());
	}

	@Test
	void test_VehicleAttributesReadCorrectly(){
		assertNotNull(vehicleTypes);
		/* First vehicle has an attribute. */
		Vehicle v1 = vehicles.get(Id.createVehicleId("23"));
		assertNotNull(v1.getAttributes());
		assertNotNull(v1.getAttributes().getAttribute("testAttributeString"));
		assertEquals("firstVehicle", v1.getAttributes().getAttribute("testAttributeString").toString());

		/* Second vehicle has no attributes. */
		Vehicle v2 = vehicles.get(Id.createVehicleId("42"));
		assertNotNull(v2.getAttributes());
		assertTrue(v2.getAttributes().isEmpty());
		assertNull(v2.getAttributes().getAttribute("testAttribute"));

		/* Third vehicle again has one attribute. */
		Vehicle v3 = vehicles.get(Id.createVehicleId(" 42  23"));
		assertNotNull(v3.getAttributes());
		assertNotNull(v3.getAttributes().getAttribute("testAttributeDouble"));
		assertEquals(1.234, v3.getAttributes().getAttribute("testAttributeDouble"));
	}


	@Test
	void test_VehicleTypeValuesAreReadCorrectly_normalCar() {
		VehicleType vehTypeNormalCar = vehicleTypes.get(Id.create("normal&Car", VehicleType.class));
		assertNotNull(vehTypeNormalCar);
		assertEquals(9.5, vehTypeNormalCar.getLength(), MatsimTestUtils.EPSILON);
		assertEquals(3.0, vehTypeNormalCar.getWidth(), MatsimTestUtils.EPSILON);
		assertEquals(42.0, vehTypeNormalCar.getMaximumVelocity(), MatsimTestUtils.EPSILON);

		assertNotNull(vehTypeNormalCar.getCapacity());
		assertEquals(Integer.valueOf(5), vehTypeNormalCar.getCapacity().getSeats());
		assertEquals(Integer.valueOf(20), vehTypeNormalCar.getCapacity().getStandingRoom());
		assertEquals(23.23, vehTypeNormalCar.getCapacity().getVolumeInCubicMeters(), MatsimTestUtils.EPSILON);
		assertEquals(9.5, vehTypeNormalCar.getCapacity().getWeightInTons(), MatsimTestUtils.EPSILON);
		assertEquals(200.0, vehTypeNormalCar.getCapacity().getOther(), MatsimTestUtils.EPSILON);

		assertNotNull(vehTypeNormalCar.getCostInformation());
		assertEquals(100, vehTypeNormalCar.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		assertEquals(0.15, vehTypeNormalCar.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		assertEquals(0.08, vehTypeNormalCar.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);
		assertEquals(0.06, VehicleUtils.getCostsPerSecondWaiting(vehTypeNormalCar.getCostInformation()), MatsimTestUtils.EPSILON);

		assertNotNull(vehTypeNormalCar.getEngineInformation());
		EngineInformation engineInformation = vehTypeNormalCar.getEngineInformation();
		assertEquals("pass. car", VehicleUtils.getHbefaVehicleCategory(engineInformation));
		assertEquals("petrol", VehicleUtils.getHbefaTechnology(engineInformation));
		assertEquals("< 1,4L", VehicleUtils.getHbefaSizeClass(engineInformation));
		assertEquals("EURO-5", VehicleUtils.getHbefaEmissionsConcept(engineInformation));

		assertEquals(2.0, vehTypeNormalCar.getPcuEquivalents(), 0);
		assertEquals(1.5, vehTypeNormalCar.getFlowEfficiencyFactor(), 0);
		assertEquals("pt", vehTypeNormalCar.getNetworkMode());

		assertEquals("abc", vehTypeNormalCar.getAttributes().getAttribute("Attribute1"));
		assertEquals(1.3, (double) vehTypeNormalCar.getAttributes().getAttribute("Attribute2"), MatsimTestUtils.EPSILON);
		assertEquals(0.23, VehicleUtils.getFuelConsumption(vehTypeNormalCar), MatsimTestUtils.EPSILON);
		assertEquals(23.23, VehicleUtils.getAccessTime(vehTypeNormalCar), MatsimTestUtils.EPSILON);
		assertEquals(42.42, VehicleUtils.getEgressTime(vehTypeNormalCar), MatsimTestUtils.EPSILON);
		assertEquals(VehicleType.DoorOperationMode.parallel, VehicleUtils.getDoorOperationMode(vehTypeNormalCar));
	}

	@Test
	void test_VehicleTypeValuesAreReadCorrectly_defaultCar() {
		VehicleType vehTypeDefaultCar = vehicleTypes.get(Id.create("defaultValue>Car", VehicleType.class));
		assertNotNull(vehTypeDefaultCar);
		assertEquals(7.5, vehTypeDefaultCar.getLength(), MatsimTestUtils.EPSILON);
		assertEquals(1.0, vehTypeDefaultCar.getWidth(), MatsimTestUtils.EPSILON);
		assertTrue(Double.isInfinite(vehTypeDefaultCar.getCapacity().getVolumeInCubicMeters())); // Default values
		assertTrue(Double.isInfinite(vehTypeDefaultCar.getCapacity().getWeightInTons())); // Default values
		assertTrue(Double.isInfinite(vehTypeDefaultCar.getMaximumVelocity()));
		assertNotNull(vehTypeDefaultCar.getCapacity());
		assertEquals(VehicleType.DoorOperationMode.serial, VehicleUtils.getDoorOperationMode(vehTypeDefaultCar));
		assertEquals(1.0, vehTypeDefaultCar.getPcuEquivalents(), 0);
		assertEquals(1.0, vehTypeDefaultCar.getFlowEfficiencyFactor(), 0);
		assertEquals("def", vehTypeDefaultCar.getAttributes().getAttribute("Attribute1"));
		assertEquals(2, vehTypeDefaultCar.getAttributes().getAttribute("Attribute2"));
	}

	@Test
	void test_VehicleTypeValuesAreReadCorrectly_smallTruck() {
		VehicleType vehTypeSmallTruck = vehicleTypes.get(Id.create("smallTruck", VehicleType.class));
		assertNotNull(vehTypeSmallTruck);
		assertEquals("This is a small truck", vehTypeSmallTruck.getDescription());

		assertNotNull(vehTypeSmallTruck.getCapacity());
		assertEquals(Integer.valueOf(2), vehTypeSmallTruck.getCapacity().getSeats());
		assertEquals(Integer.valueOf(0), vehTypeSmallTruck.getCapacity().getStandingRoom());
		assertTrue(Double.isInfinite(vehTypeSmallTruck.getCapacity().getVolumeInCubicMeters()));
		assertTrue(Double.isInfinite(vehTypeSmallTruck.getCapacity().getWeightInTons()));

		assertEquals(7.5, vehTypeSmallTruck.getLength(), MatsimTestUtils.EPSILON);
		assertEquals(1.0, vehTypeSmallTruck.getWidth(), MatsimTestUtils.EPSILON);
		assertEquals("diesel", VehicleUtils.getHbefaTechnology(vehTypeSmallTruck.getEngineInformation()));
		assertEquals("EURO-6", VehicleUtils.getHbefaEmissionsConcept(vehTypeSmallTruck.getEngineInformation()));
		assertEquals(100.0, vehTypeSmallTruck.getCostInformation().getFixedCosts(), MatsimTestUtils.EPSILON);
		assertEquals(0.2, vehTypeSmallTruck.getCostInformation().getCostsPerMeter(), MatsimTestUtils.EPSILON);
		assertEquals(0.10, vehTypeSmallTruck.getCostInformation().getCostsPerSecond(), MatsimTestUtils.EPSILON);
		assertEquals(0.05, VehicleUtils.getCostsPerSecondWaiting(vehTypeSmallTruck.getCostInformation()), MatsimTestUtils.EPSILON);
		assertEquals(0.15, VehicleUtils.getCostsPerSecondInService(vehTypeSmallTruck.getCostInformation()), MatsimTestUtils.EPSILON);
	}

	@Test
	void test_NumberOfVehiclesIsReadCorrectly() {
		assertNotNull(vehicles);
		assertEquals(3, vehicles.size());
	}

	@Test
	void test_VehicleTypeToVehiclesAssignmentIsReadCorrectly() {
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
