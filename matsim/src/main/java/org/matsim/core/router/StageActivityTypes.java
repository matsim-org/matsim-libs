/* *********************************************************************** *
 * project: org.matsim.*
 * StageActivityList.java
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
package org.matsim.core.router;

/**
 * Represents a list of activity types corresponding to stages in a trip.
 * A trip is defined as the longest sequence of PlanElements containing only
 * legs and stage-type activities.
 * <br>
 * It is used by the {@link TripRouter} to detect trips, and can be used by
 * replanning modules as a "black list" of activities not to touch.
 * <br>
 * Equals and hashCode methods should be implemented so that two instances
 * of the same implementation returning the same results are considered equal.
 * If it is not the case, replacement of a routing module may not work as expected.
 *
 * @author thibautd
 */
public interface StageActivityTypes {
	/**
	 * Checks whether an activity type is a stage type.
	 *
	 * @param activityType the type to check
	 * @return true if the activity type is a stage type.
	 */
	public boolean isStageActivity(String activityType);
}

