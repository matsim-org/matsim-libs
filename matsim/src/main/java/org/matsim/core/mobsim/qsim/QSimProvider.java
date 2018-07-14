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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

public class QSimProvider implements Provider<QSim> {
	private static final Logger log = Logger.getLogger( QSimProvider.class ) ;
	
	private Injector injector;
	private Collection<AbstractQSimPlugin> plugins;

	@Inject
    QSimProvider(Injector injector, Collection<AbstractQSimPlugin> plugins) {
        this.injector = injector;
		this.plugins = plugins;
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
        
        Map<String, Class<? extends MobsimEngine>> availableMobimEngines = new HashMap<>();
        Map<String, Class<? extends ActivityHandler>> availableActivityHandlers = new HashMap<>();
        Map<String, Class<? extends DepartureHandler>> availableDepartureHandlers = new HashMap<>();
        Map<String, Class<? extends AgentSource>> availableAgentSources = new HashMap<>();
        
        for (AbstractQSimPlugin plugin : plugins) {
        	availableMobimEngines.putAll(plugin.engines());
        	availableActivityHandlers.putAll(plugin.activityHandlers());
        	availableDepartureHandlers.putAll(plugin.departureHandlers());
        	availableAgentSources.putAll(plugin.agentSources());
        }
        
        
        
        
        for (AbstractQSimPlugin plugin : plugins) {
	  		// add each plugin's mobsim engines:
			for (Map.Entry<String, Class<? extends MobsimEngine>> mobsimEngine : plugin.engines().entrySet()) {
				qSim.addMobsimEngine(qSimLocalInjector.getInstance(mobsimEngine));
			}
	  		// add each plugin's activity handlers:
			for (Map.Entry<String, Class<? extends ActivityHandler>> activityHandler : plugin.activityHandlers().entrySet()) {
				qSim.addActivityHandler(qSimLocalInjector.getInstance(activityHandler));
			}
	  		// add each plugin's departure handlers:
			for (Map.Entry<String, Class<? extends DepartureHandler>> mobsimEngine : plugin.departureHandlers().entrySet()) {
				qSim.addDepartureHandler(qSimLocalInjector.getInstance(mobsimEngine));
			}
	  		// add each plugin's mobsim listeners:
			for (Class<? extends MobsimListener> mobsimListener : plugin.listeners()) {
				qSim.addQueueSimulationListeners(qSimLocalInjector.getInstance(mobsimListener));
			}
	  		// add each plugin's agent sources:
			for (Map.Entry<String, Class<? extends AgentSource>> agentSource : plugin.agentSources().entrySet()) {
				qSim.addAgentSource(qSimLocalInjector.getInstance(agentSource));
			}
		}
        return qSim;
    }

}
