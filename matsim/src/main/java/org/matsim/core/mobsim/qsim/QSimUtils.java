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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioByInstanceModule;

/**
 * @author nagel
 */
public class QSimUtils {
	 // should only contain static methods; should thus not be instantiated
	private QSimUtils() {}

	public static QSim createDefaultQSim(final Scenario scenario, final EventsManager eventsManager) {
		Injector injector = org.matsim.core.controler.Injector.createInjector(scenario.getConfig(), new StandaloneQSimModule(scenario, eventsManager));
		return (QSim) injector.getInstance(Mobsim.class);
	}
	
	public static QSim createDefaultQSimWithOverrides( final Scenario scenario, final EventsManager eventsManager, 
			List<AbstractModule> overrides ) {
//		final StandaloneQSimModule module = new StandaloneQSimModule(scenario, eventsManager);
//		for ( AbstractModule override : overrides ) {
//			org.matsim.core.controler.AbstractModule.override(Collections.singleton(module), override) ;
//		}
//		Injector injector = org.matsim.core.controler.Injector.createInjector(scenario.getConfig(), module );
//		return (QSim) injector.getInstance(Mobsim.class);
		return createQSim( scenario, eventsManager, overrides, Collections.emptyList() ) ;
	}

	public static QSim createQSim(final Scenario scenario, final EventsManager eventsManager, final Collection<AbstractQSimPlugin> plugins) {
//		final StandaloneQSimModule module = new StandaloneQSimModule(scenario, eventsManager);
//		final AbstractModule override = AbstractModule.override(Collections.singleton(module),
//				new AbstractModule() {
//					@Override
//					public void install() {
//						bind(new TypeLiteral<Collection<AbstractQSimPlugin>>() {
//						}).toInstance(plugins);
//					}
//				});
//		Injector injector = org.matsim.core.controler.Injector.createInjector(scenario.getConfig(),
//				override);
//		return (QSim) injector.getInstance(Mobsim.class);
		return createQSim(scenario, eventsManager, Collections.emptyList(), plugins ) ;
	}
	
	public static QSim createQSim( final Scenario scenario, final EventsManager eventsManager,
								   final List<AbstractModule> overrides, final Collection<AbstractQSimPlugin> plugins ) {
		AbstractModule module = new StandaloneQSimModule(scenario, eventsManager);
		for ( AbstractModule override : overrides ) {
			module = org.matsim.core.controler.AbstractModule.override(Collections.singleton(module), override) ;
		}
		final AbstractModule override = AbstractModule.override(Collections.singleton(module),
				new AbstractModule() {
					@Override
					public void install() {
						bind(new TypeLiteral<Collection<AbstractQSimPlugin>>() {
						}).toInstance(plugins);
					}
				});
		Injector injector = org.matsim.core.controler.Injector.createInjector(scenario.getConfig(), override);
		return (QSim) injector.getInstance(Mobsim.class);
	}

	private static class StandaloneQSimModule extends org.matsim.core.controler.AbstractModule {
		private final Scenario scenario;
		private final EventsManager eventsManager;

		public StandaloneQSimModule(Scenario scenario, EventsManager eventsManager) {
			this.scenario = scenario;
			this.eventsManager = eventsManager;
		}

		@Override
		public void install() {
			install(new ScenarioByInstanceModule(scenario));
			bind(EventsManager.class).toInstance(eventsManager);
			install(new QSimModule());
		}
	}
}
