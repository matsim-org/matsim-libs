/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package org.matsim.contrib.freight.mobsim;

import java.util.Collection;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.CarrierConfig;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;

import com.google.inject.Provider;


public class FreightQSimFactory implements Provider<Mobsim> {

	private final Scenario scenario;
	private EventsManager eventsManager;
	private CarrierAgentTracker carrierAgentTracker;
	private CarrierConfig carrierConfig;

	@Inject
	public FreightQSimFactory(Scenario scenario, EventsManager eventsManager, CarrierAgentTracker carrierAgentTracker, CarrierConfig carrierConfig) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.carrierAgentTracker = carrierAgentTracker;
		this.carrierConfig = carrierConfig;
	}

	@Override
	public Mobsim get() {
		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		final QSim sim = new QSimBuilder(scenario.getConfig()).useDefaults().build(scenario, eventsManager);
		Collection<MobSimVehicleRoute> vRoutes = carrierAgentTracker.createPlans();
		FreightAgentSource agentSource = new FreightAgentSource(vRoutes, new DefaultAgentFactory(sim), sim);
		sim.addAgentSource(agentSource);
		if (carrierConfig.getPhysicallyEnforceTimeWindowBeginnings()) {
			WithinDayActivityReScheduling withinDayActivityRescheduling = new WithinDayActivityReScheduling(agentSource, carrierAgentTracker);
			sim.addQueueSimulationListeners(withinDayActivityRescheduling);
		}
		return sim;
	}

}
