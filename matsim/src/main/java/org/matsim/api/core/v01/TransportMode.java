/* *********************************************************************** *
 * project: org.matsim.*
 * TransportMode.java
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

/**
 *
 */
package org.matsim.api.core.v01;

/**
 * Enumeration of frequently used modes of transportation in MATSim.
 *
 * @author mrieser
 */
public final class TransportMode {

	public static final String car = "car";
	public static final String ride = "ride";
	public static final String bike = "bike";
	public static final String pt = "pt";
	public static final String walk = "walk";
	public static final String transit_walk = "transit_walk";
	
	public static final String access_walk = "access_walk" ; // "bushwhacking" from facility to network
	public static final String egress_walk = "egress_walk" ; // "bushwhacking" from network to facility
	
	public static final String other = "other";

	private TransportMode() {
		// prevent creating instances of this class
	}

}
