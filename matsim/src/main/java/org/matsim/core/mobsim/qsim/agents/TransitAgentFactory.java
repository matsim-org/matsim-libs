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

package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;
import org.matsim.core.utils.timing.TimeInterpretation;

import jakarta.inject.Inject;


public class TransitAgentFactory implements AgentFactory {

	private final Netsim simulation;
	private final TimeInterpretation timeInterpretation;

	@Inject
	public TransitAgentFactory(final Netsim simulation, TimeInterpretation timeInterpretation) {
		this.simulation = simulation;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public MobsimDriverPassengerAgent createMobsimAgentFromPerson(final Person p) {
		MobsimDriverPassengerAgent agent = TransitAgent.createTransitAgent(p, this.simulation, timeInterpretation);
		return agent;
	}

}
