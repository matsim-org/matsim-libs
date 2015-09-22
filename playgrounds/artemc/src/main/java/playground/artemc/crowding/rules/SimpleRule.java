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

/**
 * 
 * This is a very simple seat assignment rule.
 * 
 * If someone enters the vehicle, he can sit down if there are seats left.
 * If someone leaves the vehicle, and someone is standing, the person who
 * has been standing for the longest time will sit down.
 * 
 * @author pbouman
 *
 */

public class SimpleRule implements SeatAssignmentRule {

	@Override
	public boolean getsSeatOnEnter(Id person, Vehicle vehicle, int numSitting,
			int numStanding) {
		if (vehicle.getType().getCapacity().getSeats() > numSitting) {
			return true;
		}
		return false;
	}

	@Override
	public Id giveSeatOnLeave(Id person, Vehicle vehicle, int numSitting,
			List<Id> standing) {
		if (!standing.isEmpty())
		{
			return standing.get(0);
		}
		return null;
	}

}
