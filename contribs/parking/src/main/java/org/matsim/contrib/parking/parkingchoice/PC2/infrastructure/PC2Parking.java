/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingchoice.PC2.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public interface PC2Parking {

	public Id<PC2Parking> getId();

//	public int getMaximumParkingCapacity();

	public int getAvailableParkingCapacity();

	public void parkVehicle();

	public void unparkVehicle();
	
	public double getCost(Id<Person> agentId, double arrivalTime, double parkingDurationInSecond);
	
	public Coord getCoordinate();

	public String getGroupName();
	
	public void resetAvailability();
	
}
