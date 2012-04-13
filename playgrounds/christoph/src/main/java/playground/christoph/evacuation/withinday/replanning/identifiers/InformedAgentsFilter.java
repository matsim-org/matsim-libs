/* *********************************************************************** *
 * project: org.matsim.*
 * InformedAgentsFilter.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

public class InformedAgentsFilter implements AgentFilter {

	public static enum FilterType {
		InitialReplanning, NotInitialReplanning
	}
	
	private final InformedAgentsTracker informedAgentsTracker;
	private final FilterType filterType;
	
	public InformedAgentsFilter(InformedAgentsTracker informedAgentsTracker, FilterType filterType) {
		this.informedAgentsTracker = informedAgentsTracker;
		this.filterType = filterType;
	}
		
	@Override
	public void applyAgentFilter(Set<Id> set, double time) {
	
		/*
		 * If all agents have been informed, filtering can be shortened.
		 * Either no agents are filtered (FilterType.NotInitialReplanning)
		 * or all agents are filtered (FilterType.InitialReplanning).
		 */
		if (informedAgentsTracker.allAgentsInformed()) {
			if (filterType == FilterType.InitialReplanning)	set.clear();
			return;
		}
		
		// remove all not informed agents
		removeNotInformedAgents(set);
		
		// remove other agents according to filter type
		if (filterType == FilterType.InitialReplanning) {
			removeAgentsNotToBeReplannedInitially(set);
			
			// assume that all agents in the set will be replanned
			for (Id agentId : set) this.informedAgentsTracker.setAgentInitiallyReplannedInCurrentTimeStep(agentId);
		} else if (filterType == FilterType.NotInitialReplanning) {
			removeAgentsToBeReplannedInitially(set);
		}
	}
	
	/**
	 * Removes agents from the set that have not been informed yet. The remaining
	 * set contains agents that have just been informed and might need an initial
	 * replanning!
	 * 
	 * @param set of PlanBasedWithinDayAgent
	 */
	/*package*/ void removeNotInformedAgents(Set<Id> set) {
		Iterator<Id> iter = set.iterator();
		while (iter.hasNext()) {
			Id agentId = iter.next();
			if (!informedAgentsTracker.isAgentInformed(agentId)) iter.remove();
		}
	}
	
	/**
	 * Removes agents from the set that have just been informed and therefore
	 * require an initial replanning.
	 * 
	 * @param set of PlanBasedWithinDayAgent
	 */
	/*package*/ void removeAgentsToBeReplannedInitially(Set<Id> set) {
		Iterator<Id> iter = set.iterator();
		while (iter.hasNext()) {
			Id agentId = iter.next();
			if (informedAgentsTracker.agentRequiresInitialReplanning(agentId)) iter.remove();
		}
	}
	
	/**
	 * Removes agents from the set that do not need an initial replanning. This
	 * excludes agents that have not been informed yet and agents that have
	 * already performed a replanning.
	 * 
	 * @param set of PlanBasedWithinDayAgent
	 */
	/*package*/ void removeAgentsNotToBeReplannedInitially(Set<Id> set) {
		Iterator<Id> iter = set.iterator();
		while (iter.hasNext()) {
			Id agentId = iter.next();
			if (!informedAgentsTracker.agentRequiresInitialReplanning(agentId)) iter.remove();
		}
	}

}
