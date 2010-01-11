/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.pt.queuesim;

import java.util.List;

import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;


/**
 * @author mrieser
 */
public interface PassengerAgent {

	/**
	 * Informs a passenger waiting at a stop that a transit line
	 * has arrived and is ready to be boarded.
	 *
	 * @param line the transit line that is available
	 * @param route the route being served
	 * @param stopsToCome the remaining stops on the route to be served by this vehicle before the trip ends
	 *
	 * @return <code>true<code> if the passenger wants to board the line, <code>false</code> otherwise
	 */
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, final List<TransitRouteStop> stopsToCome);

	/**
	 * Informs a passenger in a transit vehicle that the vehicle has
	 * arrived at the specified stop.
	 *
	 * @param stop the stop the vehicle arrived
	 *
	 * @return <code>true</code> if the passenger wants to exit the vehicle, <code>false</code> otherwise
	 */
	public boolean getExitAtStop(final TransitStopFacility stop);

}
