/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.external;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridQSimExternalNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

public class HybridExternalMobsimFactory implements MobsimFactory {

	private final Server server;
	private final BlockingMATSimInterfaceService service;
	private final CyclicBarrier simStepBarrier;


	public HybridExternalMobsimFactory() {
		CyclicBarrier startupBarrier = new CyclicBarrier(2);
		this.simStepBarrier = new CyclicBarrier(2);
		BlockingMATSimInterfaceService service = new BlockingMATSimInterfaceService(startupBarrier,this.simStepBarrier);
		this.service = service;
		this.server = new Server(service);
		
		//wait for connection here
		try {
			startupBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		QSim qSim = new QSim(sc, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		ExternalEngine e = new ExternalEngine(eventsManager, qSim,this.server.getRpcCtrl(),this.server.getClientService(),this.simStepBarrier);
		this.service.setSimeEngine(e);
		HybridQSimExternalNetworkFactory networkFactory = new HybridQSimExternalNetworkFactory(
				e);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, networkFactory);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		qSim.addMobsimEngine(e);

		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory;
		if (sc.getConfig().scenario().isUseTransit()) {
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
		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(
				sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
}
