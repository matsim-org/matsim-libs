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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class BasicVehicleDefinitionReaderV5Test extends MatsimTestCase {

  private static final String TESTXML  = "testVehicles.xml";

  private final Id id23 = new IdImpl("23");
  private final Id id24 = new IdImpl("24");
  
	public void testParser() {
		
		Map<String, BasicVehicleType> vehicleTypes = new HashMap<String, BasicVehicleType>();
		List<BasicVehicle> vehicles = new ArrayList<BasicVehicle>();
		BasicVehicleDefinitionReaderV1 reader = new BasicVehicleDefinitionReaderV1(vehicleTypes, vehicles);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		
		checkContent(vehicleTypes, vehicles);
	}

	private void checkContent(Map<String, BasicVehicleType> vehicleTypes,
			List<BasicVehicle> vehicles) {
		assertNotNull(vehicleTypes);
		assertEquals(2, vehicleTypes.size());
		BasicVehicleType vehType = vehicleTypes.get("normalCar");
		assertNotNull(vehType);
		assertEquals(9.5, vehType.getLength(), EPSILON);
		assertEquals(3.0, vehType.getWidth(), EPSILON);
		assertEquals(42.0, vehType.getMaximumVelocity(), EPSILON);
		assertNotNull(vehType.getCapacity());
		assertEquals(5, vehType.getCapacity().getSeats());
		assertEquals(20, vehType.getCapacity().getStandingRoom());
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
	
		assertNotNull(vehicles.get(0));
		assertEquals(id23, vehicles.get(0).getId());
		assertEquals("normalCar", vehicles.get(0).getType());

		assertNotNull(vehicles.get(1));
		assertEquals(id24, vehicles.get(1).getId());
		assertEquals("defaultValueCar", vehicles.get(1).getType());
	}

}
