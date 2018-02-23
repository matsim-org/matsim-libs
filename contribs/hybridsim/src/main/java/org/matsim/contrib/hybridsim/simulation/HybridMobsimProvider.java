/* *********************************************************************** *
 * project: org.matsim.*
 * MobsimProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.hybridsim.simulation;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.util.Collection;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.hybridsim.utils.IdIntMapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

public class HybridMobsimProvider implements Provider<Mobsim>{
	
	private final Scenario sc;
	private final EventsManager em;
	private final HybridNetworkFactory netFac;
    private final IdIntMapper mapper;
    private final MobsimTimer mobsimTimer;
    private final AgentCounter agentCounter;

    @Inject
    HybridMobsimProvider(Scenario sc, EventsManager eventsManager, HybridNetworkFactory netFac, IdIntMapper mapper, AgentCounter agentCounter, MobsimTimer mobsimTimer) {
        this.sc = sc;
		this.em = eventsManager;
		this.netFac = netFac;
        this.mapper = mapper;
        this.mobsimTimer = mobsimTimer;
        this.agentCounter = agentCounter;

    }

	@Override
	public Mobsim get() {
		QSimConfigGroup conf = this.sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}


		QSim qSim = new QSim(this.sc, this.em, agentCounter, mobsimTimer);
		ActivityEngine activityEngine = new ActivityEngine(this.em, agentCounter, mobsimTimer);
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

        ExternalEngine e = new ExternalEngine(this.em, sc, mapper);
        this.netFac.setExternalEngine(e);
//		HybridQSimExternalNetworkFactory eFac = new HybridQSimExternalNetworkFactory(e);
//		this.netFac.putNetsimNetworkFactory("2ext", eFac);
//		this.netFac.putNetsimNetworkFactory("ext2", eFac);
		
		QNetsimEngine netsimEngine = new QNetsimEngine(this.netFac, sc.getConfig(), sc, em, mobsimTimer, agentCounter);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());


		qSim.addMobsimEngine(e);


		
//		CANetworkFactory fac = new CAMultiLaneNetworkFactory();
//		CANetsimEngine cae = new CANetsimEngine(qSim, fac);
//		qSim.addMobsimEngine(cae);
//		qSim.addDepartureHandler(cae.getDepartureHandler());


		TeleportationEngine teleportationEngine = new DefaultTeleportationEngine(this.sc, this.em, mobsimTimer);
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory;
		if (this.sc.getConfig().transit().isUseTransit()) {
			agentFactory = new TransitAgentFactory(sc, em, mobsimTimer);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim, sc.getConfig(), sc, em, mobsimTimer, agentCounter);
			transitEngine
					.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(sc, em, mobsimTimer);
		}
		if (this.sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine(sc.getNetwork(), netsimEngine.getNetsimNetwork()));
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(
				this.sc.getPopulation(), agentFactory, sc.getConfig(), sc, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
}
