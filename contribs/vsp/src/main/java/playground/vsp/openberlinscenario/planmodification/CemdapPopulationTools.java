/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.vsp.openberlinscenario.planmodification;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

import playground.vsp.openberlinscenario.cemdap.output.CemdapStopsParser;

/**
* @author ikaddoura
*/
public class CemdapPopulationTools {
	
	private double maxEndTime = 0.;

	private static final Logger log = Logger.getLogger(CemdapPopulationTools.class);

	public void setActivityTypesAccordingToDurationAndMergeOvernightActivities(Population population, double timeCategorySize, double dayStartTime) {
		log.info("First, setting activity types according to duration (time bin size: " + timeCategorySize + ")");				
		log.info("Second, merging evening and morning activity if they have the same (base) type.");

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				setActivityTypesAccordingToDuration(plan, timeCategorySize, dayStartTime);
				mergeOvernightActivities(plan);				
			}
		}
		log.info("maximum duration: sec: " + maxEndTime + " / hours: " + maxEndTime / 3600.);
	}
	
	private void mergeOvernightActivities(Plan plan) {
		if (plan.getPlanElements().size() > 1) {
			Activity firstActivity = (Activity) plan.getPlanElements().get(0);
			Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
			
			String firstBaseActivity = firstActivity.getType().split("_")[0];
			String lastBaseActivity = lastActivity.getType().split("_")[0];
			
			if (firstBaseActivity.equals(lastBaseActivity)) {
				double mergedDuration = Double.parseDouble(firstActivity.getType().split("_")[1]) + Double.parseDouble(lastActivity.getType().split("_")[1]);
				
				if (mergedDuration > maxEndTime) {
					maxEndTime = mergedDuration;
				}
				firstActivity.setType(firstBaseActivity + "_" + mergedDuration);
				lastActivity.setType(lastBaseActivity + "_" + mergedDuration);
			}
		} else {
			// skipping plans with just one activity
		}	
	}

	private static void setActivityTypesAccordingToDuration(Plan plan, double timeCategorySize, double dayStartTime) {
		boolean firstActivity = true;
		
		for (PlanElement pE : plan.getPlanElements()) {
			if (pE instanceof Activity) {
				Activity act = (Activity) pE;
				double duration = Double.MIN_VALUE;
				
				if (firstActivity) {
					// first activity (or stay home plan)
					if (act.getEndTime() > 0. && act.getEndTime() <= 24. * 3600) {
						// first activity (end time specified via an end time)
						duration = act.getEndTime() - dayStartTime;
					} else if (act.getMaximumDuration() > 0. && act.getMaximumDuration() <= 24. * 3600) {
						// first activity (end time specified via a duration)
						duration = act.getMaximumDuration();
					} else {
						// stay home plan
						duration = 24. * 3600;
					}
					firstActivity = false;
				} else {
					if (act.getAttributes().getAttribute(CemdapStopsParser.CEMDAP_STOP_DURATION_S_ATTRIBUTE_NAME) == null) {
						log.warn(plan.toString());
						log.warn(plan.getPlanElements().toString());
						throw new RuntimeException("All activities (except for the first one) should have a cemdapStopDuration_s attribute. Aborting...");
					} else {
						int cemdapStopDuration = (int) act.getAttributes().getAttribute(CemdapStopsParser.CEMDAP_STOP_DURATION_S_ATTRIBUTE_NAME);
						duration = 1. * cemdapStopDuration;
					}
				}
				
				int durationCategoryNr = (int) Math.round( (duration / timeCategorySize) ) ;		
				
				if (durationCategoryNr <= 0) {
					durationCategoryNr = 1;
				}
				
				String newType = act.getType() + "_" + (durationCategoryNr * timeCategorySize);
				act.setType(newType);
			}
		}
	}

	public double getMaxEndTime() {
		return maxEndTime;
	}
}