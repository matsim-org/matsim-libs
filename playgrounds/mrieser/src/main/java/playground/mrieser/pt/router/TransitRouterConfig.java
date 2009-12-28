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

package playground.mrieser.pt.router;

public class TransitRouterConfig {

	/**
	 * The distance in meters in which stop facilities should be searched for
	 * around the start and end coordinate.
	 */
	public double searchRadius = 1000.0;
	
	/**
	 * If no stop facility is found around start or end coordinate (see 
	 * {@link #searchRadius}), the nearest stop location is searched for
	 * and the distance from start/end coordinate to this location is
	 * extended by the given amount.<br />
	 * If only one stop facility is found within {@link #searchRadius},
	 * the radius is also extended in the hope to find more stop
	 * facilities (e.g. in the opposite direction of the already found
	 * stop). 
	 */
	public double extensionRadius = 200.0;
	
	/**
	 * The distance in meters that agents can walk to get from one stop to 
	 * another stop of a nearby transit line.
	 */
	public double beelineWalkConnectionDistance = 100.0;
	
	/**
	 * Walking speed of agents on transfer links, beeline distance.
	 */
	public double beelineWalkSpeed = 3.0/3.6;
	
	public double marginalUtilityOfTravelTimeWalk = -6.0 / 3600.0;
	
	public double marginalUtilityOfTravelTimeTransit = -6.0 / 3600.0;
	
	/**
	 * The additional costs to be added when an agent switches lines.
	 */
	public double costLineSwitch = 60.0 * -this.marginalUtilityOfTravelTimeTransit; // == 1min travel time in vehicle
}
