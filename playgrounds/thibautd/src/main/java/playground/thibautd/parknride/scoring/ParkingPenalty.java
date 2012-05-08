/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingPenalty.java
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
package playground.thibautd.parknride.scoring;

import org.matsim.api.core.v01.Coord;

/**
 * @author thibautd
 */
public interface ParkingPenalty {
	/**
	 * Notify that the vehicle is parked to a location requiring penalty.
	 * @param time the time of the parking (in seconds)
	 * @param coord the coordinates of the parking point
	 */
	public void park(
			final double time,
			final Coord coord);

	/**
	 * Notifies that the vehicle leaves the parking space
	 * @param time the time of the departure
	 */
	public void unPark(final double time);

	/**
	 * called when the whole plan is parsed
	 */
	public void finish();

	/**
	 * called after the finish() method
	 * @return the penalty (should be negative)
	 */
	public double getPenalty();

	/**
	 * resets the state.
	 */
	public void reset();
}

