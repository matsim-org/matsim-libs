/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.OptionalTime;

public class ActivityDurationUtils {

	/**
	 * 
	 * Contains rules about when to leave an Activity, considering the current time and the properties of the Activity.
	 * Specifically, determines how maximum duration and specific end time are played against each other.
	 * 
	 * @param act The Activity
	 * @param now The current simulation time
	 * @param activityDurationInterpretation The name of one of several rules of how to interpret Activity fields.
	 * @return The departure time
	 */
	
	public static double calculateDepartureTime(Activity act, double now, PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation) {
		OptionalTime endTime = PopulationUtils.decideOnActivityEndTime(act, now, activityDurationInterpretation);
		if (endTime.isUndefined()) {
			return Double.POSITIVE_INFINITY;
		} else {
			// we cannot depart before we arrived, thus change the time so the time stamp in events will be right
			//			[[how can events not use the simulation time?  kai, aug'10]]
			// actually, we will depart in (now+1) because we already missed the departing in this time step
			return Math.max(endTime.seconds(), now);
		}
	}
}
