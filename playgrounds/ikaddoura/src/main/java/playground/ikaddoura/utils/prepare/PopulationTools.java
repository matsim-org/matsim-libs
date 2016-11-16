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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class PopulationTools {

	private static final Logger log = Logger.getLogger(PopulationTools.class);

	public static Population setActivityTypesAccordingToDuration(Population population, double timeCategorySize) {
		
		int warnCnt = 0;
		
		log.info("Setting activity types according to duration (time bin size: " + timeCategorySize + ")");
				
		for (Person person : population.getPersons().values()) {
			
			for (Plan plan : person.getPlans()) {
								
				Leg previousLeg = null;
				double previousActEndTime = -1.;
				
				for (PlanElement pE : plan.getPlanElements()) {
					
					if (pE instanceof Leg) {
						previousLeg = (Leg) pE;
					}
					
					if (pE instanceof Activity) {
					
						Activity act = (Activity) pE;

						double startTime = -1.;
						double endTime = -1.;
					
						if (act.getStartTime() >= 0. && act.getStartTime() <= 24 * 3600.) {
							startTime = act.getStartTime();
						} else {
							startTime = computeArrivalTime(previousLeg, previousActEndTime);
						}
						
						if (startTime < 0.) {
							throw new RuntimeException("No meaningful start time identified. Aborting...");
						}
						
						if (act.getEndTime() >= 0. && act.getEndTime() <= 24 * 3600.) {
							endTime = act.getEndTime();
						} else {
							endTime = 24 * 3600.;
						}
						
						if (endTime < 0.) {
							throw new RuntimeException("No meaningful end time identified. Aborting...");
						}
												
						if (endTime <= startTime) {
							if (warnCnt < 5) log.warn("Activity: " + act.getType() + " / Start Time: " + startTime + " / End Time: " + endTime + " --> When computing the activity duration, the activity is assumed to end 15 min after start / arrival time...");
							if (warnCnt == 5 ) log.warn("Further warnings of this type are not printed out.");
							endTime = startTime + 15 * 60.;
							warnCnt++;
						}
												
						int durationCategory = (int) ((endTime - startTime) / timeCategorySize) + 1;		
						
						String newType = act.getType() + "_" + durationCategory;
						act.setType(newType);
						
						previousActEndTime = endTime;
					}
				}
			}
		}
		
		return population;
	}
	
	private static double computeArrivalTime(Leg previousLeg, double previousActEndTime) {
		
		double arrivalTime = -1.;
		if (previousLeg != null) {
			if (previousLeg.getDepartureTime() >= 0. && previousLeg.getDepartureTime() <= 24 * 3600. && previousLeg.getTravelTime() >= 0. && previousLeg.getTravelTime() <= 24 * 3600.) {
				arrivalTime = previousLeg.getDepartureTime() + previousLeg.getTravelTime();
			} else {
				if (previousLeg.getRoute().getTravelTime() >= 0. && previousLeg.getRoute().getTravelTime() <= 24 * 3600.) {
					arrivalTime = previousActEndTime + previousLeg.getRoute().getTravelTime();
				} else {
					log.warn("No meaningful activity start time and arrival time identified...");
				}
			}
		} else {
			// no previous leg
			arrivalTime = 0.;
		}
		
		return arrivalTime;
	}

	public static Population mergeEveningAndMorningActivityIfSameBaseType(Population population) {
		
		log.info("Merging evening and morning activity if they have the same (base) type.");
		
		for (Person person : population.getPersons().values()) {
			
			for (Plan plan : person.getPlans()) {
				
				if (plan.getPlanElements().size() > 1) {
					Activity firstActivity = (Activity) plan.getPlanElements().get(0);
					Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
					
					String firstBaseActivity = firstActivity.getType().split("_")[0];
					String lastBaseActivity = lastActivity.getType().split("_")[0];
					
					if (firstBaseActivity.equals(lastBaseActivity)) {
						
						int mergedDuration = Integer.parseInt(firstActivity.getType().split("_")[1]) + Integer.parseInt(lastActivity.getType().split("_")[1]);
						
						firstActivity.setType(firstBaseActivity + "_" + mergedDuration);
						lastActivity.setType(lastBaseActivity + "_" + mergedDuration);
					}
				} else {
					// skipping plans with just one activity
				}
			}
		}
		
		return population;
	}

	public static Population removeNetworkSpecificInformation(Population population) {
		
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
		
		return population;
	}

	public static Population addPersonAttributes(Population population) {
		
		int warnCnt = 0;
		
		log.info("Writing activity times in selected plan to person attributes.");
		
		for (Person person : population.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			String actStartEndTimes = null;
			
			Leg previousLeg = null;
			double previousActEndTime = -1.;

			for (PlanElement pE : selectedPlan.getPlanElements()) {
				
				if (pE instanceof Leg) {
					previousLeg = (Leg) pE;
				}
				
				if (pE instanceof Activity) {
					
					Activity act = (Activity) pE;

					double startTime = -1.;
					double endTime = -1.;
				
					if (act.getStartTime() >= 0. && act.getStartTime() <= 24 * 3600.) {
						startTime = act.getStartTime();
					} else {
						startTime = computeArrivalTime(previousLeg, previousActEndTime);
					}
					
					if (act.getEndTime() >= 0. && act.getEndTime() <= 24 * 3600.) {
						endTime = act.getEndTime();
					} else {
						endTime = 24 * 3600.;
					}
					
					if (endTime <= startTime) {
						if (warnCnt < 5) log.warn("Activity: " + act.getType() + " / Start Time: " + startTime + " / End Time: " + endTime + " --> When computing the activity duration, the activity is assumed to end 15 min after start / arrival time...");
						if (warnCnt == 5 ) log.warn("Further warnings of this type are not printed out.");
						warnCnt++;
						endTime = startTime + 15 * 60.;
					}
					
					if (actStartEndTimes == null) {
						actStartEndTimes = startTime + ";" + endTime;
					} else {
						actStartEndTimes = actStartEndTimes + ";" + startTime + ";" + endTime;
					}

					previousActEndTime = endTime;
				}
			}
			
			person.getAttributes().putAttribute("InitialActivityTimes", actStartEndTimes);					

		}	
		
		return population;
	}

	public static Population getPopulationWithOnlySelectedPlans(Population population) {
		
		Population outputPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		
		for (Person person : population.getPersons().values()) {
			
			if (person.getSelectedPlan() == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			Person personCopy = outputPopulation.getFactory().createPerson(person.getId());
			personCopy.addPlan(person.getSelectedPlan());
			outputPopulation.addPerson(personCopy);
		}
		
		return outputPopulation;
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
	
}

