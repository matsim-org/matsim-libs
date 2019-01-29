/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.stats.operatorLogger;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Aggregate atomic data of {@linkplain LogElement} into time series data of {@link PlanElement}.
 * 
 * @author aneumann
 *
 */
public class LogElement2PlanElement {
	
	private static final Logger log = Logger.getLogger(LogElement2PlanElement.class);
	
	/**
	 * Aggregate atomic data of {@linkplain LogElement} into time series data of {@link PlanElement}.
	 * 
	 * @param logElements
	 * @return
	 */
	public static ArrayList<PlanElement> logElement2PlanElement(ArrayList<LogElement> logElements){
		ArrayList<PlanElement> planElements = new ArrayList<>();
		
		int currentIteration = 0;
		HashMap<String, PlanElement> plansActiveLastIteration = new HashMap<>();
		HashMap<String, PlanElement> plansActiveCurrentIteration = new HashMap<>();
		
		for (LogElement logElement : logElements) {
			if (logElement.getIteration() != currentIteration) {
				log.info("Terminating iteration " + currentIteration);
				
				List<String> plansToTerminate = new LinkedList<>();
				for (String planIdFromLastIteration : plansActiveLastIteration.keySet()) {
					if (!plansActiveCurrentIteration.keySet().contains(planIdFromLastIteration)) {
						plansToTerminate.add(planIdFromLastIteration);
					}
				}
				
				for (String planIdToTerminate : plansToTerminate) {
					plansActiveLastIteration.get(planIdToTerminate).setIterationCeased(currentIteration);
				}
				
				plansActiveLastIteration = plansActiveCurrentIteration;
				plansActiveCurrentIteration = new HashMap<>();
				
				currentIteration = logElement.getIteration();
				log.info("Starting with iteration " + currentIteration);
			}
			
			
			if (plansActiveLastIteration.keySet().contains(logElement.getUniquePlanIdentifier())) {
				// plan already exists - update
				plansActiveLastIteration.get(logElement.getUniquePlanIdentifier()).update(logElement);
				plansActiveCurrentIteration.put(logElement.getUniquePlanIdentifier(), plansActiveLastIteration.get(logElement.getUniquePlanIdentifier()));
			} else {
				// new plan - create one
				PlanElement planElement = new PlanElement(logElement);
				planElements.add(planElement);
				plansActiveCurrentIteration.put(logElement.getUniquePlanIdentifier(), planElement);
			}
		}
		
		// terminate all to avoid Double.maxValue iterations to be set
		for (PlanElement planElement : plansActiveLastIteration.values()) {
			planElement.setIterationCeased(currentIteration);
		}

		for (PlanElement planElement : plansActiveCurrentIteration.values()) {
			planElement.setIterationCeased(currentIteration);
		}
		
		log.info("Parsed " + logElements.size() + " log elements");
		log.info("Returning " + planElements.size() + " plan elements");
		return planElements;
	}

}
