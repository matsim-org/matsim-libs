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
		
		log.info("Setting activity types according to duration (time bin size: " + timeCategorySize + ")");
		
		// TODO: What about overnight activity... not considered here...
		
		for (Person person : population.getPersons().values()) {
			
			for (Plan plan : person.getPlans()) {
								
				Leg previousLeg = null;
				
				for (PlanElement pE : plan.getPlanElements()) {
					
					if (pE instanceof Leg) {
						previousLeg = (Leg) pE;
					}
					
					if (pE instanceof Activity) {
						
						double arrivalTime = Double.MIN_VALUE;
						if (previousLeg != null) {
							arrivalTime = previousLeg.getDepartureTime() + previousLeg.getTravelTime();
						} else {
							arrivalTime = 0.;
						}
						Activity act = (Activity) pE;
						double endTime = act.getEndTime();
						
						if (endTime <= arrivalTime || endTime == 0. || endTime == Double.MIN_VALUE || endTime == Double.MAX_VALUE) {
							log.warn("Activity: " + act.getType() + " / Start Time: " + arrivalTime + " / End Time: " + endTime + " --> When computing the activity duration, the activity is assumed to end 15 min after arrival time...");
							endTime = arrivalTime + 15 * 60.;
						}
						
						if (endTime == 0. || endTime == Double.MIN_VALUE || endTime == Double.MAX_VALUE) {
							throw new RuntimeException("start time: " + arrivalTime + " -- end time: " + endTime + " --> Aborting...");
						}
						
						int durationCategory = (int) ((endTime - arrivalTime) / timeCategorySize) + 1;		
						
						String newType = act.getType() + "_" + durationCategory;
						act.setType(newType);						
					}
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
		
		log.info("Writing activity times in selected plan to person attributes.");
		
		for (Person person : population.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			String actStartEndTimes = null;
			
			Leg previousLeg = null;

			for (PlanElement pE : selectedPlan.getPlanElements()) {
				
				if (pE instanceof Leg) {
					previousLeg = (Leg) pE;
				}
				
				if (pE instanceof Activity) {
					
					double arrivalTime = Double.MIN_VALUE;
					if (previousLeg != null) {
						arrivalTime = previousLeg.getDepartureTime() + previousLeg.getTravelTime();
					} else {
						arrivalTime = 0.;
					}
					Activity act = (Activity) pE;
					double endTime = act.getEndTime();
					
					if (endTime <= arrivalTime || endTime == 0. || endTime == Double.MIN_VALUE || endTime == Double.MAX_VALUE) {
						log.warn("Activity: " + act.getType() + " / Start Time: " + arrivalTime + " / End Time: " + endTime + " --> When computing the activity duration, the activity is assumed to end 15 min after arrival time...");
						endTime = arrivalTime + 15 * 60.;
					}
					
					if (endTime == 0. || endTime == Double.MIN_VALUE || endTime == Double.MAX_VALUE) {
						throw new RuntimeException("start time: " + arrivalTime + " -- end time: " + endTime + " --> Aborting...");
					}
					
					if (actStartEndTimes == null) {
						actStartEndTimes = arrivalTime + ";" + endTime;
					} else {
						actStartEndTimes = actStartEndTimes + ";" + arrivalTime + ";" + endTime;
					}
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

