/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToReplanIdentifier.java
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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;

/*
 * Identify Agents that need a replanning of their scheduled plan.
 */
public abstract class AgentsToReplanIdentifier {
	
	private IdentifierFactory identifierFactory;
	private Set<WithinDayAgent> handledAgents = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
	
	public abstract Set<WithinDayAgent> getAgentsToReplan(double time);

	public final void setHandledAgent(Collection<WithinDayAgent> agents) {
		this.handledAgents.clear();
		this.handledAgents.addAll(agents);
	}
	
	public final Set<WithinDayAgent> getHandledAgents() {
		return Collections.unmodifiableSet(handledAgents);
	}
		
	public final void setIdentifierFactory(IdentifierFactory factory) {
		this.identifierFactory = factory;
	}
	
	public final IdentifierFactory getIdentifierFactory() {
		return identifierFactory;
	}
}
