/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationConstants.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import org.matsim.core.api.internal.MatsimParameters;

/**
 * An abstract class containing some constants used for evacuations.
 * 
 * @author cdobler
 */
public abstract class EvacuationConstants implements MatsimParameters {

	/**
	* Marker for Facilities where agents can be picked up or dropped off. 
	*/
	public static final String PICKUP_DROP_OFF_FACILITY_SUFFIX = "_pickup_dropoff";

	public static final String PICKUP_ACTIVITY = "pickup";
	public static final String DROP_OFF_ACTIVITY = "dropoff";
}
