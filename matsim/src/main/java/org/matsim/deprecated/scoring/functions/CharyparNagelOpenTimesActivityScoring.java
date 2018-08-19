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

package org.matsim.deprecated.scoring.functions;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.OpeningTime;

import java.util.Iterator;
import java.util.Set;

/**
 * Same as CharyparNagelScoringFunction, but retrieves opening time information
 * from facility object of an activity instead of the config file.
 *
 * @author meisterk
 *
 */
@Deprecated // this version should not be used any more.  Instead the SumScoringFunction variant should be used.  kai, aug'18
public class CharyparNagelOpenTimesActivityScoring extends CharyparNagelActivityScoring {

	private final ActivityFacilities facilities;
	
	@Deprecated // this version should not be used any more.  Instead the SumScoringFunction variant should be used.  kai, aug'18
	public CharyparNagelOpenTimesActivityScoring(final ScoringParameters params, final ActivityFacilities facilities) {
		super(params);
		this.facilities = facilities;
	}

	@Override
	protected double[] getOpeningInterval(Activity act) {

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};

		boolean foundAct = false;

		ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());
		Iterator<String> facilityActTypeIterator = facility.getActivityOptions().keySet().iterator();
		String facilityActType = null;
		Set<OpeningTime> opentimes = null;

		while (facilityActTypeIterator.hasNext() && !foundAct) {

			facilityActType = facilityActTypeIterator.next();
			if (act.getType().equals(facilityActType)) {
				foundAct = true;

				opentimes = facility.getActivityOptions().get(facilityActType).getOpeningTimes();
				if (opentimes != null && !opentimes.isEmpty()) {
					// ignoring lunch breaks with the following procedure:
					// if there is only one open time interval, use it
					// if there are two or more, use the earliest start time and the latest end time
					openInterval[0] = Double.MAX_VALUE;
					openInterval[1] = Double.MIN_VALUE;

					for (OpeningTime opentime : opentimes) {

						openInterval[0] = Math.min(openInterval[0], opentime.getStartTime());
						openInterval[1] = Math.max(openInterval[1], opentime.getEndTime());
					}

				}

			}

		}

		if (!foundAct) {
			throw new RuntimeException("No suitable facility activity type found. Aborting...");
		}

		return openInterval;

	}



}
