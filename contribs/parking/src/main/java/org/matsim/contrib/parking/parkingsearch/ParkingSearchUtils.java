/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingsearch;

import org.matsim.api.core.v01.population.Activity;

/**
 * @author jbischoff, tschlenther, Ricardo Ewert
 */
public class ParkingSearchUtils {

	static public final String ParkingStageInteractionType = "parking";
	static public final String ParkingActivityType = "parking_activity";
	static public final String WaitingForParkingActivityType = "waitingForParkingSpace_activity";
	static public final int NO_OF_LINKS_TO_GET_ON_ROUTE = 5;


	public ParkingSearchUtils() {
	}

	/**
	 * Checks if the activity has parking while the activity.
	 *
	 * @param followingActivity
	 * @return
	 */
	public static boolean checkIfActivityHasNoParking(Activity followingActivity) {
		return followingActivity.getAttributes().getAsMap().containsKey("parking") && followingActivity.getAttributes().getAttribute(
			"parking").equals("noParking");

	}

	/**
	 * Sets that while this activity we simulate no parking activities.
	 *
	 * @param activity
	 */
	public static void setNoParkingForActivity(Activity activity) {
		activity.getAttributes().putAttribute("parking", "noParking");
	}

	/**
	 * This activity has a passenger interaction. This would mean that the location is fixed, and can not be changed.
	 *
	 * @param activity
	 */
	public static void setPassangerInteractionForActivity(Activity activity) {
		activity.getAttributes().putAttribute("parking", "PassangerInteraction");
	}

	/**
	 * Checks if the activity has a passanger interaction. This would mean that the location is fixed, and can not be changed.
	 *
	 * @param activity
	 * @return
	 */
	public static boolean checkIfActivityHasPassengerInteraction(Activity activity) {
		return activity.getAttributes().getAsMap().containsKey("parking") && activity.getAttributes().getAttribute(
			"parking").equals(
			"PassangerInteraction");
	}
}
