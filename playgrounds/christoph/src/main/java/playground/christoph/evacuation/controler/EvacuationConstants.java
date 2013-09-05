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

	/**
	 * Pick-up and drop-off activities.
	 */
	public static final String PICKUP_ACTIVITY = "pickup";
	public static final String DROP_OFF_ACTIVITY = "dropoff";
	
	/**
	 * Activity which agents perform outside the evacuation area.
	 */
	public static final String RESCUE_ACTIVITY = "rescue";
	
	/**
	 * Locations where agents perform a rescue activity.
	 */
	public static final String RESCUE_LINK = "rescueLink";
	public static final String RESCUE_FACILITY = "rescueFacility";
	
	/**
	 * Activity which agents perform outside the evacuation area.
	 */
	public static final String SECURE_ACTIVITY = "secure";
	
	/**
	 * Locations where agents perform a rescue activity.
	 */
	public static final String SECURE_LINK = "secureLink";
	public static final String SECURE_FACILITY = "secureFacility";
	
	/**
	 * Activity which agents perform to meet other household members.
	 */
	public static final String MEET_ACTIVITY = "meetHousehold";
	
	/**
	 * Household object attributes
	 */
	public static final String HOUSEHOLD_HHTP = "HHTP";
	public static final String HOUSEHOLD_HOMEFACILITYID = "homeFacilityId";
	public static final String HOUSEHOLD_MUNICIPALITY = "municipality";
	public static final String HOUSEHOLD_X_COORDINATE = "x";
	public static final String HOUSEHOLD_Y_COORDINATE = "y";
}
