/* *********************************************************************** *
 * project: org.matsim.*
 * KtiActivityScoring.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.balac.allcsmodestest.scoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.contrib.locationchoice.facilityload.ScoringPenalty;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.SumScoringFunction.ActivityScoring;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

import java.util.*;

/**
 * Due to the abusive use of extension, the meisterk's KTI activity scoring
 * uses a mix of facilities and config-based opening times (config is used for wrap-around
 * activities).
 * This class just implements the same scoring function extension-free, to avoid
 * this king of problems.
 *
 * There's lots of quick copy-paste here!
 * @author thibautd
 */
public class KtiActivtyWithoutPenaltiesScoring implements ActivityScoring, ScoringFunctionAccumulator.ActivityScoring {
	private static final Logger logger = Logger.getLogger(KtiActivtyWithoutPenaltiesScoring.class);

	// lock at reset or getScore, to avoid strange hard-to detect bugs
	private final Lock lock = new Lock();
	private double score = 0;

	// TODO should be in person.desires
	public static final int DEFAULT_PRIORITY = 1;
	// TODO should be in person.desires
	// TODO differentiate in any way?
	public static final double MINIMUM_DURATION = 0.5 * 3600;

	private final TreeMap<Id, FacilityPenalty> facilityPenalties;
	private final ActivityFacilities facilities;

	private static final Set<OpeningTime> DEFAULT_OPENING_TIME =
		Collections.<OpeningTime>singleton(
			new OpeningTimeImpl(
				Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY) );
	
	private Plan plan;
	private CharyparNagelScoringParameters params;
	
	public KtiActivtyWithoutPenaltiesScoring(Plan plan, CharyparNagelScoringParameters params, final TreeMap<Id, FacilityPenalty> facilityPenalties, final ActivityFacilities facilities) {
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
	public void handleFirstActivity(final Activity act) {
		endActivity( act.getEndTime() , act );
	}

	@Override
	public void handleActivity(final Activity act) {
		startActivity( act.getStartTime() , act );
		endActivity( act.getEndTime() , act );
	}

	@Override
	public void handleLastActivity(final Activity act) {
		startActivity( act.getStartTime() , act );
	}

	private Activity activityWithoutStart = null;
	@Override
	public void endActivity(double time, Activity act) {
		lock.checkLock();
		assert time == act.getEndTime();
		final double startTime = act.getStartTime();
		final double endTime = act.getEndTime();

		if ( startTime == Time.UNDEFINED_TIME ) {
			if ( activityWithoutStart != null ) throw new IllegalStateException( "several acts without start" );
			activityWithoutStart = act;
		}
		else {
			// those acts are not passed anymore to the endActivity method
			assert endTime != Time.UNDEFINED_TIME;
			handleActivity( startTime , endTime , act );
		}
	}

	@Override
	public void startActivity(double time, Activity act) {
		lock.checkLock();

		if ( act.getEndTime() == Time.UNDEFINED_TIME ) {
			if ( activityWithoutStart == null ) {
				throw new IllegalStateException( "activity "+act+" has not end time, but no activity without a start is stored for person "+plan.getPerson().getId() );
			}

			// assume wraparound
			if ( !activityWithoutStart.getType().equals( act.getType() ) ) {
				//throw new IllegalStateException( act+" cannot be wraped around with "+activityWithoutStart );
				// this can happen with unfinished plans. Just do not score.
				// Issue a warning?
				return;
			}
			// XXX this is the way it is done in "CharypayNagel", but I'm absolutely sure
			// it doesn't do what it should if opening times are defined...
			// To investigate
			handleActivity( act.getStartTime() , activityWithoutStart.getEndTime() + 24 * 3600 , act );
		}
	}

	private void handleActivity(double arrivalTime, double departureTime, Activity act) {
		if ( logger.isTraceEnabled() ) {
			logger.trace( "handling activity "+act+" from "+Time.writeTime( arrivalTime )+" to "+Time.writeTime( departureTime ) ); 
		}

		final ActivityUtilityParameters utilityParams = params.utilParams.get( act.getType() );
		// null params are allowed if there are Desires.
		if ( utilityParams != null && !utilityParams.isScoreAtAll() ) return;

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

			// if no associated activity option exists, or if the activity option does not contain an <opentimes> element,
			// assume facility is always open
			ActivityOption actOpt = this.facilities.getFacilities().get(act.getFacilityId()).getActivityOptions().get(act.getType());

			if (actOpt == null) {
				logger.error("Agent wants to perform an activity whose type is not available in the planned facility.");
				logger.error("facility id: " + act.getFacilityId());
				logger.error("activity type: " + act.getType());
				throw new RuntimeException("Agent wants to perform an activity whose type is not available in the planned facility.");
			}

			final Set<OpeningTime> openTimes =
				!actOpt.getOpeningTimes().isEmpty() ?
					actOpt.getOpeningTimes() :
					// if there is an activity option but no opening times,
					// assume always open.
					DEFAULT_OPENING_TIME;

			if ( logger.isTraceEnabled() ) {
				logger.trace( "using opening times "+openTimes );
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
				/*if (act.getType().startsWith("shop") || act.getType().startsWith("leisure")) {
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
						 Penalty due to facility load:
						 * Store the temporary score to reduce it in finish() proportionally
						 * to score and dep. on facility load.
						 
						this.penalty.add(new ScoringPenalty(
								activityStart,
								activityEnd,
								this.facilityPenalties.get(act.getFacilityId()),
								scoreImprovement));
					}

				}*/

				timeSpentPerforming += duration;

			}

			// accumulated waiting time, which is the time that could not be performed in activities due to closed facilities
			this.timeSpentWaiting += (fromArrivalToDeparture - timeSpentPerforming);
			if ( logger.isTraceEnabled() ) {
				logger.trace( "adding "+Time.writeTime(fromArrivalToDeparture - timeSpentPerforming)+" to waiting time" );
			}

			// accumulate time spent performing
			double accumulatedDuration = 0.0;
			if (this.accumulatedTimeSpentPerforming.containsKey(act.getType())) {
				accumulatedDuration = this.accumulatedTimeSpentPerforming.get(act.getType());
			}
			this.accumulatedTimeSpentPerforming.put(act.getType(), accumulatedDuration + timeSpentPerforming);
			if ( logger.isTraceEnabled() ) {
				logger.trace( "adding "+Time.writeTime( timeSpentPerforming )+" to time performing "+act.getType() );
			}

			// disutility if duration of that activity was too short
			final double minimalDuration =  
				utilityParams != null ?
					utilityParams.getMinimalDuration() :
					MINIMUM_DURATION;
			if (timeSpentPerforming < minimalDuration) {
				this.accumulatedTooShortDuration += (minimalDuration - timeSpentPerforming);
			}
			if ( logger.isTraceEnabled() ) {
				logger.trace( "adding "+Time.writeTime( minimalDuration - timeSpentPerforming )+" to short time penalty" );
			}

		}
	}

	@Override
	public void finish() {
		// not handling one-activity plan is not a problem as long as
		// one does not modifies the activity chaining
		//if (plan.getPlanElements().size() == 1) {
		//	// One Activity only. Regular scoring does not handle this because
		//	// the activity never ends and never starts.
		//	this.score += calcActScore(0, 24*3600.0, (Activity) plan.getPlanElements().get(0)); // SCENARIO_DURATION
		//}
		this.score = 0;
		this.score += this.getTooShortDurationScore();
		if ( logger.isTraceEnabled() ) {
			logger.trace( "score after too short duration: "+score );
		}
		this.score += this.getWaitingTimeScore();
		if ( logger.isTraceEnabled() ) {
			logger.trace( "score after waiting time: "+score );
		}
		this.score += this.getPerformanceScore();
		if ( logger.isTraceEnabled() ) {
			logger.trace( "score after performance: "+score );
		}
		this.score += this.getFacilityPenaltiesScore();
		if ( logger.isTraceEnabled() ) {
			logger.trace( "score after facility penalty: "+score );
		}
		this.score += this.getNegativeDurationScore();
		if ( logger.isTraceEnabled() ) {
			logger.trace( "score after negative duration: "+score );
		}
	}
	
	public double getFacilityPenaltiesScore() {
		double facilityPenaltiesScore = 0.0;

		if (this.penalty != null) {
			// copied from LocationChoiceScoringFunction
			// reduce score by penalty from capacity restraints
			for (ScoringPenalty p : penalty) {
				facilityPenaltiesScore -= p.getPenalty();
			}
		}
		return facilityPenaltiesScore;
	}

	protected double getPerformanceScore(String actType, double duration) {
		final double typicalDuration =
				params.utilParams.get( actType ).getTypicalDuration();

		// initialize zero utility durations here for better code readability, because we only need them here
		double zeroUtilityDuration;
		if (this.zeroUtilityDurations.containsKey(actType)) {
			zeroUtilityDuration = this.zeroUtilityDurations.get(actType);
		} else {
			zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / DEFAULT_PRIORITY);
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

	@Override
	public double getScore() {
		//lock.lock();
		return score;
	}

	@Override
	public void reset() {
		lock.lock();
	}
}

class Lock {
	private StackTraceElement[] stackTraceAtLock = null;

	public void lock() {
		stackTraceAtLock = Thread.currentThread().getStackTrace();
	}

	public void checkLock() {
		if (stackTraceAtLock != null) {
			throw new IllegalStateException( "a scoring function can only be used once. Was already locked with stack "+Arrays.toString( stackTraceAtLock ) );
		}
	}
}
