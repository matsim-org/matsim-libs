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

package org.matsim.basic.v01;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.population.Vehicle;
import org.matsim.population.VehicleReaderV1;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class VehicleReaderWriterV1Test extends MatsimTestCase {

  private static final String TESTXML  = "testVehicles.xml";

  private final Id id23 = new IdImpl("23");
  private final Id id42 = new IdImpl("42");
  
	public void testBasicParser() {
		Map<String, BasicVehicleType> vehicleTypes = new HashMap<String, BasicVehicleType>();
		Map<Id, BasicVehicle> vehicles = new HashMap<Id, BasicVehicle>();
		BasicVehicleReaderV1 reader = new BasicVehicleReaderV1(vehicleTypes, vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		
		checkContent(vehicleTypes, vehicles);
	}
	
	public void testParser(){
		Map<String, BasicVehicleType> vehicleTypes = new HashMap<String, BasicVehicleType>();
		Map<Id, Vehicle> vehicles = new HashMap<Id, Vehicle>();
		VehicleReaderV1 reader = new VehicleReaderV1(vehicleTypes, vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		
		checkContent(vehicleTypes, (Map)vehicles);
		checkReferences(vehicles);
	}
	
	
	private void checkReferences(Map<Id, Vehicle> vehicles) {
		for (Vehicle v : vehicles.values()){
			assertNotNull(v);
			assertNotNull(v.getTypeId());
			assertNotNull(v.getType());
			assertEquals(v.getTypeId(), v.getType().getTypeId());
		}
	}

	public void testWriter() throws FileNotFoundException, IOException {
		//read it
		Map<String, BasicVehicleType> vehicleTypes = new HashMap<String, BasicVehicleType>();
		Map<Id, BasicVehicle> vehicles = new HashMap<Id, BasicVehicle>();
		BasicVehicleReaderV1 reader = new BasicVehicleReaderV1(vehicleTypes, vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		//write it
		VehicleWriterV1 writer = new VehicleWriterV1(vehicleTypes, vehicles);
		writer.writeFile(this.getOutputDirectory() + "testOutputVehicles.xml");
		//check it, check it, check it now!
		this.checkContent(vehicleTypes, vehicles);
	}

	private void checkContent(Map<String, BasicVehicleType> vehicleTypes,
			Map<Id, BasicVehicle> vehicles) {
		assertNotNull(vehicleTypes);
		assertEquals(2, vehicleTypes.size());
		BasicVehicleType vehType = vehicleTypes.get("normalCar");
		assertNotNull(vehType);
		assertEquals(9.5, vehType.getLength(), EPSILON);
		assertEquals(3.0, vehType.getWidth(), EPSILON);
		assertEquals(42.0, vehType.getMaximumVelocity(), EPSILON);
		assertNotNull(vehType.getCapacity());
		assertEquals(Integer.valueOf(5), vehType.getCapacity().getSeats());
		assertEquals(Integer.valueOf(20), vehType.getCapacity().getStandingRoom());
		assertNotNull(vehType.getCapacity().getFreightCapacity());
		assertEquals(23.23, vehType.getCapacity().getFreightCapacity().getVolume(), EPSILON);
		assertNotNull(vehType.getEngineInformation());
		assertEquals(BasicEngineInformation.FuelType.diesel, vehType.getEngineInformation().getFuelType());
		assertEquals(0.23, vehType.getEngineInformation().getGasConsumption(), EPSILON);
		
		vehType = vehicleTypes.get("defaultValueCar");
		assertNotNull(vehType);
		assertEquals(7.5, vehType.getLength(), EPSILON);
		assertEquals(1.0, vehType.getWidth(), EPSILON);
		assertEquals(1.0, vehType.getMaximumVelocity(), EPSILON);
		assertNull(vehType.getCapacity());
	
		assertNotNull(vehicles);
		assertEquals(2, vehicles.size());
	
		assertNotNull(vehicles.get(id23));
		assertEquals(id23, vehicles.get(id23).getId());
		assertEquals("normalCar", vehicles.get(id23).getTypeId());

		assertNotNull(vehicles.get(id42));
		assertEquals(id42, vehicles.get(id42).getId());
		assertEquals("defaultValueCar", vehicles.get(id42).getTypeId());
	}

}
