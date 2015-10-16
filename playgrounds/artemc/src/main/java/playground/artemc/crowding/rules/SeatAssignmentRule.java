/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.artemc.crowding.rules;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public interface SeatAssignmentRule
{
	/**
	 * This method will be called to determine whether a person that enters a vehicle gets a seat.
	 * @param person The person who just entered the vehicle
	 * @param vehicle The vehicle the person has entered
	 * @param numSitting The number of passengers that were sitting before this person entered.
	 * @param numStanding The number of passengers that were standing before this person entered.
	 * @return true if the person gets a seat, false if he has to stand.
	 */
	boolean getsSeatOnEnter(Id person, Vehicle vehicle, int numSitting, int numStanding);
	
	/**
	 * This method will be called if there are people standing and people who are sitting leave the vehicle. 
	 * @param person The person who is leaving the vehicle.
	 * @param vehicle The vehicle the person is leaving.
	 * @param numSitting The number of passengers sitting after the person has left the vehicle.
	 * @param standing A set containing passengers who are currently standing in the vehicle. 
	 * @return Either a person from the standing set, or null if no one will sit down.
	 */
	Id giveSeatOnLeave(Id person, Vehicle vehicle, int numSitting, List<Id> standing);
}
