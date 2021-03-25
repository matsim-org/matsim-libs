/* *********************************************************************** *
 * project: org.matsim.*
 * TransitVehicleTest.java
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

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.fakes.FakePassengerAgent;
import org.matsim.vehicles.*;


/**
 * @author mrieser
 */
public abstract class AbstractTransitVehicleTest extends TestCase {

	private static final Logger log = Logger.getLogger(AbstractTransitVehicleTest.class);

	protected abstract TransitVehicle createTransitVehicle(final Vehicle vehicle);

	public void testInitialization_SeatAndStandCapacity() {
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(5));
		vehType.getCapacity().setStandingRoom(Integer.valueOf(2));
//		vehType.getCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );
		TransitVehicle veh = createTransitVehicle(vehicle);
		assertEquals(vehicle, veh.getVehicle());
		assertEquals(7, veh.getPassengerCapacity());
	}

	public void testInitialization_SeatOnlyCapacity() {
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(4));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );
		TransitVehicle veh = createTransitVehicle(vehicle);
		assertEquals(vehicle, veh.getVehicle());
		assertEquals(4, veh.getPassengerCapacity());
	}

	public void testInitialization_NoCapacity() {
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );
		try {
			createTransitVehicle(vehicle);
			fail("missing exception.");
		}
		catch (Exception e) {
			log.info("caught expected exception.", e);
		}
	}

	public void testAddPassenger() {
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(5));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );
		TransitVehicle veh = createTransitVehicle(vehicle);
		ArrayList<PTPassengerAgent> passengers = new ArrayList<PTPassengerAgent>(veh.getPassengerCapacity());
		for (int i = 0; i < veh.getPassengerCapacity(); i++) {
			PTPassengerAgent passenger = new FakePassengerAgent(null);
			passengers.add(passenger);
			assertFalse(veh.getPassengers().contains(passenger));
			assertTrue(veh.addPassenger(passenger));
			assertTrue(veh.getPassengers().contains(passenger));
		}
		assertEquals(passengers.size(), veh.getPassengers().size());
		for (PTPassengerAgent passenger : passengers) {
			assertTrue(veh.getPassengers().contains(passenger));
		}
		assertFalse(veh.addPassenger(new FakePassengerAgent(null)));
	}

	public void testRemovePassenger() {
		VehicleType vehType = VehicleUtils.createVehicleType(Id.create("busType", VehicleType.class ) );
//		VehicleCapacity capacity = new VehicleCapacity();
		vehType.getCapacity().setSeats(Integer.valueOf(5));
//		vehType.setCapacity(capacity);
		Vehicle vehicle = VehicleUtils.createVehicle(Id.create(1976, Vehicle.class ), vehType );
		TransitVehicle veh = createTransitVehicle(vehicle);

		PTPassengerAgent passenger1 = new FakePassengerAgent(null);
		PTPassengerAgent passenger2 = new FakePassengerAgent(null);
		PTPassengerAgent passenger3 = new FakePassengerAgent(null);
		assertTrue(veh.addPassenger(passenger1));
		assertTrue(veh.addPassenger(passenger2));
		assertTrue(veh.addPassenger(passenger3));
		assertTrue(veh.getPassengers().contains(passenger2));
		// try to remove a passenger
		assertTrue(veh.removePassenger(passenger2));
		assertFalse(veh.getPassengers().contains(passenger2));
		assertTrue(veh.getPassengers().contains(passenger1));
		assertTrue(veh.getPassengers().contains(passenger3));
		// try to remove a passenger that is not in the list
		assertFalse(veh.removePassenger(passenger2));
		// make sure this did not remove someone else
		assertTrue(veh.getPassengers().contains(passenger1));
		assertTrue(veh.getPassengers().contains(passenger3));
		// some more tests
		assertTrue(veh.removePassenger(passenger1));
		assertTrue(veh.removePassenger(passenger3));
		assertEquals(0, veh.getPassengers().size());
	}

}
