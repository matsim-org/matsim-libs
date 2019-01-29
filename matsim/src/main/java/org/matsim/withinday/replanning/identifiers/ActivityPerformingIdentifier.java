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

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class ActivityPerformingIdentifier extends DuringActivityAgentSelector {
	
	protected ActivityReplanningMap activityReplanningMap;
	protected MobsimDataProvider mobsimDataProvider;
	
	// use the Factory!
	/*package*/ ActivityPerformingIdentifier(ActivityReplanningMap activityReplanningMap, MobsimDataProvider mobsimDataProvider) {
		this.activityReplanningMap = activityReplanningMap;
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		Map<Id<Person>, MobsimAgent> mapping = this.mobsimDataProvider.getAgents();
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new ById());

		/*
		 * Identify those activity performing agents that should be replanned.
		 * Add them to a set of MobsimAgents.
		 */
		for (Id<Person> agentId : this.activityReplanningMap.getActivityPerformingAgents()) {
			if (this.applyFilters(agentId, time)) agentsToReplan.add(mapping.get(agentId));
		}
				
		return agentsToReplan;
	}

}