/* *********************************************************************** *
 * project: org.matsim.*
 * SpotWeighter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.hitchiking.spotweights;

import org.matsim.api.core.v01.Id;

/**
 * Gives weights to spots to guide search.
 * The weights only depend on origin, destination and
 * time of day, and not on the characteristics of the trip.
 * @author thibautd
 */
public interface SpotWeighter {
	/**
	 * @param departureTime
	 * @param originLink
	 * @param destinationLink the destination of the trip (not the drop of point)
	 * @return
	 */
	public double weightDriverOrigin(
			double departureTime,
			Id originLink,
			Id destinationLink);

	/**
	 * @param departureTime
	 * @param originLink
	 * @param dropOffLink the exact drop off link
	 * @return
	 */
	public double weightPassengerOrigin(
			double departureTime,
			Id originLink,
			Id dropOffLink);
}

