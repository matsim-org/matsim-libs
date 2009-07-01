/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouteStop.java
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

package playground.marcel.pt.transitSchedule;

import org.matsim.transitSchedule.TransitStopFacility;

/**
 * Describes the stop within a route of a transit line. Specifies also at
 * what time a headway is expected at the stop as offset from the route start.
 * 
 * @author mrieser
 */
public class TransitRouteStopImpl {

	private final TransitStopFacility stop;
	private final double departureDelay;
	private final double arrivalDelay;

	public TransitRouteStopImpl(final TransitStopFacility stop, final double arrivalDelay, final double departureDelay) {
		this.stop = stop;
		this.departureDelay = departureDelay;
		this.arrivalDelay = arrivalDelay;
	}

	public TransitStopFacility getStopFacility() {
		return this.stop;
	}

	public double getDepartureDelay() {
		return this.departureDelay;
	}

	public double getArrivalDelay() {
		return this.arrivalDelay;
	}
}
