/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class AutonomousVehicle {

	private Coord myPosition;
	private int arrivalTime;

	protected AutonomousVehicle(Coord initialCoords) {
		myPosition = initialCoords;
		arrivalTime = -1;
	}

	public Coord getMyPosition() {
		return new CoordImpl(myPosition.getX(), myPosition.getY());
	}

	/**
	 * Moves AV to the new position and returns the travel time.
	 * @param newPosition
	 * @return travelTime
	 */
	public double moveTo(Coord newPosition) {
		double distance = CoordUtils.calcDistance(myPosition, newPosition) * Constants.getBeelineFactorStreet();
		myPosition = newPosition;
		return distance / Constants.getAvSpeed();
	}

	/**
	 * If the vehicle is in use, get time when it's free again.
	 *
	 * @return -1 if not in use, the arrival time else.
	 */
	public int getArrivalTime() {
		return arrivalTime;
	}

	public void setArrivalTime(int arrivalTime) {
		if (arrivalTime > 0 || arrivalTime == -1) {
			this.arrivalTime = arrivalTime;
		} else {
			throw new IllegalArgumentException("illegal arrival time");
		}
	}

}
