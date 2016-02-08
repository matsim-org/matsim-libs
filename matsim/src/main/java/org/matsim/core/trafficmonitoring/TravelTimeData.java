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
	
	/** Selectively reset the travel time accounting for a certain time slot.  This may be necessary for some version of
	 * "consolidateData", but right now it is not needed.  kai, oct'11
	 */
//	public abstract void resetTravelTime( final int timeSlot ) ;

	abstract void addTravelTime(final int timeSlot, final double traveltime);
	
	/**
	 * A method to set the travel time directly, to handle some special cases.
	 */
	abstract void setTravelTime( final int timeSlot, final double traveltime ) ;

	/**Returns the travel time (presumably in seconds), given both the time slot and the time-of-day for the request.
	 * <p/>
	 * Design thoughts:<ul>
	 * <li> yy It does not make sense to me that both "timeSlot" and "now" are needed.  I guess that historically it was
	 * "timeSlot", and then "now" was needed for the time-dependent network.  But I would think that the conversion
	 * from "now" to "timeSlot" should be done inside the data class (implementation hiding) and so the "timeSlot" 
	 * argument should be removed.  kai, may'2011 </li>
	 * <li>Indead, this doesn't make sense at a quick glance. This is an optimization that saves lots of memory. For each
	 * link of the network a TravelTimeData Object is created. Each of these objects would need one (Map implementation) 
	 * or two (Array implementation) additional fields that hold the information that is required to calculate the time slot
	 * from the time of day. dg, april'2013. </li>
	 * <li> Well, one could insert the algo to compute the index from the time-of-day into the TravelTimeData implementing class. kai, jan'16
	 * <li> The main reason by both are needed seems to be the getFreeSpeed(...) at some point, which needs "now" as an argument.  
	 * We could, alternatively, return some "undefined" travel time (or an infinite standard deviation), and then now somewhere else
	 * that we need to use free speed as the alternative.  kai, jan'16
	 * </ul> 
	 */
	abstract double getTravelTime(final int timeSlot, final double now);

}