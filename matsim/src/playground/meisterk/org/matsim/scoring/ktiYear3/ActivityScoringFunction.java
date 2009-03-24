/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityScoringFunction.java
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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.matsim.basic.v01.BasicOpeningTime;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.PlanElement;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.locationchoice.facilityload.ScoringPenalty;
import org.matsim.scoring.CharyparNagelScoringParameters;


/**
 * This class implements the activity scoring as used in Year 3 of the KTI project.
 * 
 * It has the following features:
 * 
 * <ul>
 * <li>use opening times from facilities, not from config. scoring function can process multiple opening time intervals per activity option</li>
 * <li>use typical durations from agents' desires, not from config</li>
 * <li>typical duration applies to the sum of all instances of an activity type, not to single instances (so agent finds out itself how much time to spend in which instance)</li>
 * <li>use facility load penalties from LocationChoiceScoringFunction</li>
 * </ul>
 * 
 * @author meisterk
 *
 */
public class ActivityScoringFunction extends
org.matsim.scoring.charyparNagel.ActivityScoringFunction {

	// TODO should be in person.desires
	public static final int DEFAULT_PRIORITY = 1;
	// TODO should be in person.desires
	// TODO differentiate in any way?
	public static final double MINIMUM_DURATION = 0.5 * 3600;

	private List<ScoringPenalty> penalty = new Vector<ScoringPenalty>();
	private TreeMap<Id, FacilityPenalty> facilityPenalties;
//	private TreeMap<String, Double> durations = new TreeMap<String, Double>();
	private TreeMap<String, Vector<Double>> activitiesStartsAndEnds = new TreeMap<String, Vector<Double>>();
	private double timeWaiting;
	private double timeTooShort;
	private TreeMap<String, Double> zeroUtilityDurations = null;

	private static final double INITIAL_TIME_WAITING = 0.0;
	private static final double INITIAL_TIME_TOO_SHORT = 0.0;
	private static final DayType DEFAULT_DAY = DayType.wed;
	private static final SortedSet<BasicOpeningTime> DEFAULT_OPENING_TIME = new TreeSet<BasicOpeningTime>();
	static {
		BasicOpeningTime defaultOpeningTime = new OpeningTimeImpl(ActivityScoringFunction.DEFAULT_DAY, Double.MIN_VALUE, Double.MAX_VALUE);
		ActivityScoringFunction.DEFAULT_OPENING_TIME.add(defaultOpeningTime);
	}
	
	public ActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		super(plan, params);
		this.facilityPenalties = facilityPenalties;
	}

	@Override
	protected double calcActScore(double arrivalTime, double departureTime,
			Activity act) {

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

		SortedSet<BasicOpeningTime> openTimes = null;
		// if no associated activity option exists, assume facility is always open
		if (act.getFacility().getActivityOption(act.getType()) == null) {
			openTimes = ActivityScoringFunction.DEFAULT_OPENING_TIME;
		} else {
			openTimes = act.getFacility().getActivityOption(act.getType()).getOpeningTimes(ActivityScoringFunction.DEFAULT_DAY);
		}

		for (BasicOpeningTime openTime : openTimes) {

			double openingTime = openTime.getStartTime();
			double closingTime = openTime.getEndTime();

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

			// - calculate duration by activity type, save it, but do not process it in activity performing
			Vector<Double> activityStartsAndEnds = null;
			if (this.activitiesStartsAndEnds.containsKey(act.getType())) {
				activityStartsAndEnds = new Vector<Double>();
				this.activitiesStartsAndEnds.put(act.getType(), activityStartsAndEnds);
			} else {
				activityStartsAndEnds = this.activitiesStartsAndEnds.get(act.getType());
			}
			
			
//			double oldDuration = 0.0;
//			if (this.durations.containsKey(act.getType())) {
//				oldDuration = this.durations.get(act.getType());
//			}
//			this.durations.put(act.getType(), oldDuration + duration);

			// disutility if too early
			if (arrivalTime < activityStart) {
				// agent arrives to early, has to wait
				this.timeWaiting += (activityStart - arrivalTime);
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				this.timeWaiting += (departureTime - activityEnd);
			}

			// disutility if duration was too short
			double minimalDuration = ActivityScoringFunction.MINIMUM_DURATION;
			if ((minimalDuration >= 0) && (duration < minimalDuration)) {
				this.timeTooShort += (minimalDuration - duration);
			}

			/* Penalty due to facility load: --------------------------------------------
			 * Store the temporary score to reduce it in finish() proportionally 
			 * to score and dep. on facility load.
			 * TODO: maybe checking if activity is movable for this person (discussion)
			 */
			if (!act.getType().startsWith("h")) {
				this.penalty.add(new ScoringPenalty(
						activityStart, 
						activityEnd, 
						this.facilityPenalties.get(act.getFacility().getId()), 
						tmpScore));
			}
			//---------------------------------------------------------------------------

		}

		// it's difficult to caclulate the late/early terms with multiple instances of the same activity type
		// we do not use these terms now anyway so they are commented out
		// disutility if too late
//		double latestStartTime = actParams.getLatestStartTime();
//		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
//		tmpScore += this.params.marginalUtilityOfLateArrival * (activityStart - latestStartTime);
//		}

//		// disutility if stopping too early
//		double earliestEndTime = actParams.getEarliestEndTime();
//		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
//		tmpScore += this.params.marginalUtilityOfEarlyDeparture * (earliestEndTime - activityEnd);
//		}

		// - process all other scoring elements except performing (done in finishPerformingActivities)
//		double typicalDuration = actParams.getTypicalDuration();

//		if (duration > 0) {
//		double utilPerf = this.params.marginalUtilityOfPerforming * typicalDuration
//		* Math.log((duration / 3600.0) / actParams.getZeroUtilityDuration());
//		double utilWait = this.params.marginalUtilityOfWaiting * duration;
//		tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
//		} else {
//		tmpScore += 2*this.params.marginalUtilityOfLateArrival*Math.abs(duration);
//		}

		return tmpScore;
	}

	@Override
	public void finish() {
		super.finish();
		this.finishPerformingActivities();
		this.finishFacilityPenalties();
	}

	protected void finishFacilityPenalties() {
		// copied from LocationChoiceScoringFunction
		// reduce score by penalty from capacity restraints
		Iterator<ScoringPenalty> pen_it = this.penalty.iterator();
		while (pen_it.hasNext()){
			ScoringPenalty penalty = pen_it.next();
			this.score -= penalty.getPenalty();
		}
		this.penalty.clear();
	}

	protected void finishPerformingActivities() {

		double zeroUtilityDuration;
		double typicalDuration;
		double duration;
		Vector<Double> activityStartsAndEnds = null;

		// initialize zero utility durations here for better code readability, because we only need them here
		Activity act;
		if (this.zeroUtilityDurations == null) {
			this.zeroUtilityDurations = new TreeMap<String, Double>();
			for (PlanElement planElement : this.plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					act = (Activity) planElement;
					// - get typical duration from desires rather than from config
					typicalDuration = this.person.getDesires().getActivityDuration(act.getType());
					zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / ActivityScoringFunction.DEFAULT_PRIORITY);
					this.zeroUtilityDurations.put(act.getType(), zeroUtilityDuration);
				}
			}
		}

		// process collected activity durations by activity type
		for (String actType : this.activitiesStartsAndEnds.keySet()) {
			// - get typical duration from desires rather than from config
			typicalDuration = this.person.getDesires().getActivityDuration(actType);
			activityStartsAndEnds = this.activitiesStartsAndEnds.get(actType);
			duration = 0.0;
			for (int ii = 0; ii < activityStartsAndEnds.size(); ii++) {
				if (ii % 2 == 0) {
					duration += activityStartsAndEnds.get(ii + 1) - activityStartsAndEnds.get(ii);
				}
			}
			if (duration > 0) {
				double utilPerf = this.params.marginalUtilityOfPerforming * typicalDuration
				* Math.log((duration / 3600.0) / this.zeroUtilityDurations.get(actType));
				double utilWait = this.params.marginalUtilityOfWaiting * duration;
				this.score += Math.max(0, Math.max(utilPerf, utilWait));
			} else {
				this.score += 2*this.params.marginalUtilityOfLateArrival*Math.abs(duration);
			}
			
		}
		
		this.score += this.timeWaiting * this.params.marginalUtilityOfWaiting;
		this.score += this.timeTooShort * this.params.marginalUtilityOfEarlyDeparture;

		this.activitiesStartsAndEnds.clear();
	}

	@Override
	public void reset() {
		super.reset();
		this.timeWaiting = ActivityScoringFunction.INITIAL_TIME_WAITING;
		this.timeTooShort = ActivityScoringFunction.INITIAL_TIME_TOO_SHORT;
	}

	public TreeMap<String, Vector<Double>> getActivitiesStartsAndEnds() {
		return activitiesStartsAndEnds;
	}

	public double getTimeWaiting() {
		return timeWaiting;
	}

	public double getTimeTooShort() {
		return timeTooShort;
	}
	
}
