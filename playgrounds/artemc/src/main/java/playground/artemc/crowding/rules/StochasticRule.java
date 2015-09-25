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
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.vehicles.Vehicle;

/**
 * The general idea of the StochasticRule is that people may not sit down,
 * even if there are still (some) seats left.
 * 
 * The rule defines the probabilities in a piecewise linear fashion.
 * It behaves as follows:
 * 
 * If 0-50% of the seats are taken, the probability to get a seat is 1.
 * If 50-75% of the seats are taken, the probability to get a seat is 1 to 0.75.
 * If 75-100% of the seats are taken, the probability to get a seat is 0.75 to 0.
 * 
 * When someone leaves the vehicle and people are standing, the same procedure
 * is used to determine if someone will sit down. If someone sits down,
 * he is picked uniformly at random from the people who are standing.
 * 
 * @author pbouman
 *
 */

public class StochasticRule implements SeatAssignmentRule {

	private Random random = MatsimRandom.getRandom();
	
	@Override
	public boolean getsSeatOnEnter(Id person, Vehicle vehicle, int numSitting,
			int numStanding) {
	
		double sitCap = (double) numSitting / (double) vehicle.getType().getCapacity().getSeats();
		if (sitCap < 0.5)
		{
			return true;
		}
		if (sitCap < 0.75)
		{
			return random.nextDouble() > 0.25;
		}
		return random.nextDouble() < 4 * (1 - sitCap);
	}

	@Override
	public Id giveSeatOnLeave(Id person, Vehicle vehicle, int numSitting,
			List<Id> standing) {
		
		if (getsSeatOnEnter(person, vehicle, numSitting, standing.size()))
		{
			return standing.get(random.nextInt(standing.size()));
		}
		return null;
	}

}
