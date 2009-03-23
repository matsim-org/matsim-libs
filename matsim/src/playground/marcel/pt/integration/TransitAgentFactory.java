/* *********************************************************************** *
 * project: org.matsim.*
 * TransitAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.integration;

import java.util.Map;

import org.matsim.interfaces.core.v01.Person;
import org.matsim.mobsim.queuesim.AgentFactory;
import org.matsim.mobsim.queuesim.PersonAgent;

public class TransitAgentFactory extends AgentFactory {

	private final Map<Person, PersonAgent> agentsMap;
	
	public TransitAgentFactory(final Map<Person, PersonAgent> agents) {
		this.agentsMap = agents;
	}
	
	@Override
	public PersonAgent createPersonAgent(Person p) {
		PersonAgent agent = new TransitAgent(p);
		this.agentsMap.put(p, agent);
		return agent;
	}
	
}
