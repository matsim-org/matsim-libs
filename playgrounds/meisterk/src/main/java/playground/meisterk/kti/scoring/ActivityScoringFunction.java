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

package playground.meisterk.kti.scoring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
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

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final ActivityFacilities facilities;

	private static final DayType DEFAULT_DAY = DayType.wed;
	private static final SortedSet<OpeningTime> DEFAULT_OPENING_TIME = new TreeSet<OpeningTime>();
	
	private Plan plan;
	private CharyparNagelScoringParameters params;
	
	static {
		OpeningTime defaultOpeningTime = new OpeningTimeImpl(ActivityScoringFunction.DEFAULT_DAY, Double.MIN_VALUE, Double.MAX_VALUE);
		ActivityScoringFunction.DEFAULT_OPENING_TIME.add(defaultOpeningTime);
	}

	/*package*/ static final Logger logger = Logger.getLogger(ActivityScoringFunction.class);

	public ActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, final TreeMap<Id, FacilityPenalty> facilityPenalties, final ActivityFacilities facilities) {
		super(params);
		this.params = params;
		this.facilityPenalties = facilityPenalties;
		this.facilities = facilities;
		this.plan = plan;
	}

	/*
	 * Variables only used in activity score calculation.
	 */
	private List<ScoringPenalty> penalty = null;
	private final HashMap<String, Double> accumulatedTimeSpentPerforming = new HashMap<String, Double>();
	private final HashMap<String, Double> zeroUtilityDurations = new HashMap<String, Double>();
	private double accumulatedTooShortDuration;
	private double timeSpentWaiting;
	private double accumulatedNegativeDuration;

	@Override
	protected double calcActScore(double arrivalTime, double departureTime, Activity act) {

		double fromArrivalToDeparture = departureTime - arrivalTime;

		// technical penalty: negative activity durations are penalized heavily
		// so that 24 hour plans are enforced (home activity must not start later than it ended)
		// also: negative duration is also too short
		if (fromArrivalToDeparture < 0.0) {
			this.accumulatedNegativeDuration += fromArrivalToDeparture;
			this.accumulatedTooShortDuration += (ActivityScoringFunction.MINIMUM_DURATION - fromArrivalToDeparture);
		}
		///////////////////////////////////////////////////////////////////
		// the time between arrival and departure is either spent
		// - performing (when the associated facility is open) or
		// - waiting (when it is closed)
		// - the sum of the share performing and the share waiting equals the difference between arrival and departure
		///////////////////////////////////////////////////////////////////
		else {

			SortedSet<OpeningTime> openTimes = ActivityScoringFunction.DEFAULT_OPENING_TIME;
			// if no associated activity option exists, or if the activity option does not contain an <opentimes> element,
			// assume facility is always open
			ActivityOption actOpt = this.facilities.getFacilities().get(act.getFacilityId()).getActivityOptions().get(act.getType());
			if (actOpt != null) {
				openTimes = actOpt.getOpeningTimes(ActivityScoringFunction.DEFAULT_DAY);
				if (openTimes == null) {
					openTimes = ActivityScoringFunction.DEFAULT_OPENING_TIME;
				}
			} else {
				logger.error("Agent wants to perform an activity whose type is not available in the planned facility.");
				logger.error("facility id: " + act.getFacilityId());
				logger.error("activity type: " + act.getType());
				Gbl.errorMsg("Agent wants to perform an activity whose type is not available in the planned facility.");
			}

			// calculate effective activity duration bounded by opening times
			double timeSpentPerforming = 0.0; // accumulates performance intervals for this activity
			double activityStart, activityEnd; // hold effective activity start and end due to facility opening times
			double scoreImprovement; // calculate score improvement only as basis for facility load penalties
			double openingTime, closingTime; // hold time information of an opening time interval
			for (OpeningTime openTime : openTimes) {

				// see explanation comments for processing opening time intervals in super class
				openingTime = openTime.getStartTime();
				closingTime = openTime.getEndTime();

				activityStart = Math.max(arrivalTime, openingTime);
				activityEnd = Math.min(departureTime, closingTime);

				if ((openingTime > departureTime) || (closingTime < arrivalTime)) {
					// agent could not perform action
					activityStart = departureTime;
					activityEnd = departureTime;
				}

				double duration = activityEnd - activityStart;

				// calculate penalty due to facility load only when:
				// - activity type is penalized (currently only shop and leisure-type activities)
				// - duration is bigger than 0
				if (act.getType().startsWith("shop") || act.getType().startsWith("leisure")) {
					if (duration > 0) {

						double accumulatedDuration = 0.0;
						if (this.accumulatedTimeSpentPerforming.containsKey(act.getType())) {
							accumulatedDuration = this.accumulatedTimeSpentPerforming.get(act.getType());
						}

						scoreImprovement =
							this.getPerformanceScore(act.getType(), accumulatedDuration + duration) -
							this.getPerformanceScore(act.getType(), accumulatedDuration);

						// lazy init of penalty data structure
						if (this.penalty == null) {
							this.penalty = new Vector<ScoringPenalty>();
						}
						/* Penalty due to facility load:
						 * Store the temporary score to reduce it in finish() proportionally
						 * to score and dep. on facility load.
						 */
						this.penalty.add(new ScoringPenalty(
								activityStart,
								activityEnd,
								this.facilityPenalties.get(act.getFacilityId()),
								scoreImprovement));
					}

				}

				timeSpentPerforming += duration;

			}

			// accumulated waiting time, which is the time that could not be performed in activities due to closed facilities
			this.timeSpentWaiting += (fromArrivalToDeparture - timeSpentPerforming);

			// accumulate time spent performing
			double accumulatedDuration = 0.0;
			if (this.accumulatedTimeSpentPerforming.containsKey(act.getType())) {
				accumulatedDuration = this.accumulatedTimeSpentPerforming.get(act.getType());
			}
			this.accumulatedTimeSpentPerforming.put(act.getType(), accumulatedDuration + timeSpentPerforming);

			// disutility if duration of that activity was too short
			if (timeSpentPerforming < ActivityScoringFunction.MINIMUM_DURATION) {
				this.accumulatedTooShortDuration += (ActivityScoringFunction.MINIMUM_DURATION - timeSpentPerforming);
			}

		}

		// no actual score is computed here
		return 0.0;

	}

	@Override
	public void finish() {
		super.finish();
		if (plan.getPlanElements().size() == 1) {
			// One Activity only. Regular scoring does not handle this because
			// the activity never ends and never starts.
			this.score += calcActScore(0, 24*3600.0, (Activity) plan.getPlanElements().get(0)); // SCENARIO_DURATION
		}
		this.score += this.getTooShortDurationScore();
		this.score += this.getWaitingTimeScore();
		this.score += this.getPerformanceScore();
		this.score += this.getFacilityPenaltiesScore();
		this.score += this.getNegativeDurationScore();
	}
	
	public double getFacilityPenaltiesScore() {

		double facilityPenaltiesScore = 0.0;

		if (this.penalty != null) {
			// copied from LocationChoiceScoringFunction
			// reduce score by penalty from capacity restraints
			Iterator<ScoringPenalty> pen_it = this.penalty.iterator();
			while (pen_it.hasNext()) {
				ScoringPenalty penalty = pen_it.next();
				facilityPenaltiesScore -= penalty.getPenalty();
			}
		}
		return facilityPenaltiesScore;
	}

	protected double getPerformanceScore(String actType, double duration) {

		double typicalDuration = ((PersonImpl) plan.getPerson()).getDesires().getActivityDuration(actType);

		// initialize zero utility durations here for better code readability, because we only need them here
		double zeroUtilityDuration;
		if (this.zeroUtilityDurations.containsKey(actType)) {
			zeroUtilityDuration = this.zeroUtilityDurations.get(actType);
		} else {
			zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / ActivityScoringFunction.DEFAULT_PRIORITY);
			this.zeroUtilityDurations.put(actType, zeroUtilityDuration);
		}

		double tmpScore = 0.0;
		if (duration > 0.0) {
			double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
			* Math.log((duration / 3600.0) / this.zeroUtilityDurations.get(actType));
			double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
			tmpScore = Math.max(0, Math.max(utilPerf, utilWait));
		} else if (duration < 0.0) {
			logger.error("Accumulated activity durations < 0.0 must not happen.");
		}

		return tmpScore;
	}

	public double getTooShortDurationScore() {
		return this.params.marginalUtilityOfEarlyDeparture_s * this.accumulatedTooShortDuration;
	}

	public double getWaitingTimeScore() {
		return this.params.marginalUtilityOfWaiting_s * this.timeSpentWaiting;
	}

	public double getPerformanceScore() {
		double performanceScore = 0.0;
		for (String actType : this.accumulatedTimeSpentPerforming.keySet()) {
			performanceScore += this.getPerformanceScore(actType, this.accumulatedTimeSpentPerforming.get(actType));
		}
		return performanceScore;
	}

	public double getNegativeDurationScore() {
		return (2 * this.params.marginalUtilityOfLateArrival_s * Math.abs(this.accumulatedNegativeDuration));
	}

	public Map<String, Double> getAccumulatedDurations() {
		return Collections.unmodifiableMap(this.accumulatedTimeSpentPerforming);
	}

	public Map<String, Double> getZeroUtilityDurations() {
		return Collections.unmodifiableMap(this.zeroUtilityDurations);
	}

	public double getAccumulatedTooShortDuration() {
		return accumulatedTooShortDuration;
	}

	public double getTimeSpentWaiting() {
		return timeSpentWaiting;
	}

	public double getAccumulatedNegativeDuration() {
		return accumulatedNegativeDuration;
	}

}
