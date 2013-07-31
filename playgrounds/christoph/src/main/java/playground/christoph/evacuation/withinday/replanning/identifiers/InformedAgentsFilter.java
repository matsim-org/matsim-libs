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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
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
	 * @param set of PlanBasedWithinDayAgent
	 */
	/*package*/ void applyNotInitialReplanningFilter(Set<Id> set) {
		
		Set<Id> alreadyInitiallyReplannedAgents = this.replanningTracker.getInformedAndInitiallyReplannedAgents();
		if (alreadyInitiallyReplannedAgents.size() < set.size()) {
			Set<Id> agents = new HashSet<Id>();
			for(Id agentId : alreadyInitiallyReplannedAgents) {
				if(set.contains(agentId)) agents.add(agentId);
			}
			set.clear();
			set.addAll(agents);
		} else {
			Iterator<Id> iter = set.iterator();
			while (iter.hasNext()) {
				Id agentId = iter.next();
				if (!alreadyInitiallyReplannedAgents.contains(agentId)) iter.remove();
			}			
		}
		
//		Set<Id> initialReplanningRequiringAgents = informedAgentsTracker.getAgentsRequiringInitialReplanning();
//		Set<Id> informedAgents = informedAgentsTracker.getInformedAgents();
//		Set<Id> notInformedAgents = informedAgentsTracker.getNotInformedAgents();
//		
//		/*
//		 * 6 possible combinations
//		 * - set < informed agents < not informed agents
//		 * - informed agents < set < not informed agents
//		 * - informed agents < not informed agents < set
//		 * - set < not informed agents < informed agents
//		 * - not informed agents < set < informed agents
//		 * - not informed agents < informed agents < set
//		 */
//		if (informedAgents.size() < notInformedAgents.size()) {
//			
//			// set < informed agents < not informed agents
//			if (set.size() < informedAgents.size()) {
//				Iterator<Id> iter = set.iterator();
//				while (iter.hasNext()) {
//					Id agentId = iter.next();
//
//					// if the agent is not informed or requires an initial replanning, remove it from the set
//					if (!informedAgents.contains(agentId) || initialReplanningRequiringAgents.contains(agentId)) iter.remove();
//				}
//			} else {				
//				// informed agents < set < not informed agents				
//				if (set.size() < notInformedAgents.size()) {
//					Set<Id> agents = new HashSet<Id>();
//					for (Id agentId : informedAgents) {
//						if (!this.informedAgentsTracker.agentRequiresInitialReplanning(agentId) && set.contains(agentId)) agents.add(agentId);
//					}
//					set.clear();
//					set.addAll(agents);
//				} 
//				// informed agents < not informed agents < set
//				else {
//					Set<Id> agents = new HashSet<Id>();
//					for (Id agentId : informedAgents) {
//						if (!this.informedAgentsTracker.agentRequiresInitialReplanning(agentId) && set.contains(agentId)) agents.add(agentId);
//					}
//					set.clear();
//					set.addAll(agents);
//				}
//			}
//		} 
//		// not informed agents < informed agents
//		else {	
//			// set < not informed agents < informed agents
//			if (set.size() < notInformedAgents.size()) {
//				Iterator<Id> iter = set.iterator();
//				while (iter.hasNext()) {
//					Id agentId = iter.next();
//					// if the agent is not informed or requires an initial replanning, remove it from the set
//					if (notInformedAgents.contains(agentId) || initialReplanningRequiringAgents.contains(agentId)) iter.remove();
//				}
//			} else {
//				// not informed agents < set < informed agents
//				if (set.size() < informedAgents.size()) {
//					set.removeAll(notInformedAgents);
//					set.removeAll(initialReplanningRequiringAgents);
//				}
//				// not informed agents < informed agents < set
//				else {
//					set.removeAll(notInformedAgents);
//					set.removeAll(initialReplanningRequiringAgents);
//				}
//			}
//		}
	}
	
	/**
	 * Removes agents from the set that do not need an initial replanning. This
	 * excludes agents that have not been informed yet and agents that have
	 * already performed a replanning.
	 * 
	 * @param set of PlanBasedWithinDayAgent
	 */
	/*package*/ void applyInitialReplanningFilter(Set<Id> set) {
		
		Set<Id> initialReplanningRequiringAgents = this.replanningTracker.getInformedButNotInitiallyReplannedAgents();
		if (initialReplanningRequiringAgents.size() < set.size()) {
			Set<Id> agents = new HashSet<Id>();
			for(Id agentId : initialReplanningRequiringAgents) {
				if(set.contains(agentId)) agents.add(agentId);
			}
			set.clear();
			set.addAll(agents);
		} else {
			Iterator<Id> iter = set.iterator();
			while (iter.hasNext()) {
				Id agentId = iter.next();
				if (!initialReplanningRequiringAgents.contains(agentId)) iter.remove();
			}			
		}
	}
}