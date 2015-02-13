/* *********************************************************************** *
 * project: org.matsim.*
 * TransitFares.java
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

package playground.mrieser.pt.fares.api;

import org.matsim.facilities.ActivityFacilityImpl;

public interface TransitFares {

	/**
	 * Returns the cost for a single trip from one stop to another stop.
	 *
	 * @param fromStop
	 * @param toStop
	 * @return cost for single trip
	 */
	public double getSingleTripCost(final ActivityFacilityImpl fromStop, final ActivityFacilityImpl toStop); // TODO [MR] how to handle different paths between from/to? and what about time of day?

	/**
	 * Returns the total cost for multiple trips. This could allow the agents to choose a ticket
	 * that's valid the whole day (instead of only a single trip).
	 *
	 * @return combined cost for multiple trips.
	 */
//	public double getCombinedTripCost(); // TODO_ [MR] define arguments required for this.

}
