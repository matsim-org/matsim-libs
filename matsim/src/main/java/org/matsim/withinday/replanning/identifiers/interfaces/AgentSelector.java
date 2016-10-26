/* *********************************************************************** *
 * project: org.matsim.*
 * Identifier.java
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

package org.matsim.withinday.replanning.identifiers.interfaces;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;

/**
 * Identifies agents that need a replanning of their scheduled plan.
 * <p></p>
 * Used to be called "Identifier" until approx. mar'15, but was renamed since "Identifier", albeit correct, conflicts with "Id"
 * which also stands for "Identifier".
 * 
 * @author cdobler
 */
public abstract class AgentSelector {
	
	private AgentSelectorFactory identifierFactory;
	private final Set<AgentFilter> agentFilters = new LinkedHashSet<AgentFilter>();
	
	public abstract Set<MobsimAgent> getAgentsToReplan(double time);
	
	public final void addAgentFilter(AgentFilter agentFilter) {
		this.agentFilters.add(agentFilter);
	}
	
	public final boolean removeAgentFilter(AgentFilter agentFilter) {
		return this.agentFilters.remove(agentFilter);
	}

	public final Set<AgentFilter> getAgentFilters() {
		return Collections.unmodifiableSet(agentFilters);
	}
	
	public final void applyFilters(Set<Id<Person>> set, double time) {
		for (AgentFilter agentFilter : agentFilters) agentFilter.applyAgentFilter(set, time);
	}
	
	public final boolean applyFilters(Id<Person> id, double time) {
		for (AgentFilter agentFilter : agentFilters) {
			if(!agentFilter.applyAgentFilter(id, time)) return false;
		}
		return true;
	}
	
	public final void setAgentSelectorFactory(AgentSelectorFactory factory) {
		this.identifierFactory = factory;
	}
	
	public final AgentSelectorFactory getAgentSelectorFactory() {
		return identifierFactory;
	}
}
