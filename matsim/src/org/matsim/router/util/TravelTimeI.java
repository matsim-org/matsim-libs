/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeI.java
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

package org.matsim.router.util;

import org.matsim.network.Link;


/**
 * A simple interface to retrieve the travel time on links.
 * 
 * @author mrieser
 */
public interface TravelTimeI {

	/**
	 * Returns the travel time for the specified link at the specified time.
	 * 
	 * @param link The link for which the travel time is calculated.
	 * @param time The departure time (in seconds since 00:00) at the beginning 
	 * 		of the link for which the travel time is calculated.
	 * @return The time (in seconds) needed to travel over the link 
	 * 		<code>link</code>, departing at time <code>time</code>.
	 */
	public double getLinkTravelTime(Link link, double time);
}
