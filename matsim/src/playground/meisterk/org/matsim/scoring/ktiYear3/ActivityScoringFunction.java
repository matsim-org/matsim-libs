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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.facilities.BasicOpeningTime;
import org.matsim.api.basic.v01.facilities.BasicOpeningTime.DayType;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Plan;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.locationchoice.facilityload.ScoringPenalty;


/**
 * This class implements the activity scoring as used in Year 3 of the KTI project.
 * 
 * It has the following features:
 * 
 * <ul>
 * <li>use opening times from facilities, not from config. scoring function can process multiple opening time intervals per activity option</li>
 * <li>use typical durations from agents' desires, not from config</li>
 * <li>typical duration applies to the sum of all instances of an activity type, not to single instances (so agent finds out itself how much time to spend in which instance)</li>
 * <li>use facility load penalties from LocationChoiceScoringFunction</li>
 * <li>no penalties for late arrival and early departure are computed</li>
 * </ul>
 * 
 * @author meisterk
 *
 */
public class ActivityScoringFunction extends
org.matsim.core.scoring.charyparNagel.ActivityScoringFunction {

	// TODO should be in person.desires
	public static final int DEFAULT_PRIORITY = 1;
	// TODO should be in person.desires
	// TODO differentiate in any way?
	public static final double MINIMUM_DURATION = 0.5 * 3600;

	private List<ScoringPenalty> penalty = new Vector<ScoringPenalty>();
	private TreeMap<Id, FacilityPenalty> facilityPenalties;
	private TreeMap<String, Double> accumulatedDurations = new TreeMap<String, Double>();
	private TreeMap<String, Double> zeroUtilityDurations = new TreeMap<String, Double>();
	private double accumulatedTooShortDuration;
	private double accumulatedWaitingTime;

	private static final DayType DEFAULT_DAY = DayType.wed;
	private static final SortedSet<BasicOpeningTime> DEFAULT_OPENING_TIME = new TreeSet<BasicOpeningTime>();
	static {
		BasicOpeningTime defaultOpeningTime = new OpeningTimeImpl(ActivityScoringFunction.DEFAULT_DAY, Double.MIN_VALUE, Double.MAX_VALUE);
		ActivityScoringFunction.DEFAULT_OPENING_TIME.add(defaultOpeningTime);
	}

	public ActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		super(plan, params);
		this.facilityPenalties = facilityPenalties;
	}

	@Override
	protected double calcActScore(double arrivalTime, double departureTime,
			Activity act) {

		SortedSet<BasicOpeningTime> openTimes = ActivityScoringFunction.DEFAULT_OPENING_TIME;
		// if no associated activity option exists, assume facility is always open
		if (act.getFacility().getActivityOption(act.getType()) != null) {
			openTimes = act.getFacility().getActivityOption(act.getType()).getOpeningTimes(ActivityScoringFunction.DEFAULT_DAY);
		}

		// calculate effective activity duration bounded by opening times
		double accumulatedDuration; // retrievs activity duration for this activity type up to now
		double additionalDuration = 0.0; // accumulates performance intervals for this activity
		double activityStart, activityEnd; // hold effective activity start and end due to facility opening times
		double openingTime, closingTime; // hold time information of an opening time interval
		for (BasicOpeningTime openTime : openTimes) {

			// see explanation comments for processing opening time intervals in super class
			openingTime = openTime.getStartTime();
			closingTime = openTime.getEndTime();

			activityStart = arrivalTime;
			activityEnd = departureTime;

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
			additionalDuration += duration;
			
			// - accumulate duration by activity type, save it, but do not score it here
			accumulatedDuration = 0.0;
			if (this.accumulatedDurations.containsKey(act.getType())) {
				accumulatedDuration = this.accumulatedDurations.get(act.getType());
			}
			this.accumulatedDurations.put(act.getType(), accumulatedDuration + duration);

			// calculate score improvement as basis for facility load penalties
			double scoreImprovement = 
				this.getPerformanceScore(act.getType(), accumulatedDuration + duration) - 
				this.getPerformanceScore(act.getType(), accumulatedDuration);
			/* Penalty due to facility load: --------------------------------------------
			 * Store the temporary score to reduce it in finish() proportionally 
			 * to score and dep. on facility load.
			 * TODO: maybe checking if activity is movable for this person (discussion)
			 */
			if (!act.getType().startsWith("h")) {
				this.penalty.add(new ScoringPenalty(
						activityStart, 
						activityEnd, 
						this.facilityPenalties.get(act.getFacility().getId()), 
						scoreImprovement));
			}
			//---------------------------------------------------------------------------
		}
		
		// accumulate waiting time, which is the time that copuld not be spent in activities due to closed facilities
		this.accumulatedWaitingTime += (departureTime - arrivalTime) - additionalDuration;

		// disutility if duration was too short
		double minimalDuration = ActivityScoringFunction.MINIMUM_DURATION;
		if ((minimalDuration >= 0) && (additionalDuration < minimalDuration)) {
			this.accumulatedTooShortDuration += (minimalDuration - additionalDuration);
		}

		// no actual score is computed here
		return 0.0;

	}

	@Override
	public void finish() {
		super.finish();
		this.finishTooShortDuration();
		this.finishWaitingTime();
		this.finishPerformingActivities();
		this.finishFacilityPenalties();
	}

	protected void finishFacilityPenalties() {
		// copied from LocationChoiceScoringFunction
		// reduce score by penalty from capacity restraints
		Iterator<ScoringPenalty> pen_it = this.penalty.iterator();
		while (pen_it.hasNext()){
			ScoringPenalty penalty = pen_it.next();
			this.score -= penalty.getPenalty();
		}
		this.penalty.clear();
	}

	protected void finishPerformingActivities() {
		// process collected activity durations by activity type
		for (String actType : this.accumulatedDurations.keySet()) {
			this.score += this.getPerformanceScore(actType, this.accumulatedDurations.get(actType));
		}
	}

	protected double getPerformanceScore(String actType, double duration) {

		double typicalDuration = this.person.getDesires().getActivityDuration(actType);

		// initialize zero utility durations here for better code readability, because we only need them here
		double zeroUtilityDuration;
		if (this.zeroUtilityDurations.containsKey(actType)) {
			zeroUtilityDuration = this.zeroUtilityDurations.get(actType);
		} else {
			zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / ActivityScoringFunction.DEFAULT_PRIORITY);
			this.zeroUtilityDurations.put(actType, zeroUtilityDuration);
		}

		double tmpScore;
		if (duration > 0) {
			double utilPerf = this.params.marginalUtilityOfPerforming * typicalDuration
			* Math.log((duration / 3600.0) / this.zeroUtilityDurations.get(actType));
			double utilWait = this.params.marginalUtilityOfWaiting * duration;
			tmpScore = Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			tmpScore = 2 * this.params.marginalUtilityOfLateArrival * Math.abs(duration);
		}

		return tmpScore;
	}

	protected void finishTooShortDuration() {
		this.score += this.params.marginalUtilityOfEarlyDeparture * this.accumulatedTooShortDuration;
	}

	protected void finishWaitingTime() {
		this.score += this.params.marginalUtilityOfWaiting * this.accumulatedWaitingTime;
	}

	@Override
	public void reset() {
		super.reset();
		if (this.accumulatedDurations != null) {
			this.accumulatedDurations.clear();
		}
		this.accumulatedTooShortDuration = 0.0;
		this.accumulatedWaitingTime = 0.0;
	}

	public Map<String, Double> getAccumulatedDurations() {
		return Collections.unmodifiableMap(this.accumulatedDurations);
	}

	public Map<String, Double> getZeroUtilityDurations() {
		return Collections.unmodifiableMap(this.zeroUtilityDurations);
	}

	public double getAccumulatedTooShortDuration() {
		return accumulatedTooShortDuration;
	}

	public double getAccumulatedWaitingTime() {
		return accumulatedWaitingTime;
	}

}
