/* *********************************************************************** *
 * project: org.matsim.*
 * DesiresAndOpenTimesActivityScoring.java
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

package playground.christoph.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.population.Desires;

/**
 * Same as CharyparNagelOpenTimesActivityScoring, but retrieves desired activity
 * durations from a person's desires instead of the config file.
 * 
 * Moreover, zeroUtilityDuration_h is calculated based on the agent's desired
 * activity duration and not taken from the actParams object.
 *
 * @author cdobler
 *
 */
public class DesiresAndOpenTimesActivityScoring extends CharyparNagelOpenTimesActivityScoring {

	private final CharyparNagelScoringParameters params;
	private final Desires desires;
	private final Id personId;
	
	public DesiresAndOpenTimesActivityScoring(Plan plan, final CharyparNagelScoringParameters params, final ActivityFacilities facilities) {
		super(params, facilities);
		this.desires = ((PersonImpl) plan.getPerson()).getDesires();
		this.params = params;
		this.personId = plan.getPerson().getId();
	}

	/*
	 * Copied the method from CharyparNagelActivityScoring. The only difference is
	 * that the desired activity duration is not taken from the config file but from
	 * the person's desires.
	 * It should be easy to move this part to a sub-method which could then be overwritten here. 
	 */
	@Override
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

			double latestStartTime = actParams.getLatestStartTime();
			if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
				tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
			}

			// utility of performing an action, duration is >= 1, thus log is no problem
//			double typicalDuration = actParams.getTypicalDuration();
			// the next three lines have been added
			Double typicalDuration = this.desires.getActivityDuration(act.getType());
			if (typicalDuration == null) {
				throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in person's desires " + personId.toString());
			}				

			if (duration > 0) {
//				double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
//						* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration_h());
				// the next four lines have also been inserted - copied and adapted them from ActivityUtilityParameters class
				double priority = 1.0;
//				double zeroUtilityDuration_h = CharyparNagelScoringUtils.computeZeroUtilityDuration_s(priority, typicalDuration) / 3600;
				double zeroUtilityDuration_h = actParams.getZeroUtilityDuration_h() ;

				double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
						* Math.log((duration / 3600.0) / zeroUtilityDuration_h);
				
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
}