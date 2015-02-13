/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.rc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.OpeningTime;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class RCActivityScoringFunction extends CharyparNagelActivityScoring {
	static final Logger log = Logger.getLogger(RCActivityScoringFunction.class);	
	private final ActivityFacilities facilities;
	private Person person;
	private final CharyparNagelScoringParameters params;
	public static final int DEFAULT_PRIORITY = 1;
	private final HashMap<String, Double> zeroUtilityDurations = new HashMap<String, Double>();
	private ObjectAttributes prefs;
		
	public RCActivityScoringFunction(Person person, final CharyparNagelScoringParameters params, final ActivityFacilities facilities,
			ObjectAttributes prefs) {
		super(params);
		this.facilities = facilities; 
		this.person = person;
		this.params = params;
		this.prefs = prefs;
	}
		
	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {
		
		if (act.getType().equals("pt interaction")) return 0.0;

		double tmpScore = 0.0;

			/* Calculate the times the agent actually performs the
			 * activity.  The facility must be open for the agent to
			 * perform the activity.  If it's closed, but the agent is
			 * there, the agent must wait instead of performing the
			 * activity (until it opens).
			 *
			 *                                             Interval during which
			 * Relationship between times:                 activity is performed:
			 *
			 *      O________C A~~D  ( 0 <= C <= A <= D )   D...D (not performed)
			 * A~~D O________C       ( A <= D <= O <= C )   D...D (not performed)
			 *      O__A+++++C~~D    ( O <= A <= C <= D )   A...C
			 *      O__A++D__C       ( O <= A <= D <= C )   A...D
			 *   A~~O++++++++C~~D    ( A <= O <= C <= D )   O...C
			 *   A~~O+++++D__C       ( A <= O <= D <= C )   O...D
			 *
			 * Legend:
			 *  A = arrivalTime    (when agent gets to the facility)
			 *  D = departureTime  (when agent leaves the facility)
			 *  O = openingTime    (when facility opens)
			 *  C = closingTime    (when facility closes)
			 *  + = agent performs activity
			 *  ~ = agent waits (agent at facility, but not performing activity)
			 *  _ = facility open, but agent not there
			 *
			 * assume O <= C
			 * assume A <= D
			 */

			double[] openingInterval = this.getOpeningInterval(act);
			double openingTime = openingInterval[0];
			double closingTime = openingInterval[1];

			double activityStart = arrivalTime;
			double activityEnd = departureTime;

			if ((openingTime >=  0) && (arrivalTime < openingTime)) {
				activityStart = openingTime;
			}
			if ((closingTime >= 0) && (closingTime < departureTime)) {
				activityEnd = closingTime;
			}
			if ((openingTime >= 0) && (closingTime >= 0)
					&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
				// agent could not perform action
				activityStart = departureTime;
				activityEnd = departureTime;
			}
			double duration = activityEnd - activityStart;

			// disutility if too early
			if (arrivalTime < activityStart) {
				// agent arrives to early, has to wait
				tmpScore += this.params.marginalUtilityOfWaiting_s * (activityStart - arrivalTime);
			}

			// disutility if too late

			double latestStartTime = (Double)this.prefs.getAttribute(person.getId().toString(), "latestStartTime_" + act.getType());	
			if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
				tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
			}

			// utility of performing an action, duration is >= 1, thus log is no problem
			double typicalDuration = (Double)this.prefs.getAttribute(person.getId().toString(), "typicalDuration_" + act.getType());			
			// initialize zero utility durations here for better code readability, because we only need them here
			double zeroUtilityDuration;
			if (this.zeroUtilityDurations.containsKey(act.getType())) {
				zeroUtilityDuration = this.zeroUtilityDurations.get(act.getType());
			} else {
				zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / RCActivityScoringFunction.DEFAULT_PRIORITY);
				this.zeroUtilityDurations.put(act.getType(), zeroUtilityDuration);
			}
			
			// disutility if stopping too early
			double earliestEndTime = (Double)this.prefs.getAttribute(person.getId().toString(), "earliestEndTime_" + act.getType());	
			if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
			}

			// disutility if duration was too short
			double minimalDuration = (Double)this.prefs.getAttribute(person.getId().toString(), "minimalDuration_" + act.getType());	
			if ((minimalDuration >= 0) && (duration < minimalDuration)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
			}

			if (duration > 0) {
				
				double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
						* Math.log((duration / 3600.0) / zeroUtilityDuration);
				
				double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
				tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
				
			} else {
				tmpScore += 2*this.params.marginalUtilityOfLateArrival_s*Math.abs(duration);
			}
		return tmpScore;
	}
	
	
	@Override
	protected double[] getOpeningInterval(Activity act) {
		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};
		boolean foundAct = false;
		
		if (act.getType().equals("pt interaction") || 
				(act.getType().startsWith("h")) || 
				act.getType().equals("home")) {
			return openInterval;
		} // pt interaction and home always open
		
		ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());		
		Iterator<String> facilityActTypeIterator = facility.getActivityOptions().keySet().iterator();
		String facilityActType = null;
		Set<OpeningTime> opentimes = null;

		while (facilityActTypeIterator.hasNext() && !foundAct) {
			facilityActType = facilityActTypeIterator.next();
			if (act.getType().equals(facilityActType)) { // TODO: check here actType conversions
				foundAct = true;
				// choose appropriate opentime: either wed, wkday or wk
				// if none is given, use undefined opentimes
				opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(facilityActType).getOpeningTimes();
				if (opentimes != null) {
					// ignoring lunch breaks with the following procedure:
					// if there is only one wed/wkday/wk open time interval, use it
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
			throw new RuntimeException("No suitable facility activity type found for activity " + act.getType() + 
			" and facility " + facility.getId() + ". Aborting...");
		}
		return openInterval;
	}
}
