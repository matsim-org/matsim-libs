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

package playground.ikaddoura.utils.prepare;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

/**
* @author ikaddoura
*/

public class PopulationTools {

	private static final Logger log = Logger.getLogger(PopulationTools.class);

	public static void setActivityTypesAccordingToDurationAndMergeOvernightActivities(Population population, double timeCategorySize) {
				
		log.info("First, setting activity types according to duration (time bin size: " + timeCategorySize + ")");				
		log.info("Second, merging evening and morning activity if they have the same (base) type.");

		for (Person person : population.getPersons().values()) {
			
			for (Plan plan : person.getPlans()) {
					
				setActivityTypesAccordingToDuration(plan, timeCategorySize);
				mergeOvernightActivities(plan);				
			}
		}		
	}
	
	private static void mergeOvernightActivities(Plan plan) {
		
		if (plan.getPlanElements().size() > 1) {
		
			Activity firstActivity = (Activity) plan.getPlanElements().get(0);
			Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
			
			String firstBaseActivity = firstActivity.getType().split("_")[0];
			String lastBaseActivity = lastActivity.getType().split("_")[0];
			
			if (firstBaseActivity.equals(lastBaseActivity)) {
				
				double mergedDuration = Double.parseDouble(firstActivity.getType().split("_")[1]) + Double.parseDouble(lastActivity.getType().split("_")[1]);
				
				firstActivity.setType(firstBaseActivity + "_" + mergedDuration);
				lastActivity.setType(lastBaseActivity + "_" + mergedDuration);
			}
			
		} else {
			// skipping plans with just one activity
		}
	}

	private static void setActivityTypesAccordingToDuration(Plan plan, double timeCategorySize) {
				
		Leg previousLeg = null;
		double previousActEndTime = Double.NEGATIVE_INFINITY;
		
		for (PlanElement pE : plan.getPlanElements()) {
			
			if (pE instanceof Leg) {
				previousLeg = (Leg) pE;
			}
			
			if (pE instanceof Activity) {
			
				Activity act = (Activity) pE;

				double startTime = Double.NEGATIVE_INFINITY;
				double endTime = Double.NEGATIVE_INFINITY;
			
				if (act.getStartTime() >= 0. && act.getStartTime() <= 24 * 3600.) {
					startTime = act.getStartTime();
				} else {
					
					// trying to identify the activity start time via the arrival time...
					double arrivalTime = Double.NEGATIVE_INFINITY;
					if (previousLeg != null) {
						if (previousLeg.getDepartureTime() >= 0. && previousLeg.getDepartureTime() <= 24 * 3600. && previousLeg.getTravelTime() >= 0. && previousLeg.getTravelTime() <= 24 * 3600.) {
							arrivalTime = previousLeg.getDepartureTime() + previousLeg.getTravelTime();
						} else {
							if (previousLeg.getRoute().getTravelTime() >= 0. && previousLeg.getRoute().getTravelTime() <= 24 * 3600.) {
								arrivalTime = previousActEndTime + previousLeg.getRoute().getTravelTime();
							} else {
								log.warn("No meaningful activity start time and arrival time identified even though it is not the first activity...");
							}
						}
					} else {
						// First activity!
						
						// For the computation of activity durations: Assume the day start at 0 and end at 24 * 3600.
						arrivalTime = 0.;
					}
					
					startTime = arrivalTime;
				}
				
				if (startTime < 0.) {
					throw new RuntimeException("No meaningful start time identified. Aborting...");
				}
				
				if (act.getEndTime() >= 0. && act.getEndTime() <= 24 * 3600.) {
					endTime = act.getEndTime();
				} else {
					// Last activity!
					
					// For the computation of activity durations: Assume the day start at 0 and end at 24 * 3600.
					endTime = 24 * 3600.;
				}
				
				if (endTime < 0.) {
					throw new RuntimeException("No meaningful end time identified. Aborting...");
				}
						
				int durationCategoryNr = (int) Math.round( ((endTime - startTime) / timeCategorySize) ) ;		
				if (durationCategoryNr <= 0) {
					durationCategoryNr = 1;
				}
				
				String newType = act.getType() + "_" + (durationCategoryNr * timeCategorySize);
				act.setType(newType);
				
				previousActEndTime = endTime;
			}
		}
	}

	public static void removeNetworkSpecificInformation(Population population) {
		
		log.info("Removing network specific information (routes, link IDs)");
		
		for (Person person : population.getPersons().values()) {
			
			for (Plan plan : person.getPlans()) {
				
				for (PlanElement pE : plan.getPlanElements()) {
					
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						act.setLinkId(null);
					}
					
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						leg.setRoute(null);							
					}		
				}
			}
		}		
	}

	public static void addActivityTimesOfSelectedPlanToPersonAttributes(Population population) {
				
		log.info("Writing activity times in selected plan to person attributes.");
		
		for (Person person : population.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			String actStartEndTimes = null;
			
			Leg previousLeg = null;
			double previousActEndTime = Double.NEGATIVE_INFINITY;
			
			int pECounter = 0;
			
			for (PlanElement pE : selectedPlan.getPlanElements()) {
				
				if (pE instanceof Leg) {
					previousLeg = (Leg) pE;
				}
				
				if (pE instanceof Activity) {
				
					Activity act = (Activity) pE;
					
					double startTime = Double.NEGATIVE_INFINITY;
					double endTime = Double.NEGATIVE_INFINITY;
				
					if (act.getStartTime() >= 0. && act.getStartTime() <= 24 * 3600.) {
						startTime = act.getStartTime();
					} else {
						
						// trying to identify the activity start time via the arrival time...
						double arrivalTime = Double.NEGATIVE_INFINITY;
						if (previousLeg != null) {
							if (previousLeg.getDepartureTime() >= 0. && previousLeg.getDepartureTime() <= 24 * 3600. && previousLeg.getTravelTime() >= 0. && previousLeg.getTravelTime() <= 24 * 3600.) {
								arrivalTime = previousLeg.getDepartureTime() + previousLeg.getTravelTime();
							} else {
								if (previousLeg.getRoute().getTravelTime() >= 0. && previousLeg.getRoute().getTravelTime() <= 24 * 3600.) {
									arrivalTime = previousActEndTime + previousLeg.getRoute().getTravelTime();
								} else {
									log.warn("No meaningful activity start time and arrival time identified even though it is not the first activity...");
								}
							}
						} else {
							// First activity!
							
							arrivalTime = Double.NEGATIVE_INFINITY;
						}
						
						startTime = arrivalTime;
					}
					
					
					if (act.getEndTime() >= 0. && act.getEndTime() <= 24 * 3600.) {
						endTime = act.getEndTime();
					} else {
						// Last activity!
						
						endTime = Double.NEGATIVE_INFINITY;
					}
					
					previousActEndTime = endTime;
					
					// set opening and closing time for overnight activity to -Infinity
					
					if (pECounter == 0) {
						endTime = Double.NEGATIVE_INFINITY;
						
						if (startTime >= 0.) {
							log.warn("Start time should already be -Infinity. Setting start time to -Infinity.");
							startTime = Double.NEGATIVE_INFINITY;
						}
					}
					
					if (pECounter == selectedPlan.getPlanElements().size() - 1) {
						startTime = Double.NEGATIVE_INFINITY;
						
						if (endTime >= 0.) {
							log.warn("End time should already be -Infinity. Setting end time to -Infinity.");
							endTime = Double.NEGATIVE_INFINITY;
						}
					}
					
					if (actStartEndTimes == null) {
						actStartEndTimes = startTime + ";" + endTime;
					} else {
						actStartEndTimes = actStartEndTimes + ";" + startTime + ";" + endTime;
					}

				}
				
				pECounter++;
			}
			
			person.getAttributes().putAttribute("OpeningClosingTimes", actStartEndTimes);					

		}	
	}
	
	public static void analyze(Population population) {
		
		final Map<String, Integer> activityType2Counter = new HashMap<>();
		
		int personCounter = 0;
		for (Person person : population.getPersons().values()) {
			personCounter++;
			for (Plan plan : person.getPlans()) {				
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						
						if (activityType2Counter.containsKey(act.getType())) {
							activityType2Counter.put(act.getType(), activityType2Counter.get(act.getType()) + 1);
						} else {
							activityType2Counter.put(act.getType(), 1);
						}
						
					}
				}
			}
		}
		
		log.info("Number of persons: " + personCounter);
		
		log.info("----");
		log.info("Activity Type; Counter");
		for (String actType : activityType2Counter.keySet()) {
			log.info(actType + " ; " + activityType2Counter.get(actType));
		}
		log.info("----");
	}

	public static void setScoresToZero(Population population) {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {				
				plan.setScore(0.);
			}
		}
	}
	
}

