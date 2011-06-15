/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEndIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.identifiers;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class ActivityEndIdentifier extends DuringActivityIdentifier {
	
	protected ActivityReplanningMap activityReplanningMap;
	
	// use the Factory!
	/*package*/ ActivityEndIdentifier(ActivityReplanningMap activityReplanningMap) {
		this.activityReplanningMap = activityReplanningMap;
	}
	
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Set<PlanBasedWithinDayAgent> activityPerformingAgents = activityReplanningMap.getReplanningDriverAgents(time);
		Collection<PlanBasedWithinDayAgent> handledAgents = this.getHandledAgents();
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		if (this.handleAllAgents()) {
			agentsToReplan.addAll(activityPerformingAgents);
		} else {
			if (activityPerformingAgents.size() > handledAgents.size()) {
				for (PlanBasedWithinDayAgent agent : handledAgents) {
					if (activityPerformingAgents.contains(agent)) {
						agentsToReplan.add(agent);
					}
				}
			} else {
				for (PlanBasedWithinDayAgent agent : activityPerformingAgents) {
					if (handledAgents.contains(agent)) {
						agentsToReplan.add(agent);
					}
				}
			}			
		}
		
		
		Iterator<PlanBasedWithinDayAgent> iter = agentsToReplan.iterator();
		while(iter.hasNext()) {
			PlanBasedWithinDayAgent withinDayAgent = iter.next();	
						
			/*
			 * Check whether the next Leg has to be replanned.
			 * 
			 * Additionally check some special cases (next Leg on the same Link,
			 * next Activity with duration 0)
			 */
			// get executed plan
			PlanImpl executedPlan = (PlanImpl)withinDayAgent.getSelectedPlan();
			
			// If we don't have a selected plan
			if (executedPlan == null) {
				iter.remove();
				continue;
			}
			
			Activity currentActivity;
			Leg nextLeg;
			Activity nextActivity;

			/*
			 *  Get the current PlanElement and check if it is an Activity
			 */
			PlanElement currentPlanElement = withinDayAgent.getCurrentPlanElement();
			if (currentPlanElement instanceof Activity) {
				currentActivity = (Activity) currentPlanElement;
			} else {
				iter.remove();
				continue;
			}
			
			nextLeg = executedPlan.getNextLeg(currentActivity);
			if (nextLeg == null) {
				iter.remove();
				continue;
			}
			
			nextActivity = executedPlan.getNextActivity(nextLeg);
			
			/*
			 * By default we replan the leg after the current Activity, but there are
			 * some exclusions.
			 * 
			 * If the next Activity takes place at the same Link we don't have to 
			 * replan the next leg.
			 * BUT: if the next Activity has a duration of 0 seconds we have to replan
			 * the leg after that Activity.
			 */
			boolean removeAgent = false;
			while (nextLeg.getRoute().getStartLinkId().equals(nextLeg.getRoute().getEndLinkId())) {
				/*
				 *  If the next Activity has a duration > 0 we don't have to replan
				 *  the leg after that Activity now - it will be scheduled later.
				 */
				if (nextActivity.getEndTime() > time) {	
					removeAgent = true;
					break;
				}				
			
				nextLeg = executedPlan.getNextLeg(nextActivity);
				if (nextLeg == null) {
					removeAgent = true;
					break;
				}
				nextActivity = executedPlan.getNextActivity(nextLeg);
			}
			
			/*
			 * If no "next" leg to replan has been found - remove Agent from list.
			 */
			if (removeAgent) {
				iter.remove();
				continue;
			}
			
			// If the agent has not been removed from the Set yet, it will be replanned.
		}
		
		return agentsToReplan;
	}

}