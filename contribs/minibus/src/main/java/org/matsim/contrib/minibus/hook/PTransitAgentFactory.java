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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.MobsimDriverPassengerAgent;


/**
 * @author aneumann
 */
class PTransitAgentFactory implements AgentFactory {

	private final EventsManager eventsManager;
	private final Scenario scenario;
	private final MobsimTimer mobsimTimer;

    public PTransitAgentFactory(EventsManager eventsManager, Scenario scenario, MobsimTimer mobsimTimer) {
		this.eventsManager = eventsManager;
		this.scenario = scenario;
		this.mobsimTimer = mobsimTimer;
    }

	@Override
	public MobsimDriverPassengerAgent createMobsimAgentFromPerson(final Person p) {
		MobsimDriverPassengerAgent agent = PTransitAgent.createTransitAgent(p, scenario, eventsManager, mobsimTimer);
		return agent;
	}

}
