/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.scoring;

import java.util.Iterator;
import java.util.TreeSet;

import org.matsim.facilities.Facility;
import org.matsim.facilities.Opentime;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.Plan;
import org.matsim.utils.misc.Time;

/**
 * Same as CharyparNagelScoringFunction, but retrieves opening time information
 * from facility object of an activity instead of the config file.
 * 
 * @author meisterk
 *
 */
public class CharyparNagelOpenTimesScoringFunction extends
		CharyparNagelScoringFunction {

	public CharyparNagelOpenTimesScoringFunction(Plan plan) {
		super(plan);
	}

	@Override
	protected double[] getOpeningInterval(Act act) {

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};

		boolean foundAct = false;

		Facility facility = act.getFacility();
		Iterator<String> facilityActTypeIterator = facility.getActivities().keySet().iterator();
		String facilityActType = null;
		TreeSet<Opentime> opentimes = null;

		while (facilityActTypeIterator.hasNext() && !foundAct) {

			facilityActType = facilityActTypeIterator.next();
			if (act.getType().substring(0, 1).equals(facilityActType.substring(0, 1))) {
				foundAct = true;

				// choose appropriate opentime:
				// either wed or wkday
				// if none is given, use undefined opentimes
				opentimes = facility.getActivity(facilityActType).getOpentimes("wed");
				if (opentimes == null) {
					opentimes = facility.getActivity(facilityActType).getOpentimes("wkday");
				}
				if (opentimes != null) {
					// ignoring lunch breaks with the following procedure:
					// if there is only one wed/wkday open time interval, use it
					// if there are two or more, use the earliest start time and the latest end time
					openInterval[0] = Double.MAX_VALUE;
					openInterval[1] = Double.MIN_VALUE;
					
					for (Opentime opentime : opentimes) {

						openInterval[0] = Math.min(openInterval[0], opentime.getStartTime());
						openInterval[1] = Math.max(openInterval[1], opentime.getEndTime());
					}
					
				}

			}

		}

		if (!foundAct) {
			Gbl.errorMsg("No suitable facility activity type found. Aborting...");
		}

		return openInterval;

	}

	
	
}
