/* *********************************************************************** *
 * project: org.matsim.*
 * DistanceBasedFares.java
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

package playground.marcel.pt.fares;

import org.matsim.interfaces.core.v01.Facility;

import playground.marcel.pt.interfaces.TransitFares;

public class BeelineDistanceBasedFares implements TransitFares {

	private final double costPerKilometer;

	public BeelineDistanceBasedFares(final double costPerKilometer) {
		this.costPerKilometer = costPerKilometer;
	}

	public double getSingleTripCost(final Facility fromStop, final Facility toStop) {
		return this.costPerKilometer * (fromStop.calcDistance(toStop.getCenter()) / 1000.0);
	}

}
