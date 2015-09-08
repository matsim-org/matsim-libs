/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.core.mobsim.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.scenario.ScenarioElementsModule;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author nagel
 */
public class QSimUtils {
	 // should only contain static methods; should thus not be instantiated
	private QSimUtils() {}

	public static QSim createDefaultQSim(final Scenario scenario, final EventsManager eventsManager) {
		final Collection<AbstractQSimPlugin> plugins = defaultQSimPlugins(scenario.getConfig());
		return createQSim(scenario, eventsManager, plugins);
	}

	public static QSim createQSim(final Scenario scenario, final EventsManager eventsManager, final Collection<AbstractQSimPlugin> plugins) {
		AbstractModule module = new AbstractModule() {
			@Override
			public void install() {
				install(new ScenarioElementsModule());
				for (AbstractQSimPlugin plugin : plugins) {
					for (AbstractModule module : plugin.modules()) {
						install(module);
					}
				}
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
			}
		};
		Injector injector = Injector.createInjector(scenario.getConfig(), module);
		QSim qSim = injector.getInstance(QSim.class);
		for (AbstractQSimPlugin plugin : plugins) {
			for (Class<? extends MobsimEngine> mobsimEngine : plugin.engines()) {
				qSim.addMobsimEngine(injector.getInstance(mobsimEngine));
			}
			for (Class<? extends ActivityHandler> activityHandler : plugin.activityHandlers()) {
				qSim.addActivityHandler(injector.getInstance(activityHandler));
			}
			for (Class<? extends DepartureHandler> mobsimEngine : plugin.departureHandlers()) {
				qSim.addDepartureHandler(injector.getInstance(mobsimEngine));
			}
			for (Class<? extends MobsimListener> mobsimListener : plugin.listeners()) {
				qSim.addQueueSimulationListeners(injector.getInstance(mobsimListener));
			}
			for (Class<? extends AgentSource> agentSource : plugin.agentSources()) {
				qSim.addAgentSource(injector.getInstance(agentSource));
			}
		}
		return qSim;
	}

	private static Collection<AbstractQSimPlugin> defaultQSimPlugins(Config config) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin());
		plugins.add(new ActivityEnginePlugin());
		plugins.add(new QNetsimEnginePlugin());
		if (config.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin());
		}
		if (config.transit().isUseTransit()) {
			plugins.add(new TransitEnginePlugin());
		}
		plugins.add(new TeleportationPlugin());
		plugins.add(new PopulationPlugin());
		return plugins;
	}
}
