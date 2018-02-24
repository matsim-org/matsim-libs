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

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.AgentCounterImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;

/**
 * The MobsimFactory is only necessary so that I can add the {@link PTransitAgent}.
 *
 * @author aneumann
 *
 */
class PQSimProvider implements Provider<Mobsim> {

	@SuppressWarnings("unused")
	private final static Logger log = Logger.getLogger(PQSimProvider.class);

	@Inject Scenario scenario ;
	@Inject EventsManager eventsManager ;
	@Inject MobsimTimer mobsimTimer;
	@Inject AgentCounter agentCounter;
	@Inject ActiveQSimBridge activeQSimBridge;

	@Override
	public Netsim get() {

		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		
		QSim qSim = new QSim(scenario, eventsManager, agentCounter, mobsimTimer, activeQSimBridge);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, agentCounter, mobsimTimer);
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngineModule.configure(qSim, scenario.getConfig(), scenario, eventsManager, mobsimTimer, agentCounter);
		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager, mobsimTimer);
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory;

		if (scenario.getConfig().transit().isUseTransit()) {
			agentFactory = new PTransitAgentFactory(eventsManager, scenario, mobsimTimer);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim, scenario.getConfig(), scenario, eventsManager, mobsimTimer, agentCounter);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(scenario, eventsManager, mobsimTimer);
		}

		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, scenario.getConfig(), scenario, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
