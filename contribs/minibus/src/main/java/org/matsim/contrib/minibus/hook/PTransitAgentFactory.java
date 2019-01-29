/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.hook;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;


/**
 * @author aneumann
 */
class PTransitAgentFactory implements AgentFactory {

	private final Netsim simulation;

    public PTransitAgentFactory(final Netsim simulation) {
		this.simulation = simulation;
    }

	@Override
	public MobsimDriverPassengerAgent createMobsimAgentFromPerson(final Person p) {
		MobsimDriverPassengerAgent agent = PTransitAgent.createTransitAgent(p, this.simulation);
		return agent;
	}

}
