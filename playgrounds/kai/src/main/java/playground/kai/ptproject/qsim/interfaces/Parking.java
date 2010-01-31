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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.QVehicle;

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
public class Parking {
	WaitQueue wq = null ;
	Map<Id,QVehicle> vehicles = null ;
	
	/**Receives the vehicle from the traffic link.  In this situation, it contains at least a driver, and possibly passengers. */
	void addVehicleFromTrafficLink( QVehicle veh ) {}
	
	/**Receives an empty vehicle.  Normally during initialization. */
	void addEmptyVehicle( QVehicle veh ) {
		vehicles.put( veh.getId(), veh ) ;
	}
	
	/**Receives a driver, typically from an activity. */
	void addDriver( DriverAgent driver ) {
		// person.getVehicleId ;
		Id vehId = null ; // dummy 
		QVehicle veh = vehicles.get( vehId ) ;
		veh.setDriver( driver ) ;
		wq.add( veh ) ;
	}
	
}
