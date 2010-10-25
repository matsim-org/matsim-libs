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

package playground.christoph.withinday.replanning.identifiers.interfaces;

import java.util.List;

import org.matsim.core.mobsim.framework.PersonAgent;

import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayReplanner;

/*
 * Identify Agents that need a replanning of their scheduled plan.
 */
public abstract class AgentsToReplanIdentifier {
	
	private IdentifierFactory identifierFactory;
		
	public abstract List<PersonAgent> getAgentsToReplan(double time, WithinDayReplanner withinDayReplanner);
	
	public final void setIdentifierFactory(IdentifierFactory factory) {
		this.identifierFactory = factory;
	}
	
	public final IdentifierFactory getIdentifierFactory() {
		return identifierFactory;
	}
}
