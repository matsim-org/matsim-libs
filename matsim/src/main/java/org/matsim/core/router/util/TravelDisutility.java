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

/**
 * A simple interface to retrieve the disutility to travel on links.
 *
 * @author mrieser
 */
public interface TravelDisutility {

	/**
	 * Returns the disutility to travel on the specified link at the specified time.
	 *
	 * @param link The link for which the travel disutility is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning of the link for which the disutility is calculated.
	 * @return The disutility to travel over the link <code>link</code>, departing at time <code>time</code>.
	 */
	public double getLinkTravelDisutility(Link link, double time);
	
	/**
	 * @param the link for which the minimal travel disutility over all time slots is calculated
	 * @return Minimal costs to travel over the link <pre>link</pre>, departing at time <pre>time</pre>
	 */
	public double getLinkMinimumTravelDisutility(Link link);

}
