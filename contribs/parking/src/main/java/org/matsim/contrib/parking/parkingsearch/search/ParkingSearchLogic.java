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

package org.matsim.contrib.parking.parkingsearch.search;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */
public interface ParkingSearchLogic {
	/**
	 * currentLinkId link last visited
	 * @param vehicleId vehicleId
	 */
	Id<Link> getNextLink(Id<Link> currentLinkId, Id<Vehicle> vehicleId, String mode);
	
	/**
	 * fixed route search strategies (i.e. find the next carsharing parking lot) might require a reset once search is completed
	 */
	void reset();
}
