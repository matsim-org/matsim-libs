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

package playground.christoph.evacuation.withinday.replanning.identifiers.filters;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

import playground.christoph.evacuation.mobsim.InformedAgentsTracker;
import playground.christoph.evacuation.mobsim.ReplanningTracker;

public class InformedAgentsFilter implements AgentFilter {

	/*
	 * InitialReplanning ... only agents which need an initial replanning are kept
	 * NotInitialReplanning ... only agents which are informed but do not need an initial replanning are kept
	 */
	public static enum FilterType {
		InitialReplanning, NotInitialReplanning
	}
	
	private final InformedAgentsTracker informedAgentsTracker;
	private final ReplanningTracker replanningTracker;
	private final FilterType filterType;
	
	public InformedAgentsFilter(InformedAgentsTracker informedAgentsTracker, ReplanningTracker replanningTracker, FilterType filterType) {
		this.informedAgentsTracker = informedAgentsTracker;
		this.replanningTracker = replanningTracker;
		this.filterType = filterType;
	}
	
	@Override
	public void applyAgentFilter(Set<Id<Person>> set, double time) {
	
		/*
		 * If all agents have been informed, filtering can be shortened.
		 * Either no agents are filtered (FilterType.NotInitialReplanning)
		 * or all agents are filtered (FilterType.InitialReplanning).
		 */
		if (this.informedAgentsTracker.allAgentsInformed() && this.replanningTracker.allAgentsInitiallyReplanned()) {
			if (filterType == FilterType.InitialReplanning)	set.clear();
			return;
		}
		
		// keep only agents that need an initial replanning
		if (filterType == FilterType.InitialReplanning) {
			applyInitialReplanningFilter(set);
		} 
		
		// only agents which do not need an initial replanning are kept
		else if(filterType == FilterType.NotInitialReplanning) {
			applyNotInitialReplanningFilter(set);
		}
		// this should not happen...
		else { 
			throw new RuntimeException("Unknown filter type: " + filterType);
		}
	}
	
	/**
	 * Removes agents from the set that are not informed yet or that have just 
	 * been informed and therefore require an initial replanning.
	 * 
	 * @param set of MobsimAgent
	 */
	/*package*/ void applyNotInitialReplanningFilter(Set<Id<Person>> set) {
		
		Set<Id<Person>> alreadyInitiallyReplannedAgents = this.replanningTracker.getInformedAndInitiallyReplannedAgents();
		if (alreadyInitiallyReplannedAgents.size() < set.size()) {
			Set<Id<Person>> agents = new HashSet<>();
			for(Id<Person> agentId : alreadyInitiallyReplannedAgents) {
				if(set.contains(agentId)) agents.add(agentId);
			}
			set.clear();
			set.addAll(agents);
		} else {
			Iterator<Id<Person>> iter = set.iterator();
			while (iter.hasNext()) {
				Id<Person> agentId = iter.next();
				if (!alreadyInitiallyReplannedAgents.contains(agentId)) iter.remove();
			}			
		}
	}
	
	/*package*/ boolean applyNotInitialReplanningFilter(Id<Person> id) {
		if (this.replanningTracker.hasAgentBeenInitiallyReplanned(id)) return true;
		else return false;
	}
	
	/**
	 * Removes agents from the set that do not need an initial replanning. This
	 * excludes agents that have not been informed yet and agents that have
	 * already performed a replanning.
	 * 
	 * @param set of PlanBasedWithinDayAgent
	 */
	/*package*/ void applyInitialReplanningFilter(Set<Id<Person>> set) {
		
		Set<Id<Person>> initialReplanningRequiringAgents = this.replanningTracker.getInformedButNotInitiallyReplannedAgents();
		if (initialReplanningRequiringAgents.size() < set.size()) {
			Set<Id<Person>> agents = new HashSet<>();
			for(Id<Person> agentId : initialReplanningRequiringAgents) {
				if(set.contains(agentId)) agents.add(agentId);
			}
			set.clear();
			set.addAll(agents);
		} else {
			Iterator<Id<Person>> iter = set.iterator();
			while (iter.hasNext()) {
				Id<Person> agentId = iter.next();
				if (!initialReplanningRequiringAgents.contains(agentId)) iter.remove();
			}			
		}
	}
	
	/*package*/ boolean applyInitialReplanningFilter(Id<Person> id) {
		final Set<Id<Person>> initialReplanningRequiringAgents = this.replanningTracker.getInformedButNotInitiallyReplannedAgents();
		if (initialReplanningRequiringAgents.contains(id)) return true;
		else return false;
	}
	
	@Override
	public boolean applyAgentFilter(Id<Person> id, double time) {
		/*
		 * If all agents have been informed, filtering can be shortened.
		 * Either no agents are filtered (FilterType.NotInitialReplanning)
		 * or all agents are filtered (FilterType.InitialReplanning).
		 */
		if (this.informedAgentsTracker.allAgentsInformed() && this.replanningTracker.allAgentsInitiallyReplanned()) {
			if (filterType == FilterType.InitialReplanning)	return false;
		}
		
		// keep only agents that need an initial replanning
		if (filterType == FilterType.InitialReplanning) {
			if (!applyInitialReplanningFilter(id)) return false;
			else return true;
		} 
		// only agents which do not need an initial replanning are kept
		else if(filterType == FilterType.NotInitialReplanning) {
			if (!applyNotInitialReplanningFilter(id)) return false;
			else return true;
		}
		else { 
			// this should never happen...
			throw new RuntimeException("Unknown filter type: " + filterType);
		}
	}
}