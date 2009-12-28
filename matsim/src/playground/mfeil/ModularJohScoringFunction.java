/* *********************************************************************** *
 * project: org.matsim.*
 * JohScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C)2008 by the members listed in the COPYING,  *
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

package playground.mfeil;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.interfaces.ActivityScoring;
import org.matsim.core.scoring.interfaces.BasicScoring;
import org.matsim.core.utils.misc.Time;

/**
 * New scoring function following Joh's dissertation:
 * <blockquote>
 *  <p>Joh, C.-H. (2004) <br>
 *  Measuring and Predicting Adaptation in Multidimensional Activity-Travel Patterns,<br>
 *  Bouwstenen 79, Eindhoven University Press, Eindhoven.</p>
 * </blockquote>
 *
 *
 * @author mfeil
 */

public class ModularJohScoringFunction implements ActivityScoring, BasicScoring {

	protected final Person person;
	protected final Plan plan;
	protected final Id id;

	protected double score;
	private double lastTime;
	private int index; // the current position in plan.actslegs
	private double firstActTime;
	private final int lastActIndex;

	private static final double INITIAL_LAST_TIME = 0.0;
	private static final int INITIAL_INDEX = 0;
	private static final double INITIAL_FIRST_ACT_TIME = Time.UNDEFINED_TIME;
	private static final double INITIAL_SCORE = 0.0;

	/* TODO [MR] the following field should not be public, but I need a way to reset the initialized state
	 * for the test cases.  Once we have the better config-objects, where we do not need to parse the
	 * values each time from a string, this whole init() concept can be removed and with this
	 * also this public member.  -marcel, 07aug07
	 */
	public static boolean initialized = false;

	/** True if one at least one of marginal utilities for performing, waiting, being late or leaving early is not equal to 0. */
	private static boolean scoreActs = true;

	private static final Logger log = Logger.getLogger(ModularJohScoringFunction.class);
	
	private static final TreeMap<String, JohActUtilityParameters> utilParams = new TreeMap<String, JohActUtilityParameters>();
	private static double marginalUtilityOfWaiting = -6;
	private static double marginalUtilityOfLateArrival = -40; 
	private static double marginalUtilityOfEarlyDeparture = -6;
	private static double marginalUtilityOfTraveling = -12;
	private static double marginalUtilityOfTravelingPT = -12; // public transport
	private static double marginalUtilityOfTravelingWalk = -12;
	private static double marginalUtilityOfDistance = 0;
	
	private static final double uMin_home = 0;
	private static final double uMin_work = 0;
	private static final double uMin_shopping = 0;
	private static final double uMin_leisure = 0;
	private static final double uMax_home = 120;
	private static final double uMax_work= 80;
	private static final double uMax_shopping = 40;
	private static final double uMax_leisure = 60;
	private static final double alpha_home = 6;
	private static final double alpha_work = 4;
	private static final double alpha_shopping = 1;
	private static final double alpha_leisure = 2;
	private static final double beta_home = 1.2;
	private static final double beta_work = 1.2;
	private static final double beta_shopping = 1.2;
	private static final double beta_leisure = 1.2;
	private static final double gamma_home = 1;
	private static final double gamma_work = 1;
	private static final double gamma_shopping = 1;
	private static final double gamma_leisure = 1;
	
	private static final double uMin_education = 0;
	private static final double uMax_education = 95;
	private static final double alpha_education = 3;
	private static final double beta_education = 1.2;
	private static final double gamma_education = 1;
	
	
	
	public ModularJohScoringFunction(final Plan plan) {
		init();
		this.reset();

		this.plan = plan;
		this.person = this.plan.getPerson();
		this.lastActIndex = this.plan.getPlanElements().size() - 1;
		this.id = plan.getPerson().getId();
	}

	public void reset() {
		this.lastTime = INITIAL_LAST_TIME;
		this.index = INITIAL_INDEX;
		this.firstActTime = INITIAL_FIRST_ACT_TIME;
		this.score = INITIAL_SCORE;

	}
	/*
	// the activity is currently handled by startLeg()
	public void startActivity(final double time, final Activity act) {
	}

	public void endActivity(final double time) {
	}
	*/
	public void startActivity(final double time, final Activity act) {
		this.lastTime = time;
	}

	public void endActivity(final double time) {
		handleAct(time);
		this.lastTime = time;
	}
	/*
	public void startLeg(final double time, final Leg leg) {
		
		if (this.index % 2 == 0) {
			handleAct(time);
		}
		this.lastTime = time;
	}
	
	public void addMoney(final double amount){
		this.score+=amount; //linear mapping of money to utility
	}

	public void endLeg(final double time) {
		handleLeg(time);
		this.lastTime = time;
	}
	 */
	public void agentStuck(final double time) {
		this.lastTime = time;
	}

	public void finish() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
	}

	public double getScore() {
		return this.score;
	}

	private static void init() {
		if (initialized) return;
		utilParams.clear();
		readUtilityValues();
		scoreActs = ((marginalUtilityOfWaiting != 0) ||
				(marginalUtilityOfLateArrival != 0) || (marginalUtilityOfEarlyDeparture != 0));
		initialized = true;
	}

	private final double calcActScore(final double arrivalTime, final double departureTime, final ActivityImpl act) {
		
		JohActUtilityParameters params = utilParams.get(act.getType());
		
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
			tmpScore += marginalUtilityOfWaiting / 3600 * (activityStart - arrivalTime);
		}

		// disutility if too late
		double latestStartTime = params.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += marginalUtilityOfLateArrival / 3600 * (activityStart - latestStartTime);
		}

		// utility of performing an action
		if (duration > 0) {
			//if (duration>=3600){ // to adjust Planomat to TimeModeChoicer minimum duration
				/* NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW NEW */
				double utilPerf = params.getUMin() + (params.getUMax()-params.getUMin())/(java.lang.Math.pow(1+params.getGamma()*java.lang.Math.exp(params.getBeta()*(params.getAlpha()-(duration/3600))),1/params.getGamma()));
				double utilWait = marginalUtilityOfWaiting / 3600 * duration;
				tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
			//}
		} else if (duration<0){
			tmpScore += 2*marginalUtilityOfLateArrival / 3600 *Math.abs(duration);
			//tmpScore += -100000; // Negative durations results in refusal of plan

		}

		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			tmpScore += marginalUtilityOfEarlyDeparture / 3600 * (earliestEndTime - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += marginalUtilityOfWaiting / 3600 * (departureTime - activityEnd);
		}
		//if (this.id.toString().equals("10")) System.out.println (tmpScore);
		return tmpScore;
	}

	protected double[] getOpeningInterval(final ActivityImpl act) {

		JohActUtilityParameters params = utilParams.get(act.getType());
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
			tmpScore += travelTime * marginalUtilityOfTraveling / 3600 + marginalUtilityOfDistance / 3600 * dist;
		} else if (TransportMode.pt.equals(leg.getMode())) {
			tmpScore += travelTime * marginalUtilityOfTravelingPT / 3600 + marginalUtilityOfDistance / 3600 * dist;
		} else if (TransportMode.walk.equals(leg.getMode())) {
			tmpScore += travelTime * marginalUtilityOfTravelingWalk / 3600 + marginalUtilityOfDistance / 3600 * dist;
		} else {
			// use the same values as for "car"
			tmpScore += travelTime * marginalUtilityOfTraveling / 3600 + marginalUtilityOfDistance / 3600 * dist;
		}

		return tmpScore;
	}
	
	/*
	private static double getStuckPenalty() {
		return abortedPlanScore;
	}
	 */
	
	/**
	 * reads all activity utility values from the config-file
	 */
	private static final void readUtilityValues() {
		
		/* TODO @MF To be replaced by config file*/
		String type;
		JohActUtilityParameters actParams;
			
		type = "home";
		actParams = new JohActUtilityParameters("home", uMin_home, uMax_home, alpha_home, beta_home, gamma_home);
		utilParams.put(type, actParams);
		
		type = "work";
		actParams = new JohActUtilityParameters("work", uMin_work, uMax_work, alpha_work, beta_work, gamma_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
		actParams.setLatestStartTime(10*3600);
		actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);

		type = "shopping";
		actParams = new JohActUtilityParameters("shopping", uMin_shopping, uMax_shopping, alpha_shopping, beta_shopping, gamma_shopping);
		actParams.setOpeningTime(10*3600);
		actParams.setClosingTime(18*3600);
		utilParams.put(type, actParams);

		type = "leisure";
		actParams = new JohActUtilityParameters("leisure", uMin_leisure, uMax_leisure, alpha_leisure, beta_leisure, gamma_leisure);
		actParams.setOpeningTime(18*3600);
		actParams.setClosingTime(22*3600);			
		utilParams.put(type, actParams);
		
		
		//TODO @ mfeil: bad programming style, I know...
		type = "education_higher";
		actParams = new JohActUtilityParameters("education_higher", uMin_education, uMax_education, alpha_education, beta_education, gamma_education);
		actParams.setOpeningTime(7*3600);
		actParams.setClosingTime(16*3600);
		actParams.setLatestStartTime(9*3600);
		actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_kindergarten";
		actParams = new JohActUtilityParameters("education_kindergarten", uMin_education, uMax_education, alpha_education, beta_education, gamma_education);
		actParams.setOpeningTime(7*3600);
		actParams.setClosingTime(16*3600);
		actParams.setLatestStartTime(9*3600);
		actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_other";
		actParams = new JohActUtilityParameters("education_other", uMin_education, uMax_education, alpha_education, beta_education, gamma_education);
		actParams.setOpeningTime(7*3600);
		actParams.setClosingTime(16*3600);
		actParams.setLatestStartTime(9*3600);
		actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_primary";
		actParams = new JohActUtilityParameters("education_primary", uMin_education, uMax_education, alpha_education, beta_education, gamma_education);
		actParams.setOpeningTime(7*3600);
		actParams.setClosingTime(16*3600);
		actParams.setLatestStartTime(9*3600);
		actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "education_secondary";
		actParams = new JohActUtilityParameters("education_secondary", uMin_education, uMax_education, alpha_education, beta_education, gamma_education);
		actParams.setOpeningTime(7*3600);
		actParams.setClosingTime(16*3600);
		actParams.setLatestStartTime(9*3600);
		actParams.setEarliestEndTime(12*3600);
		utilParams.put(type, actParams);
		
		type = "shop";
		actParams = new JohActUtilityParameters("shop", uMin_shopping, uMax_shopping, alpha_shopping, beta_shopping, gamma_shopping);
		actParams.setOpeningTime(10*3600);
		actParams.setClosingTime(18*3600);
		utilParams.put(type, actParams);
		
		type = "work_sector2";
		actParams = new JohActUtilityParameters("work_sector2", uMin_work, uMax_work, alpha_work, beta_work, gamma_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
		actParams.setLatestStartTime(10*3600);
		actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);
		
		type = "work_sector3";
		actParams = new JohActUtilityParameters("work_sector3", uMin_work, uMax_work, alpha_work, beta_work, gamma_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
		actParams.setLatestStartTime(10*3600);
		actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);
		
		type = "tta";
		actParams = new JohActUtilityParameters("tta", uMin_work, uMax_home, alpha_home, beta_home, gamma_home);
		actParams.setOpeningTime(3*3600);
		actParams.setClosingTime(24*3600);
		utilParams.put(type, actParams);
		
		type = "w";
		actParams = new JohActUtilityParameters("w", uMin_work, uMax_work, alpha_work, beta_work, gamma_work);
		actParams.setOpeningTime(8*3600);
		actParams.setClosingTime(18*3600);
		actParams.setLatestStartTime(10*3600);
		actParams.setEarliestEndTime(15*3600);
		utilParams.put(type, actParams);
		
		type = "h";
		actParams = new JohActUtilityParameters("h", uMin_home, uMax_home, alpha_home, beta_home, gamma_home);
		utilParams.put(type, actParams);

		
	}

	private void handleAct(final double time) {
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
		this.index+=2;
	}
}
