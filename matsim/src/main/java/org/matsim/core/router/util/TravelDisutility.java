/* *********************************************************************** *
 * project: org.matsim.*
 * TravelCost.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 * A simple interface to retrieve the disutility to travel on links.
 * <p></p>
 * Design comments:<ul>
 * <li>If I understand this correctly, implementations of this interface need to be thread-safe, since the same code may be called from different threads
 * simultaneously.  If I understand this correctly, this can only be achieved by never writing to fields in any methods besides the constructor.  kai, jan'13
 * </ul>
 * <p></p>
 * See {@link tutorial.programming.randomizingRouter.RunRandomizingRouterExample} for an example.
 *
 * @author mrieser
 * @see {@link org.matsim.core.router.costcalculators.TravelDisutilityFactory}
 */
public interface TravelDisutility {

	/**
	 * Returns the disutility to travel on the specified link at the specified time.
	 *
	 * @param link The link for which the travel disutility is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning of the link for which the disutility is calculated.
	 * @param person The person that wants to travel along the link. Note that this parameter can be <code>null</code>!
	 * @param vehicle The vehicle with which the person wants to travel along the link. Note that this parameter can be <code>null</code>!
	 * @return The disutility to travel over the link <code>link</code>, departing at time <code>time</code>.
	 */
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle);
	// There was some thought of replacing the "link" argument (resulting in a link travel time lookup) by a "linkTtime" argument (since
	// the link travel time is known in the router anyways. 
	// I have at least one argument why this will not carry through in an easy way: We also need the link argument for toll, and other
	// people may want to look up other link attributes and include them into their router, such as slope or road type.
	// kai, oct'13
	
	/**
	 * @param link the link for which the minimal travel disutility over all time slots is calculated
	 * @return Minimal costs to travel over the link <pre>link</pre>, departing at time <pre>time</pre>
	 */
	public double getLinkMinimumTravelDisutility(final Link link);

}
