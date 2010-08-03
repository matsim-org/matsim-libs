/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.christoph.withinday.mobsim;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.helpers.QPersonAgent;

/*
 * Creates WithinDayPersonAgents instead of PersonAgents.
 * They are able to reset their cachedNextLink what is
 * necessary when doing LeaveLinkReplanning.
 */
public class WithinDayAgentFactory extends AgentFactory {

	protected Map<Id, PersonAgent> personAgents;
	
	public WithinDayAgentFactory(final QSim simulation) {
		super(simulation);
		
		personAgents = new HashMap<Id, PersonAgent>();
	}

	@Override
	public QPersonAgent createPersonAgent(final Person p) {
		WithinDayPersonAgent agent = new WithinDayPersonAgent(p, this.simulation);
		personAgents.put(agent.getPerson().getId(), agent);
		return agent;
	}
	
	public Map<Id, PersonAgent> getPersonAgents() {
		return this.personAgents;
	}
	
}