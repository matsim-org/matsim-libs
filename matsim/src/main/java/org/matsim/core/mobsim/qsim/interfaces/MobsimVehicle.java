/* *********************************************************************** *
 * project: matsim
 * MobsimVehicle.java
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

package org.matsim.core.mobsim.qsim.interfaces;

import java.util.Collection;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.vis.snapshotwriters.VisVehicle;

/**
 * @author nagel
 *
 */
public interface MobsimVehicle extends VisVehicle{
	
	Link getCurrentLink();
	
	double getSizeInEquivalents();
	
	/**
	 * Adds a passenger to this vehicle.
	 *
	 * @return <tt>true</tt> when the agent was added as a passenger (as per the general contract of the Collection.add method).
	 */
	boolean addPassenger(final PassengerAgent passenger);

	/**
	 * Removes the passenger from this vehicle.
	 *
	 * @return <tt>true</tt> when the agent was removed as a passenger, <tt>false</tt> if the agent was not a passenger of this vehicle or could not be removed for other reasons
	 */
	boolean removePassenger(final PassengerAgent passenger);

	/**
	 * @return an immutable Collection of all passengers in this vehicle.  Should <i> not </i> include the driver
	 */
	Collection<? extends PassengerAgent> getPassengers();

	/**
	 * @return number of passengers this vehicle can transport
	 */
	int getPassengerCapacity();

}
