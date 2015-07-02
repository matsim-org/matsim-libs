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

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
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
		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new ById());

		/*
		 * Apply filter to remove agents that should not be replanned.
		 */
		for (Entry<Id<Person>, MobsimAgent> entry : this.mobsimDataProvider.getAgents().entrySet()) {
			Id<Person> agentId = entry.getKey();
			if (this.applyFilters(agentId, time))  agentsToReplan.add(entry.getValue());
		}
		
		return agentsToReplan;
	}

}
