/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.ActivityUtilityParameters;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.misc.Time;

/**
 * This is the default scoring function for MATSim, referring to:
 *
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369–397.</p>
 * </blockquote>
 *
 * The scoring function takes
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
 * </dl>
 *
 * @author mrieser
 */

public class CharyparNagelReportingScoringFunction implements ScoringFunction {

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

	/* TODO [MR] the following field should not be public, but I need a way to reset the initialized state
	 * for the test cases.  Once we have the better config-objects, where we do not need to parse the
	 * values each time from a string, this whole init() concept can be removed and with this
	 * also this public member.  -marcel, 07aug07
	 */
	public static boolean initialized = false;

	/** True if one at least one of marginal utilities for performing, waiting, being late or leaving early is not equal to 0. */
	private static boolean scoreActs = true;

	private static final Logger log = Logger.getLogger(CharyparNagelReportingScoringFunction.class);

	public CharyparNagelReportingScoringFunction(final Plan plan) {
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
		this.score += amount;
	}

	public void finish() {
		if (this.index == this.lastActIndex) {
			handleAct(24*3600); // handle the last act
		}
	}

	public double getScore() {
		return this.score;
	}
	public double getDudur(ActivityImpl a){
		if((dudur.size()>0)&&dudur.keySet().contains(a)){
			return dudur.get(a);
			}else{
				return -999;
			}
	}
	public double getDuw(ActivityImpl a){
		if((duw.size()>0)&&duw.keySet().contains(a)){
			return duw.get(a);
			}else{
				return 0;
			}
	}
	public double getDus(ActivityImpl a){
		if((dus.size()>0)&&dus.keySet().contains(a)){
			return dus.get(a);
			}else{
				return 0;
			}
	}
	public double getDula(ActivityImpl a){
		if((dula.size()>0)&&dula.keySet().contains(a)){
		return dula.get(a);
		}else
			return 0;
	}
	public double getDued(ActivityImpl a){
		if((dued.size()>0)&&dued.keySet().contains(a)){
		return dued.get(a);
		}else{
			return 0;
		}
		
	}
	public double getDuld(ActivityImpl a){
		if((duld.size()>0)&&duld.keySet().contains(a)){
			return duld.get(a);
			}else{
				return 0;
			}
	}
	public double getDulegt(LegImpl l){
		if((dulegt.size()>0)&&dulegt.keySet().contains(l)){
			return dulegt.get(l);
			}else{
				return 0;
			}
	}
	public double getDulegd(LegImpl l){
		if((dulegd.size()>0)&&dulegd.keySet().contains(l)){
			return dulegd.get(l);
			}else{
				return 0;
			}
	}
	
	public double getUdur(ActivityImpl a){
		if((udur.size()>0)&&udur.keySet().contains(a)){
			return udur.get(a);
			}else{
				return -999;
			}
	}
	public double getUw(ActivityImpl a){
		if((uw.size()>0)&&uw.keySet().contains(a)){
			return uw.get(a);
			}else{
				return 0;
			}
	}
	public double getUs(ActivityImpl a){
		if((us.size()>0)&&us.keySet().contains(a)){
			return us.get(a);
			}else{
				return 0;
			}
	}
	public double getUla(ActivityImpl a){
		if((ula.size()>0)&&ula.keySet().contains(a)){
		return ula.get(a);
		}else
			return 0;
	}
	public double getUed(ActivityImpl a){
		if((ued.size()>0)&&ued.keySet().contains(a)){
		return ued.get(a);
		}else{
			return 0;
		}
		
	}
	public double getUld(ActivityImpl a){
		if((uld.size()>0)&&uld.keySet().contains(a)){
			return uld.get(a);
			}else{
				return 0;
			}
	}
	public double getUlegt(LegImpl l){
		if((ulegt.size()>0)&&ulegt.keySet().contains(l)){
			return ulegt.get(l);
			}else{
				return 0;
			}
	}
	public double getUlegd(LegImpl l){
		if((ulegd.size()>0)&&ulegd.keySet().contains(l)){
			return ulegd.get(l);
			}else{
				return 0;
			}
	}
	/* At the moment, the following values are all static's. But in the longer run,
	 * they should be agent-specific or facility-specific values...
	 */
	private static final TreeMap<String, ActivityUtilityParameters> utilParams = new TreeMap<String, ActivityUtilityParameters>();
	private static double marginalUtilityOfWaiting = Double.NaN;
	private static double marginalUtilityOfLateArrival = Double.NaN;
	private static double marginalUtilityOfEarlyDeparture = Double.NaN;
	protected static double marginalUtilityOfTraveling = Double.NaN;
	private static double marginalUtilityOfTravelingPT = Double.NaN; // public transport
	private static double marginalUtilityOfTravelingWalk = Double.NaN;
	private static double marginalUtilityOfPerforming = Double.NaN;
	private static double marginalUtilityOfDistance = Double.NaN;
	private static double abortedPlanScore = Double.NaN;
	
	private LinkedHashMap<ActivityImpl,Double> udur=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> uw=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> us=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> ula=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> ued=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> uld=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<LegImpl,Double> ulegt=new LinkedHashMap<LegImpl,Double>();
	private LinkedHashMap<LegImpl,Double> ulegd = new LinkedHashMap<LegImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> dudur=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> duw=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> dus=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> dula=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> dued=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<ActivityImpl,Double> duld=new LinkedHashMap<ActivityImpl,Double>();
	private LinkedHashMap<LegImpl,Double> dulegt=new LinkedHashMap<LegImpl,Double>();
	private LinkedHashMap<LegImpl,Double> dulegd = new LinkedHashMap<LegImpl,Double>();
	
	private static void init() {
		if (initialized) return;

		utilParams.clear();
		CharyparNagelScoringConfigGroup params = Gbl.getConfig().charyparNagelScoring();
		marginalUtilityOfWaiting = params.getWaiting() / 3600.0;
		marginalUtilityOfLateArrival = params.getLateArrival() / 3600.0;
		marginalUtilityOfEarlyDeparture = params.getEarlyDeparture() / 3600.0;
		marginalUtilityOfTraveling = params.getTraveling() / 3600.0;
		marginalUtilityOfTravelingPT = params.getTravelingPt() / 3600.0;
		marginalUtilityOfTravelingWalk = params.getTravelingWalk() / 3600.0;
		marginalUtilityOfPerforming = params.getPerforming() / 3600.0;

		marginalUtilityOfDistance = params.getMarginalUtlOfDistanceCar();

		abortedPlanScore = Math.min(
				Math.min(marginalUtilityOfLateArrival, marginalUtilityOfEarlyDeparture),
				Math.min(marginalUtilityOfTraveling, marginalUtilityOfWaiting)) * 3600.0 * 24.0; // SCENARIO_DURATION
		// TODO 24 has to be replaced by a variable like scenario_dur (see also other places below)

		readUtilityValues();
		scoreActs = ((marginalUtilityOfPerforming != 0) || (marginalUtilityOfWaiting != 0) ||
				(marginalUtilityOfLateArrival != 0) || (marginalUtilityOfEarlyDeparture != 0));
		initialized = true;
	}

	private final double calcActScore(final double arrivalTime, final double departureTime, final ActivityImpl act) {

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

		//
				
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
			uw.put(act,marginalUtilityOfWaiting * (activityStart - arrivalTime));
		}else uw.put(act,0.);
		duw.put(act, marginalUtilityOfWaiting);
		
		// disutility if too late

		double latestStartTime = params.getLatestStartTime();
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += marginalUtilityOfLateArrival * (activityStart - latestStartTime);
			ula.put(act,marginalUtilityOfLateArrival * (activityStart - latestStartTime));
		}else ula.put(act,0.);
		dula.put(act,marginalUtilityOfLateArrival);
		
		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = params.getTypicalDuration();

		if (duration > 0) {
			double utilPerf = marginalUtilityOfPerforming * typicalDuration
					* Math.log((duration / 3600.0) / params.getZeroUtilityDuration());
			double utilWait = marginalUtilityOfWaiting * duration;
			double dutilPerf =marginalUtilityOfPerforming * typicalDuration/duration;
			double dutilWait =marginalUtilityOfWaiting;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
			udur.put(act,Math.max(0, Math.max(utilPerf, utilWait)));			
			dudur.put(act,Math.max(0, Math.max(dutilPerf, dutilWait)));
		} else {
			tmpScore += 2*marginalUtilityOfLateArrival*Math.abs(duration);
			udur.put(act,2*marginalUtilityOfLateArrival*Math.abs(duration));
			dudur.put(act,2*marginalUtilityOfLateArrival);
		}
				
		// disutility if stopping too early
		double earliestEndTime = params.getEarliestEndTime();
		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
			tmpScore += marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
			ued.put(act,marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd));
		}else ued.put(act,0.);
		dued.put(act,marginalUtilityOfEarlyDeparture);
		
		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += marginalUtilityOfWaiting * (departureTime - activityEnd);
			uld.put(act,marginalUtilityOfWaiting * (departureTime - activityEnd));
		}else uld.put(act, 0.);
		duld.put(act,marginalUtilityOfWaiting);
		
		// disutility if duration was too short
		double minimalDuration = params.getMinimalDuration();
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += marginalUtilityOfEarlyDeparture * (minimalDuration - duration);
			us.put(act,marginalUtilityOfEarlyDeparture * (minimalDuration - duration));
		}else {
			us.put(act, 0.);
		}
		dus.put(act,marginalUtilityOfEarlyDeparture);
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
		double dtmpScore=0.;
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		double dist = 0.0; // distance in meters
		
		if (marginalUtilityOfDistance != 0.0) {
			/* we only as for the route when we have to calculate a distance cost,
			 * because route.getDist() may calculate the distance if not yet
			 * available, which is quite an expensive operation
			 */
			RouteWRefs route = leg.getRoute();
			dist = route.getDistance();
			/* TODO the route-distance does not contain the length of the first or
			 * last link of the route, because the route doesn't know those. Should
			 * be fixed somehow, but how? MR, jan07
			 */
			/* TODO in the case of within-day replanning, we cannot be sure that the
			 * distance in the leg is the actual distance driven by the agent.
			 */
		}
		if (TransportMode.car.equals(leg.getMode())) {
			tmpScore=travelTime * marginalUtilityOfTraveling - marginalUtilityOfDistance * dist; 
		} else if (TransportMode.pt.equals(leg.getMode())) {
			tmpScore= travelTime * marginalUtilityOfTravelingPT - marginalUtilityOfDistance * dist;
		} else if (TransportMode.walk.equals(leg.getMode())) {
			tmpScore= travelTime * marginalUtilityOfTravelingWalk - marginalUtilityOfDistance * dist;
		} else {
			// use the same values as for "car"
			tmpScore= travelTime * marginalUtilityOfTraveling - marginalUtilityOfDistance * dist;
		}
		dulegd.put(leg,dtmpScore);
		ulegd.put(leg,tmpScore);
		return tmpScore;
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

	private void handleAct(final double time) {
		double actScore=0;
		ActivityImpl act = (ActivityImpl)this.plan.getPlanElements().get(this.index);
		if (this.index == 0) {
			this.firstActTime = time;
		} else if (this.index == this.lastActIndex) {
			String lastActType = act.getType();
			if (lastActType.equals(((ActivityImpl) this.plan.getPlanElements().get(0)).getType())) {
				// the first Act and the last Act have the same type
				actScore= calcActScore(this.lastTime, this.firstActTime + 24*3600, act); // SCENARIO_DURATION
				this.score += actScore;
			} else {
				if (scoreActs) {
					log.warn("The first and the last activity do not have the same type. The correctness of the scoring function can thus not be guaranteed.");
					// score first activity
					ActivityImpl firstAct = (ActivityImpl)this.plan.getPlanElements().get(0);
					actScore=calcActScore(0.0, this.firstActTime, firstAct);
					this.score += actScore;
					// score last activity
					actScore=calcActScore(this.lastTime, 24*3600, act); // SCENARIO_DURATION
					this.score += actScore;
				}
			}
		} else {
			actScore=calcActScore(this.lastTime, time, act);
			this.score += actScore;
		}
//		System.out.print(act.getType()+"\t"+act.getFacilityId()+"\t"+udur+"\t"+uw+"\t"+ula+"\t"+ued+"\t"+uld+"\t"+us+"|3|");
//		System.out.print(act.getType()+"\t"+act.getFacilityId()+"\t"+actScore+"|3|");
		this.index++;
	}

	private void handleLeg(final double time) {
		LegImpl leg = (LegImpl)this.plan.getPlanElements().get(this.index);
//		this.score += calcLegScore(this.lastTime, time, leg);
		double temp=calcLegScore(this.lastTime, time, leg);
		this.score += temp;
		double dtemp=marginalUtilityOfTraveling+marginalUtilityOfDistance;
		dulegt.put(leg,dtemp);
		ulegt.put(leg, temp);
//		System.out.print("\n|1|"+plan.getPerson().getId()+"\t"+uleg+"\t|2|");
		this.index++;
	}

}

