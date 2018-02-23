/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.wagonSim.mobsim.qsim.agents;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

/**
 * 
 * it does not make any sense to let the agent check if there vehicle will depart on time
 * 
 * @author droeder
 *
 */
@Deprecated
public class WagonSimAgentFactory implements AgentFactory {
	final private Scenario scenario;
	final private EventsManager eventsManager; 
	final private MobsimTimer mobsimTimer;

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(WagonSimAgentFactory.class);
	private Netsim sim;

	public WagonSimAgentFactory(Scenario scenario, EventsManager eventsManager, MobsimTimer mobsimTimer) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(Person p) {
		return WagonSimAgent.createInstance(p, scenario, eventsManager, mobsimTimer);
	}
}

