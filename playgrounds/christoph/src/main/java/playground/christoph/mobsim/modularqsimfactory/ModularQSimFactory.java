/* *********************************************************************** *
 * project: org.matsim.*
 * ModularQSimFactory.java
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

package playground.christoph.mobsim.modularqsimfactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.SimStepParallelEventsManagerImpl;
import org.matsim.core.events.SynchronizedEventsManagerImpl;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

public class ModularQSimFactory implements MobsimFactory {
	
	private AgentFactoryFactory agentFactoryFactory;
	private final List<MobsimEngineFactory> mobsimEngineFactories = new ArrayList<MobsimEngineFactory>();
	private final List<AgentSourceFactory> agentSourceFactories = new ArrayList<AgentSourceFactory>();
	
	public static ModularQSimFactory createAndInitDefaultModularQSimFactory(Config config) {
		
		ModularQSimFactory factory = new ModularQSimFactory();

		factory.addMobsimEngineFactory(new ActivityEngineFactory());

		factory.addMobsimEngineFactory(new QNetsimEngineFactory());

		if (config.network().isTimeVariantNetwork()) {
			factory.addMobsimEngineFactory(new NetworkChangeEventsEngineFactory());
		}
		
		if (config.scenario().isUseTransit()) {
			factory.setAgentFactoryFactory(new TransitAgentFactoryFactory());
			factory.addMobsimEngineFactory(new TransitQSimEngineFactory());
		} else {
			factory.setAgentFactoryFactory(new DefaultAgentFactoryFactory());
		}
		
		factory.addMobsimEngineFactory(new TeleportationEngineFactory());
		
		factory.addAgentSourceFactories(new PopulationAgentSourceFactory());
		
		return factory;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		
		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();
		if (numOfThreads > 1) {
			/*
			 * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
			 * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
			 * SynchronizedEventsManagerImpl.
			 */
			if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
				eventsManager = new SynchronizedEventsManagerImpl(eventsManager);				
			}
		}
		
		QSim qSim = new QSim(sc, eventsManager);
		
		for (MobsimEngineFactory factory : this.mobsimEngineFactories) {
			MobsimEngine mobsimEngine = factory.createMobsimSimEngine(qSim);
			qSim.addMobsimEngine(mobsimEngine);
			
			if (mobsimEngine instanceof HasActivityHandler) {
				qSim.addActivityHandler(((HasActivityHandler) mobsimEngine).getActivityHandler());
			}
			if (mobsimEngine instanceof HasDepartureHandler) {
				qSim.addDepartureHandler(((HasDepartureHandler) mobsimEngine).getDepartureHandler());
			}
			if (mobsimEngine instanceof HasAgentSource) {
				qSim.addAgentSource(((HasAgentSource) mobsimEngine).getAgentSource());
			}
		}
		
		AgentFactory agentFactory = this.agentFactoryFactory.createAgentFactory(qSim);
		for (AgentSourceFactory factory : this.agentSourceFactories) {
			AgentSource agentSource = factory.createAgentSource(sc.getPopulation(), agentFactory, qSim);
			qSim.addAgentSource(agentSource);
		}

		return qSim;
	}
	
	public AgentFactoryFactory getAgentFactoryFactory() {
		return this.agentFactoryFactory;
	}

	public void setAgentFactoryFactory(AgentFactoryFactory agentFactoryFactory) {
		this.agentFactoryFactory = agentFactoryFactory;
	}

	public List<MobsimEngineFactory> getMobsimEngineFactories() {
		return Collections.unmodifiableList(this.mobsimEngineFactories);
	}

	public void addMobsimEngineFactory(MobsimEngineFactory factory) {
		this.mobsimEngineFactories.add(factory);
	}
	
	public void removeMobsimEngineFactory(MobsimEngineFactory factory) {
		this.mobsimEngineFactories.remove(factory);
	}
	
	public List<AgentSourceFactory> getAgentSourceFactories() {
		return Collections.unmodifiableList(this.agentSourceFactories);
	}

	public void addAgentSourceFactories(AgentSourceFactory factory) {
		this.agentSourceFactories.add(factory);
	}
	
	public void removeAgentSourceFactories(AgentSourceFactory factory) {
		this.agentSourceFactories.remove(factory);
	}
}