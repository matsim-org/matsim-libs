/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.ffcs.manager;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */

public interface FreefloatingCarsharingManager {
	/**
	 * @param Leg leg: We need to decide whether carsharing is an option BOTH for origin and destination
	 * @param PersonId: Availability may be person dependent
	 * @param double time: Availability may be time dependent
	 */
	Tuple<Id<Link>,Id<Vehicle>> findAndReserveFreefloatingVehicleForLeg(Leg leg, Id<Person> personId, double time);
	boolean endRental(Id<Link> linkId, Id<Person> personId, Id<Vehicle> vehicleId, double time);
	void reset(int iteration);
	
	public Map<Id<Vehicle>, Id<Link>> getIdleVehicleLocations();
	/**
	 * 
	 * @param vehicleId
	 * @return whether the vehicle is a freefloating vehicle managed here.
	 */
	boolean isFFCSVehicle(Id<Vehicle> vehicleId);

}
