/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterConfig.java
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

package playground.marcel.pt.router;

public class TransitRouterConfig {

	public double searchRadius = 1000.0;
	public double extensionRadius = 200.0;
	public double beelineWalkSpeed = 3.0/3.6;
//	public double marginalUtilityOfDistanceWalk = 0.0;
	public double marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0;
	public double marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0;
	public double costLineSwitch = 60.0 * -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle
}
