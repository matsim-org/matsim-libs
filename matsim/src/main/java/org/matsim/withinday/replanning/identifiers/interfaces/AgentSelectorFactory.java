/* *********************************************************************** *
 * project: org.matsim.*
 * IdentifierFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

public abstract class AgentSelectorFactory {

	private final Set<AgentFilterFactory> agentFilterFactories = new LinkedHashSet<AgentFilterFactory>();
	
	public abstract AgentSelector createIdentifier();
	
	protected final void addAgentFiltersToIdentifier(AgentSelector identifier) {
		for (AgentFilterFactory agentFilterFactory : agentFilterFactories) {
			identifier.addAgentFilter(agentFilterFactory.createAgentFilter());
		}
	}
	
	public final void addAgentFilterFactory(AgentFilterFactory agentFilterFactory) {
		this.agentFilterFactories.add(agentFilterFactory);
	}
	
	public final boolean removeAgentFilterFactory(AgentFilterFactory agentFilterFactory) {
		return this.agentFilterFactories.remove(agentFilterFactory);
	}

	public final Set<AgentFilterFactory> getAgentFilterFactories() {
		return Collections.unmodifiableSet(agentFilterFactories);
	}
}
