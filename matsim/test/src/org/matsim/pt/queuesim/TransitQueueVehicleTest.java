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

package org.matsim.pt.queuesim;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.queuesim.TransitQueueVehicle;
import org.matsim.pt.queuesim.TransitVehicle;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;

/**
 * @author mrieser
 */
public class TransitQueueVehicleTest extends AbstractTransitVehicleTest {

	@Override
	protected TransitVehicle createTransitVehicle(final BasicVehicle vehicle) {
		return new TransitQueueVehicle(vehicle, 1);
	}

	public void testSizeInEquivalents() {
		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		TransitQueueVehicle veh = new TransitQueueVehicle(vehicle, 1.0);
		assertEquals(1.0, veh.getSizeInEquivalents(), MatsimTestCase.EPSILON);
		veh = new TransitQueueVehicle(vehicle, 2.5);
		assertEquals(2.5, veh.getSizeInEquivalents(), MatsimTestCase.EPSILON);
	}
}
