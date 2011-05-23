/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeRole.java
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

package org.matsim.core.trafficmonitoring;

public interface TravelTimeData {

	public abstract void resetTravelTimes();

	abstract void addTravelTime(final int timeSlot, final double traveltime);

	/**Returns the travel time (presumably in seconds), given both the time slot and the time-of-day for the request.
	 * <p/>
	 * Design thoughts:<ul>
	 * <li> yy It does not make sense to me that both "timeSlot" and "now" are needed.  I guess that historically it was
	 * "timeSlot", and then "now" was needed for the time-dependent network.  But I would think that the conversion
	 * from "now" to "timeSlot" should be done inside the data class (implementation hiding) and so the "timeSlot" 
	 * argument should be removed.  kai, may'2011
	 * </ul> 
	 */
	abstract double getTravelTime(final int timeSlot, final double now);

}