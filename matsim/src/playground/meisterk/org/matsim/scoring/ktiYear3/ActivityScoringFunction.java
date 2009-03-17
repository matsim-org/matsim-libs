/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityScoringFunction.java
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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.scoring.CharyparNagelScoringParameters;
import org.matsim.utils.misc.Time;


public class ActivityScoringFunction extends org.matsim.scoring.charyparNagel.ActivityScoringFunction {

	public ActivityScoringFunction(Plan plan,
			CharyparNagelScoringParameters params) {
		super(plan, params);
	}

	/* 
	 * Copied from org.matsim.scoring.CharyparNagelOpenTimesScoringFunction.
	 * 
	 * (non-Javadoc)
	 * @see org.matsim.scoring.charyparNagel.ActivityScoringFunction#getOpeningInterval(org.matsim.interfaces.core.v01.Activity)
	 */
	@Override
	protected double[] getOpeningInterval(Activity act) {
		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};

		boolean foundAct = false;

		Facility facility = act.getFacility();
		Iterator<String> activityOptions = facility.getActivityOptions().keySet().iterator();
		String facilityActType = null;
		Set<BasicOpeningTime> opentimes = null;

		while (activityOptions.hasNext() && !foundAct) {
			facilityActType = activityOptions.next();
			// first, look if an exact match of the activity type exists 
			if (act.getType().equals(facilityActType)) {
				foundAct = true;
			} 
			// second, look if a similar type exists
			else if (Pattern.matches(".*" + act.getType() + ".*", facilityActType)) {
				foundAct = true;
			}
		}

		if (foundAct) {
			// choose appropriate opentime:
			// either wed or wkday
			// use wednesday opening times if available
			opentimes = facility.getActivityOption(facilityActType).getOpeningTimes(DayType.wed);
			// if not, use wkday opening times
			if (opentimes == null) {
				opentimes = facility.getActivityOption(facilityActType).getOpeningTimes(DayType.wkday);
			}
			// if none is given, use undefined opentimes
			if (opentimes != null) {
				// ignoring multiple opening intervals on one day with the following procedure, that is:
				// use the earliest available opening time and the latest available closing time
				openInterval[0] = Double.MAX_VALUE;
				openInterval[1] = Double.MIN_VALUE;

				for (BasicOpeningTime opentime : opentimes) {
					openInterval[0] = Math.min(openInterval[0], opentime.getStartTime());
					openInterval[1] = Math.max(openInterval[1], opentime.getEndTime());
				}
			}
		} else {
			Gbl.errorMsg("Facility does not contain the activity option \"" + act.getType() + "\" or a similar one.");
		}

		return openInterval;
	}

}
