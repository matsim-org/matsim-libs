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

package org.matsim.core.basic.v01.vehicles;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.basic.v01.BasicScenarioImpl;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class VehicleReaderWriterV1Test extends MatsimTestCase {

  private static final String TESTXML  = "testVehicles.xml";

  private final Id id23 = new IdImpl("23");
  private final Id id42 = new IdImpl("42");
  
	public void testBasicParser() {
		BasicScenarioImpl scenario = new BasicScenarioImpl();
		BasicVehicles vehicles = scenario.getVehicles();
		BasicVehicleReaderV1 reader = new BasicVehicleReaderV1(vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		
		checkContent(vehicles);
	}
	
	

	public void testWriter() throws FileNotFoundException, IOException {
		//read it
		BasicScenarioImpl scenario = new BasicScenarioImpl();
		BasicVehicles vehicles = scenario.getVehicles();
		BasicVehicleReaderV1 reader = new BasicVehicleReaderV1(vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		//write it
		VehicleWriterV1 writer = new VehicleWriterV1(vehicles);
		writer.writeFile(this.getOutputDirectory() + "testOutputVehicles.xml");
		//check it, check it, check it now!
		this.checkContent(vehicles);
	}

	private void checkContent(BasicVehicles vehdef) {
		Map<Id, BasicVehicleType> vehicleTypes = vehdef.getVehicleTypes();
		Map<Id, BasicVehicle> vehicles = vehdef.getVehicles();
		assertNotNull(vehicleTypes);
		assertEquals(2, vehicleTypes.size());
		BasicVehicleType vehType = vehicleTypes.get(new IdImpl("normalCar"));
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
		
		vehType = vehicleTypes.get(new IdImpl("defaultValueCar"));
		assertNotNull(vehType);
		assertEquals(7.5, vehType.getLength(), EPSILON);
		assertEquals(1.0, vehType.getWidth(), EPSILON);
		assertEquals(1.0, vehType.getMaximumVelocity(), EPSILON);
		assertNull(vehType.getCapacity());
	
		assertNotNull(vehicles);
		assertEquals(2, vehicles.size());
	
		assertNotNull(vehicles.get(id23));
		assertEquals(id23, vehicles.get(id23).getId());
		assertEquals(new IdImpl("normalCar"), vehicles.get(id23).getType().getId());

		assertNotNull(vehicles.get(id42));
		assertEquals(id42, vehicles.get(id42).getId());
		assertEquals(new IdImpl("defaultValueCar"), vehicles.get(id42).getType().getId());
	}

}
