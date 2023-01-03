/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueVehicleTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.*;

/**
 * @author mrieser
 */
public class TransitQueueVehicleTest extends AbstractTransitVehicleTest {

	@Override
	protected TransitVehicle createTransitVehicle(final Vehicle vehicle) {
		return new TransitQVehicle(vehicle);
	}

	public void testSizeInEquivalents() {
		VehicleType carType = VehicleUtils.createVehicleType(Id.create("carType", VehicleType.class ) );
		VehicleType busType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
		busType.setPcuEquivalents(2.5);
//		VehicleCapacity capacity = new VehicleCapacity();
		carType.getCapacity().setSeats(Integer.valueOf(5));
		busType.getCapacity().setSeats( 5 );
//		carType.setCapacity(capacity);
//		busType.setCapacity(capacity);
		Vehicle car = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), carType );
		Vehicle bus = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), busType );
		TransitQVehicle veh = new TransitQVehicle(car);
		assertEquals(1.0, veh.getSizeInEquivalents(), MatsimTestCase.EPSILON);
		veh = new TransitQVehicle(bus);
		assertEquals(2.5, veh.getSizeInEquivalents(), MatsimTestCase.EPSILON);
	}
}
