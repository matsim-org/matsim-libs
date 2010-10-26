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

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.PersonAgent;

/*
 * Identify Agents that need a replanning of their scheduled plan.
 */
public abstract class AgentsToReplanIdentifier {
	
	private IdentifierFactory identifierFactory;
	
	// yyyy change this to a (Hash)Set? Are there situations where duplicated entries might be meaningful? cdobler, Oct'10
	public abstract Set<PersonAgent> getAgentsToReplan(double time, Id withinDayReplannerId);
	
	public final void setIdentifierFactory(IdentifierFactory factory) {
		this.identifierFactory = factory;
	}
	
	public final IdentifierFactory getIdentifierFactory() {
		return identifierFactory;
	}
}
