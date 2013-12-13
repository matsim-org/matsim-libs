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

package playground.telaviv.locationchoice.matsimdc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.locationchoice.BestReplyDestinationChoice;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.facilityload.FacilityPenalty;
import org.matsim.contrib.locationchoice.facilityload.ScoringPenalty;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.telaviv.locationchoice.CalculateDestinationChoice;
import playground.telaviv.zones.ZoneMapping;

public class DCActivityScoringFunction extends org.matsim.contrib.locationchoice.bestresponse.scoring.DCActivityScoringFunction {
	static final Logger log = Logger.getLogger(DCActivityScoringFunction.class);
	private TelAvivDestinationScoring destinationChoiceScoring;	
	private Plan plan;
	private final CharyparNagelScoringParameters params;
	public static final int DEFAULT_PRIORITY = 1;
	private final HashMap<String, Double> zeroUtilityDurations = new HashMap<String, Double>();
	private ActTypeConverter converter;
	private ObjectAttributes prefs;
	private DestinationChoiceBestResponseContext dcContext;
	private List<ScoringPenalty> penalty = null;
		
	public DCActivityScoringFunction(Plan plan, final TreeMap<Id, FacilityPenalty> facilityPenalties, 
			DestinationChoiceBestResponseContext dcContext, ZoneMapping zoneMapping, CalculateDestinationChoice dcCalculator) {
		super(plan, facilityPenalties, dcContext);
		this.destinationChoiceScoring = new TelAvivDestinationScoring(dcContext, zoneMapping, dcCalculator);
		this.plan = plan;
		this.params = dcContext.getParams();
		this.converter = dcContext.getConverter();
		this.prefs = dcContext.getPrefsAttributes();
		this.dcContext = dcContext;
		this.penalty = new Vector<ScoringPenalty>();
	}
	
	@Override
	public void finish() {				
		super.finish();	
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof Activity) {
				this.score += destinationChoiceScoring.getDestinationScore((PlanImpl)plan, (ActivityImpl)pe, 
						BestReplyDestinationChoice.useScaleEpsilonFromConfig);
				
				this.score += destinationChoiceScoring.getZonalScore((PlanImpl)plan, (ActivityImpl)pe);
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
	
	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {
		
		if (act.getType().equals("pt interaction")) return 0.0;

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

			double latestStartTime = (Double)this.prefs.getAttribute(this.plan.getPerson().getId().toString(), "latestStartTime_" + act.getType());	
			if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
				tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
			}

			// utility of performing an action, duration is >= 1, thus log is no problem
			double typicalDuration = (Double)this.prefs.getAttribute(this.plan.getPerson().getId().toString(), "typicalDuration_" + act.getType());			
			// initialize zero utility durations here for better code readability, because we only need them here
			double zeroUtilityDuration;
			if (this.zeroUtilityDurations.containsKey(act.getType())) {
				zeroUtilityDuration = this.zeroUtilityDurations.get(act.getType());
			} else {
				zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / DCActivityScoringFunction.DEFAULT_PRIORITY);
				this.zeroUtilityDurations.put(act.getType(), zeroUtilityDuration);
			}
			
			// disutility if stopping too early
			double earliestEndTime = (Double)this.prefs.getAttribute(this.plan.getPerson().getId().toString(), "earliestEndTime_" + act.getType());	
			if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (earliestEndTime - activityEnd);
			}

			// disutility if going to away to late
			if (activityEnd < departureTime) {
				tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
			}

			// disutility if duration was too short
			double minimalDuration = (Double)this.prefs.getAttribute(this.plan.getPerson().getId().toString(), "minimalDuration_" + act.getType());	
			if ((minimalDuration >= 0) && (duration < minimalDuration)) {
				tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
			}

			if (duration > 0) {
				
				double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
						* Math.log((duration / 3600.0) / zeroUtilityDuration);
				
				double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
				tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
				
				if (this.dcContext.getScaleEpsilon().isFlexibleType(this.converter.convertType(act.getType())) &&
						Double.parseDouble(this.dcContext.getScenario().getConfig().locationchoice().getRestraintFcnExp()) > 0.0 &&
						Double.parseDouble(this.dcContext.getScenario().getConfig().locationchoice().getRestraintFcnFactor()) > 0.0) {
					
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
	protected double[] getOpeningInterval(Activity act) {
		return super.getOpeningInterval(act);
	}
}
