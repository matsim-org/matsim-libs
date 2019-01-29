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

package org.matsim.pt.transitSchedule;

import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * Describes the stop within a route of a transit line. Specifies also at
 * what time a headway is expected at the stop as offset from the route start.
 *
 * @author mrieser
 */
public class TransitRouteStopImpl implements TransitRouteStop {

	private TransitStopFacility stop;
	private final double departureOffset;
	private final double arrivalOffset;
	private boolean awaitDepartureTime = false;

	protected TransitRouteStopImpl(final TransitStopFacility stop, final double arrivalDelay, final double departureDelay) {
		this.stop = stop;
		this.departureOffset = departureDelay;
		this.arrivalOffset = arrivalDelay;
	}

	@Override
	public TransitStopFacility getStopFacility() {
		return this.stop;
	}

	@Override
	public void setStopFacility(final TransitStopFacility stopFacility) {
		this.stop = stopFacility;
	}

	@Override
	public double getDepartureOffset() {
		return this.departureOffset;
	}

	@Override
	public double getArrivalOffset() {
		return this.arrivalOffset;
	}

	@Override
	public boolean isAwaitDepartureTime() {
		return this.awaitDepartureTime;
	}

	@Override
	public void setAwaitDepartureTime(final boolean awaitDepartureTime) {
		this.awaitDepartureTime = awaitDepartureTime;
	}

	/**
	 * TransitRouteStops are typical Value Objects, so we consider two stops equal if they are equal field-wise.
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TransitRouteStopImpl)) {
			return false;
		}
		TransitRouteStopImpl other = (TransitRouteStopImpl) obj;
		if (this.stop == null) {
			if (other.getStopFacility() != null) {
				return false;
			}
		} else {
			if (!stop.equals(other.getStopFacility())) {
				return false;
			}
		}
		if (this.departureOffset != other.getDepartureOffset()) {
			return false;
		} 
		if (this.arrivalOffset != other.getArrivalOffset()) {
			return false;
		}
		if (this.awaitDepartureTime != other.isAwaitDepartureTime()) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return stop.hashCode();
	}
	
	@Override
	public String toString() {
		return "[TransitRouteStop stop=" + this.stop.getId() + " offset=" + this.departureOffset +" ]";
	}

}
