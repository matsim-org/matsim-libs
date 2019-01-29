/* *********************************************************************** *
 * project: org.matsim.*
 * LegStartedIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;

public class LegStartedIdentifier extends DuringLegAgentSelector {

	private final LinkReplanningMap linkReplanningMap;
	private final MobsimDataProvider mobsimDataProvider;
	
	// use the Factory!
	/*package*/ LegStartedIdentifier(LinkReplanningMap linkReplanningMap, MobsimDataProvider mobsimDataProvider) {
		this.linkReplanningMap = linkReplanningMap;
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {		
		Map<Id<Person>, MobsimAgent> mapping = this.mobsimDataProvider.getAgents();
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new ById());

		/*
		 * Identify those leg performing agents that should be replanned.
		 * Add them to a set of MobsimAgents.
		 */
		for (Id<Person> agentId : this.linkReplanningMap.getLegStartedAgents()) {
			if (this.applyFilters(agentId, time)) agentsToReplan.add(mapping.get(agentId));
		}
		
		return agentsToReplan;
	}

}
