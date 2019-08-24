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

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;

import java.io.File;
import java.util.Map;

/**
 * @author dgrether
 */
public class VehicleWriterV2Test extends MatsimTestCase {
	private static final Logger log = Logger.getLogger( VehicleWriterV2Test.class ) ;

	private static final String TESTXML  = "testVehicles_v2.xml";

	private final Id<Vehicle> id23 = Id.create("23", Vehicle.class);
	private final Id<Vehicle> id42 = Id.create("42", Vehicle.class);
	private final Id<Vehicle> id42_23 = Id.create(" 42  23", Vehicle.class); //indeed this should be double blank in the middle but due to collapse this is only one blank

	private Map<Id<VehicleType>, VehicleType> vehicleTypes ;
	private Map<Id<Vehicle>, Vehicle> vehicles;

	@BeforeClass
	public void setUp() throws Exception {
		super.setUp();
		String outfileName = this.getOutputDirectory() + "../testOutputVehicles.xml";

		//read it
		Vehicles vehicles1 = VehicleUtils.createVehiclesContainer();
		MatsimVehicleReader reader = new MatsimVehicleReader(vehicles1);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);

		//write it
		VehicleWriterV2 writer = new VehicleWriterV2(vehicles1);
		writer.writeFile(outfileName);
		assertTrue(new File(outfileName).exists()); 
//		//read it again
		Vehicles vehicles2 = VehicleUtils.createVehiclesContainer();
		reader = new MatsimVehicleReader(vehicles2);
		reader.readFile(this.getOutputDirectory() + "../testOutputVehicles.xml");

		vehicleTypes = vehicles2.getVehicleTypes();
		vehicles = vehicles2.getVehicles();
	}

	@Test
	public void test_NumberOfVehicleTypeisReadCorrectly() {
		assertNotNull(vehicleTypes);
		assertEquals(3, vehicleTypes.size());
	}

	@Test
	public void test_VehicleTypeValuesAreReadCorrectly_normalCar() {
		VehicleType vehTypeNormalCar = vehicleTypes.get(Id.create("normal&Car", VehicleType.class));
		assertNotNull(vehTypeNormalCar);
		assertEquals(9.5, vehTypeNormalCar.getLength(), EPSILON);
		assertEquals(3.0, vehTypeNormalCar.getWidth(), EPSILON);
		assertEquals(42.0, vehTypeNormalCar.getMaximumVelocity(), EPSILON);

		assertNotNull(vehTypeNormalCar.getCapacity());
		assertEquals(Integer.valueOf(5), vehTypeNormalCar.getCapacity().getSeats());
		assertEquals(Integer.valueOf(20), vehTypeNormalCar.getCapacity().getStandingRoom());
		assertEquals(23.23, vehTypeNormalCar.getCapacity().getVolumeInCubicMeters(), EPSILON);
		assertEquals(9.5, vehTypeNormalCar.getCapacity().getWeightInTons(), EPSILON);
		assertEquals(200.0, VehicleUtils.getFreightCapacityUnits(vehTypeNormalCar.getCapacity()), EPSILON);

		assertNotNull(vehTypeNormalCar.getCostInformation());
		assertEquals(100, vehTypeNormalCar.getCostInformation().getFixedCosts(), EPSILON);
		assertEquals(0.15, vehTypeNormalCar.getCostInformation().getCostsPerMeter(), EPSILON);
		assertEquals(0.08, vehTypeNormalCar.getCostInformation().getCostsPerSecond(), EPSILON);
		assertEquals(0.06, VehicleUtils.getCostsPerSecondWaiting(vehTypeNormalCar.getCostInformation()), EPSILON);

		assertNotNull(vehTypeNormalCar.getEngineInformation());
		EngineInformation engineInformation = vehTypeNormalCar.getEngineInformation();;
		assertEquals("pass. car", VehicleUtils.getHbefaVehicleCategory(engineInformation));
		assertEquals("petrol", VehicleUtils.getHbefaTechnology(engineInformation));
		assertEquals("< 1,4L", VehicleUtils.getHbefaSizeClass(engineInformation));
		assertEquals("EURO-5", VehicleUtils.getHbefaEmissionsConcept(engineInformation));

		assertEquals(2.0, vehTypeNormalCar.getPcuEquivalents());
		assertEquals(1.5, vehTypeNormalCar.getFlowEfficiencyFactor());
		assertEquals("pt", vehTypeNormalCar.getNetworkMode());

		assertEquals("abc", vehTypeNormalCar.getAttributes().getAttribute("Attribute1"));
		assertEquals(1.3, (double) vehTypeNormalCar.getAttributes().getAttribute("Attribute2"), EPSILON);
		assertEquals(0.23, VehicleUtils.getFuelConsumption(vehTypeNormalCar), EPSILON);
		assertEquals(23.23, VehicleUtils.getAccessTime(vehTypeNormalCar), EPSILON);
		assertEquals(42.42, VehicleUtils.getEgressTime(vehTypeNormalCar), EPSILON);
		assertEquals( VehicleUtils.DoorOperationMode.parallel, VehicleUtils.getDoorOperationMode(vehTypeNormalCar ) );
	}

	@Test
	public void test_VehicleTypeValuesAreReadCorrectly_defaultCar() {
		VehicleType vehTypeDefaultCar = vehicleTypes.get(Id.create("defaultValue>Car", VehicleType.class));
		assertNotNull(vehTypeDefaultCar);
		assertEquals(7.5, vehTypeDefaultCar.getLength(), EPSILON);
		assertEquals(1.0, vehTypeDefaultCar.getWidth(), EPSILON);
		assertTrue(Double.isInfinite(vehTypeDefaultCar.getMaximumVelocity()));
		assertNull(vehTypeDefaultCar.getCapacity());
		assertEquals( VehicleUtils.DoorOperationMode.serial, VehicleUtils.getDoorOperationMode(vehTypeDefaultCar ) );
		assertEquals(1.0, vehTypeDefaultCar.getPcuEquivalents());
		assertEquals(1.0, vehTypeDefaultCar.getFlowEfficiencyFactor());
		assertEquals("def", vehTypeDefaultCar.getAttributes().getAttribute("Attribute1"));
		assertEquals(2, vehTypeDefaultCar.getAttributes().getAttribute("Attribute2"));
	}

	@Test
	public void test_VehicleTypeValuesAreReadCorrectly_smallTruck() {
		VehicleType vehTypeSmallTruck = vehicleTypes.get(Id.create("smallTruck", VehicleType.class));
		assertNotNull(vehTypeSmallTruck);
		assertEquals("This is a small truck", vehTypeSmallTruck.getDescription());
		assertEquals(7.5, vehTypeSmallTruck.getLength(), EPSILON);
		assertEquals(1.0, vehTypeSmallTruck.getWidth(), EPSILON);
		assertEquals("diesel", VehicleUtils.getHbefaTechnology(vehTypeSmallTruck.getEngineInformation()));
		assertEquals("EURO-6", VehicleUtils.getHbefaEmissionsConcept(vehTypeSmallTruck.getEngineInformation()));
		assertEquals(100.0, vehTypeSmallTruck.getCostInformation().getFixedCosts(), EPSILON);
		assertEquals(0.2,vehTypeSmallTruck.getCostInformation().getCostsPerMeter(), EPSILON);
		assertEquals(0.10, vehTypeSmallTruck.getCostInformation().getCostsPerSecond(),EPSILON);
		assertEquals(0.05, VehicleUtils.getCostsPerSecondWaiting(vehTypeSmallTruck.getCostInformation()), EPSILON);
		assertEquals(0.15, VehicleUtils.getCostsPerSecondInService(vehTypeSmallTruck.getCostInformation()), EPSILON);
	}

	@Test
	public void test_NumberOfVehiclesIsReadCorrectly() {
		assertNotNull(vehicles);
		assertEquals(3, vehicles.size());
	}

	@Test
	public void test_VehicleTypeToVehiclesAssignmentIsReadCorrectly(){
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
