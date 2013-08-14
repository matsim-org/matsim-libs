/* *********************************************************************** *
 * project: org.matsim.*
 * InitialIdentifierImpl.java
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

package org.matsim.withinday.replanning.identifiers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;

public class InitialIdentifierImpl extends InitialIdentifier {

	protected MobsimDataProvider mobsimDataProvider;

	// use the Factory!
	/*package*/ InitialIdentifierImpl(MobsimDataProvider mobsimDataProvider) {
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		
		Collection<MobsimAgent> mobsimAgents = new LinkedHashSet<MobsimAgent>(this.mobsimDataProvider.getAgents().values()); 
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new PersonAgentComparator());

		/*
		 * Apply filter to remove agents that should not be replanned.
		 * We need a workaround since applyFilters expects Ids and not Agents.
		 */
		Set<Id> agentIds = new HashSet<Id>();
		for (MobsimAgent agent : mobsimAgents) agentIds.add(agent.getId());
		this.applyFilters(agentIds, time);
		Iterator<MobsimAgent> iter = mobsimAgents.iterator();
		while (iter.hasNext()) {
			MobsimAgent agent = iter.next();
			if (agentIds.contains(agent.getId())) agentsToReplan.add(agent);
		}

		return agentsToReplan;
	}

}
