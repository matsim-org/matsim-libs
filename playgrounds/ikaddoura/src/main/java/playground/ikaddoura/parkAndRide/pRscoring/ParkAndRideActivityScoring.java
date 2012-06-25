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

package playground.ikaddoura.parkAndRide.pRscoring;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ActivityUtilityParameters;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideConstants;

/**
 * modified normal ActivityScoringFunction to ignore "pt interaction" and "parkAndRide"
 * and adding a transfer penalty if Park'n'Ride-Activity is performed
 * @author Ihab
 */
public class ParkAndRideActivityScoring implements ActivityScoring, BasicScoring {

	protected double score;
	private double currentActivityStartTime;
	private double firstActivityEndTime;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;

	private static int firstLastActWarning = 0;

	private final CharyparNagelScoringParameters params;
	private Activity currentActivity;
	private boolean firstAct = true;

	private Activity firstActivity;
	private PlanCalcScoreConfigGroup config;
	private double transferPenalty;

	private static final Logger log = Logger.getLogger(ParkAndRideActivityScoring.class);

	public ParkAndRideActivityScoring(final CharyparNagelScoringParameters params, PlanCalcScoreConfigGroup config, double transferPenalty) {
		this.params = params;
		this.config = config;
		this.transferPenalty = transferPenalty;
		this.reset();
	}

	@Override
	public void reset() {
		this.firstAct = true;
		this.currentActivityStartTime = INITIAL_LAST_TIME;
		this.firstActivityEndTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;
	}

	@Override
	public void startActivity(final double time, final Activity act) {
		assert act != null;
		this.currentActivity = act;
		this.currentActivityStartTime = time;
	}

	@Override
	public void endActivity(final double time, final Activity act) {
		assert act != null;
		assert currentActivity == null || currentActivity.getType().equals(act.getType());
		if (this.firstAct) {
			this.firstActivityEndTime = time;
			this.firstActivity = act;
			this.firstAct = false;
		} else {
			this.score += calcActScore(this.currentActivityStartTime, time, act);
		}
		currentActivity = null;
	}

	@Override
	public void finish() {
		if (this.currentActivity != null) {
			handleOvernightActivity(this.currentActivity); 
		} else {
			if (this.firstActivity != null) {
				handleMorningActivity();
			}
			// Else, no activity has started so far.
			// This probably means that the plan contains at most one activity.
			// We cannot handle that correctly, because we do not know what it is.
		}
	}

	@Override
	public double getScore() {
		return this.score;
	}

	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {

		double tmpScore = 0.0;
		
		if (act.getType().toString().equals(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
			
			double penalty = this.transferPenalty;
//			double penalty = this.config.getUtilityOfLineSwitch() * 3.0;
//			System.out.println("******************************* Park'n'Ride penalty: " + penalty);
			tmpScore += penalty;
		}

		if (act.getType().toString().equals(PtConstants.TRANSIT_ACTIVITY_TYPE) || act.getType().toString().equals(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
					
		} else {
			ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
			if (actParams == null) {
				throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
						"(module name=\"planCalcScore\" in the config file).");
			}
	
			double[] openingInterval = this.getOpeningInterval(act);
			double openingTime = openingInterval[0];
			double closingTime = openingInterval[1];
	
			double activityStart = arrivalTime;
			double activityEnd = departureTime;
	
			if ((openingTime >= 0) && (arrivalTime < openingTime)) {
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
	
			if (duration > 0) {
					
//					System.out.println("Scored Activity: "+act.getType());
					
					double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
							* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration());
					double utilWait = this.params.marginalUtilityOfWaiting_s * duration;	
					tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
				
			} else {
				tmpScore += 2*this.params.marginalUtilityOfLateArrival_s*Math.abs(duration);
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

	protected double[] getOpeningInterval(final Activity act) {

		ActivityUtilityParameters actParams = this.params.utilParams.get(act.getType());
		if (actParams == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters " +
					"(module name=\"planCalcScore\" in the config file).");
		}

		double openingTime = actParams.getOpeningTime();
		double closingTime = actParams.getClosingTime();

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{openingTime, closingTime};

		return openInterval;
	}

	private void handleOvernightActivity(Activity lastActivity) {
		assert firstActivity != null;
		assert lastActivity != null;
		if (lastActivity.getType().equals(this.firstActivity.getType())) {
			// the first Act and the last Act have the same type
			this.score += calcActScore(this.currentActivityStartTime, this.firstActivityEndTime + 24*3600, lastActivity); // SCENARIO_DURATION
		} else {
			if (this.params.scoreActs) {
				if (firstLastActWarning <= 10) {
					log.warn("The first and the last activity do not have the same type. The correctness of the scoring function can thus not be guaranteed.");
					if (firstLastActWarning == 10) {
						log.warn("Additional warnings of this type are suppressed.");
					}
					firstLastActWarning++;
				}

				// score first activity
				this.score += calcActScore(0.0, this.firstActivityEndTime, firstActivity);
				// score last activity
				this.score += calcActScore(this.currentActivityStartTime, 24*3600, lastActivity); // SCENARIO_DURATION
			}
		}
	}

	private void handleMorningActivity() {
		assert firstActivity != null;
		// score first activity
		this.score += calcActScore(0.0, this.firstActivityEndTime, firstActivity);
	}

}

