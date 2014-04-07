/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.rc;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

/**
 * Filter that removes agents which are not located on a link included
 * in a predefined set after 15:30 when the accident happened.
 * 
 * @author anhorni
 */
public class TunnelLinksFilter implements AgentFilter {

	private final Map<Id, MobsimAgent> agents;
	private final Set<Id> links;
	
	// use the factory
	/*package*/ TunnelLinksFilter(Map<Id, MobsimAgent> agents, Set<Id> links) {
		this.agents = agents;
		this.links = links;
	}
	
	@Override
	public void applyAgentFilter(Set<Id> set, double time) {
		Iterator<Id> iter = set.iterator();
		
		while (iter.hasNext()) {
			Id id = iter.next();
			if (!this.applyAgentFilter(id, time)) iter.remove();
		}
	}

	@Override
	public boolean applyAgentFilter(Id id, double time) {
		MobsimAgent agent = this.agents.get(id);
		
		if (!(links.contains(agent.getCurrentLinkId())) || time < 15.5 * 3600.0) return false;
		else return true;
	}
}
