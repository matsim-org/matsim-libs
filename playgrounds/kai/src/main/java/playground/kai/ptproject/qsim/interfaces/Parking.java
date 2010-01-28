/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kai.ptproject.qsim.interfaces;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.jdeqsim.Vehicle;

/**<p>
 * This is there to contain "vehicles".  
 * </p><p>
 * If the vehicle comes with persons, these persons are passed on
 * to where they belong.  
 * </p><p>
 * If a person comes without a vehicle, the appropriate vehicle is retrieved and the vehicle
 * is passed on with the driver.
 * </p><p>
 * In my view, it is NOT necessary to load passengers (for ride sharing) at this point, since these are (presumably) much easier
 * to load with the transit vehicle logic.
 * </p><p>
 * The container type should probably be of type Map, since the typical lookup is by vehicle Id.
 * </p>
 * @author nagel
 *
 */
@Deprecated // do not yet use
public interface Parking {
	
	/**Receives the vehicle from the traffic link.  In this situation, it contains at least a driver, and possibly passengers.
	 * 
	 * @param veh
	 * @return
	 */
	boolean addVehicleFromTrafficLink( Vehicle veh ) ;
	
	/**Receives an empty vehicle.  Normally during initialization.
	 * 
	 * @param veh
	 * @return
	 */
	boolean addEmptyVehicle( Vehicle veh ) ;
	
	/**Receives a driver, typically from an activity.
	 * 
	 * @param person
	 * @return
	 */
	boolean addDriver( Person person ) ;
	// Should this be a person?  Or a DriverAgent?  Or a QPerson?

	
	// If we assume that the departure sequence is:
	// - agent enters parking facility, 
	// - enters vehicle, and 
	// - departs, 
	// then a "removeVehicle" command is not needed, since this can be done inside
	// the driver departure
	
}
