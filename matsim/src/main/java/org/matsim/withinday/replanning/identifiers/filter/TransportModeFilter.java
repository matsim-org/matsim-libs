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
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

public class TransportModeFilter implements AgentFilter {

	private final Map<Id, MobsimAgent> agents;
	private final Set<String> modes;
	
	// use the factory
	/*package*/ TransportModeFilter(Map<Id, MobsimAgent> agents, Set<String> modes) {
		this.agents = agents;
		this.modes = modes;
	}
	
	@Override
	public void applyAgentFilter(Set<Id> set, double time) {
		Iterator<Id> iter = set.iterator();
		
		while (iter.hasNext()) {
			Id id = iter.next();
			MobsimAgent agent = this.agents.get(id);
			
			if (!(agent.getState() == MobsimAgent.State.LEG)) iter.remove();
			if (!(modes.contains(agent.getMode()))) iter.remove();			
		}
	}

}
