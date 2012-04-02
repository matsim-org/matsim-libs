/* *********************************************************************** *
 * project: org.matsim.*
 * LegPerformingIdentifier.java
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
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class LegPerformingIdentifier extends DuringLegIdentifier {

	protected LinkReplanningMap linkReplanningMap;
	
	// use the Factory!
	/*package*/ LegPerformingIdentifier(LinkReplanningMap linkReplanningMap) {
		this.linkReplanningMap = linkReplanningMap;
	}
	
	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		Set<PlanBasedWithinDayAgent> legPerformingAgents =  linkReplanningMap.getLegPerformingAgents();
		Collection<PlanBasedWithinDayAgent> handledAgents = this.getHandledAgents();
		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		if (this.handleAllAgents()) return legPerformingAgents;
		
		if (legPerformingAgents.size() > handledAgents.size()) {
			for (PlanBasedWithinDayAgent agent : handledAgents) {
				if (legPerformingAgents.contains(agent)) {
					agentsToReplan.add(agent);
				}
			}
		} else {
			for (PlanBasedWithinDayAgent agent : legPerformingAgents) {
				if (handledAgents.contains(agent)) {
					agentsToReplan.add((PlanBasedWithinDayAgent)agent);
				}
			}
		}

		return agentsToReplan;
	}

}
