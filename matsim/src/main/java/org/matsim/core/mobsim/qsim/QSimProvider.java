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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.config.Config;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

public class QSimProvider implements Provider<QSim> {
	private static final Logger log = Logger.getLogger( QSimProvider.class ) ;
	
	private Injector injector;
	private Collection<AbstractQSimPlugin> plugins;
	private Config config;

	@Inject
    QSimProvider(Injector injector, Collection<AbstractQSimPlugin> plugins, Config config) {
        this.injector = injector;
		this.plugins = plugins;
		this.config = config;
    }

    @Override
    public QSim get() {
	    com.google.inject.AbstractModule module = new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				for (AbstractQSimPlugin plugin : plugins) {
					// install each plugin's modules:
					for (Module module1 : plugin.modules()) {
						install(module1);
					}
				}
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
			}
		};
        Injector qSimLocalInjector = injector.createChildInjector(module);
        org.matsim.core.controler.Injector.printInjector( qSimLocalInjector, log ) ;
        QSim qSim = qSimLocalInjector.getInstance(QSim.class);
//        qSim.setChildInjector( qSimLocalInjector ) ;
        
        ComponentRegistry<MobsimEngine> mobsimEngineRegistry = new ComponentRegistry<>("MobsimEngine");
        ComponentRegistry<ActivityHandler> activityHandlerRegister = new ComponentRegistry<>("ActivityHandler");
        ComponentRegistry<DepartureHandler> departureHandlerRegistry = new ComponentRegistry<>("DepartureHandler");
        ComponentRegistry<AgentSource> agentSourceRegistry = new ComponentRegistry<>("AgentSource");
        
        for (AbstractQSimPlugin plugin : plugins) {
        	plugin.engines().forEach(mobsimEngineRegistry::register);
        	plugin.activityHandlers().forEach(activityHandlerRegister::register);
        	plugin.departureHandlers().forEach(departureHandlerRegistry::register);
        	plugin.agentSources().forEach(agentSourceRegistry::register);
        }
        
        List<String> mobsimEngineOrdering = config.qsim().getActiveMobsimEngines();
        List<String> activityHandlerOrdering = config.qsim().getActiveActivityHandlers();
        List<String> departureHandlerOrdering = config.qsim().getActiveDepartureHandlers();
        List<String> agentSourceOrdering = config.qsim().getActiveAgentSources();
        
        /* if (config.network().isTimeVariantNetwork()) {
        	mobsimEngineOrdering.add("NetworkChangeEventsEngine");
        } */
        
        /* if (config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim()) {
            mobsimEngineOrdering.add("TransitEngine");
        	departureHandlerOrdering.add("TransitEngine");
        	agentSourceOrdering.add("TransitEngine");
        } */
        
        //departureHandlerOrdering.add("TeleportationEngine");
        
        mobsimEngineRegistry.getOrderedComponents(mobsimEngineOrdering).stream().map(qSimLocalInjector::getInstance).forEach(qSim::addMobsimEngine);
        activityHandlerRegister.getOrderedComponents(activityHandlerOrdering).stream().map(qSimLocalInjector::getInstance).forEach(qSim::addActivityHandler);
        departureHandlerRegistry.getOrderedComponents(departureHandlerOrdering).stream().map(qSimLocalInjector::getInstance).forEach(qSim::addDepartureHandler);
        agentSourceRegistry.getOrderedComponents(agentSourceOrdering).stream().map(qSimLocalInjector::getInstance).forEach(qSim::addAgentSource);
        
        for (AbstractQSimPlugin plugin : plugins) {
        	plugin.listeners().stream().map(qSimLocalInjector::getInstance).forEach(qSim::addQueueSimulationListeners);
        }
        
        return qSim;
    }

}
