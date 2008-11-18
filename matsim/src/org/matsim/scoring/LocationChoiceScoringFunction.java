/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoiceScoringFunction.java
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

package org.matsim.scoring;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

//import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.locationchoice.facilityload.ScoringPenalty;
import org.matsim.population.Act;
import org.matsim.population.ActUtilityParameters;
import org.matsim.population.Plan;

/* Scoring function factoring in:
 * - opentimes
 * - facility attractivity
 * - capacity restraints:
 *  TODO: should be done for for arrival to departure time not act start to act end time,
 *  because seraching for a parking lot possibly happens before the opening time
 *  see EventsToFacilityLoad
 */

public class LocationChoiceScoringFunction extends CharyparNagelOpenTimesScoringFunction {

	private List<ScoringPenalty> penalty = null;
	private TreeMap<Id, FacilityPenalty> facilityPenalties;
	//private static final Logger log = Logger.getLogger(LocationChoiceScoringFunction.class);

	public LocationChoiceScoringFunction(final Plan plan, TreeMap<Id, FacilityPenalty> facilityPenalties) {
		super(plan);
		this.penalty = new Vector<ScoringPenalty>();
		this.facilityPenalties = facilityPenalties;
	}
	
	public void finish() {

		super.finish();

		// reduce score by penalty from capacity restraints
		Iterator<ScoringPenalty> pen_it = this.penalty.iterator();
		while (pen_it.hasNext()){
			ScoringPenalty penalty = pen_it.next();
			
			// TODO: check activity is secondary
			this.score -=penalty.getPenalty();
		}
		this.penalty.clear();
	}

	protected double calcActScore(final double arrivalTime, final double departureTime, final Act act) {

		ActUtilityParameters params = utilParams.get(act.getType());
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}
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
	
		// utility of performing an action, duration is >= 1, thus log is no problem ----------------
		double typicalDuration = params.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = marginalUtilityOfPerforming * typicalDuration
					* Math.log((duration / 3600.0) / params.getZeroUtilityDuration());

			double utilWait = marginalUtilityOfWaiting * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			tmpScore += 2*marginalUtilityOfLateArrival*Math.abs(duration);
		}
		
		// used arrival and departure time because of parking cap restr. before act actually starts
		if (!act.getType().equalsIgnoreCase("home")) {
			this.penalty.add(new ScoringPenalty(arrivalTime, departureTime, 
					this.facilityPenalties.get(act.getFacility().getId()), tmpScore));
		}	
		
		// DISUTILITIES: -------------------------------------------------------------------------------	
		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			tmpScore += marginalUtilityOfWaiting * (activityStart - arrivalTime);
		}

		// disutility if too late
		double latestStartTime = params.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += marginalUtilityOfLateArrival * (activityStart - latestStartTime);
		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			tmpScore += marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += marginalUtilityOfWaiting * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = params.getMinimalDuration();
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += marginalUtilityOfEarlyDeparture * (minimalDuration - duration);
		}	
		return tmpScore;
	}

}
