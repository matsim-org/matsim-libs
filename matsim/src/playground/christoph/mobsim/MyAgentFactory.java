/* *********************************************************************** *
 * project: org.matsim.*
 * MyAgentFactory.java
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
package playground.christoph.mobsim;

import org.matsim.core.mobsim.queuesim.AgentFactory;
import org.matsim.core.mobsim.queuesim.PersonAgent;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.population.PersonImpl;

public class MyAgentFactory extends AgentFactory {

	public MyAgentFactory(final QueueSimulation simulation) {
		super(simulation);
	}

	@Override
	public PersonAgent createPersonAgent(final PersonImpl p)
	{
		MyPersonAgent agent = new MyPersonAgent(p, this.simulation);
		return agent;
	}

}