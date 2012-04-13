/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionAgentFilter.java
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

public class CollectionAgentFilter implements AgentFilter {

	private final Set<Id> includedAgents;
	
	// use the factory
	/*package*/ CollectionAgentFilter() {
		this.includedAgents = new HashSet<Id>();
	}

	public boolean addIncludedAgent(Id id) {
		return this.includedAgents.add(id);
	}
	
	public boolean addIncludedAgents(Collection<Id> ids) {
		return this.includedAgents.addAll(ids);
	}
	
	public boolean removeIncludedAgent(Id id) {
		return this.includedAgents.remove(id);
	}

	public Collection<Id> getIncludedAgents() {
		return Collections.unmodifiableSet(this.includedAgents);
	}
	
	@Override
	public void applyAgentFilter(Set<Id> set, double time) {
		Iterator<Id> iter = set.iterator();
		
		while (iter.hasNext()) {
			Id id = iter.next();
			if (!includedAgents.contains(id)) iter.remove();
		}
	}

}
