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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * @author mrieser
 */
public class TransitQueueVehicleTest extends AbstractTransitVehicleTest {

	@Override
	protected TransitVehicle createTransitVehicle(final Vehicle vehicle) {
		return new TransitQVehicle(vehicle);
	}

	public void testSizeInEquivalents() {
		VehicleType carType = new VehicleTypeImpl(new IdImpl("carType"));
		VehicleType busType = new VehicleTypeImpl(new IdImpl("busType"));
		busType.setPcuEquivalents(2.5);
		VehicleCapacity capacity = new VehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		carType.setCapacity(capacity);
		busType.setCapacity(capacity);
		Vehicle car = new VehicleImpl(new IdImpl(1976), carType);
		Vehicle bus = new VehicleImpl(new IdImpl(1976), busType);
		TransitQVehicle veh = new TransitQVehicle(car);
		assertEquals(1.0, veh.getSizeInEquivalents(), MatsimTestCase.EPSILON);
		veh = new TransitQVehicle(bus);
		assertEquals(2.5, veh.getSizeInEquivalents(), MatsimTestCase.EPSILON);
	}
}
