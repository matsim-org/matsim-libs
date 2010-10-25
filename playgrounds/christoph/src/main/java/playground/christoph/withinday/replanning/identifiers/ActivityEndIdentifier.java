/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityEndIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.replanning.identifiers;

import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

public class ActivityEndIdentifier extends DuringActivityIdentifier {
	
	protected ActivityReplanningMap activityReplanningMap;
	
	// use the Factory!
	/*package*/ ActivityEndIdentifier(ActivityReplanningMap activityReplanningMap) {
		this.activityReplanningMap = activityReplanningMap;
	}
	
	public List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner) {
		List<PersonAgent> agentsToReplan = activityReplanningMap.getReplanningDriverAgents(time);

		Iterator<PersonAgent> iter = agentsToReplan.iterator();
		while(iter.hasNext()) {
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) iter.next();
			
			/*
			 * Remove the Agent from the list, if the replanning flag is not set.
			 */
			if (!withinDayPersonAgent.getWithinDayReplanners().contains(withinDayReplanner)) {
				iter.remove();
				continue;
			}
			
			/*
			 * Check whether the next Leg has to be replanned.
			 * 
			 * Additionally check some special cases (next Leg on the same Link,
			 * next Activity with duration 0)
			 */
			// get person and selected plan
			PersonImpl person = (PersonImpl)withinDayPersonAgent.getPerson();
			PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan(); 
			
			// If we don't have a selected plan
			if (selectedPlan == null) {
				iter.remove();
				continue;
			}
			
			Activity currentActivity;
			Leg nextLeg;
			Activity nextActivity;
			
			currentActivity = withinDayPersonAgent.getCurrentActivity();
			
			// If we don't have a current Activity
			if (currentActivity == null) {
				iter.remove();
				continue;
			}
			
			nextLeg = selectedPlan.getNextLeg(currentActivity);
			if (nextLeg == null) {
				iter.remove();
				continue;
			}
			
			nextActivity = selectedPlan.getNextActivity(nextLeg);
			
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
			
				nextLeg = selectedPlan.getNextLeg(nextActivity);
				if (nextLeg == null) {
					removeAgent = true;
					break;
				}
				nextActivity = selectedPlan.getNextActivity(nextLeg);
			}
			
			/*
			 * If no "next" leg to replan has been found - remove Agent from list.
			 */
			if (removeAgent) {
				iter.remove();
				continue;
			}
		}
		
		return agentsToReplan;
	}

}