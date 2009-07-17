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

package playground.marcel.pt.queuesim;

import java.util.Map;

import org.matsim.core.mobsim.queuesim.AgentFactory;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.population.PersonImpl;


public class TransitAgentFactory extends AgentFactory {

	private final Map<PersonImpl, DriverAgent> agentsMap;

	public TransitAgentFactory(final QueueSimulation simulation, final Map<PersonImpl, DriverAgent> agents) {
		super(simulation);
		this.agentsMap = agents;
	}

	@Override
	public PersonAgent createPersonAgent(final PersonImpl p) {
		PersonAgent agent = new TransitAgent(p, this.simulation);
		this.agentsMap.put(p, agent);
		return agent;
	}

}
