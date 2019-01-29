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

package org.matsim.pt.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser / senozon
 */
public interface TransitTravelDisutility {

	/**
	 * Returns the disutility to travel on the specified link at the specified time.
	 *
	 * @param link The link for which the travel disutility is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning of the link for which the disutility is calculated.
	 * @param person The person that wants to travel along the link. Note that this parameter can be <code>null</code>!
	 * @param vehicle The vehicle with which the person wants to travel along the link. Note that this parameter can be <code>null</code>!
	 * @param dataManager A helper class to enable the cost calculator store arbitrary data during one routing request.
	 * @return The disutility to travel over the link <code>link</code>, departing at time <code>time</code>.
	 */
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager);
	
	/**
	 * This is used for walking to and from the nearest transit stop from the start and end location,
	 * as well as for the "direct" walk from start to finish without using a pt line at all.
	 * It is not used for transfer links (these are handled by the transitTravelDisutility).
	 */
	double getWalkTravelTime(Person person, Coord coord, Coord toCoord);

	double getWalkTravelDisutility(Person person, Coord coord, Coord toCoord);
	
	
}
