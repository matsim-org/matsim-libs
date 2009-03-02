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

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.locationchoice.facilityload.ScoringPenalty;
import org.matsim.population.ActUtilityParameters;

/* 
 * Scoring function factoring in capacity restraints
 */
public class LocationChoiceScoringFunction extends CharyparNagelOpenTimesScoringFunction {

	private List<ScoringPenalty> penalty = null;
	private TreeMap<Id, FacilityPenalty> facilityPenalties;

	public LocationChoiceScoringFunction(final Plan plan, final CharyparNagelScoringParameters params, final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		super(plan, params);
		this.penalty = new Vector<ScoringPenalty>();
		this.facilityPenalties = facilityPenalties;
	}
	
	public void finish() {

		super.finish();

		// reduce score by penalty from capacity restraints
		Iterator<ScoringPenalty> pen_it = this.penalty.iterator();
		while (pen_it.hasNext()){
			ScoringPenalty penalty = pen_it.next();
			this.score -=penalty.getPenalty();
		}
		this.penalty.clear();
	}

	protected double calcActScore(final double arrivalTime, final double departureTime, final Act act) {

		ActUtilityParameters params = this.params.utilParams.get(act.getType());
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}
		double tmpScore = 0.0;

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
			double utilPerf = this.params.marginalUtilityOfPerforming * typicalDuration
					* Math.log((duration / 3600.0) / params.getZeroUtilityDuration());
			
			

			double utilWait = this.params.marginalUtilityOfWaiting * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
					
			/* Penalty due to facility load: --------------------------------------------
			 * Store the temporary score to reduce it in finish() proportionally 
			 * to score and dep. on facility load.
			 * TODO: maybe checking if activity is movable for this person (discussion)
			 */
			if (!act.getType().startsWith("h")) {
				this.penalty.add(new ScoringPenalty(activityStart, activityEnd, 
						this.facilityPenalties.get(act.getFacility().getId()), tmpScore));
			}
			//---------------------------------------------------------------------------
				
		} else {
			tmpScore += 2*this.params.marginalUtilityOfLateArrival*Math.abs(duration);
		}
		
				
		// DISUTILITIES: ==============================================================================	
		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			tmpScore += this.params.marginalUtilityOfWaiting * (activityStart - arrivalTime);
		}

		// disutility if too late
		double latestStartTime = params.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += this.params.marginalUtilityOfLateArrival * (activityStart - latestStartTime);
		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = params.getMinimalDuration();
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture * (minimalDuration - duration);
		}	
		return tmpScore;
	}

}
