/* *********************************************************************** *
 * project: org.matsim.*
 * ParkNRideConstants.java
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
package playground.thibautd.parknride;

import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;

/**
 * defines some constants for P n R
 * @author thibautd
 */
public class ParkAndRideConstants {
	private ParkAndRideConstants() {}

	/**
	 * Defines the name of the mode allowed on pnr "links"
	 */
	public static final String PARK_N_RIDE_LINK_MODE = "park_and_ride";

	/**
	 * type of a park and ride "interaction"
	 */
	public static final String PARKING_ACT = "park_n_ride_interaction";
	public static final StageActivityTypes PARKING_ACT_TYPE = new StageActivityTypesImpl( PARKING_ACT );

	public static final String PARK_N_RIDE_PLAN_TYPE = "parkAndRide";
}

