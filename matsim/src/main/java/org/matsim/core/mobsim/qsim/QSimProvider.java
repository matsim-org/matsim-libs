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
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;

import javax.inject.Inject;
import java.util.Collection;

public class QSimProvider implements Provider<QSim> {

	private Injector injector;
	private Collection<AbstractQSimPlugin> plugins;

	@Inject
    QSimProvider(Injector injector, Collection<AbstractQSimPlugin> plugins) {
        this.injector = injector;
		this.plugins = plugins;
    }

    @Override
    public QSim get() {
        AbstractModule module = new AbstractModule() {
			@Override
			protected void configure() {
				for (AbstractQSimPlugin plugin : plugins) {
					for (Module module : plugin.modules()) {
						install(module);
					}
				}
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
			}
		};
        Injector qSimLocalInjector = injector.createChildInjector(module);
        QSim qSim = qSimLocalInjector.getInstance(QSim.class);
        for (AbstractQSimPlugin plugin : plugins) {
			for (Class<? extends MobsimEngine> mobsimEngine : plugin.engines()) {
				qSim.addMobsimEngine(qSimLocalInjector.getInstance(mobsimEngine));
			}
			for (Class<? extends ActivityHandler> activityHandler : plugin.activityHandlers()) {
				qSim.addActivityHandler(qSimLocalInjector.getInstance(activityHandler));
			}
			for (Class<? extends DepartureHandler> mobsimEngine : plugin.departureHandlers()) {
				qSim.addDepartureHandler(qSimLocalInjector.getInstance(mobsimEngine));
			}
			for (Class<? extends MobsimListener> mobsimListener : plugin.listeners()) {
				qSim.addQueueSimulationListeners(qSimLocalInjector.getInstance(mobsimListener));
			}
			for (Class<? extends AgentSource> agentSource : plugin.agentSources()) {
				qSim.addAgentSource(qSimLocalInjector.getInstance(agentSource));
			}
		}
        return qSim;
    }

}
