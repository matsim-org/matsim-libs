/* *********************************************************************** *
 * project: org.matsim.*
 * GuidanceMobsimFactory
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
package playground.vsptelematics.ha2;

import com.google.inject.Provider;

import playground.vsptelematics.common.TelematicsConfigGroup;
import playground.vsptelematics.common.TelematicsConfigGroup.Infotype;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;

import javax.inject.Inject;
import javax.inject.Singleton;


/**
 * @author dgrether
 * 
 */
@Singleton
public class GuidanceMobsimFactory implements Provider<Mobsim>, ShutdownListener {

	private double equipmentFraction;
	private Guidance guidance = null;
	private Infotype type;
	private String outfile;
	private GuidanceRouteTTObserver ttObserver;
	private Scenario scenario;
	private EventsManager eventsManager;

	@Inject
	GuidanceMobsimFactory(GuidanceRouteTTObserver ttObserver, Scenario scenario, EventsManager eventsManager, OutputDirectoryHierarchy outputDirectoryHierarchy) {
		TelematicsConfigGroup tcg = ConfigUtils.addOrGetModule(scenario.getConfig(), TelematicsConfigGroup.GROUPNAME, TelematicsConfigGroup.class);
		this.equipmentFraction = tcg.getEquipmentRate();
		this.type = tcg.getInfotype();
		this.outfile = outputDirectoryHierarchy.getOutputFilename("guidance.txt");
		this.ttObserver = ttObserver;
		this.scenario = scenario;
		this.eventsManager = eventsManager;
	}
	
	private void initGuidance(Network network){
		switch (type){
		case reactive:
			this.guidance = new ReactiveGuidance(network, outfile);
			break;
		case estimated:
			this.guidance = new EstimatedGuidance(network, outfile);
			break;
		default:
			throw new IllegalStateException("Guidance type " + type + " is not known!");
		}
	}

	Mobsim createMobsim(Scenario scenario, EventsManager eventsManager) {
		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
        QSim qSim = new QSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim);
		TeleportationEngine teleportationEngine = new TeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);

		if (scenario.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());
		}

		this.initGuidance(scenario.getNetwork());
		eventsManager.addHandler(this.guidance);
		qSim.addQueueSimulationListeners(this.guidance);
		AgentFactory agentFactory = new GuidanceAgentFactory(qSim, equipmentFraction, this.guidance, this.ttObserver);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory,
				qSim);
		qSim.addAgentSource(agentSource);
		return qSim;

	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		this.guidance.notifyShutdown();
	}

	@Override
	public Mobsim get() {
		return createMobsim(scenario, eventsManager);
	}
}
