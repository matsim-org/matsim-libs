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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

/**
 * Created by IntelliJ IDEA. User: zilske Date: 10/31/11 Time: 4:34 PM To change
 * this template use File | Settings | File Templates.
 */
public class FreightQSimFactory implements MobsimFactory {

	static class Internals implements MobsimEngine {

		private InternalInterface internalInterface;
		
		@Override
		public void doSimStep(double time) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPrepareSim() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void afterSim() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {
			this.internalInterface = internalInterface;
		}
		
		public InternalInterface getInternalInterface(){
			return this.internalInterface;
		}
		
	}
	
	private CarrierAgentTracker carrierAgentTracker;
	private boolean withinDayReScheduling;

	public FreightQSimFactory(CarrierAgentTracker carrierAgentTracker) {
		this.carrierAgentTracker = carrierAgentTracker;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
		if (conf == null) {
			throw new NullPointerException(
					"There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		final QSim sim = (QSim) new QSimFactory().createMobsim(sc,eventsManager);
		Collection<Plan> plans = carrierAgentTracker.createPlans();
//		FreightAgentSource agentSource = new FreightAgentSource(plans, new DefaultAgentFactory(sim), sim);
		FreightAgentSource agentSource = new FreightAgentSource(plans, new ExperimentalBasicWithindayAgentFactory(sim), sim);
		sim.addAgentSource(agentSource);
		Internals internals = new Internals();
		sim.addMobsimEngine(internals);
		if(withinDayReScheduling){
			sim.addQueueSimulationListeners(new WithinDayActivityReScheduling(agentSource,internals.getInternalInterface(), carrierAgentTracker));
		}
		return sim;
	}

	public void setWithinDayActivityReScheduling(boolean withinDayReScheduling) {
		this.withinDayReScheduling = withinDayReScheduling;
	}
}
