/* *********************************************************************** *
 * project: org.matsim.*
 * TransportModeFilter.java
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

package org.matsim.withinday.replanning.identifiers.filter;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

/**
 * Remove all agents from the set that...
 * <ul>
 * 	<li>do not perform a leg.</li>
 * 	<li>do not use one of the modes included in the given set of modes.</li>
 * </ul>
 * 
 * @author cdobler
 */
public class TransportModeFilter implements AgentFilter {

	private final Map<Id<Person>, MobsimAgent> agents;
	private final Set<String> modes;
	
	// use the factory
	/*package*/ TransportModeFilter(Map<Id<Person>, MobsimAgent> agents, Set<String> modes) {
		this.agents = agents;
		this.modes = modes;
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
		
		if (!(agent.getState() == MobsimAgent.State.LEG)) return false;
		if (!(modes.contains(agent.getMode()))) return false;
		
		return true;
	}
}
