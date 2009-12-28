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

package playground.mrieser.pt.fares;

import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.mrieser.pt.fares.api.TransitFares;

public class BeelineDistanceBasedFares implements TransitFares {

	private final double costPerKilometer;

	public BeelineDistanceBasedFares(final double costPerKilometer) {
		this.costPerKilometer = costPerKilometer;
	}

	public double getSingleTripCost(final ActivityFacilityImpl fromStop, final ActivityFacilityImpl toStop) {
		return this.costPerKilometer * (fromStop.calcDistance(toStop.getCoord()) / 1000.0);
	}

}
