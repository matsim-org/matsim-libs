/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityPerformingIdentifier.java
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

package org.matsim.withinday.replanning.identifiers;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class ActivityPerformingIdentifier extends DuringActivityIdentifier {
	
	protected ActivityReplanningMap activityReplanningMap;
	
	// use the Factory!
	/*package*/ ActivityPerformingIdentifier(ActivityReplanningMap activityReplanningMap) {
		this.activityReplanningMap = activityReplanningMap;
	}
	
	@Override
	public Set<WithinDayAgent> getAgentsToReplan(double time) {
		Set<WithinDayAgent> activityPerformingAgents = activityReplanningMap.getActivityPerformingAgents();	
		Collection<WithinDayAgent> handledAgents = this.getHandledAgents();
		Set<WithinDayAgent> agentsToReplan = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
		
		if (this.handleAllAgents()) return activityPerformingAgents;
		
		if (activityPerformingAgents.size() > handledAgents.size()) {
			for (WithinDayAgent agent : handledAgents) {
				if (activityPerformingAgents.contains(agent)) {
					agentsToReplan.add(agent);
				}
			}
		} else {
			for (WithinDayAgent agent : activityPerformingAgents) {
				if (handledAgents.contains(agent)) {
					agentsToReplan.add(agent);
				}
			}
		}
		
		return agentsToReplan;
	}

}