/* *********************************************************************** *
 * project: org.matsim.*
 * SNScoringMaxFriendFoeRatio.java
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

package playground.jhackney.scoring;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.facilities.Activity;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Act;
import org.matsim.plans.ActUtilityParameters;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.scoring.ScoringFunction;
import org.matsim.utils.misc.Time;

/**
 * This is a modification of the default scoring function for MATSim:
 *
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369–397.</p>
 * </blockquote>
 *
 * The default scoring function takes
 * the following aspects into account when calculating a score:
 * <dl>
 * <dt>trip duration<dt>
 * <dd>The longer an agent is traveling, the lower its score will be usually.
 * The score will be reduced by an amount linear to the travel time.</dd>
 * <dt>activity duration</dt>
 * <dd>The time spent at an activity can be further distinguished into time the
 * agent spends <em>waiting</em> at the place because the facility is currently
 * closed, or time the agent spends <em>performing</em> the activity. The time
 * spent waiting will decrease the score by an amount linear to the time spent
 * waiting, while the time spent performing an activity increases the score
 * logarithmically.
 * </dd>
 * <dt>stuck penalty</dt>
 * <dd>If the agent is not able to move further on a link in the simulation,
 * the simulation may decide that the agent is stuck and remove it from the
 * simulation. In this case, the agent's score will be decrease with a
 * penalty.</dd>
 * <dt>distance</dt>
 * <dd>The score may decrease by an amount linear to the traveled distance.</dd>
 * </dl>
 *
 * The actual amounts for how much the score increases or decreases for the
 * different aspects are to be set in the configuration. Besides the penalty
 * for being stuck, the following penalties can also decrease the score:
 * <dl>
 * <dt>late arrival</dt>
 * <dd>If the agent arrives too late at an activity (as specified in the
 * configuration for each activity type), a penalty linear to the time being
 * late will be subtracted from the score.</dd>
 * <dt>early departure</dt>
 * <dd>If the agent leaves an activity too early (as specified in the
 * configuration for each activity type), a penalty linear to the time left
 * early will be subtracted from the score.</dd>
 * <dt>social context</dt>
 * <dd>A location is more attractive if more friends of the agent are there.
 * In this function, the ratio of friends/unknown others ([0,1]) is scaled
 * and added to the score.</dd>
 * </dl>
 *
 * @author mrieser, jhackney
 */

public class SNScoringMaxFriendFoeRatio implements ScoringFunction {
	/* TODO [MR] this class should take a ScoringFunctionConfigModule on
	 * initialization once the new config-modules are available, instead
	 * of reading everything from the config directly.
	 */

	protected final Person person;
	protected final Plan plan;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs
	private double firstActTime;
	private final int lastActIndex;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 0;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;

//	private HashMap<Activity, SocialAct> socialPlansMap= SNScoringGeneralFactory.getSocialActsMap();
	private SpatialScorer spatialScorer = SNScoringGeneralFactory.getScorer();
	private String factype = SNScoringGeneralFactory.getFacType();// Type of activity in which social group matters to Person;

	/* TODO [MR] the following field should not be public, but I need a way to reset the initialized state
	 * for the test cases.  Once we have the better config-objects, where we do not need to parse the
	 * values each time from a string, this whole init() concept can be removed and with this
	 * also this public member.  -marcel, 07aug07
	 */
	public static boolean initialized = false;

	/** True if one at least one of marginal utilities for performing, waiting, being late or leaving early is not equal to 0. */
	private static boolean scoreActs = true;

	private static final Logger log = Logger.getLogger(SNScoringMaxFriendFoeRatio.class);

	public SNScoringMaxFriendFoeRatio(final Plan plan) {
		init();
		this.reset();

		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getActsLegs().size() - 1;
	}

	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.firstActTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;
	}

	public void startActivity(final double time, final Act act) {
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

	public void finish() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
	}

	public double getScore() {
		return this.score;
	}

	/* At the moment, the following values are all static's. But in the longer run,
	 * they should be agent-specific or facility-specific values...
	 */
	public final static String CONFIG_MODULE = "planCalcScore";

	public final static String CONFIG_WAITING = "waiting";
	public final static String CONFIG_LATE_ARRIVAL = "lateArrival";
	public final static String CONFIG_EARLY_DEPARTURE = "earlyDeparture";
	public final static String CONFIG_TRAVELING = "traveling";
	public final static String CONFIG_TRAVELING_PT = "travelingPT";
	public final static String CONFIG_PERFORMING = "performing";
	public final static String CONFIG_LEARNINGRATE = "learningRate";
	public final static String CONFIG_DISTANCE_COST = "distanceCost";

	protected static final TreeMap<String, ActUtilityParameters> utilParams = new TreeMap<String, ActUtilityParameters>();
	private static double marginalUtilityOfWaiting = Double.NaN;
	private static double marginalUtilityOfLateArrival = Double.NaN;
	private static double marginalUtilityOfEarlyDeparture = Double.NaN;
	private static double marginalUtilityOfTraveling = Double.NaN;
	private static double marginalUtilityOfTravelingPT = Double.NaN; // public transport
	private static double marginalUtilityOfPerforming = Double.NaN;
	private static double distanceCost = Double.NaN;
	private static double abortedPlanScore = Double.NaN;

	private static void init() {
		if (initialized) return;

		CharyparNagelScoringConfigGroup params = Gbl.getConfig().charyparNagelScoring();
		marginalUtilityOfWaiting = params.getWaiting() / 3600.0;
		marginalUtilityOfLateArrival = params.getLateArrival() / 3600.0;
		marginalUtilityOfEarlyDeparture = params.getEarlyDeparture() / 3600.0;
		marginalUtilityOfTraveling = params.getTraveling() / 3600.0;
		marginalUtilityOfTravelingPT = params.getTravelingPt() / 3600.0;
		marginalUtilityOfPerforming = params.getPerforming() / 3600.0;

		distanceCost = params.getDistanceCost() / 1000.0;

		abortedPlanScore = Math.min(
				Math.min(marginalUtilityOfLateArrival, marginalUtilityOfEarlyDeparture),
				Math.min(marginalUtilityOfTraveling, marginalUtilityOfWaiting)) * 3600.0 * 24.0; // SCENARIO_DURATION
		// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)

		readUtilityValues();
		scoreActs = (marginalUtilityOfPerforming != 0 || marginalUtilityOfWaiting != 0 ||
				marginalUtilityOfLateArrival != 0 || marginalUtilityOfEarlyDeparture != 0);
		initialized = true;
	}

	private final double calcActScore(final double arrivalTime, final double departureTime, final Act act) {

		ActUtilityParameters params = utilParams.get(act.getType());
		if (params == null) {
			throw new IllegalArgumentException("acttype \"" + act.getType() + "\" is not known in utility parameters.");
		}

		double score = 0.0;

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

		if (openingTime >=  0 && arrivalTime < openingTime) {
			activityStart = openingTime;
		}
		if (closingTime >= 0 && closingTime < departureTime) {
			activityEnd = closingTime;
		}
		if (openingTime >= 0 && closingTime >= 0
				&& ((openingTime > departureTime) || (closingTime < arrivalTime))) {
			// agent could not perform action
			activityStart = departureTime;
			activityEnd = departureTime;
		}
		double duration = activityEnd - activityStart;

		// disutility if too early
		if (arrivalTime < activityStart) {
			// agent arrives to early, has to wait
			score += marginalUtilityOfWaiting * (activityStart - arrivalTime);
		}

		// disutility if too late

		double latestStartTime = params.getLatestStartTime();
		if (latestStartTime >= 0 && activityStart > latestStartTime) {
			score += marginalUtilityOfLateArrival * (activityStart - latestStartTime);
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = params.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = marginalUtilityOfPerforming * typicalDuration
			* Math.log((duration / 3600.0) / params.getZeroUtilityDuration());
			double utilWait = marginalUtilityOfWaiting * duration;
			score += Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			score += 2*marginalUtilityOfLateArrival*Math.abs(duration);
		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if (earliestEndTime >= 0 && activityEnd < earliestEndTime) {
			score += marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			score += marginalUtilityOfWaiting * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = params.getMinimalDuration();
		if (minimalDuration >= 0 && duration < minimalDuration) {
			score += marginalUtilityOfEarlyDeparture * (minimalDuration - duration);
		}

		//JH add positive utility for the ratio of friends to non-friends at
		// a factype activity
		//FIRST call SOCIAL.PLANS.GENERATOR AGAIN
		// get the random act
		// for all acts of type "factype"
		if(act.getType().equals(factype)){
//			int friend=0;
//			int foe=0;
//			Person p1=plan.getPerson();
////			Activity myActivity=p1.getKnowledge().getMentalMap().getActivity(act);
//			Activity myActivity = act.getFacility().getActivity(act.getType());
//
//			Vector<Person> othersThere = socialPlansMap.get(myActivity).getAttendeesInTimeWindow(p1, activityStart, activityEnd);
//			//for all agents in (social.plans.get(act){
//			Enumeration<Person> e = othersThere.elements();
//
//			while(e.hasMoreElements()){
//				Person p2 =(Person) e.nextElement();
//				if(p1.getKnowledge().getEgoNet().knows(p2)){
//					friend++;
//				}else{
//					foe++;
//				}
//			}
//			double ratio = (double)friend/(double)foe;
//			if(ratio>0. && ratio <= 1.){
//
//				if(myActivity.getType().equals("leisure")){
//					score+=10.*(double)friend/(double)foe;
		score+=10.*spatialScorer.scoreFriendtoFoeInTimeWindow(plan);
				}
//			}else{
//
//			}
//			// JH end
//		}
		return score;
	}

	private double calcLegScore(final double departureTime, final double arrivalTime, final Leg leg) {
		double score = 0.0;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double dist = 0.0; // distance in meters

		if (distanceCost != 0.0) {
			/* we only as for the route when we have to calculate a distance cost,
			 * because route.getDist() may calculate the distance if not yet
			 * available, which is quite an expensive operation
			 */
			Route route = leg.getRoute();
			dist = route.getDist();
			/* TODO the route-distance does not contain the length of the first or
			 * last link of the route, because the route doesn't know those. Should
			 * be fixed somehow, but how? MR, jan07
			 */
			/* TODO in the case of within-day replanning, we cannot be sure that the
			 * distance in the leg is the actual distance driven by the agent.
			 */
		}

		if ("car".equals(leg.getMode())) {
			score += travelTime * marginalUtilityOfTraveling - distanceCost * dist;
		} else if ("pt".equals(leg.getMode())) {
			score += travelTime * marginalUtilityOfTravelingPT - distanceCost * dist;
		} else {
			// use the same values as for "car"
			score += travelTime * marginalUtilityOfTraveling - distanceCost * dist;
		}

		return score;
	}

	private static double getStuckPenalty() {
		return abortedPlanScore;
	}

	/**
	 * reads all activity utility values from the config-file
	 */
	private static final void readUtilityValues() {
		CharyparNagelScoringConfigGroup config = Gbl.getConfig().charyparNagelScoring();

		for (ActivityParams params : config.getActivityParams()) {
			String type = params.getType();
			double priority = params.getPriority();
			double typDurationSecs = params.getTypicalDuration();
			ActUtilityParameters actParams = new ActUtilityParameters(type, priority, typDurationSecs);
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

	private void handleAct(final double time) {
		Act act = (Act)this.plan.getActsLegs().get(this.index);
		if (this.index == 0) {
			this.firstActTime = time;
		} else if (this.index == this.lastActIndex) {
			String lastActType = act.getType();
			if (lastActType.equals(((Act) this.plan.getActsLegs().get(0)).getType())) {
				// the first Act and the last Act have the same type
				this.score += calcActScore(this.lastTime, this.firstActTime + 24*3600, act); // SCENARIO_DURATION
			} else {
				if (scoreActs) {
					log.warn("The first and the last activity do not have the same type. The correctness of the scoring function can thus not be guaranteed.");
					// score first activity
					Act firstAct = (Act)this.plan.getActsLegs().get(0);
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
		Leg leg = (Leg)this.plan.getActsLegs().get(this.index);
		this.score += calcLegScore(this.lastTime, time, leg);
		this.index++;
	}

}
