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

package playground.anhorni.surprice.scoring;

import java.util.Iterator;
import java.util.Set;



//import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTime.DayType;

import playground.anhorni.surprice.DayConverter;
import playground.anhorni.surprice.Surprice;

public class SurpriceActivityScoringFunction extends CharyparNagelActivityScoring {
	private CharyparNagelScoringParameters params;
	private final ActivityFacilities facilities;
	private DayType day;
	private Plan plan;
	double dayOffset = 0.0;
		
	public SurpriceActivityScoringFunction(Plan plan, CharyparNagelScoringParameters params, final Config config,
			ActivityFacilities facilities, String day) {
		super(params);
		this.facilities = facilities;
		this.day = DayConverter.getDayType(day);
		this.plan = plan;
		this.params = params;
		this.dayOffset = Surprice.days.indexOf(day) * 24.0 * 3600.0; // working with single days for the moment. No week-optimization!!!
		this.resetting();				
	}
	
	protected double calcActScore(final double arrivalTime, final double departureTime, final Activity act) {
		double tmpScore = 0.0;
		double[] openingInterval = new double[]{0.0, 24.0 * 3600.0};
		
		if (!act.getType().startsWith("h")) {
			openingInterval = this.getOpeningInterval(act);
		}
		
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
		double latestStartTime = closingTime;
		if ((latestStartTime >= 0) && (activityStart > latestStartTime)) {
			tmpScore += this.params.marginalUtilityOfLateArrival_s * (activityStart - latestStartTime);
		}

		// utility of performing an action, duration is >= 1, thus log is no problem
		double typicalDuration = ((PersonImpl) this.plan.getPerson()).getDesires().getActivityDuration(act.getType());
		if (duration > 0) {
			double zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0));
			double utilPerf = this.params.marginalUtilityOfPerforming_s * typicalDuration
					* Math.log((duration / 3600.0) / zeroUtilityDuration);
			double utilWait = this.params.marginalUtilityOfWaiting_s * duration;
			tmpScore += Math.max(0, Math.max(utilPerf, utilWait));
		} else {
			tmpScore += 2 * this.params.marginalUtilityOfLateArrival_s * Math.abs(duration);
		}

//		// disutility if stopping too early
//		double earliestEndTime = actParams.getEarliestEndTime();
//		if ((earliestEndTime >= 0) && (activityEnd < earliestEndTime)) {
//			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * f * (earliestEndTime - activityEnd);
//		}

		// disutility if going away too late
		if (activityEnd < departureTime) {
			tmpScore += this.params.marginalUtilityOfWaiting_s * (departureTime - activityEnd);
		}

		// disutility if duration was too short
		double minimalDuration = typicalDuration / 3.0;
		if ((minimalDuration >= 0) && (duration < minimalDuration)) {
			tmpScore += this.params.marginalUtilityOfEarlyDeparture_s * (minimalDuration - duration);
		}
		double prevVal = 0.0;
		if (this.plan.getPerson().getCustomAttributes().get(day + ".actScore") != null) {
			prevVal = (Double)this.plan.getPerson().getCustomAttributes().get(day + ".actScore");
		}
		this.plan.getPerson().getCustomAttributes().put(day + ".actScore", prevVal + tmpScore);
		return tmpScore;
	}
	
	protected double[] getOpeningInterval(Activity act) {
		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};
		double h = 3600.0;
		
		if (act.getType().equals("work") || act.getType().equals("business")) { 
			openInterval[0] = 6.0 * h; openInterval[1] = 22.0 * h;
		} else if (act.getType().equals("education")) { 
			openInterval[0] = 8.0 * h; openInterval[1] = 22.0 * h;
		} if (act.getType().equals("shop")) { 
			openInterval[0] = 7.0 * h; openInterval[1] = 19.5 * h;
		} else if (act.getType().equals("leisure")) { 
			openInterval[0] = 8.0 * h; openInterval[1] = 23.0 * h;
		}
		
		if (this.day.equals("sun")) {
			if (act.getType().equals("shop")) { 
				openInterval[0] = 10.0 * h; openInterval[1] = 16.0 * h;
			} else if (act.getType().equals("leisure")) {
				openInterval[0] = 8.5 * h; openInterval[1] = 23.0 * h;
			} 
		} else if (this.day.equals("sat")) {
			if (act.getType().equals("shop")) { 
				openInterval[0] = 8.5 * h; openInterval[1] = 18.0 * h;
			} else if (act.getType().equals("leisure")) {
				openInterval[0] = 8.5 * h; openInterval[1] = 24.0 * h;
			} 
		} 	
		return openInterval;
	}
	
//	protected double[] getOpeningInterval(Activity act) {
//
//		// openInterval has two values
//		// openInterval[0] will be the opening time
//		// openInterval[1] will be the closing time
//		double[] openInterval = new double[]{Time.UNDEFINED_TIME, Time.UNDEFINED_TIME};
//
//		boolean foundAct = false;
//
//		ActivityFacility facility = this.facilities.getFacilities().get(act.getFacilityId());
//		Iterator<String> facilityActTypeIterator = facility.getActivityOptions().keySet().iterator();
//		String facilityActType = null;
//		Set<OpeningTime> opentimes = null;
//
//		while (facilityActTypeIterator.hasNext() && !foundAct) {
//
//			facilityActType = facilityActTypeIterator.next();
//			if (act.getType().substring(0, 1).equals(facilityActType.substring(0, 1))) {
//				foundAct = true;
//
//				// choose appropriate opentime:
//				// if none is given, use undefined
//				opentimes = ((ActivityFacilityImpl) facility).getActivityOptions().get(facilityActType).getOpeningTimes();
//				if (opentimes != null) {
//					// ignoring lunch breaks with the following procedure:
//					// if there is only one wed/wkday open time interval, use it
//					// if there are two or more, use the earliest start time and the latest end time
//					openInterval[0] = Double.MAX_VALUE;
//					openInterval[1] = Double.MIN_VALUE;
//					for (OpeningTime opentime : opentimes) {
//						if (opentime.getStartTime() >= dayOffset && 
//								opentime.getEndTime() >= dayOffset &&
//								opentime.getStartTime() <= (dayOffset + 24.0 * 3600.0) &&
//								opentime.getEndTime() <= (dayOffset + 24.0 * 3600.0)) {
//						
//							openInterval[0] = Math.min(openInterval[0], opentime.getStartTime());
//							openInterval[1] = Math.max(openInterval[1], opentime.getEndTime());
//						}
//					}
//				}
//				// else return undefined				
//			}
//		}
//		if (!foundAct) {
//			throw new RuntimeException("No suitable facility activity type found. Aborting...");
//		}
//		return openInterval;
//	}
	
	public void resetting() {
		this.plan.getPerson().getCustomAttributes().put(day + ".actScore", null);
		super.reset();		
	}
}
