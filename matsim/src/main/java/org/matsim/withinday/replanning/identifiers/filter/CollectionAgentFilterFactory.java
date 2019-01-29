/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionAgentFilterFactory.java
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

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;

public class CollectionAgentFilterFactory implements AgentFilterFactory {

	private final Set<Id<Person>> includedAgents;

	public CollectionAgentFilterFactory(Set<Id<Person>> includedAgents) {
		this.includedAgents = includedAgents;
	}
	
	@Override
	public CollectionAgentFilter createAgentFilter() {
		return new CollectionAgentFilter(includedAgents);
	}
}