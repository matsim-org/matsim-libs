/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
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

package org.matsim.core.scoring.functions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author rashid_waraich
 */
public final class CharyparNagelActivityScoring implements org.matsim.core.scoring.SumScoringFunction.ActivityScoring {
	private static final double INITIAL_SCORE = 0.0;

	private final Score score = new Score();

	private static int firstLastActWarning = 0;
	private static short firstLastActOpeningTimesWarning = 0;

	private final ScoringParameters params;
	private final OpeningIntervalCalculator openingIntervalCalculator;

	private Activity firstActivity;

	private static final Logger log = LogManager.getLogger(CharyparNagelActivityScoring.class);

	public CharyparNagelActivityScoring(final ScoringParameters params) {
		this(params, new ActivityTypeOpeningIntervalCalculator(params));
	}

	public CharyparNagelActivityScoring(final ScoringParameters params, final OpeningIntervalCalculator openingIntervalCalculator) {
		this.params = params;

//		firstLastActWarning = 0 ;
		firstLastActOpeningTimesWarning = 0 ;
		this.openingIntervalCalculator = openingIntervalCalculator;
	}

	@Override
	public void finish() {
		if (this.firstActivity != null) {
			handleMorningActivity();
		}
		// Else, no activity has started so far.
		// This probably means that the plan contains at most one activity.
		// We cannot handle that correctly, because we do not know what it is.
	}

	@Override
	public double getScore() {
		return this.score.actPerforming_util + this.score.actWaiting_util + this.score.actLateArrival_util + this.score.actEarlyDeparture_util;
	}

	@Override
	public void explainScore(StringBuilder out) {
		out.append("actPerforming_util=").append(this.score.actPerforming_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actPerforming_s=").append(this.score.actPerforming_s).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actWaiting_util=").append(this.score.actWaiting_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actWaiting_s=").append(this.score.actWaiting_s).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actLateArrival_util=").append(this.score.actLateArrival_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actLateArrival_s=").append(this.score.actLateArrival_s).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actEarlyDeparture_util=").append(this.score.actEarlyDeparture_util).append(ScoringFunction.SCORE_DELIMITER);
		out.append("actEarlyDeparture_s=").append(this.score.actEarlyDeparture_s);
	}

	private Score calcActScore(final double arrivalTime, final double departureTime, final Activity act) {

		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"scoring\" in the config file).");
		}

		Score tmpScore = new Score();

		if (actParams.isScoreAtAll()) {
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

			OptionalTime[] openingInterval = openingIntervalCalculator.getOpeningInterval(act);
			OptionalTime openingTime = openingInterval[0];
			OptionalTime closingTime = openingInterval[1];

			double activityStart = arrivalTime;
			double activityEnd = departureTime;

			if (openingTime.isDefined() && arrivalTime < openingTime.seconds()) {
				activityStart = openingTime.seconds();
			}
			if (closingTime.isDefined() && closingTime.seconds() < departureTime) {
				activityEnd = closingTime.seconds();
			}
			if (openingTime.isDefined() && closingTime.isDefined()
					&& (openingTime.seconds() > departureTime || closingTime.seconds() < arrivalTime)) {
				// agent could not perform action
				activityStart = departureTime;
				activityEnd = departureTime;
			}
			double duration = activityEnd - activityStart;

			// disutility if too early
			if (arrivalTime < activityStart) {
				// agent arrives to early, has to wait
				double waitTime = activityStart - arrivalTime;
				tmpScore.actWaiting_s += waitTime;
				tmpScore.actWaiting_util += this.params.marginalUtilityOfWaiting_s * waitTime;
			}

			// disutility if too late

			OptionalTime latestStartTime = actParams.getLatestStartTime();
			if (latestStartTime.isDefined() && (activityStart > latestStartTime.seconds())) {
				double lateTime = activityStart - latestStartTime.seconds();
				tmpScore.actLateArrival_s += lateTime;
				tmpScore.actLateArrival_util += this.params.marginalUtilityOfLateArrival_s * lateTime;
			}

			// utility of performing an action, duration is >= 1, thus log is no problem
			double typicalDuration = actParams.getTypicalDuration();
			tmpScore.actPerforming_s += duration;

			if ( this.params.usingOldScoringBelowZeroUtilityDuration ) {
				if (duration > 0) {
					double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
							* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
					double utilWait = this.params.marginalUtilityOfWaiting_s * duration;

					tmpScore.actPerforming_util += Math.max(0, Math.max(utilPerf, utilWait));
				} else {
					tmpScore.actLateArrival_util += 2*this.params.marginalUtilityOfLateArrival_s*Math.abs(duration);
				}
			} else {
				if ( duration >= 3600.*actParams.getZeroUtilityDuration_h() ) {
					double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
							* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
					// also removing the "wait" alternative scoring.
					tmpScore.actPerforming_util += utilPerf ;
				} else {
//					if ( wrnCnt < 1 ) {
//						wrnCnt++ ;
//						log.warn("encountering duration < zeroUtilityDuration; the logic for this was changed around mid-nov 2013.") ;
//						log.warn( "your final score thus will be different from earlier runs; set usingOldScoringBelowZeroUtilityDuration to true if you "
//								+ "absolutely need the old version.  See https://matsim.atlassian.net/browse/MATSIM-191." );
//						log.warn( Gbl.ONLYONCE ) ;
//					}

					// below zeroUtilityDuration, we linearly extend the slope ...:
					double slopeAtZeroUtility = this.params.marginalUtilityOfPerforming_s * typicalDuration / ( 3600.*actParams.getZeroUtilityDuration_h() ) ;
					if ( slopeAtZeroUtility < 0. ) {
						// (beta_perf might be = 0)
						System.err.println("beta_perf: " + this.params.marginalUtilityOfPerforming_s);
						System.err.println("typicalDuration: " + typicalDuration );
						System.err.println( "zero utl duration: " + actParams.getZeroUtilityDuration_h() );
						throw new RuntimeException( "slope at zero utility < 0.; this should not happen ...");
					}
					double durationUnderrun = actParams.getZeroUtilityDuration_h()*3600. - duration ;
					if ( durationUnderrun < 0. ) {
						throw new RuntimeException( "durationUnderrun < 0; this should not happen ...") ;
					}
					tmpScore.actPerforming_util -= slopeAtZeroUtility * durationUnderrun ;
				}

			}

			// disutility if stopping too early
			OptionalTime earliestEndTime = actParams.getEarliestEndTime();
			if ((earliestEndTime.isDefined()) && (activityEnd < earliestEndTime.seconds())) {
				double earlyDeparture = earliestEndTime.seconds() - activityEnd;
				tmpScore.actEarlyDeparture_s += earlyDeparture;
				tmpScore.actEarlyDeparture_util += this.params.marginalUtilityOfEarlyDeparture_s * earlyDeparture;
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				double waiting = departureTime - activityEnd;
				tmpScore.actWaiting_s += waiting;
				tmpScore.actWaiting_util += this.params.marginalUtilityOfWaiting_s * waiting;
			}

			// disutility if duration was too short
			OptionalTime minimalDuration = actParams.getMinimalDuration();
			if ((minimalDuration.isDefined()) && (duration < minimalDuration.seconds())) {
				double earlyDeparture = minimalDuration.seconds() - duration;
				tmpScore.actEarlyDeparture_s += earlyDeparture;
				tmpScore.actEarlyDeparture_util += this.params.marginalUtilityOfEarlyDeparture_s * earlyDeparture;
			}
		}
		return tmpScore;
	}

	private void handleOvernightActivity(Activity lastActivity) {
		assert firstActivity != null;
		assert lastActivity != null;


		if (lastActivity.getType().equals(this.firstActivity.getType()) || this.firstActivity.getType().equals("not specified") ) {
			// yyyy find better way to encode "not specified".  It is quite common for travel surveys that the type of the
			// first activity is not encoded at all, and then we can as well assume that it is the same as that of the last.  kai, sep'16

			// the first Act and the last Act have the same type:
			if (firstLastActOpeningTimesWarning <= 10) {
				OptionalTime[] openInterval = openingIntervalCalculator.getOpeningInterval(lastActivity);
				if (openInterval[0].isDefined() || openInterval[1].isDefined()){
					log.warn("There are opening or closing times defined for the first and last activity. The correctness of the scoring function can thus not be guaranteed.");
					log.warn("first activity: " + firstActivity ) ;
					log.warn("last activity: " + lastActivity ) ;
					if (firstLastActOpeningTimesWarning == 10) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActOpeningTimesWarning++;
				}
			}

			Score calcActScore = calcActScore(lastActivity.getStartTime().seconds(),
					this.firstActivity.getEndTime().seconds() + 24 * 3600, lastActivity);
			this.score.add(calcActScore); // SCENARIO_DURATION
		} else {
			// the first Act and the last Act have NOT the same type:
			if (this.params.scoreActs) {
				int last=0 ;
				if (firstLastActWarning <= last) {
					log.warn("The first and the last activity do not have the same type. " ) ;
					log.warn( "Will score the first activity from midnight to its end, and the last activity from its start to midnight.") ;
					log.warn("Because of the nonlinear function, this is not the same as scoring from start to end.");
					log.warn("first activity: " + firstActivity ) ;
					log.warn("last activity: " + lastActivity ) ;
					log.warn("This may also happen when plans are not completed when the simulation ends.") ;
					if (firstLastActWarning == last) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActWarning++;
				}

				// score first activity
				this.score.add(calcActScore(0.0, this.firstActivity.getEndTime().seconds(), firstActivity));
				// score last activity
				this.score.add(calcActScore(lastActivity.getStartTime().seconds(),
						this.params.simulationPeriodInDays * 24 * 3600, lastActivity));
			}
		}
	}

	private void handleMorningActivity() {
		assert firstActivity != null;
		// score first activity
		this.score.add(calcActScore(0.0, this.firstActivity.getEndTime().seconds(), firstActivity));
	}

	@Override
	public void handleFirstActivity(Activity act) {
		assert act != null;
		this.firstActivity = act;
	}

	@Override
	public void handleActivity(Activity act) {
		this.score.add(calcActScore(act.getStartTime().seconds(), act.getEndTime().seconds(), act));
	}

	@Override
	public void handleLastActivity(Activity act) {
		this.handleOvernightActivity(act);
		this.firstActivity = null;
	}


	private static final class Score {

		private double actPerforming_util = INITIAL_SCORE;
		private double actPerforming_s = INITIAL_SCORE;
		private double actWaiting_util = INITIAL_SCORE;
		private double actWaiting_s = INITIAL_SCORE;
		private double actLateArrival_util = INITIAL_SCORE;
		private double actLateArrival_s = INITIAL_SCORE;

		private double actEarlyDeparture_util = INITIAL_SCORE;
		private double actEarlyDeparture_s = INITIAL_SCORE;

		private void add(Score s) {
			actPerforming_util += s.actPerforming_util;
			actPerforming_s += s.actPerforming_s;
			actWaiting_util += s.actWaiting_util;
			actWaiting_s += s.actWaiting_s;
			actLateArrival_util += s.actLateArrival_util;
			actLateArrival_s += s.actLateArrival_s;
			actEarlyDeparture_util += s.actEarlyDeparture_util;
			actEarlyDeparture_s += s.actEarlyDeparture_s;
		}

	}

}

