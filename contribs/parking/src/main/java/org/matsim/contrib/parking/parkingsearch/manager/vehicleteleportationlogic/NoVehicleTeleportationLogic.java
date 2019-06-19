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
package org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *	In this implementation, agents always walk back to their car, regardless of its location.
 *
 */
public class NoVehicleTeleportationLogic implements VehicleTeleportationLogic {

	/* (non-Javadoc)
	 * @see playground.jbischoff.parking.manager.vehicleteleportationlogic.VehicleTeleportationLogic#getVehicleLocation(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
	public Id<Link> getVehicleLocation(Id<Link> agentLinkId, Id<Vehicle> vehicleId, Id<Link> vehicleLinkId, double time, String mode) {
		return vehicleLinkId;
	}

}
