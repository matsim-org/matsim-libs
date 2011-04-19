/* *********************************************************************** *
 * project: org.matsim.*
 * InsecureActivityPerformingIdentifiers.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

import playground.christoph.evacuation.config.EvacuationConfig;

public class InsecureActivityPerformingIdentifier extends DuringActivityIdentifier {

	private static final Logger log = Logger.getLogger(InsecureActivityPerformingIdentifier.class);
	
	protected ActivityReplanningMap activityReplanningMap;
	protected Coord centerCoord;
	protected double secureDistance;
		
	/*package*/ InsecureActivityPerformingIdentifier(ActivityReplanningMap activityReplanningMap, Coord centerCoord, double secureDistance) {
		this.activityReplanningMap = activityReplanningMap;
		this.centerCoord = centerCoord;
		this.secureDistance = secureDistance;
	}
	
	public Set<WithinDayAgent> getAgentsToReplan(double time) {
		Set<WithinDayAgent> activityPerformingAgents = activityReplanningMap.getActivityPerformingAgents();
		Collection<WithinDayAgent> handledAgents = this.getHandledAgents();
		Set<WithinDayAgent> agentsToReplan = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
		
		for (PersonAgent personAgent : activityPerformingAgents) {			
			/*
			 * Remove the Agent from the list, if the replanning flag is not set.
			 */
			if (!handledAgents.contains(personAgent)) continue;
			
			/*
			 *  Get the current PlanElement and check if it is an Activity
			 */
			Activity currentActivity;
			PlanElement currentPlanElement = personAgent.getCurrentPlanElement();
			if (currentPlanElement instanceof Activity) {
				currentActivity = (Activity) currentPlanElement;
			} else continue;
			/*
			 * Remove the Agent from the list, if the performed Activity is in a secure Area.
			 */
			double distance = CoordUtils.calcDistance(currentActivity.getCoord(), centerCoord);
			if (distance > secureDistance) {
				continue;
			}
			
			/*
			 * Add the Agent to the Replanning List
			 */
			agentsToReplan.add((WithinDayAgent)personAgent);
		}
		if (time == EvacuationConfig.evacuationTime) log.info("Found " + activityPerformingAgents.size() + " Agents performing an Activity in an insecure area.");
		
		return agentsToReplan;
	}

}
