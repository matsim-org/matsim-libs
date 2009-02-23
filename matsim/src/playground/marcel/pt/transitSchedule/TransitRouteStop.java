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

import org.matsim.facilities.Facility;

public class TransitRouteStop {

	private final Facility stop;
	private final double departureDelay;
	private final double arrivalDelay;

	public TransitRouteStop(final Facility stop, final double departureDelay, final double arrivalDelay) {
		this.stop = stop;
		this.departureDelay = departureDelay;
		this.arrivalDelay = arrivalDelay;
	}

	public Facility getStopFacility() {
		return this.stop;
	}

	public double getDepartureDelay() {
		return this.departureDelay;
	}

	public double getArrivalDelay() {
		return this.arrivalDelay;
	}
}
