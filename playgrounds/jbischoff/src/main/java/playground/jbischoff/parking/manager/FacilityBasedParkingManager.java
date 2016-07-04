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
package playground.jbischoff.parking.manager;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FacilityBasedParkingManager implements ParkingManager {

	ActivityFacilities parkingFacilities;
	
	/**
	 * 
	 */
	@Inject
	public FacilityBasedParkingManager() {
		// TODO Auto-generated constructor stub
	}
	
	
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#canVehicleParkHere(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
	public boolean canVehicleParkHere(Id<Vehicle> vehicleId, Id<Link> linkId) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#getVehicleParkingLocation(org.matsim.api.core.v01.Id)
	 */
	@Override
	public Id<Link> getVehicleParkingLocation(Id<Vehicle> vehicleId) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#parkVehicleHere(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.ParkingManager#unParkVehicleHere(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, double)
	 */
	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		// TODO Auto-generated method stub
		return false;
	}

}
