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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.VehicleType.DoorOperationMode;

/**
 * @author dgrether
 */
public class VehicleReaderV2Test extends MatsimTestCase {

	private static final String TESTXML2  = "testVehicles_v2.xml";

	private final Id<Vehicle> id23 = Id.create("23", Vehicle.class);
	private final Id<Vehicle> id42 = Id.create("42", Vehicle.class);
	private final Id<Vehicle> id42_23 = Id.create(" 42  23", Vehicle.class); //indeed this should be double blank in the middle but due to collapse this is only one blank

	// todo: test for minimal vehicle (was kann man alles weglassen)?
	// TODO: FEF, Einlesen und setztn neuer Attributes: DoorOperationm, accessTime,egressTime, doorOperationMode, LitersPerMeter, FreightCapaUnits.
	// TODO: Use VehicleUtils ... 25.3.19

	public void testBasicParser_v2() {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader = new MatsimVehicleReader(vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTXML2);

		checkContent(vehicles);
	}

	private void checkContent(Vehicles vehdef) {
		Map<Id<VehicleType>, VehicleType> vehicleTypes = vehdef.getVehicleTypes();
		Map<Id<Vehicle>, Vehicle> vehicles = vehdef.getVehicles();

		assertNotNull(vehicleTypes);
		assertEquals(2, vehicleTypes.size());
		VehicleType vehTypeNormalCar = vehicleTypes.get(Id.create("normal&Car", VehicleType.class));
		assertNotNull(vehTypeNormalCar);
		assertEquals(9.5, vehTypeNormalCar.getLength(), EPSILON);
		assertEquals(3.0, vehTypeNormalCar.getWidth(), EPSILON);
		assertEquals(42.0, vehTypeNormalCar.getMaximumVelocity(), EPSILON);
		assertNotNull(vehTypeNormalCar.getCapacity());
		assertEquals(Integer.valueOf(5), vehTypeNormalCar.getCapacity().getSeats());
		assertEquals(Integer.valueOf(20), vehTypeNormalCar.getCapacity().getStandingRoom());
		assertNotNull(vehTypeNormalCar.getCapacity().getFreightCapacity());
		assertEquals(23.23, vehTypeNormalCar.getCapacity().getFreightCapacity().getVolume(), EPSILON);
		assertEquals(9.5, vehTypeNormalCar.getCapacity().getFreightCapacity().getWeight(), EPSILON);
//		assertEquals(200, vehTypeNormalCar.getCapacity().getFreightCapacity().getUnits());      //TODO wie zugreifen?

		assertEquals(100, vehTypeNormalCar.getCostInformation().getFixedCosts(), EPSILON);
		assertEquals(0.15,vehTypeNormalCar.getCostInformation().getCostsPerMeter(), EPSILON);
		assertEquals(0.08, vehTypeNormalCar.getCostInformation().getCostsPerSecond(),EPSILON);

		assertNotNull(vehTypeNormalCar.getEngineInformation());
		assertEquals(EngineInformation.FuelType.diesel, vehTypeNormalCar.getEngineInformation().getFuelType());
//		assertEquals(0.23, vehTypeNormalCar.getEngineInformation().getGasConsumption(), EPSILON); //TODO wie zugreifen?
		assertEquals(23.23, vehTypeNormalCar.getAccessTime(), EPSILON);
		assertEquals(42.42, vehTypeNormalCar.getEgressTime(), EPSILON);
//		assertEquals(DoorOperationMode.parallel, vehTypeNormalCar.getDoorOperationMode()); //TODO: make compatible
		assertEquals(2.0, vehTypeNormalCar.getPcuEquivalents());
		assertEquals("abc", vehTypeNormalCar.getAttributes().getAttribute("Attribute1"));
		assertEquals(1.3, (double) vehTypeNormalCar.getAttributes().getAttribute("Attribute2"), EPSILON);


		VehicleType vehTypeDefaultCar = vehicleTypes.get(Id.create("defaultValue>Car", VehicleType.class));
//		assertNotNull(vehTypeDefaultCar);
//		assertEquals(7.5, vehTypeDefaultCar.getLength(), EPSILON);
//		assertEquals(1.0, vehTypeDefaultCar.getWidth(), EPSILON);
//		assertTrue(Double.isInfinite(vehTypeDefaultCar.getMaximumVelocity()));
//		assertNull(vehTypeDefaultCar.getCapacity());
//		assertEquals(DoorOperationMode.serial, vehTypeDefaultCar.getDoorOperationMode());
//		assertEquals(1.0, vehTypeDefaultCar.getPcuEquivalents());
		assertEquals("def", vehTypeDefaultCar.getAttributes().getAttribute("Attribute1"));
		assertEquals(2, vehTypeDefaultCar.getAttributes().getAttribute("Attribute2"));

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
