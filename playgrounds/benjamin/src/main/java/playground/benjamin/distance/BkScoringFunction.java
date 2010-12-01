/* *********************************************************************** *
 * project: org.matsim.*																															*
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
package playground.benjamin.distance;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.ActivityUtilityParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class BkScoringFunction implements ScoringFunction {

	protected final Person person;
	protected final Plan plan;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs
	private double firstActTime;
	private final int lastActIndex;
	private static CharyparNagelScoringConfigGroup configGroup;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 0;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;
	private static boolean initialized = false;

	/** True if one at least one of marginal utilities for performing, waiting, being late or leaving early is not equal to 0. */
	private static boolean scoreActs = true;

	private static final Logger log = Logger.getLogger(BkScoringFunction.class);

	/* At the moment, the following values are all static's. But in the longer run,
	 * they should be agent-specific or facility-specific values...
	 */
	protected static final TreeMap<String, ActivityUtilityParameters> utilParams = new TreeMap<String, ActivityUtilityParameters>();
	protected static double marginalUtilityOfWaiting = Double.NaN;
	protected static double marginalUtilityOfLateArrival = Double.NaN;
	protected static double marginalUtilityOfEarlyDeparture = Double.NaN;
	protected static double marginalUtilityOfTraveling = Double.NaN;
	private static double marginalUtilityOfTravelingPT = Double.NaN; // public transport
	private static double marginalUtilityOfTravelingWalk = Double.NaN;
	protected static double marginalUtilityOfPerforming = Double.NaN;
	private static double marginalUtilityOfFuel = Double.NaN;
	private static double abortedPlanScore = Double.NaN;
  private static double marginalUtilityOfPtFare = - 0.0535d;



	public BkScoringFunction(final Plan plan, CharyparNagelScoringConfigGroup config) {
		configGroup = config;
		init();
		this.reset();

		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getPlanElements().size() - 1;
	}

	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.firstActTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;
	}

	public void startActivity(final double time, final Activity act) {
		// the activity is currently handled by startLeg()
	}

	public void endActivity(final double time) {
	}

	public void startLeg(final double time, final Leg leg) {
		if (this.index % 2 == 0) {
			// it seems we were not informed about activities
			handleAct(time);
		}
		this.lastTime = time;
	}

	public void endLeg(final double time) {
		handleLeg(time);
		this.lastTime = time;
	}

	public void agentStuck(final double time) {
		this.lastTime = time;
		this.score += getStuckPenalty();
	}

	public void addMoney(final double amount) {
		this.score += amount; // linear mapping of money to score
	}

	public void finish() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
	}

	public double getScore() {
		return this.score;
	}


	protected static void init() {
		if (initialized) return;

		utilParams.clear();
		marginalUtilityOfWaiting = configGroup.getWaiting_utils_hr() / 3600.0;
		marginalUtilityOfLateArrival = configGroup.getLateArrival_utils_hr() / 3600.0;
		marginalUtilityOfEarlyDeparture = configGroup.getEarlyDeparture_utils_hr() / 3600.0;
		marginalUtilityOfTraveling = configGroup.getTraveling_utils_hr() / 3600.0;
		marginalUtilityOfTravelingPT = configGroup.getTravelingPt_utils_hr() / 3600.0;
		marginalUtilityOfTravelingWalk = configGroup.getTravelingWalk_utils_hr() / 3600.0;
		marginalUtilityOfPerforming = configGroup.getPerforming_utils_hr() / 3600.0;

//		marginalUtilityOfPtFare = marginalUtilityOfPtFare;

//		marginalUtilityOfFuel = configGroup.getMarginalUtlOfDistanceCar();
		marginalUtilityOfFuel = configGroup.getMonetaryDistanceCostRateCar() * configGroup.getMarginalUtilityOfMoney() ;

		abortedPlanScore = Math.min(
				Math.min(marginalUtilityOfLateArrival, marginalUtilityOfEarlyDeparture),
				Math.min(marginalUtilityOfTraveling, marginalUtilityOfWaiting)) * 3600.0 * 24.0; // SCENARIO_DURATION
		// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)

		readUtilityValues(configGroup);
		scoreActs = ((marginalUtilityOfPerforming != 0) || (marginalUtilityOfWaiting != 0) ||
				(marginalUtilityOfLateArrival != 0) || (marginalUtilityOfEarlyDeparture != 0));
		initialized = true;
	}

	protected double calcActScore(final double arrivalTime, final double departureTime, final ActivityImpl act) {

		ActivityUtilityParameters params = utilParams.get(act.getType());
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
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
			tmpScore += marginalUtilityOfWaiting * (activityStart - arrivalTime);
		}

		// disutility if too late

		double latestStartTime = params.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += marginalUtilityOfLateArrival * (activityStart - latestStartTime);
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = params.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = marginalUtilityOfPerforming * typicalDuration
					* Math.log((duration / 3600.0) / params.getZeroUtilityDuration());
			double utilWait = marginalUtilityOfWaiting * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			tmpScore += 2*marginalUtilityOfLateArrival*Math.abs(duration);
		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			tmpScore += marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += marginalUtilityOfWaiting * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = params.getMinimalDuration();
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += marginalUtilityOfEarlyDeparture * (minimalDuration - duration);
		}

		return tmpScore;
	}

	protected double[] getOpeningInterval(final ActivityImpl act) {

		ActivityUtilityParameters params = utilParams.get(act.getType());
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}

		double openingTime = params.getOpeningTime();
		double closingTime = params.getClosingTime();

		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		double[] openInterval = new double[]{openingTime, closingTime};

		return openInterval;
	}

	protected double calcLegScore(final double departureTime, final double arrivalTime, final LegImpl leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double dist = 0.0; // distance in meters


		if (TransportMode.car.equals(leg.getMode())) {
			if (marginalUtilityOfFuel != 0.0) {
				/* we only as for the route when we have to calculate a distance cost,
				 * because route.getDist() may calculate the distance if not yet
				 * available, which is quite an expensive operation
				 */
				Route route = leg.getRoute();
				dist = route.getDistance();
				/* TODO the route-distance does not contain the length of the first or
				 * last link of the route, because the route doesn't know those. Should
				 * be fixed somehow, but how? MR, jan07
				 */
				/* TODO in the case of within-day replanning, we cannot be sure that the
				 * distance in the leg is the actual distance driven by the agent.
				 */
			}
			tmpScore += travelTime * marginalUtilityOfTraveling + marginalUtilityOfFuel * 0.12d/1000.0d * dist;

		}
		else if (TransportMode.pt.equals(leg.getMode())) {
			if (marginalUtilityOfPtFare != 0.0) {
				Route route = leg.getRoute();
				dist = route.getDistance();
			}
			tmpScore = tmpScore + travelTime * marginalUtilityOfTravelingPT + marginalUtilityOfPtFare * 0.28d/1000.0d * dist;
		}
		else if (TransportMode.walk.equals(leg.getMode())) {
			tmpScore += travelTime * marginalUtilityOfTravelingWalk;
		}
		else {
			// use the same values as for "car"
			tmpScore += travelTime * marginalUtilityOfTraveling + marginalUtilityOfFuel * dist;
		}

		return tmpScore;
	}

	private static double getStuckPenalty() {
		return abortedPlanScore;
	}

	/**
	 * reads all activity utility values from the config-file
	 */
	private static final void readUtilityValues(CharyparNagelScoringConfigGroup config) {
		for (ActivityParams params : config.getActivityParams()) {
			String type = params.getType();
			double priority = params.getPriority();
			double typDurationSecs = params.getTypicalDuration();
			ActivityUtilityParameters actParams = new ActivityUtilityParameters(type, priority, typDurationSecs);
			if (params.getMinimalDuration() >= 0) {
				actParams.setMinimalDuration(params.getMinimalDuration());
			}
			if (params.getOpeningTime() >= 0) {
				actParams.setOpeningTime(params.getOpeningTime());
			}
			if (params.getLatestStartTime() >= 0) {
				actParams.setLatestStartTime(params.getLatestStartTime());
			}
			if (params.getEarliestEndTime() >= 0) {
				actParams.setEarliestEndTime(params.getEarliestEndTime());
			}
			if (params.getClosingTime() >= 0) {
				actParams.setClosingTime(params.getClosingTime());
			}
			utilParams.put(type, actParams);
		}
	}

	protected void handleAct(final double time) {
		ActivityImpl act = (ActivityImpl)this.plan.getPlanElements().get(this.index);
		if (this.index == 0) {
			this.firstActTime = time;
		} else if (this.index == this.lastActIndex) {
			String lastActType = act.getType();
			if (lastActType.equals(((ActivityImpl) this.plan.getPlanElements().get(0)).getType())) {
				// the first Act and the last Act have the same type
				this.score += calcActScore(this.lastTime, this.firstActTime + 24*3600, act); // SCENARIO_DURATION
			} else {
				if (scoreActs) {
					log.warn("The first and the last activity do not have the same type. The correctness of the scoring function can thus not be guaranteed.");
					// score first activity
					ActivityImpl firstAct = (ActivityImpl)this.plan.getPlanElements().get(0);
					this.score += calcActScore(0.0, this.firstActTime, firstAct);
					// score last activity
					this.score += calcActScore(this.lastTime, 24*3600, act); // SCENARIO_DURATION
				}
			}
		} else {
			this.score += calcActScore(this.lastTime, time, act);
		}
		this.index++;
	}

	private void handleLeg(final double time) {
		LegImpl leg = (LegImpl)this.plan.getPlanElements().get(this.index);
		this.score += calcLegScore(this.lastTime, time, leg);
		this.index++;
	}

}
