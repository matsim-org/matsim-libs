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

package org.matsim.pt.qsim;

import java.util.Map;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.ptproject.qsim.agents.AgentFactory;
import org.matsim.ptproject.qsim.interfaces.Netsim;


public class TransitAgentFactory implements AgentFactory {

	private final Map<Person, MobsimAgent> agentsMap;
	private final Netsim simulation;


	public TransitAgentFactory(final Netsim simulation, final Map<Person, MobsimAgent> agents) {
		this.simulation = simulation ;
		this.agentsMap = agents;
	}

	@Override
	public MobsimDriverPassengerAgent createMobsimAgentFromPerson(final Person p) {
		MobsimDriverPassengerAgent agent = new TransitAgent(p, this.simulation);
		this.agentsMap.put(p, agent);
		return agent;
	}

}
