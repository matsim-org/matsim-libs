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

package org.matsim.pt.qsim;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.pt.fakes.FakePassengerAgent;
import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.qsim.TransitVehicle;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;


/**
 * @author mrieser
 */
public abstract class AbstractTransitVehicleTest extends TestCase {

	private static final Logger log = Logger.getLogger(AbstractTransitVehicleTest.class);

	protected abstract TransitVehicle createTransitVehicle(final BasicVehicle vehicle);

	public void testInitialization_SeatAndStandCapacity() {
		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		capacity.setStandingRoom(Integer.valueOf(3));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		TransitVehicle veh = createTransitVehicle(vehicle);
		assertEquals(vehicle, veh.getBasicVehicle());
		assertEquals(7, veh.getPassengerCapacity()); // 1 place used by driver
	}

	public void testInitialization_SeatOnlyCapacity() {
		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		TransitVehicle veh = createTransitVehicle(vehicle);
		assertEquals(vehicle, veh.getBasicVehicle());
		assertEquals(4, veh.getPassengerCapacity()); // 1 place used by driver
	}

	public void testInitialization_NoCapacity() {
		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		try {
			createTransitVehicle(vehicle);
			fail("missing exception.");
		}
		catch (Exception e) {
			log.info("catched expected exception.", e);
		}
	}

	public void testAddPassenger() {
		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		TransitVehicle veh = createTransitVehicle(vehicle);
		ArrayList<PassengerAgent> passengers = new ArrayList<PassengerAgent>(veh.getPassengerCapacity());
		for (int i = 0; i < veh.getPassengerCapacity(); i++) {
			PassengerAgent passenger = new FakePassengerAgent(null);
			passengers.add(passenger);
			assertFalse(veh.getPassengers().contains(passenger));
			assertTrue(veh.addPassenger(passenger));
			assertTrue(veh.getPassengers().contains(passenger));
		}
		assertEquals(passengers.size(), veh.getPassengers().size());
		for (PassengerAgent passenger : passengers) {
			assertTrue(veh.getPassengers().contains(passenger));
		}
		assertFalse(veh.addPassenger(new FakePassengerAgent(null)));
	}

	public void testRemovePassenger() {
		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		TransitVehicle veh = createTransitVehicle(vehicle);

		PassengerAgent passenger1 = new FakePassengerAgent(null);
		PassengerAgent passenger2 = new FakePassengerAgent(null);
		PassengerAgent passenger3 = new FakePassengerAgent(null);
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

	public void testGetPassengers_Immutable() {
		BasicVehicleType vehType = new BasicVehicleTypeImpl(new IdImpl("busType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(5));
		vehType.setCapacity(capacity);
		BasicVehicle vehicle = new BasicVehicleImpl(new IdImpl(1976), vehType);
		TransitVehicle veh = createTransitVehicle(vehicle);

		PassengerAgent passenger1 = new FakePassengerAgent(null);
		try {
			veh.getPassengers().add(passenger1);
			fail("missing exception.");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

}
