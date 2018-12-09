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

import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.IterationCounter;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class QSimProvider implements Provider<QSim> {
	private static final Logger log = Logger.getLogger(QSimProvider.class);

	private Injector injector;
	private Config config;
	private Collection<AbstractQSimModule> modules;
	private List<AbstractQSimModule> overridingModules;
	private final Map<Named, QSimComponent> qSimComponentMap;
	private QSimComponentsConfig annotationsRegistry;
	@Inject(optional = true)
	private IterationCounter iterationCounter;

	@Inject
	QSimProvider( Injector injector, Config config, Collection<AbstractQSimModule> modules,
			  QSimComponentsConfig components, @Named("overrides") List<AbstractQSimModule> overridingModules, Map<Named,QSimComponent> qSimComponentMap ) {
		this.injector = injector;
		this.modules = modules;
		// (these are the implementations)
		this.config = config;
		this.annotationsRegistry = components;
		this.overridingModules = overridingModules;
		this.qSimComponentMap = qSimComponentMap;
	}

	@Override
	public QSim get() {
		performHistoricalCheck(injector);

		modules.forEach(m -> m.setConfig(config));
		overridingModules.forEach(m -> m.setConfig(config));

		QSimComponentsConfigGroup cConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );;
		log.warn( cConfig ) ;

		AbstractQSimModule qsimModule = AbstractQSimModule.overrideQSimModules(modules, overridingModules);

		AbstractModule module = new AbstractModule() {
			@Override
			protected void configure() {
				install(qsimModule);
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
			}
		};

		// create the child injector:
		Injector qsimInjector = injector.createChildInjector(module);

//		if (iterationCounter == null || config.controler().getFirstIteration() == iterationCounter.getIterationNumber()) {
//			// trying to somewhat reduce logfile verbosity. kai, aug'18
//			org.matsim.core.controler.Injector.printInjector(qsimInjector, log);
//		}
		// (printing out the injector is somewhat useless since it contains more bindings than what is afterwards used. kai, dec'18)

		// pull out the qsim:
		QSim qSim = qsimInjector.getInstance(QSim.class);


//		QSimComponentsRegistry componentRegistry = QSimComponentsRegistry.create(qsimInjector );

//		log.warn("") ;
//		log.warn( "annotationsRegistry=" ) ;
//		for( Object activeComponentAnnotation : this.annotationsRegistry.getActiveComponentAnnotations() ){
//			log.warn("registered annotation=" + activeComponentAnnotation ) ;
//		}
//		log.warn("") ;

//		for (Key<? extends QSimComponent> component : componentRegistry.getOrderedComponents( annotationsRegistry )) {
		for ( Object annotation : annotationsRegistry.getActiveComponentAnnotations() ) {

			log.warn("looking at annotation=" + annotation ) ;

			if ( ! ( annotation instanceof  String ) ) {
				continue ;
			}
			String str = (String) annotation;;
			QSimComponent instance = this.qSimComponentMap.get( Names.named(str) );;

			//			QSimComponent instance = qsimInjector.getInstance( component );;

			if ( instance instanceof  MobsimEngine ) {
				qSim.addMobsimEngine( (MobsimEngine) instance );
				log.info("Added MobsimEngine " + instance.getClass());
			}

			if ( instance instanceof ActivityHandler) {
				qSim.addActivityHandler( (ActivityHandler) instance );
				log.info("Added ActivityHandler " + instance.getClass());
			}

			if ( instance instanceof DepartureHandler ){
				qSim.addDepartureHandler( (DepartureHandler) instance );
				log.info("Added DepartureHandler " + instance.getClass());
			}

			if ( instance instanceof AgentSource ) {
				qSim.addAgentSource( (AgentSource) instance );
				log.info("Added AgentSource " + instance.getClass());
			}

			if ( instance instanceof  MobsimListener ) {
				qSim.addQueueSimulationListeners( (MobsimListener) instance);
				log.info("Added MobsimListener " + instance.getClass());
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
