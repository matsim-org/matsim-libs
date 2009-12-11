/* *********************************************************************** *
 * project: org.matsim.*
 * Vehicle.java
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

package org.matsim.pt.queuesim;

import java.util.Collection;

import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.vehicles.BasicVehicle;


public interface TransitVehicle {

	/**
	 * Adds a passenger to this vehicle.
	 *
	 * @param passenger
	 * @return <tt>true</tt> when the agent was added as a passenger (as per the general contract of the Collection.add method).
	 */
	public boolean addPassenger(final PassengerAgent passenger);

	/**
	 * Removes the passenger from this vehicle.
	 *
	 * @param passenger
	 * @return <tt>true</tt> when the agent was removed as a passenger, <tt>false</tt> if the agent was not a passenger of this vehicle or could not be removed for other reasons
	 */
	public boolean removePassenger(final PassengerAgent passenger);

	/**
	 * @return an immutable Collection of all passengers in this vehicle
	 */
	public Collection<PassengerAgent> getPassengers();

	/**
	 * @return number of passengers this vehicle can transport
	 */
	public int getPassengerCapacity();

	/**
	 * @return the <code>BasicVehicle</code> that this simulation vehicle represents
	 */
	public BasicVehicle getBasicVehicle();

	public DriverAgent getDriver();

}
