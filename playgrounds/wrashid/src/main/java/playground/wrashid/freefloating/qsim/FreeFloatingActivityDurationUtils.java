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

package playground.wrashid.freefloating.qsim;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.utils.misc.Time;

class FreeFloatingActivityDurationUtils {

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
	
	static double calculateDepartureTime(Activity act, double now, PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation) {
		if ( act.getMaximumDuration() == Time.UNDEFINED_TIME && (act.getEndTime() == Time.UNDEFINED_TIME)) {
			// yyyy does this make sense?  below there is at least one execution path where this should lead to an exception.  kai, oct'10
			return Double.POSITIVE_INFINITY ;
		} else {
			double departure = 0;
			if (activityDurationInterpretation.equals(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime)) {
				// person stays at the activity either until its duration is over or until its end time, whatever comes first
				if (act.getMaximumDuration() == Time.UNDEFINED_TIME) {
					departure = act.getEndTime();
				} else if (act.getEndTime() == Time.UNDEFINED_TIME) {
					departure = now + act.getMaximumDuration();
				} else {
					departure = Math.min(act.getEndTime(), now + act.getMaximumDuration());
				}
			} else if (activityDurationInterpretation.equals(PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly )) {
				if (act.getEndTime() != Time.UNDEFINED_TIME) {
					departure = act.getEndTime();
				} else {
					throw new IllegalStateException("activity end time not set and using something else not allowed.");
				}
			} else if (activityDurationInterpretation.equals(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration )) {
				// In fact, as of now I think that _this_ should be the default behavior.  kai, aug'10
				if ( act.getEndTime() != Time.UNDEFINED_TIME ) {
					departure = act.getEndTime();
				} else if ( act.getMaximumDuration() != Time.UNDEFINED_TIME ) {
					departure = now + act.getMaximumDuration() ;
				} else {
					throw new IllegalStateException("neither activity end time nor activity duration defined; don't know what to do.");
				}
			} else {
				throw new IllegalStateException("should not happen") ;
			}
	
			if (departure < now) {
				// we cannot depart before we arrived, thus change the time so the time stamp in events will be right
				//			[[how can events not use the simulation time?  kai, aug'10]]
				departure = now;
				// actually, we will depart in (now+1) because we already missed the departing in this time step
			}
			return departure;
		}
	}

}
