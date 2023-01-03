/* *********************************************************************** *
 * project: org.matsim.*
 * TurningMoveTravelTime
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;



/**
 * Interface for travel time queries that include the link travel time and the
 * time needed for the turning move.
 * @author dgrether
 *
 */
public interface LinkToLinkTravelTime {

	/**
	 * Returns the travel time at the specified time for the given fromLink inclusive the 
	 * turning move time to enter the given toLink.
	 *
	 * @param fromLink The link for which the travel time is calculated.
	 * @param toLink The link that is entered from the fromLink the turning
	 * move time is calculated for the fromLink toLink relationship.
	 * @param time The departure time (in seconds since 00:00) at the beginning
	 * 		of the fromLink for which the travel time is calculated.
	 * @return The time (in seconds) needed to travel over the fromLink
	 * 		<code>fromLink</code> and enter the toLink <code>toLink</code>, 
	 * departing at time <code>time</code>.
	 */
	public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time, Person person, Vehicle vehicle);

}