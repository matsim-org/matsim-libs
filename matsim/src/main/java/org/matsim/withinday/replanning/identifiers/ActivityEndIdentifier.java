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

import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;

public class ActivityEndIdentifier extends DuringActivityAgentSelector {
	
	protected ActivityReplanningMap activityReplanningMap;
	
	// use the Factory!
	/*package*/ ActivityEndIdentifier(ActivityReplanningMap activityReplanningMap) {
		this.activityReplanningMap = activityReplanningMap;
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new ById());

		for (MobsimAgent mobsimAgent : this.activityReplanningMap.getActivityEndingAgents(time)) {
			Id<Person> agentId = mobsimAgent.getId();
			if (this.applyFilters(agentId, time)) agentsToReplan.add(mobsimAgent);
		}
			
		/*
		 * Here was lots of additional code that identified agents which ended their activity, then
		 * performed a leg starting and ending on the same link and then performed an activity with
		 * duration of 0 seconds. Such agents then start the leg after the next activity in the next
		 * time step. As a result, that leg was not replanned. However, Performing this check should
		 * be implemented at another place (i.e. in a replanner or in a module that ensure that each
		 * activity has a duration of at least one second).
		 * 
		 * cdobler, aug'13
		 */
		
		return agentsToReplan;
	}

}