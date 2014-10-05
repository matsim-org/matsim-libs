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
import java.util.Iterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;

public class CollectionAgentFilter implements AgentFilter {

	private final Set<Id<Person>> includedAgents;
	
	// use the factory
	/*package*/ CollectionAgentFilter(Set<Id<Person>> includedAgents) {
		this.includedAgents = includedAgents;
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
		if (!includedAgents.contains(id)) return false;
		else return true;
	}
	
	public Collection<Id<Person>> getIncludedAgents() {
		return Collections.unmodifiableSet(this.includedAgents);
	}
}
