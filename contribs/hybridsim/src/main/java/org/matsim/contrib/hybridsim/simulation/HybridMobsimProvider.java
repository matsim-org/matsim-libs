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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridQSimExternalNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

public class HybridMobsimProvider implements Provider<Mobsim>{
	
	private final Scenario sc;
	private final EventsManager em;
	private final HybridNetworkFactory netFac;

	@Inject
	HybridMobsimProvider(Scenario sc, EventsManager eventsManager, HybridNetworkFactory netFac) {
		this.sc = sc;
		this.em = eventsManager;
		this.netFac = netFac;
		
	}

	@Override
	public Mobsim get() {
		QSimConfigGroup conf = this.sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}


		QSim qSim = new QSim(this.sc, this.em);
		ActivityEngine activityEngine = new ActivityEngine(this.em, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		ExternalEngine e = new ExternalEngine(this.em, qSim);
		HybridQSimExternalNetworkFactory eFac = new HybridQSimExternalNetworkFactory(e);
		this.netFac.putNetsimNetworkFactory("2ext", eFac);
		this.netFac.putNetsimNetworkFactory("ext2", eFac);
		
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, this.netFac);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());


		qSim.addMobsimEngine(e);


		
//		CANetworkFactory fac = new CAMultiLaneNetworkFactory();
//		CANetsimEngine cae = new CANetsimEngine(qSim, fac);
//		qSim.addMobsimEngine(cae);
//		qSim.addDepartureHandler(cae.getDepartureHandler());


		TeleportationEngine teleportationEngine = new TeleportationEngine(this.sc, this.em);
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory;
		if (this.sc.getConfig().transit().isUseTransit()) {
			agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine
					.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		} else {
			agentFactory = new DefaultAgentFactory(qSim);
		}
		if (this.sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(
				this.sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

}
