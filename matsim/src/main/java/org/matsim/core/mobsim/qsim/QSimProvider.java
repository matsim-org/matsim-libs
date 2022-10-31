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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

public class QSimProvider implements Provider<QSim> {
	private static final Logger log = LogManager.getLogger(QSimProvider.class);

	private Injector injector;
	private Config config;
	private IterationCounter iterationCounter;
	private Collection<AbstractQSimModule> modules;
	private List<AbstractQSimModule> overridingModules;
	private Set<AbstractQSimModule> overridingModulesFromAbstractModule;
	private QSimComponentsConfig components;

	@Inject
	QSimProvider(Injector injector, Config config, IterationCounter iterationCounter,
			Collection<AbstractQSimModule> modules, QSimComponentsConfig components,
			@Named("overrides") List<AbstractQSimModule> overridingModules,
		     @Named("overridesFromAbstractModule") Set<AbstractQSimModule> overridingModulesFromAbstractModule ) {
		this.injector = injector;
		this.modules = modules;
		// (these are the implementations)
		this.config = config;
		this.iterationCounter = iterationCounter;
		this.components = components;
		this.overridingModules = overridingModules;
		this.overridingModulesFromAbstractModule = overridingModulesFromAbstractModule;
	}

	@Override
	public QSim get() {
		performHistoricalCheck(injector);

		modules.forEach(m -> m.setConfig(config));
		overridingModules.forEach(m -> m.setConfig(config));

		int iterationNumber = iterationCounter.getIterationNumber();
		modules.forEach(m -> m.setIterationNumber(iterationNumber));
		overridingModules.forEach(m -> m.setIterationNumber(iterationNumber));

		AbstractQSimModule qsimModule = AbstractQSimModule.overrideQSimModules(modules, Collections.emptyList());

		for (AbstractQSimModule override : overridingModulesFromAbstractModule) {
			qsimModule = AbstractQSimModule.overrideQSimModules(Collections.singleton(qsimModule), Collections.singletonList(override));
		}

		for (AbstractQSimModule override : overridingModules) {
			qsimModule = AbstractQSimModule.overrideQSimModules(Collections.singleton(qsimModule), Collections.singletonList(override));
		}

		final AbstractQSimModule finalQsimModule = qsimModule;
		
		AbstractModule module = new AbstractModule() {
			@Override
			protected void configure() {
				install(finalQsimModule);
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
			}
		};

		Injector qsimInjector = injector.createChildInjector(module);

		QSim qSim = qsimInjector.getInstance(QSim.class);

		for (Object activeComponent : components.getActiveComponents()) {
			Key<Collection<Provider<QSimComponent>>> activeComponentKey;
			if (activeComponent instanceof Annotation) {
				activeComponentKey = Key.get(new TypeLiteral<Collection<Provider<QSimComponent>>>(){}, (Annotation) activeComponent);
			} else {
				activeComponentKey = Key.get(new TypeLiteral<Collection<Provider<QSimComponent>>>(){}, (Class<? extends Annotation>) activeComponent);
			}

			Collection<Provider<QSimComponent>> providers = qsimInjector.getInstance(activeComponentKey);
			for (Provider<QSimComponent> provider : providers) {
				QSimComponent qSimComponent = provider.get();
				if (qSimComponent instanceof MobsimEngine) {
					MobsimEngine instance = (MobsimEngine) qSimComponent;
					qSim.addMobsimEngine(instance);
					log.info("Added MobsimEngine " + instance.getClass());
				}

				if (qSimComponent instanceof ActivityHandler) {
					ActivityHandler instance = (ActivityHandler) qSimComponent;
					qSim.addActivityHandler(instance);
					log.info("Added Activityhandler " + instance.getClass());
				}

				if (qSimComponent instanceof DepartureHandler) {
					DepartureHandler instance = (DepartureHandler) qSimComponent;
					qSim.addDepartureHandler(instance);
					log.info("Added DepartureHandler " + instance.getClass());
				}

				if (qSimComponent instanceof AgentSource) {
					AgentSource instance = (AgentSource) qSimComponent;
					qSim.addAgentSource(instance);
					log.info("Added AgentSource " + instance.getClass());
				}

				if (qSimComponent instanceof MobsimListener) {
					MobsimListener instance = (MobsimListener) qSimComponent;
					qSim.addQueueSimulationListeners(instance);
					log.info("Added MobsimListener " + instance.getClass());
				}

			}
		}

		return qSim;
	}

	/**
	 * Historically, some bindings that are used in the QSim were defined in the
	 * outer controller scope. This method checks that those bindings are now
	 * registered in the QSim scope.
	 */
	private void performHistoricalCheck(Injector injector) {
		boolean foundNetworkFactoryBinding = true;

		try {
			injector.getBinding(QNetworkFactory.class);
		} catch (ConfigurationException e) {
			foundNetworkFactoryBinding = false;
		}

		if (foundNetworkFactoryBinding) {
			throw new IllegalStateException("QNetworkFactory should only be bound via AbstractQSimModule");
		}

		boolean foundTransitStopHandlerFactoryBinding = true;

		try {
			injector.getBinding(TransitStopHandlerFactory.class);
		} catch (ConfigurationException e) {
			foundTransitStopHandlerFactoryBinding = false;
		}

		if (foundTransitStopHandlerFactoryBinding) {
			throw new IllegalStateException("TransitStopHandlerFactory should be bound via AbstractQSimModule");
		}
	}
}
