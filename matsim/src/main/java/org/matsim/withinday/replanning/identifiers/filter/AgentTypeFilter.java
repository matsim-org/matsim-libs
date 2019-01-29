/* *********************************************************************** *
 * project: org.matsim.*
 * AgentTypeFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.identifiers.filter;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

public class AgentTypeFilter implements AgentFilter {

	private final Map<Id<Person>, MobsimAgent> agents;
	private final Set<Class<?>> includedAgentTypes;
	
	// use the factory
	/*package*/ AgentTypeFilter(Map<Id<Person>, MobsimAgent> agents, Set<Class<?>> includedAgentTypes) {
		this.agents = agents;
		this.includedAgentTypes = includedAgentTypes;
	}
	
	@Override
	public void applyAgentFilter(Set<Id<Person>> set, double time) {
		Iterator<Id<Person>> iter = set.iterator();
		
		while (iter.hasNext()) {
			Id<Person> id = iter.next();
			if (!this.applyAgentFilter(id, time)) iter.remove();
		}
	}
	
	@Override
	public boolean applyAgentFilter(Id<Person> id, double time) {
		MobsimAgent agent = this.agents.get(id);
		
		if (!includedAgentTypes.contains(agent.getClass())) return false;
		else return true;
	}
}