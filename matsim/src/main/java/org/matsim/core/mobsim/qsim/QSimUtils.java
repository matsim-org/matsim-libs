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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.scenario.ScenarioElementsModule;

/**
 * @author nagel
 */
public class QSimUtils {
	 // should only contain static methods; should thus not be instantiated
	private QSimUtils() {}

	public static QSim createDefaultQSim(final Scenario scenario, final EventsManager eventsManager) {
		AbstractModule module = new AbstractModule() {
			@Override
			public void install() {
				install(new ScenarioElementsModule());
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(QSim.class).asEagerSingleton();
				bind(Netsim.class).to(QSim.class);
				bind(ActivityEngine.class).asEagerSingleton();
				bind(QNetsimEngine.class);
				bind(TeleportationEngine.class).asEagerSingleton();
				if (getConfig().transit().isUseTransit()) {
					bind(AgentFactory.class).to(TransitAgentFactory.class).asEagerSingleton();
					bind(TransitQSimEngine.class).asEagerSingleton();
					bind(TransitStopHandlerFactory.class).to(ComplexTransitStopHandlerFactory.class).asEagerSingleton();
				} else {
					bind(AgentFactory.class).to(DefaultAgentFactory.class).asEagerSingleton();
				}
				if (scenario.getConfig().network().isTimeVariantNetwork()) {
					bind(NetworkChangeEventsEngine.class).asEagerSingleton();
				}
				bind(PopulationAgentSource.class).asEagerSingleton();
			}
		};
		Injector injector = Injector.createInjector(scenario.getConfig(), module);
		QSim qSim = injector.getInstance(QSim.class);
		if (scenario.getConfig().transit().isUseTransit()) {
			TransitQSimEngine transitEngine = injector.getInstance(TransitQSimEngine.class);
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		}
		if (scenario.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(injector.getInstance(NetworkChangeEventsEngine.class));
		}
		ActivityEngine activityEngine = injector.getInstance(ActivityEngine.class);
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = injector.getInstance(QNetsimEngine.class);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		qSim.addMobsimEngine(injector.getInstance(TeleportationEngine.class));
		qSim.addAgentSource(injector.getInstance(PopulationAgentSource.class));
		return qSim;
	}
}
