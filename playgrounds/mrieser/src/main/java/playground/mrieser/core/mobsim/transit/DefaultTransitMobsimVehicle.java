/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.transit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.qsim.TransitStopHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;

import playground.mrieser.core.mobsim.impl.DefaultMobsimVehicle;

public class DefaultTransitMobsimVehicle extends DefaultMobsimVehicle implements TransitMobsimVehicle {

	private final List<PassengerAgent> passengers = new ArrayList<PassengerAgent>();
	private final List<PassengerAgent> safePassengers = Collections.unmodifiableList(this.passengers);
	private final double passengerCapacity;
	private double availCapacity = 0.0;
	private final TransitStopHandler stopHandler;

	public DefaultTransitMobsimVehicle(final Vehicle vehicle, final double vehicleSizeInEquivalents, final TransitStopHandler stopHandler) {
		super(vehicle, vehicleSizeInEquivalents);
		this.stopHandler = stopHandler;
		VehicleCapacity capacity = vehicle.getType().getCapacity();
		if (capacity == null) {
			throw new NullPointerException("No capacity set in vehicle type.");
		}
		this.passengerCapacity = capacity.getSeats().intValue() +
				(capacity.getStandingRoom() == null ? 0 : capacity.getStandingRoom().intValue()) - 1; //the driver takes also the seat
		this.availCapacity = this.passengerCapacity;
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		if (passenger.getWeight() < this.availCapacity) {
			this.passengers.add(passenger);
			this.availCapacity -= passenger.getWeight();
			return true;
		}
		return false;
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		boolean removed = this.passengers.remove(passenger);
		if (removed) {
			this.availCapacity += passenger.getWeight();
		}
		return removed;
	}

	@Override
	public Collection<PassengerAgent> getPassengers() {
		return this.safePassengers;
	}

	@Override
	public double getFreeCapacity() {
		return this.availCapacity;
	}

	@Override
	public TransitStopHandler getStopHandler() {
		return this.stopHandler;
	}

}
