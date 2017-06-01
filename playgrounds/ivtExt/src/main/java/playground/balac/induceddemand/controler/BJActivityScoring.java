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

package playground.balac.induceddemand.controler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.scoring.functions.ActivityTypeOpeningIntervalCalculator;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.OpeningIntervalCalculator;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.misc.Time;

/**
 * This is an adaptation of CharyparNagel activity function using a piece-wise linear function
 * istead of the logarithmic.
 * @see <a href="http://www.matsim.org/node/263">http://www.matsim.org/node/263</a>
 * @author balac,janzen
 */
public final class BJActivityScoring implements org.matsim.core.scoring.SumScoringFunction.ActivityScoring {

	protected double score;
	private double currentActivityStartTime;
	private double firstActivityEndTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_FIRST_ACT_END_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;

	private static int firstLastActWarning = 0;
	private static short firstLastActOpeningTimesWarning = 0;

	private final ScoringParameters params;
	private final OpeningIntervalCalculator openingIntervalCalculator;

	private Activity firstActivity;
	
	private Map<String, Double> slopesAfterTypical;
	
	private static final Logger log = Logger.getLogger(BJActivityScoring.class);

	public BJActivityScoring(final ScoringParameters params, Map<String, Double> slopesAfterTypical) {
		this(params, new ActivityTypeOpeningIntervalCalculator(params));
		this.slopesAfterTypical = slopesAfterTypical;
	}

	public BJActivityScoring(final ScoringParameters params, final OpeningIntervalCalculator openingIntervalCalculator) {
		this.params = params;
		this.currentActivityStartTime = INITIAL_LAST_TIME;
		this.firstActivityEndTime = INITIAL_FIRST_ACT_END_TIME;
		this.score = INITIAL_SCORE;

		firstLastActWarning = 0 ;
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
		return this.score;
	}

	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {

		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		double tmpScore = 0.0;

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

			double[] openingInterval = openingIntervalCalculator.getOpeningInterval(act);
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

			double latestStartTime = actParams.getLatestStartTime();
			if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
				tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
			}

			// utility of performing an action, duration is >= 1, thus log is no problem
			double typicalDuration = actParams.getTypicalDuration();
			double umax = 10.0;
			
			if (act.getType().equals("shopping")) {
				umax /= 1.0;
			}
			double zeroUtilityDUration = (typicalDuration - 1800.0) / 3600.0;

			if (act.getType() == "shopping")
				zeroUtilityDUration = (typicalDuration - 2700.0) / 3600.0;
			
			if (duration < zeroUtilityDUration * 3600.0) {
				double utilPerf = this.params.marginalUtilityOfPerforming_s * 3600.0 * umax / (typicalDuration / 3600.0 - zeroUtilityDUration ) *
				(duration / 3600.0 - zeroUtilityDUration);
				tmpScore += utilPerf;
			}
			else if (duration > typicalDuration) {
				double utilPerf = this.params.marginalUtilityOfPerforming_s * 3600.0 * umax + this.slopesAfterTypical.get(act.getType()) * (duration - typicalDuration);
				tmpScore += utilPerf;

			}
			else {
				double utilPerf = this.params.marginalUtilityOfPerforming_s * 3600.0 * umax / (typicalDuration / 3600.0 - zeroUtilityDUration ) *
						(duration / 3600.0 - zeroUtilityDUration);
				tmpScore += utilPerf;
				
			}			
			// disutility if stopping too early
			double earliestEndTime = actParams.getEarliestEndTime();
			if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
			}

			// disutility if duration was too short
			double minimalDuration = actParams.getMinimalDuration();
			if ((minimalDuration >= 0) && (duration < minimalDuration)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
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
				double[] openInterval = openingIntervalCalculator.getOpeningInterval(lastActivity);
				if (openInterval[0] >= 0 || openInterval[1] >= 0){
					log.warn("There are opening or closing times defined for the first and last activity. The correctness of the scoring function can thus not be guaranteed.");
					log.warn("first activity: " + firstActivity ) ;
					log.warn("last activity: " + lastActivity ) ;
					if (firstLastActOpeningTimesWarning == 10) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActOpeningTimesWarning++;
				}
			}
			
			double calcActScore = calcActScore(this.currentActivityStartTime, this.firstActivityEndTime + 24*3600, lastActivity);
			this.score += calcActScore; // SCENARIO_DURATION
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
				this.score += calcActScore(0.0, this.firstActivityEndTime, firstActivity);
				// score last activity
				this.score += calcActScore(this.currentActivityStartTime, this.params.simulationPeriodInDays * 24*3600, lastActivity);
			}
		}
	}

	private void handleMorningActivity() {
		assert firstActivity != null;
		// score first activity
		this.score += calcActScore(0.0, this.firstActivityEndTime, firstActivity);
	}

	@Override
	public void handleFirstActivity(Activity act) {
		assert act != null;
		this.firstActivityEndTime = act.getEndTime();
		this.firstActivity = act;
	}

	@Override
	public void handleActivity(Activity act) {
		this.score += calcActScore(act.getStartTime(), act.getEndTime(), act);
	}

	@Override
	public void handleLastActivity(Activity act) {
		this.currentActivityStartTime = act.getStartTime();
		this.handleOvernightActivity(act);
		this.firstActivity = null;
	}

}

