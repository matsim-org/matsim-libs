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

import com.google.inject.*;
import com.google.inject.util.Modules;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioElementsModule;

import java.util.Collection;

/**
 * @author nagel
 */
public class QSimUtils {
	 // should only contain static methods; should thus not be instantiated
	private QSimUtils() {}

	public static QSim createDefaultQSim(final Scenario scenario, final EventsManager eventsManager) {
		Injector injector = Guice.createInjector(new StandaloneQSimModule(scenario, eventsManager));
		return (QSim) injector.getInstance(Mobsim.class);
	}

	public static QSim createQSim(final Scenario scenario, final EventsManager eventsManager, final Collection<AbstractQSimPlugin> plugins) {
		Module module = Modules.override(new StandaloneQSimModule(scenario, eventsManager))
				.with(new AbstractModule() {
					@Override
					protected void configure() {
						bind(new TypeLiteral<Collection<AbstractQSimPlugin>>(){}).toInstance(plugins);
					}
				});
		Injector injector = Guice.createInjector(module);
		return (QSim) injector.getInstance(Mobsim.class);
	}

	private static class StandaloneQSimModule extends AbstractModule {
		private final Scenario scenario;
		private final EventsManager eventsManager;

		public StandaloneQSimModule(Scenario scenario, EventsManager eventsManager) {
			this.scenario = scenario;
			this.eventsManager = eventsManager;
		}

		@Override
		protected void configure() {
			binder().requireExplicitBindings();
			bind(Config.class).toInstance(scenario.getConfig());
			bind(Scenario.class).toInstance(scenario);
			install(new ScenarioElementsModule(scenario.getConfig()));
			bind(EventsManager.class).toInstance(eventsManager);
			install(new QSimModule());
		}
	}
}
