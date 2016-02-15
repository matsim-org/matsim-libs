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

/**
 * 
 */
package playground.johannes.gsv.sim;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

import java.util.LinkedList;

/**
 * @author johannes
 *
 */
public interface LegSimEngine {

	public double simulate(Person person, Leg leg, double departureTime, LinkedList<Event> eventList);
	
}
