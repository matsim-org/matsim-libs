/* *********************************************************************** *
 * project: matsim
 * Deprecated.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.dgrether.analysis.activity;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.Time;

/**"Container" for deprecated static methods.  For the moment, I have only one, "calculate(Some)Duration".
 * It is, with the api, now in principle a static utility method, but I don't like the way it calculates durations
 * from or to midnight.  Could move it into mfeil's playground (since he is the main user) but I would need
 * to fix maven dependencies in the playground.  Someone else could do this if desired.  kai, feb'11
 * 
 * @author nagel
 */
class DeprecatedStaticMethods {
	
	private DeprecatedStaticMethods() {}

	//	/**
	//	 * This method calculates the duration of the activity from the start and endtimes if set.
	//	 * If neither end nor starttime is set, but the duration is stored in the attribute of the
	//	 * class the duration is returned.
	//	 * If only start time is set, assume this is the last activity of the day.
	//	 * If only the end time is set, assume this is the first activity of the day.
	//	 * If the duration could neither be calculated nor the act.dur attribute is set to a value
	//	 * not equal to Time.UNDEFINED_TIME an exception is thrown.
	//	 * @return the duration in seconds
	//	 * @deprecated activities do not really start or end at midnight, and so this computation in my opinion does not make
	//	 * sense.  kai, feb'11
	//	 */
	//	@Deprecated
	//	private double calculateSomeDuration() {
	//		if ( true ) {
	//			return calculateSomeDuration( this ) ;
	//		} else {
	//			if ((this.getStartTime() == Time.UNDEFINED_TIME) && (this.getEndTime() == Time.UNDEFINED_TIME)) {
	//				if (this.getMaximumDuration() != Time.UNDEFINED_TIME) {
	//					return this.getMaximumDuration();
	//				}
	//				throw new IllegalArgumentException("No valid time set to calculate duration of activity: StartTime: " 
	//						+ this.getStartTime() + " EndTime : " + this.getEndTime()+ " Duration: " + this.getMaximumDuration());
	//			}
	//			//if only start time is set, assume this is the last activity of the day
	//			else if ((this.getStartTime() != Time.UNDEFINED_TIME) && (this.getEndTime() == Time.UNDEFINED_TIME)) {
	//				return Time.MIDNIGHT - this.getStartTime();
	//			}
	//			//if only the end time is set, assume this is the first activity of the day
	//			else if ((this.getStartTime() == Time.UNDEFINED_TIME) && (this.getEndTime() != Time.UNDEFINED_TIME)) {
	//				return this.getEndTime();
	//			}
	//			else {
	//				return this.getEndTime() - this.getStartTime();
	//			}
	//		}
	//	}
		@Deprecated
		 static double calculateSomeDuration(Activity act) {
			if ((act.getStartTime() == Time.UNDEFINED_TIME) && (act.getEndTime() == Time.UNDEFINED_TIME)) {
				if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
					return act.getMaximumDuration();
				}
				throw new IllegalArgumentException("No valid time set to calculate duration of activity: StartTime: " 
						+ act.getStartTime() + " EndTime : " + act.getEndTime()+ " Duration: " + act.getMaximumDuration());
			}
			//if only start time is set, assume this is the last activity of the day
			else if ((act.getStartTime() != Time.UNDEFINED_TIME) && (act.getEndTime() == Time.UNDEFINED_TIME)) {
				return Time.MIDNIGHT - act.getStartTime();
			}
			//if only the end time is set, assume this is the first activity of the day
			else if ((act.getStartTime() == Time.UNDEFINED_TIME) && (act.getEndTime() != Time.UNDEFINED_TIME)) {
				return act.getEndTime();
			}
			else {
				return act.getEndTime() - act.getStartTime();
			}
		}  // do not instantiate

}
