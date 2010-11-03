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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;

import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class LeaveLinkIdentifier extends DuringLegIdentifier {

	protected LinkReplanningMap linkReplanningMap;
	
	// use the Factory!
	/*package*/ LeaveLinkIdentifier(LinkReplanningMap linkReplanningMap) {
		this.linkReplanningMap = linkReplanningMap;
	}
	
	@Override
	public Set<WithinDayAgent> getAgentsToReplan(double time, Id withinDayReplannerId) {
		List<PersonAgent> agentsToReplanLeaveLink = linkReplanningMap.getReplanningAgents(time);
		Set<WithinDayAgent> agentsToReplan = new HashSet<WithinDayAgent>(); 

		for (PersonAgent personAgent : agentsToReplanLeaveLink) {
			WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) personAgent;
			if (withinDayPersonAgent.getReplannerAdministrator().getWithinDayReplannerIds().contains(withinDayReplannerId)) {
				agentsToReplan.add(withinDayPersonAgent);
			}
		}

		return agentsToReplan;
	}

}
