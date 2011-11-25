/* *********************************************************************** *
 * project: org.matsim.*
 * CarPoolingOpeningTimesActivityScoring.java
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
package playground.thibautd.scoring;

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
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.charyparNagel.ActivityScoringFunction;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.locationchoice.facilityload.ScoringPenalty;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;

/**
 * Activity scoring function based on the KTI activity scoring.
 *
 * It has the following features (from KTI):
 *
 * <ul>
 * <li>use opening times from facilities, not from config. scoring function can process multiple opening time intervals per activity option</li>
 * <li>use typical durations from agents' desires, not from config</li>
 * <li>typical duration applies to the sum of all instances of an activity type, not to single instances (so agent finds out itself how much time to spend in which instance)</li>
 * <li>use facility load penalties from LocationChoiceScoringFunction</li>
 * <li>no penalties for late arrival and early departure are computed</li>
 * </ul>
 *
 * Additional car-pooling related features are:
 *
 * <ul>
 * <li> pick-up and drop off activities are scored as an activity to a closed facility,
 * no matter what opening times are defined at the (normally dummy) facility.
 * <li> 0-typical duration and 0-durations are handled (this is typically the case
 * for drop-offs). In the usual scoring function, this results in a 0 divided by 0
 * operation.
 * <ul>
 *
 * @author thibautd
 *
 */
public class CarPoolingOpeningTimesActivityScoringFunction extends ActivityScoringFunction {

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
		OpeningTime defaultOpeningTime = new OpeningTimeImpl(DEFAULT_DAY, Double.MIN_VALUE, Double.MAX_VALUE);
		DEFAULT_OPENING_TIME.add(defaultOpeningTime);
	}

	/*package*/ static final Logger logger = Logger.getLogger(ActivityScoringFunction.class);

	public CarPoolingOpeningTimesActivityScoringFunction(
			final Plan plan,
			final CharyparNagelScoringParameters params,
			final TreeMap<Id, FacilityPenalty> facilityPenalties,
			final ActivityFacilities facilities) {
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
	protected double calcActScore(
			final double arrivalTime,
			final double departureTime,
			final Activity act) {

		double fromArrivalToDeparture = departureTime - arrivalTime;

		// technical penalty: negative activity durations are penalized heavily
		// so that 24 hour plans are enforced (home activity must not start later than it ended)
		// also: negative duration is also too short
		if (fromArrivalToDeparture < 0.0) {
			this.accumulatedNegativeDuration += fromArrivalToDeparture;
			this.accumulatedTooShortDuration += (MINIMUM_DURATION - fromArrivalToDeparture);
		}
		///////////////////////////////////////////////////////////////////
		// the time between arrival and departure is either spent
		// - performing (when the associated facility is open) or
		// - waiting (when it is closed)
		// - the sum of the share performing and the share waiting equals the difference between arrival and departure
		///////////////////////////////////////////////////////////////////
		else {
			SortedSet<OpeningTime> openTimes = DEFAULT_OPENING_TIME;
			// if no associated activity option exists, or if the activity option does not contain an <opentimes> element,
			// assume facility is always open
			ActivityOption actOpt = this.facilities.getFacilities().get(act.getFacilityId()).getActivityOptions().get(act.getType());

			if ( JointActingTypes.PICK_UP.equals( act.getType() ) ||
					JointActingTypes.DROP_OFF.equals( act.getType() ) ) {
				openTimes = null;
			}
			else if (actOpt != null) {
				openTimes = actOpt.getOpeningTimes(DEFAULT_DAY);
				if (openTimes == null) {
					openTimes = DEFAULT_OPENING_TIME;
				}
			}
			else {
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

			if (openTimes != null) {
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
			}

			// accumulated waiting time, which is the time that could not be performed in activities due to closed facilities
			this.timeSpentWaiting += (fromArrivalToDeparture - timeSpentPerforming);

			// accumulate time spent performing
			double accumulatedDuration;
			try {
				accumulatedDuration = this.accumulatedTimeSpentPerforming.get(act.getType());
			} catch (NullPointerException e) {
				accumulatedDuration = 0.0;
			}

			this.accumulatedTimeSpentPerforming.put(act.getType(), accumulatedDuration + timeSpentPerforming);

			// disutility if duration of that activity was too short
			if (timeSpentPerforming < MINIMUM_DURATION) {
				this.accumulatedTooShortDuration += (MINIMUM_DURATION - timeSpentPerforming);
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

	protected double getPerformanceScore(
			final String actType,
			final double duration) {
		double typicalDuration = ((PersonImpl) plan.getPerson()).getDesires().getActivityDuration(actType);

		if (typicalDuration < 0) {
			if (actType.equals( JointActingTypes.PICK_UP ) &&
				actType.equals( JointActingTypes.DROP_OFF ) ) {
				// no desired duration is OK
				typicalDuration = 0;
			}
			else {
				// not OK: zero or negative would make no sense, and
				// no obvious default value.
				throw new RuntimeException( "could not find a valid desired duration for activity "+actType+
						" for person "+plan.getPerson().getId() );
			}
		}

		// initialize zero utility durations here for better code readability, because we only need them here
		Double zeroUtilityDuration = this.zeroUtilityDurations.get(actType);

		if (zeroUtilityDuration == null) {
			if (typicalDuration > 0) {
				zeroUtilityDuration = (typicalDuration / 3600.0) *
					Math.exp( -10.0 / (typicalDuration / 3600.0) / DEFAULT_PRIORITY);
			}
			else {
				// typicalDuration == 0
				// 0 * exp( -Infinity )
				zeroUtilityDuration = 0d;
			}

			this.zeroUtilityDurations.put(actType, zeroUtilityDuration);
		}

		double tmpScore = 0.0;
		// only consider the case where both duration and typical duration
		// are positive. Otherwise, the log term goes to -Infinity, whereas the
		// utility is constrained to be positive.
		if (duration > 0.0 && typicalDuration > 0.0) {
			// TODO: zeroUtilityDuration == 0
			double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
					* Math.log((duration / 3600.0) / zeroUtilityDuration);

			// XXX: What the hell can that mean ??? 
			// double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
			// tmpScore = Math.max(0, Math.max(utilPerf, utilWait));

			tmpScore = Math.max(0, utilPerf);
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
		for (Map.Entry<String , Double> entry : this.accumulatedTimeSpentPerforming.entrySet()) {
			performanceScore += this.getPerformanceScore(
					entry.getKey(),
					entry.getValue());
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
