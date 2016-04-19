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

package playground.singapore.scoring;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.OpeningTime;

import playground.singapore.springcalibration.run.RunSingapore;

/**
 * Same as CharyparNagelScoringFunction, but retrieves opening time information
 * from facility object of an activity instead of the config file.
 *
 * @author meisterk
 *
 */
public class CharyparNagelOpenTimesActivityScoring extends CharyparNagelActivityScoring {

	private final ActivityFacilities facilities;
	private final static Logger log = Logger.getLogger(RunSingapore.class);
	
	public CharyparNagelOpenTimesActivityScoring(final CharyparNagelScoringParameters params, final ActivityFacilities facilities) {
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
		
		if (facility == null) {
			log.error("facility " + act.getFacilityId() + " is null for link + " + act.getLinkId().toString() + " and end time " + act.getEndTime());
		}
				
		Iterator<String> facilityActTypeIterator = facility.getActivityOptions().keySet().iterator();
		String facilityActType = null;
		Set<OpeningTime> opentimes = null;

		while (facilityActTypeIterator.hasNext() && !foundAct) {

			facilityActType = facilityActTypeIterator.next();
			if (act.getType().equals(facilityActType)) {
				foundAct = true;

				// choose appropriate opentime:
				// either wed or wkday
				// if none is given, use undefined opentimes
				opentimes = facility.getActivityOptions().get(facilityActType).getOpeningTimes();
				if (opentimes != null && opentimes.size()>0) {
					// ignoring lunch breaks with the following procedure:
					// if there is only one wed/wkday open time interval, use it
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
			return new double[]{0, Double.MAX_VALUE};
		}

		return openInterval;

	}



}
