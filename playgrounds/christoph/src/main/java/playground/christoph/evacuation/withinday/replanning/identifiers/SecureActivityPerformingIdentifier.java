/* *********************************************************************** *
 * project: org.matsim.*
 * SecureActivityPerformingIdentifier.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class SecureActivityPerformingIdentifier extends DuringActivityIdentifier {

	private static final Logger log = Logger.getLogger(SecureActivityPerformingIdentifier.class);
	
	protected ActivityReplanningMap activityReplanningMap;
	protected Coord centerCoord;
	protected double secureDistance;
	
	// Only for Cloning.
	/*package*/ SecureActivityPerformingIdentifier(ActivityReplanningMap activityReplanningMap, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = activityReplanningMap;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	public Set<WithinDayAgent> getAgentsToReplan(double time, Id withinDayReplannerId) {
	
		List<PersonAgent> activityPerformingAgents = activityReplanningMap.getActivityPerformingAgents();
		Set<WithinDayAgent> agentsToReplan = new HashSet<WithinDayAgent>();
		
		Iterator<PersonAgent> iter = activityPerformingAgents.iterator();
		while(iter.hasNext()) {
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) iter.next();
			
			/*
			 * Remove the Agent from the list, if the replanning flag is not set.
			 */
			if (!withinDayPersonAgent.getReplannerAdministrator().getWithinDayReplannerIds().contains(withinDayReplannerId)) {
				continue;
			}
			
			/*
			 *  Get the current PlanElement and check if it is an Activity
			 */
			Activity currentActivity;
			PlanElement currentPlanElement = withinDayPersonAgent.getCurrentPlanElement();
			if (currentPlanElement instanceof Activity) {
				currentActivity = (Activity) currentPlanElement;
			} else continue;
			
			/*
			 * Remove the Agent from the list, if the performed Activity is in an insecure Area.
			 */
			double distance = CoordUtils.calcDistance(currentActivity.getCoord(), centerCoord);
			if (distance <= secureDistance) {
				continue;
			}
			
			// If we reach this point, the agent can be replanned.
			agentsToReplan.add(withinDayPersonAgent);
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + activityPerformingAgents.size() + " Agents performing an Activity in a secure area.");
		
		return agentsToReplan;
	}

}
