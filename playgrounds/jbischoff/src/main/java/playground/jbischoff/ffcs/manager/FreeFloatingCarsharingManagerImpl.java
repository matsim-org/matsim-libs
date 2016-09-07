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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.jbischoff.ffcs.FFCSConfigGroup;
import playground.jbischoff.parking.manager.ParkingManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FreeFloatingCarsharingManagerImpl implements FreefloatingCarsharingManager {

	
	/**
	 * 
	 */
	@Inject
	public FreeFloatingCarsharingManagerImpl(Scenario scenario, ParkingManager parkingManager, FFCSConfigGroup ffcsconfig) {
		
		
	}
	
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager#findAndReserveFreefloatingVehicleForLeg(org.matsim.api.core.v01.population.Leg, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public Tuple<Id<Link>, Id<Vehicle>> findAndReserveFreefloatingVehicleForLeg(Leg leg, Id<Person> personId, double time) {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager#endRental(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean endRental(Id<Link> linkId, Id<Person> personId, Id<Vehicle> vehicleId, double time) {
		// TODO Auto-generated method stub
		return false;
	}

}
