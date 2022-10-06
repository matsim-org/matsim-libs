/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.frozenepsilons;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.OpeningTime;
import org.matsim.pt.PtConstants;
import org.matsim.utils.objectattributes.ObjectAttributes;

//import org.matsim.contrib.locationchoice.utils.ActTypeConverter;

// needs to be re-designed with delegation instead of inheritance. kai, oct'14
class DCActivityScoringFunction extends org.matsim.deprecated.scoring.functions.CharyparNagelActivityScoring {
	static final Logger log = LogManager.getLogger(DCActivityScoringFunction.class);
	private DestinationScoring destinationChoiceScoring;	
	private final ActivityFacilities facilities;
	private Plan plan;
	private final ScoringParameters params;
	public static final int DEFAULT_PRIORITY = 1;
	private final HashMap<String, Double> zeroUtilityDurations = new HashMap<String, Double>();
//	private ActTypeConverter converter;
	private ObjectAttributes prefs;
	private DestinationChoiceContext dcContext;
	private FrozenTastesConfigGroup dccg;
	private List<ScoringPenalty> penalty = null;

	// needs to be re-designed with delegation instead of inheritance. kai, oct'14
	public DCActivityScoringFunction(Plan plan, DestinationChoiceContext dcContext) {
		super(dcContext.getParams());
		this.destinationChoiceScoring = new DestinationScoring(dcContext);
		this.facilities = dcContext.getScenario().getActivityFacilities();
		this.plan = plan;
		this.params = dcContext.getParams();
//		this.converter = dcContext.getConverter();
		this.prefs = dcContext.getPrefsAttributes();
		this.dcContext = dcContext;
		this.dccg = (FrozenTastesConfigGroup) this.dcContext.getScenario().getConfig().getModule( FrozenTastesConfigGroup.GROUP_NAME );
		this.penalty = new Vector<ScoringPenalty>();
	}
	
	@Override
	@Deprecated // needs to be re-designed with delegation instead of inheritance. kai, oct'14
	public void finish() {
		super.finish();
		int activityIndex = -1 ;
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				activityIndex++ ;
				this.score += destinationChoiceScoring.getDestinationScore(  (Activity)pe, 
						BestReplyLocationChoiceStrategymodule.useScaleEpsilonFromConfig, activityIndex, this.plan.getPerson().getId() );
			}
		}
		// reduce score by penalty from capacity restraints
		Iterator<ScoringPenalty> pen_it = this.penalty.iterator();
		while (pen_it.hasNext()){
			ScoringPenalty penalty = pen_it.next();
			this.score -=penalty.getPenalty();
		}
		this.penalty.clear();
	}

	@Deprecated // needs to be re-designed with delegation instead of inheritance. kai, oct'14
	protected final double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {
		
		if (act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) return 0.0;

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

			OptionalTime[] openingInterval = this.getOpeningInterval(act);
			OptionalTime openingTime = openingInterval[0];
			OptionalTime closingTime = openingInterval[1];

			double activityStart = arrivalTime;
			double activityEnd = departureTime;

			if (openingTime.isDefined() && arrivalTime < openingTime.seconds()) {
				activityStart = openingTime.seconds();
			}
			if (closingTime.isDefined() && closingTime.seconds() < departureTime) {
				activityEnd = closingTime.seconds();
			}
			if (openingTime.isDefined() && closingTime.isDefined()
					&& (openingTime.seconds() > departureTime || closingTime.seconds() < arrivalTime)) {
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

		Gbl.assertNotNull(this.prefs);
		Gbl.assertNotNull( this.plan );
		Gbl.assertNotNull( this.plan.getPerson() );

		final OptionalTime latestStartTime = (OptionalTime) this.prefs.getAttribute( this.plan.getPerson().getId().toString(), "latestStartTime_" + act.getType() );

		//double latestStartTime = attribute;
		if (latestStartTime.isDefined() && (latestStartTime.seconds() >= 0) && (activityStart > latestStartTime.seconds())) {
			tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime.seconds());
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		OptionalTime typicalDuration = (OptionalTime)this.prefs.getAttribute(this.plan.getPerson().getId().toString(), "typicalDuration_" + act.getType());
		// initialize zero utility durations here for better code readability, because we only need them here
		double zeroUtilityDuration;
		if (this.zeroUtilityDurations.containsKey(act.getType())) {
			zeroUtilityDuration = this.zeroUtilityDurations.get(act.getType());
		} else {
			zeroUtilityDuration = (typicalDuration.seconds() / 3600.0) * Math.exp( -10.0 / (typicalDuration.seconds() / 3600.0) / DCActivityScoringFunction.DEFAULT_PRIORITY);
			this.zeroUtilityDurations.put(act.getType(), zeroUtilityDuration);
		}

		// disutility if stopping too early
		OptionalTime earliestEndTime = (OptionalTime)this.prefs.getAttribute(this.plan.getPerson().getId().toString(), "earliestEndTime_" + act.getType());
		if (earliestEndTime.isDefined() && (earliestEndTime.seconds() >= 0) && (activityEnd < earliestEndTime.seconds())) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime.seconds() - activityEnd);
		}

		// disutility if going to away to late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		OptionalTime minimalDuration = (OptionalTime)this.prefs.getAttribute(this.plan.getPerson().getId().toString(), "minimalDuration_" + act.getType());
		if (minimalDuration.isDefined() && (minimalDuration.seconds() >= 0) && (duration < minimalDuration.seconds())) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration.seconds() - duration);
		}

		if (duration > 0) {
			double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration.seconds()
					* Math.log((duration / 3600.0) / zeroUtilityDuration);

			double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));

			if (this.dcContext.getScaleEpsilon().isFlexibleType( act.getType() ) &&
					this.dccg.getRestraintFcnExp() > 0.0 &&
					this.dccg.getRestraintFcnFactor() > 0.0) {

					/* Penalty due to facility load: --------------------------------------------
					 * Store the temporary score to reduce it in finish() proportionally
					 * to score and dep. on facility load.
					 * TODO: maybe checking if activity is movable for this person (discussion)
					 */
					this.penalty.add(new ScoringPenalty(activityStart, activityEnd,
								this.dcContext.getFacilityPenalties().get(act.getFacilityId()), tmpScore));
					//---------------------------------------------------------------------------
			}
		} else {
			tmpScore += 2*this.params.marginalUtilityOfLateArrival_s*Math.abs(duration);
		}

		return tmpScore;
	}
	
	
	@Override
	@Deprecated // needs to be re-designed with delegation instead of inheritance. kai, oct'14
	protected OptionalTime[] getOpeningInterval(Activity act) {
		// openInterval has two values
		// openInterval[0] will be the opening time
		// openInterval[1] will be the closing time
		OptionalTime[] openInterval = {OptionalTime.undefined(), OptionalTime.undefined()};
		boolean foundAct = false;

		if (act.getType().contains("interaction") || // yyyy might be too loose. kai, feb'16
//				(act.getType().startsWith("h" ) && this.converter.isV1()) ||
				act.getType().equals("home")) {
			return openInterval;
		} // pt interaction and home always open
		
		ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());		
		Iterator<String> facilityActTypeIterator = facility.getActivityOptions().keySet().iterator();
		String facilityActType = null;
		Set<OpeningTime> opentimes = null;

		while (facilityActTypeIterator.hasNext() && !foundAct) {
			facilityActType = facilityActTypeIterator.next();
			if ( act.getType().equals( facilityActType )) { // TODO: check here actType conversions
				foundAct = true;
				// choose appropriate opentime: either wed, wkday or wk
				// if none is given, use undefined opentimes
				opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(facilityActType).getOpeningTimes();
				if (opentimes != null) {
					// ignoring lunch breaks with the following procedure:
					// if there is only one wed/wkday/wk open time interval, use it
					// if there are two or more, use the earliest start time and the latest end time
					double opening = Double.MAX_VALUE;
					double closing = Double.MIN_VALUE;

					for (OpeningTime opentime : opentimes) {
						opening = Math.min(opening, opentime.getStartTime());
						closing = Math.max(closing, opentime.getEndTime());
					}

					openInterval[0] = OptionalTime.defined(opening);
					openInterval[1] = OptionalTime.defined(closing);
				}
			}
		}
		if (!foundAct) {
			throw new RuntimeException("No suitable facility activity type found for activity " + act.getType() + 
			" and facility " + facility.getId() + ". Aborting...");
		}
		return openInterval;
	}
}
