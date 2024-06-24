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

package org.matsim.pt.transitSchedule.api;

import org.matsim.core.utils.misc.OptionalTime;

/**
 * Describes the stop within a route of a transit line. Specifies also at
 * what time a headway is expected at the stop as offset from the route start.
 *
 * @author mrieser
 */
public interface TransitRouteStop {

	public abstract TransitStopFacility getStopFacility();

	public abstract void setStopFacility(final TransitStopFacility stopFacility);

	public abstract OptionalTime getDepartureOffset();

	public abstract OptionalTime getArrivalOffset();

	/** @return <code>true</code> if agents are allowed to board the transit vehicle at this route stop. */
	boolean isAllowBoarding();

	void setAllowBoarding(boolean allowBoarding);

	/** @return <code>true</code> if agents are allowed to exit the transit vehicle at this route stop. */
	boolean isAllowAlighting();

	void setAllowAlighting(boolean allowAlighting);

	/**
	 * Specifies if a driver should wait until the specified departure time
	 * has come before departing, especially if the driver is too early at
	 * the stop. Requires that a departure offset is set for this stop.
	 *
	 * @param awaitDepartureTime <code>true</code> if the driver should wait if too early
	 * @see #getDepartureOffset()
	 */
	public abstract void setAwaitDepartureTime(final boolean awaitDepartureTime);

	/**
	 * Returns whether a driver should wait until the specified departure
	 * time has come before departing, especially if the driver is too
	 * early at the stop. Can only be interpreted if a departure offset
	 * is set for this stop.
	 *
	 * @return <code>true</code> if drivers should wait until the departure
	 * time has passed before departing, <code>false</code> otherwise.
	 * @see #getDepartureOffset()
	 */
	public abstract boolean isAwaitDepartureTime();

	interface Builder<B extends Builder<B>> {
		Builder<B> stop(TransitStopFacility val);

		Builder<B> departureOffset(double val);

		Builder<B> arrivalOffset(double val);

		Builder<B> awaitDepartureTime(boolean val);

		Builder<B> allowBoarding(boolean val);

		Builder<B> allowAlighting(boolean val);

		TransitRouteStop build();
	}
}
