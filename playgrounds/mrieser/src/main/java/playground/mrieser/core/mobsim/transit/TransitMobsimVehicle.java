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

import java.util.Collection;

import org.matsim.pt.qsim.PassengerAgent;
import org.matsim.pt.qsim.TransitStopHandler;

import playground.mrieser.core.mobsim.api.MobsimVehicle;

public interface TransitMobsimVehicle extends MobsimVehicle {

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
	 * @return the still available capacity in the vehicle.
	 */
	public double getFreeCapacity();

	public TransitStopHandler getStopHandler();

}
