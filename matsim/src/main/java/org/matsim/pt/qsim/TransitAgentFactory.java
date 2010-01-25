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
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.DriverAgent;
import org.matsim.ptproject.qsim.PersonAgent;
import org.matsim.ptproject.qsim.QSim;


public class TransitAgentFactory extends AgentFactory {

	private final Map<Person, DriverAgent> agentsMap;

	public TransitAgentFactory(final QSim simulation, final Map<Person, DriverAgent> agents) {
		super(simulation);
		this.agentsMap = agents;
	}

	@Override
	public PersonAgent createPersonAgent(final Person p) {
		PersonAgent agent = new TransitAgent(p, this.simulation);
		this.agentsMap.put(p, agent);
		return agent;
	}

}
