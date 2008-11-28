/* *********************************************************************** *
 * project: org.matsim.*
 * AppraisalScorer.java
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

package playground.marcel;

import java.util.TreeMap;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.population.Act;
import org.matsim.population.ActUtilityParameters;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.routes.CarRoute;
import org.matsim.roadpricing.CalcPaidToll;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.scoring.ScoringFunction;
import org.matsim.utils.misc.Time;

public class AppraisalScorer implements ScoringFunction {

	private final CalcPaidToll tollCalc;

	protected final Person person;
	protected final Plan plan;
	protected double lastTime = 0.0;
	protected double score = 0.0;
	protected int index = 0; // the current position in plan.actslegs
	protected String firstActType = null;
	protected double firstActTime = Time.UNDEFINED_TIME;
	protected final int lastActIndex;

	public final int persontype;

	public double scoreWaiting = 0.0;
	public double scoreLateArrival = 0.0;
	public double scoreEarlyDeparture = 0.0;
	public double scoreTraveling = 0.0;
	public double scorePerforming = 0.0;
	public double scoreDistance = 0.0;
	public double tollAmount = 0.0;
	public double scoreStuck = 0.0;

	public double[] traveltime = {-1.0, -1.0};
	public double workduration = 0.0;
	public double homeduration = 0.0;

	private final RoadPricingScheme toll;

	private static boolean initialized = false;

	public final static String CONFIG_MODULE = "planCalcScore";

	public final static String CONFIG_WAITING = "waiting";
	public final static String CONFIG_LATE_ARRIVAL = "lateArrival";
	public final static String CONFIG_EARLY_DEPARTURE = "earlyDeparture";
	public final static String CONFIG_TRAVELING = "traveling";
	public final static String CONFIG_PERFORMING = "performing";
	public final static String CONFIG_LEARNINGRATE = "learningRate";
	public final static String CONFIG_DISTANCE_COST = "distanceCost";

	protected static final TreeMap<String, ActUtilityParameters> utilParams = new TreeMap<String, ActUtilityParameters>();
	protected static double marginalUtilityOfWaiting = Double.NaN;
	protected static double marginalUtilityOfLateArrival = Double.NaN;
	protected static double marginalUtilityOfEarlyDeparture = Double.NaN;
	protected static double marginalUtilityOfTraveling = Double.NaN;
	protected static double marginalUtilityOfPerforming = Double.NaN;
	protected static double marginalUtilityOfDistance = Double.NaN;
	protected static double abortedPlanScore = Double.NaN;
	protected static double learningRate = Double.NaN;

	public AppraisalScorer(final Plan plan, final CalcPaidToll tollCalc, final RoadPricingScheme toll) {
		init();

		this.tollCalc = tollCalc;
		this.toll = toll;
		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getActsLegs().size() - 1;
		this.persontype = calcPersontype();
	}

	private int calcPersontype() {
		Link homeLink = ((Act)(this.plan.getActsLegs().get(0))).getLink();
		Link workLink = ((Act)(this.plan.getActsLegs().get(2))).getLink();
		boolean homeInside = this.toll.getLinkIds().contains(homeLink.getId());
		boolean workInside = this.toll.getLinkIds().contains(workLink.getId());

		if (!homeInside && !workInside) return 0;
		if (!homeInside && workInside) return 1;
		if (homeInside && !workInside) return 2;
		return 3;
	}

	/* At the moment, the following values are all static's. But in the longer run,
	 * they should be agent-specific or facility-specific values...
	 */
	private static void init() {
		if (initialized) return;

		Config config = Gbl.getConfig();

		marginalUtilityOfWaiting = config.charyparNagelScoring().getWaiting() / 3600.0;
		marginalUtilityOfLateArrival = config.charyparNagelScoring().getLateArrival() / 3600.0;
		marginalUtilityOfEarlyDeparture = config.charyparNagelScoring().getEarlyDeparture() / 3600.0;
		marginalUtilityOfTraveling = config.charyparNagelScoring().getTraveling() / 3600.0;
		marginalUtilityOfPerforming = config.charyparNagelScoring().getPerforming() / 3600.0;

		marginalUtilityOfDistance = config.charyparNagelScoring().getMarginalUtlOfDistance();

		learningRate = config.charyparNagelScoring().getLearningRate();

		abortedPlanScore = Math.min(
				Math.min(marginalUtilityOfLateArrival, marginalUtilityOfEarlyDeparture),
				Math.min(marginalUtilityOfTraveling, marginalUtilityOfWaiting)) * 3600.0 * 24.0;
		// TODO: 24 has to be replaced by a variable like scenarioConfig::scenario_dur

		readUtilityValues();

		initialized = true;
	}

	public final double calcActScore(final double arrivalTime, final double departureTime, final Act act) {

		ActUtilityParameters params = utilParams.get(act.getType());
		if (params == null) {
			throw new RuntimeException("Could not find utility param for " + act.getType());
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
		double openingTime = params.getOpeningTime();
		double closingTime = params.getClosingTime();
		double activityStart = arrivalTime;
		double activityEnd = departureTime;

		if (openingTime >= 0 && closingTime >= 0) {
			if ((openingTime > departureTime) || (closingTime < arrivalTime)) {
				// agent could not perform action
				activityStart = departureTime;
				activityEnd = departureTime;
			} else {
				if (arrivalTime < openingTime) {
					activityStart = openingTime;
				}
				if (closingTime < departureTime) {
					activityEnd = closingTime;
				}
			}
		}
		double duration = activityEnd - activityStart;

		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			double partialScore = marginalUtilityOfWaiting * (activityStart - arrivalTime);
			this.scoreWaiting += partialScore;
			tmpScore += partialScore;
		}

		// disutility if too late

		double latestStartTime = params.getLatestStartTime();
		if (latestStartTime >= 0 && activityStart > latestStartTime) {
			double partialScore = marginalUtilityOfLateArrival * (activityStart - latestStartTime);
			this.scoreLateArrival += partialScore;
			tmpScore += partialScore;
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = params.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = marginalUtilityOfPerforming * typicalDuration
					* Math.log((duration / 3600.0) / params.getZeroUtilityDuration());
			double utilWait = marginalUtilityOfWaiting * duration;
			double partialScore = Math.max(0, Math.max(utilPerf, utilWait));
			this.scorePerforming += partialScore;
			tmpScore += partialScore;
		} else {
			double partialScore = 2*marginalUtilityOfLateArrival*Math.abs(duration);
			this.scorePerforming += partialScore;
			tmpScore += partialScore;
		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if (earliestEndTime >= 0 && activityEnd < earliestEndTime) {
			double partialScore = marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
			this.scoreEarlyDeparture += partialScore;
			tmpScore += partialScore;
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			double partialScore = marginalUtilityOfWaiting * (departureTime - activityEnd);
			this.scoreWaiting += partialScore;
			tmpScore += partialScore;
		}

		// disutility if duration was too short
		double minimalDuration = params.getMinimalDuration();
		if (minimalDuration >= 0 && duration < minimalDuration) {
			double partialScore = marginalUtilityOfEarlyDeparture * (minimalDuration - duration);
			this.scoreEarlyDeparture += partialScore;
			tmpScore += partialScore;
		}

		if (act.getType().equals("h")) {
			this.homeduration = duration;
		} else {
			this.workduration = duration;
		}

		return tmpScore;
	}

	public double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double tmpScore = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double dist = 0.0; // distance in meters

		if (marginalUtilityOfDistance != 0.0) {
			/* we only as for the route when we have to calculate a distance cost,
			 * because route.getDist() may calculate the distance if not yet
			 * available, which is quite an expensive operation
			 */
			CarRoute route = leg.getRoute();
			dist = route.getDist();
			/* TODO the route-distance does not contain the length of the first or
			 * last link of the route, because the route doesn't know those. Should
			 * be fixed somehow, but how? MR, jan07
			 */
		}

		double partialScore = travelTime * marginalUtilityOfTraveling	+ marginalUtilityOfDistance * dist;
		this.scoreTraveling += partialScore;
		tmpScore += partialScore;

		this.traveltime[leg.getNum()] = travelTime;

		return tmpScore;
	}

	public static double getStuckPenalty() {
		return abortedPlanScore;
	}

	public static double getLearningRate() {
		return learningRate;
	}

	/**
	 * reads all activity utility values from the config-file
	 */
	private static final void readUtilityValues() {
		int i=0;
		Config config = Gbl.getConfig();

		while (true) {
			String type = config.findParam("planCalcScore", "activityType_" + i);
			if (type == null) break;
			int priority = Integer.parseInt(config.getParam("planCalcScore", "activityPriority_" + i));
			String typDuration = config.getParam("planCalcScore", "activityTypicalDuration_" + i);
			int typDurationSecs = (int)Time.parseTime(typDuration); // TODO [MR] switch to double for times
			ActUtilityParameters params = new ActUtilityParameters(type, priority, typDurationSecs);

			String paramValue;
			int paramValueSecs;

			paramValue = config.findParam("planCalcScore", "activityMinimalDuration_" + i);
			if (paramValue != null && !paramValue.equals("")) {
				paramValueSecs = (int)Time.parseTime(paramValue); // TODO [MR] switch to double for times, and below too!
				params.setMinimalDuration(paramValueSecs);
			}
			paramValue = config.findParam("planCalcScore", "activityOpeningTime_" + i);
			if (paramValue != null && !paramValue.equals("")) {
				paramValueSecs = (int)Time.parseTime(paramValue);
				params.setOpeningTime(paramValueSecs);
			}
			paramValue = config.findParam("planCalcScore", "activityLatestStartTime_" + i);
			if (paramValue != null && !paramValue.equals("")) {
				paramValueSecs = (int)Time.parseTime(paramValue);
				params.setLatestStartTime(paramValueSecs);
			}
			paramValue = config.findParam("planCalcScore", "activityEarliestEndTime_" + i);
			if (paramValue != null && !paramValue.equals("")) {
				paramValueSecs = (int)Time.parseTime(paramValue);
				params.setEarliestEndTime(paramValueSecs);
			}
			paramValue = config.findParam("planCalcScore", "activityClosingTime_" + i);
			if (paramValue != null && !paramValue.equals("")) {
				paramValueSecs = (int)Time.parseTime(paramValue);
				params.setClosingTime(paramValueSecs);
			}

			utilParams.put(type, params);

			i++;
		}
	}

	public void startActivity(final double time, final Act act) {
		// we do not use this "event" at the moment
	}

	public void endActivity(final double time) {
		// we do not use this "event" at the moment
	}

	public void addMoney(final double utility) {
		// not used at the moment
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
		this.scoreStuck += getStuckPenalty();
		this.score += getStuckPenalty();
	}

	public void finish() {
		finishAgent();
	}

	public double getScore() {
		return this.score;
	}

	private void handleAct(final double time) {
		Act act = (Act)this.plan.getActsLegs().get(this.index);
		if (this.index == 0) {
			this.firstActTime = time;
			this.firstActType = act.getType();
		} else if (this.index == this.lastActIndex) {
			this.score += calcActScore(this.lastTime, this.firstActTime + 24*3600, act);
		} else {
			this.score += calcActScore(this.lastTime, time, act);
		}
		this.index++;
	}

	private void handleLeg(final double time) {
		Leg leg = (Leg)this.plan.getActsLegs().get(this.index);
		this.score += calcLegScore(this.lastTime, time, leg);
		this.index++;
	}

	/**
	 * Finishes the calculation of a single agent's score. Overwrite this method
	 * if you want to modify the score before it's assigned to the plan, but do
	 * not forget to call <code>super.finishAgent(info)</code> in your method.
	 */
	protected void finishAgent() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
		this.tollAmount = this.tollCalc.getAgentToll(this.person.getId().toString());
		this.score -= this.tollAmount;
	}

	public void reset() {
	}
}
