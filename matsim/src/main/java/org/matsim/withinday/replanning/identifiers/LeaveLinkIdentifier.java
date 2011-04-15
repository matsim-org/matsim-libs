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

package org.matsim.withinday.replanning.identifiers;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class LeaveLinkIdentifier extends DuringLegIdentifier {

	protected LinkReplanningMap linkReplanningMap;
	
	// use the Factory!
	/*package*/ LeaveLinkIdentifier(LinkReplanningMap linkReplanningMap) {
		this.linkReplanningMap = linkReplanningMap;
	}
	
	@Override
	public Set<WithinDayAgent> getAgentsToReplan(double time) {
		Collection<PersonAgent> legPerformingAgents =  linkReplanningMap.getReplanningAgents(time);
		Collection<WithinDayAgent> handledAgents = this.getHandledAgents();
		Set<WithinDayAgent> agentsToReplan = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
		
		if (handledAgents == null) return agentsToReplan;
		
		if (legPerformingAgents.size() > handledAgents.size()) {
			for (WithinDayAgent agent : handledAgents) {
				if (legPerformingAgents.contains(agent)) {
					agentsToReplan.add(agent);
				}
			}
		} else {
			for (PersonAgent agent : legPerformingAgents) {
				if (handledAgents.contains(agent)) {
					agentsToReplan.add((WithinDayAgent)agent);
				}
			}
		}

		return agentsToReplan;
	}

}
