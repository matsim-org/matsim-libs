/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public interface Parking {

	public Id<Parking> getId();

	public int getMaximumParkingCapacity();

	public int getAvailableParkingCapacity();

	public void parkVehicle(Id agentId);

	public void unparkVehicle(Id agentId);
	
	public double getCost(Id agentId, double arrivalTime, double parkingDurationInSecond);
	
	public Coord getCoordinate();
}
