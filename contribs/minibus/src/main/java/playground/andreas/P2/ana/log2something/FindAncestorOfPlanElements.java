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

package playground.andreas.P2.ana.log2something;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import playground.andreas.P2.replanning.CreateNewPlan;

public class FindAncestorOfPlanElements {
	private static final Logger log = Logger.getLogger(FindAncestorOfPlanElements.class);
	
	
	public static ArrayList<PlanElement> findAncestorOfPlanElements(ArrayList<PlanElement> planElements){
		
		int currentIteration = 0;
		LinkedList<PlanElement> candidates = new LinkedList<PlanElement>();
		
		for (PlanElement planElement : planElements) {
			if (currentIteration != planElement.getIterationFounded()) {
				// new generation - remove ceased plans from the candidates
				log.info("Terminating iteration " + currentIteration);
				
				List<PlanElement> candidatesToBeRemoved = new LinkedList<PlanElement>();
				for (PlanElement candidate : candidates) {
					if (candidate.getIterationCeased() == currentIteration) {
						candidatesToBeRemoved.add(candidate);
					}
				}
				
				for (PlanElement candidate : candidatesToBeRemoved) {
					candidates.remove(candidate);
				}
				
				currentIteration = planElement.getIterationFounded();
				log.info("Starting iteration " + currentIteration);
			}
			
			
			if (!planElement.getCreatorId().equalsIgnoreCase(CreateNewPlan.STRATEGY_NAME)) {
				// This it NOT a new plan created from scratch. Thus, there must be an ancestor. - Find it
				boolean ancestorFound = false;
				for (PlanElement candidate : candidates) {
					if (candidate.canBeChild(planElement)) {
						planElement.setAncestor(candidate);
						ancestorFound = true;
						break;
					}
				}
				if (!ancestorFound) {
					log.warn("Could not find an ancestor from canditates.");
				}
			}

			
			candidates.add(planElement);
		}
		
		
		
		
		
		return planElements;
	}

}
