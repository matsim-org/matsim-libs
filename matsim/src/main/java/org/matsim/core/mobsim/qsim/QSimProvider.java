/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * QSimProvider.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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
 */

package org.matsim.core.mobsim.qsim;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.components.ComponentRegistry;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class QSimProvider implements Provider<QSim> {
	private static final Logger log = Logger.getLogger(QSimProvider.class);

	private Injector injector;
	private Config config;
	private Collection<AbstractQSimModule> modules;
	private List<AbstractQSimModule> overridingModules;
	private QSimComponentsConfig components;
	@Inject(optional = true)
	private IterationCounter iterationCounter;

	@Inject
	QSimProvider(Injector injector, Config config, Collection<AbstractQSimModule> modules, QSimComponentsConfig components,
			@Named("overrides") List<AbstractQSimModule> overridingModules) {
		this.injector = injector;
		this.modules = modules;
		// (these are the implementations)
		this.config = config;
		this.components = components;
		this.overridingModules = overridingModules;
	}

	@Override
	public QSim get() {
		modules.forEach(m -> m.setConfig(config));
		overridingModules.forEach(m -> m.setConfig(config));

		AbstractQSimModule qsimModule = AbstractQSimModule.overrideQSimModules(modules, overridingModules);

		AbstractModule module = new AbstractModule() {
			@Override
			protected void configure() {
				install(qsimModule);
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
			}
		};

		Injector qsimInjector = injector.createChildInjector(module);
		if (iterationCounter == null
				|| config.controler().getFirstIteration() == iterationCounter.getIterationNumber()) {
			// trying to somewhat reduce logfile verbosity. kai, aug'18
			org.matsim.core.controler.Injector.printInjector(qsimInjector, log);
		}
		QSim qSim = qsimInjector.getInstance(QSim.class);

		ComponentRegistry componentRegistry = ComponentRegistry.create(qsimInjector);

		for (Key<? extends QSimComponent> component : componentRegistry.getOrderedComponents(components)) {
			//if (component.getTypeLiteral().getRawType().isAssignableFrom(MobsimEngine.class)) {
			if (MobsimEngine.class.isAssignableFrom(component.getTypeLiteral().getRawType())) {
				MobsimEngine instance = (MobsimEngine) qsimInjector.getInstance(component);
				qSim.addMobsimEngine(instance);
				log.info("Added MobsimEngine " + instance.getClass());
			}

			if (ActivityHandler.class.isAssignableFrom(component.getTypeLiteral().getRawType())) {
				ActivityHandler instance = (ActivityHandler) qsimInjector.getInstance(component);
				qSim.addActivityHandler(instance);
				log.info("Added Activityhandler " + instance.getClass());
			}

			if (DepartureHandler.class.isAssignableFrom(component.getTypeLiteral().getRawType())) {
				DepartureHandler instance = (DepartureHandler) qsimInjector.getInstance(component);
				qSim.addDepartureHandler(instance);
				log.info("Added DepartureHandler " + instance.getClass());
			}

			if (AgentSource.class.isAssignableFrom(component.getTypeLiteral().getRawType())) {
				AgentSource instance = (AgentSource) qsimInjector.getInstance(component);
				qSim.addAgentSource(instance);
				log.info("Added AgentSource " + instance.getClass());
			}

			if (MobsimListener.class.isAssignableFrom(component.getTypeLiteral().getRawType())) {
				MobsimListener instance = (MobsimListener) qsimInjector.getInstance(component);
				qSim.addQueueSimulationListeners(instance);
				log.info("Added MobsimListener " + instance.getClass());
			}
		}

		return qSim;
	}

}
