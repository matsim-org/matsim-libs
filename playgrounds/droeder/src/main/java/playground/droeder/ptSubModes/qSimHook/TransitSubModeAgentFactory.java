/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.droeder.ptSubModes.qSimHook;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

/**
 * @author droeder
 *
 */
class TransitSubModeAgentFactory implements AgentFactory{

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(TransitSubModeAgentFactory.class);
	private boolean fixedMode;
	private Netsim sim;

	protected TransitSubModeAgentFactory(Netsim simulation, boolean fixedMode) {
		this.sim = simulation;
		this.fixedMode = fixedMode;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		return TransitSubModeAgent.createAgent(p, this.sim, this.fixedMode);
	}
}

