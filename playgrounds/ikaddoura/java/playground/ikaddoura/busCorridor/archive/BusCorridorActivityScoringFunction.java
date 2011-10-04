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

package playground.ikaddoura.busCorridor.archive;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.scoring.ActivityUtilityParameters;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.utils.misc.Time;

/**
 * This is a re-implementation of the original CharyparNagel function, based on a
 * modular approach.
 * @see http://www.matsim.org/node/263
 * @author rashid_waraich
 */
public class BusCorridorActivityScoringFunction implements ActivityScoring, BasicScoring {


	protected final Person person;
	protected final Plan plan;

	protected double score;
	private double lastTime;
	protected int index; // the current position in plan.actslegs
	// yyyy [status pointer] weiteres Beispiel, wo ich das eigenständige Konstruieren des "status index" problematisch finde.  kai, jul'10
	private double firstActTime;
	protected final int lastActIndex;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 0;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;

	private static int firstLastActWarning = 0;

	/** The parameters used for scoring */
	protected final CharyparNagelScoringParameters params;

	private static final Logger log = Logger.getLogger(BusCorridorActivityScoringFunction.class);

	public BusCorridorActivityScoringFunction(final Plan plan, final CharyparNagelScoringParameters params) {
		this.params = params;
		this.reset();

		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getPlanElements().size() - 1;
	}

	@Override
	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.firstActTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;
	}

	@Override
	public void startActivity(final double time, final Activity act) {
		this.lastTime = time;
	}

	@Override
	public void endActivity(final double time) {
		handleAct(time);
		this.lastTime = time;
	}

	@Override
	public void finish() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
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
		double typicalDuration = actParams.getTypicalDuration();

		if (duration > 0) {
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

	protected void handleAct(final double time) {
		Activity act = (Activity)this.plan.getPlanElements().get(this.index);
		if (this.index == 0) {
			this.firstActTime = time;
		} else if (this.index == this.lastActIndex) {
			String lastActType = act.getType();
			if (lastActType.equals(((Activity) this.plan.getPlanElements().get(0)).getType())) {
				// the first Act and the last Act have the same type
				this.score += calcActScore(this.lastTime, this.firstActTime + 24*3600, act); // SCENARIO_DURATION
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
					Activity firstAct = (Activity)this.plan.getPlanElements().get(0);
					this.score += calcActScore(0.0, this.firstActTime, firstAct);
					// score last activity
					this.score += calcActScore(this.lastTime, 24*3600, act); // SCENARIO_DURATION
				}
			}
		} else {
			this.score += calcActScore(this.lastTime, time, act);
		}
		this.index+=2;
	}

}
