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

package org.matsim.core.mobsim.qsim.pt;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author mrieser
 */
public interface PTPassengerAgent extends PassengerAgent {

	/**
	 * Informs a passenger waiting at a stop that a transit line
	 * has arrived and is ready to be boarded.
	 *
	 * @param line the transit line that is available
	 * @param stopsToCome the remaining stops on the route to be served by this vehicle before the trip ends
	 * @param transitVehicle TODO
	 * @param route the route being served
	 * @return <code>true<code> if the passenger wants to board the line, <code>false</code> otherwise
	 */
	public boolean getEnterTransitRoute(final TransitLine line, final TransitRoute transitRoute, 
			final List<TransitRouteStop> stopsToCome, TransitVehicle transitVehicle);

	/**
	 * Informs a passenger in a transit vehicle that the vehicle has
	 * arrived at the specified stop.
	 *
	 * @param stop the stop the vehicle arrived
	 *
	 * @return <code>true</code> if the passenger wants to exit the vehicle, <code>false</code> otherwise
	 */
	public boolean getExitAtStop(final TransitStopFacility stop);
	
	/**
	 * Asks a passenger which is departing on a transit leg about the stop it wants to use for accessing the transit line.
	 * 
	 * @return The transit stop id.
	 */
	public Id<TransitStopFacility> getDesiredAccessStopId();
	
	public Id<TransitStopFacility> getDesiredDestinationStopId();

	/**
	 * @return a statistical weight, how many "real" agents this agent represents, e.g. "5.0" if you simulate a 20%-sample.
	 */
	public double getWeight();

}
