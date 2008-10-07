/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleImpl.java
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

package playground.marcel.pt.implementations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import playground.marcel.pt.interfaces.DriverAgent;
import playground.marcel.pt.interfaces.PassengerAgent;
import playground.marcel.pt.interfaces.Vehicle;

public class VehicleImpl implements Vehicle {

	private final int passengerCapacity;
	private DriverAgent driver = null;
	private final List<PassengerAgent> passengers;

	public VehicleImpl(final int passengerCapacity) {
		this.passengerCapacity = passengerCapacity;
		this.passengers = new ArrayList<PassengerAgent>(this.passengerCapacity);
	}

	public boolean addPassenger(final PassengerAgent passenger) {
		if (this.passengers.size() < this.passengerCapacity) {
			return this.passengers.add(passenger);
		}
		return false;
	}

	public int getPassengerCapacity() {
		return this.passengerCapacity;
	}

	public Collection<PassengerAgent> getPassengers() {
		return Collections.unmodifiableList(this.passengers);
	}

	public boolean removePassenger(final PassengerAgent passenger) {
		boolean removed = this.passengers.remove(passenger);
		if (removed) {
			// create Event, but where to?
		}
		return removed;
	}

	public void setDriver(final DriverAgent driver) {
		this.driver = driver;
	}

	public DriverAgent getDriver() {
		return this.driver;
	}

}
