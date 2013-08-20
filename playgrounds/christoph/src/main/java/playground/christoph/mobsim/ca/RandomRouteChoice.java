/* *********************************************************************** *
 * project: org.matsim.*
 * RandomRouteChoice.java
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

package playground.christoph.mobsim.ca;

import java.util.Deque;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;

import playground.christoph.mobsim.ca.CAAgent.ParkingMemory;

public class RandomRouteChoice implements RouteChoice {

	private final Random random;
	
	public RandomRouteChoice() {
		this.random = MatsimRandom.getLocalInstance();
	}
	
	public CALink chooseLink(Coord currentPosition, CANode destination, 
			Deque<ParkingMemory> shortTermMemory, List<CALink> linkAlternatives) {
        int rand = random.nextInt(linkAlternatives.size());
        return linkAlternatives.get(rand);
	}
}
