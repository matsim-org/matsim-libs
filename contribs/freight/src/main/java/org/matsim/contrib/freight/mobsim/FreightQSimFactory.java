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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;

import java.util.Collection;


public class FreightQSimFactory implements MobsimFactory {

	private CarrierAgentTracker carrierAgentTracker;
	
	private boolean physicallyEnforceTimeWindowBeginnings = false;

	public FreightQSimFactory(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		final QSim sim = (QSim) QSimUtils.createDefaultQSim(sc, eventsManager);
		Collection<MobSimVehicleRoute> vRoutes = carrierAgentTracker.createPlans();
		FreightAgentSource agentSource = new FreightAgentSource(vRoutes, new DefaultAgentFactory(sim), sim);
		sim.addAgentSource(agentSource);
		if (physicallyEnforceTimeWindowBeginnings) {
			WithinDayActivityReScheduling withinDayActivityRescheduling = new WithinDayActivityReScheduling(agentSource, carrierAgentTracker);
			sim.addQueueSimulationListeners(withinDayActivityRescheduling);
			sim.addMobsimEngine(withinDayActivityRescheduling);
		}
		return sim;
	}

	/**
	 * Physically enforces beginnings of time windows for freight activities, i.e. freight agents
	 * wait before closed doors until they can deliver / pick up their goods, and then take their required duration.
	 * 
	 * <p>The default value is false. Time windows will be ignored by the physical simulation, leaving treatment
	 * of early arrival to the Scoring.
	 * 
	 * 
	 * @param physicallyEnforceTimeWindowBeginnings
	 * @see WithinDayActivityReScheduling
	 */
	public void setPhysicallyEnforceTimeWindowBeginnings(boolean physicallyEnforceTimeWindowBeginnings) {
		this.physicallyEnforceTimeWindowBeginnings = physicallyEnforceTimeWindowBeginnings;
	}

}
