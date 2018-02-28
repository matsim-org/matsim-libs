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
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.scenario.ScenarioByInstanceModule;

/**
 * @author nagel
 */
public class QSimUtils {
	 // should only contain static methods; should thus not be instantiated
	private QSimUtils() {}

	public static QSim createDefaultQSim(final Scenario scenario, final EventsManager eventsManager, MobsimTimer mobsimTimer, AgentCounter agentCounter, ActiveQSimBridge activeQSimBridge) {
		return createDefaultQSimWithOverrides(scenario, eventsManager, Collections.singleton(new AuxiliaryQSimModule(agentCounter, mobsimTimer, activeQSimBridge)));
	}
	
	// This has been commented out, because it was often used in test, but it is misleading in terms of what 
	// need to be injected into the QSim or not. (Before nothing needed to be injected). shoerl, feb18
	/*public static QSim createDefaultQSim(final Scenario scenario, final EventsManager eventsManager) {
		MobsimTimer mobsimTimer = new MobsimTimer(scenario.getConfig());
		AgentCounter agentCounter = new AgentCounterImpl();
		
		return createDefaultQSim(scenario, eventsManager, mobsimTimer, agentCounter);
	}*/
	
	/**
	 * This is a convenience factory method! Whenever you construct more complex QSims, 
	 * especially when you need a consistent MobsimTimer or AgentCounter in multiple classes, 
	 * use the factory method where you can define them explicitly!
	 * 
	 * Another hint: You especially need to avoid this method if you create a Provider<QSim>. 
	 * Inject the global AgentCounter and MobsimTimer into your Provider and use those with the
	 * other factory method.
	 */
	public static QSim createDefaultQSimWithDefaultTimerAndCounter(final Scenario scenario, final EventsManager eventsManager) {
		MobsimTimer mobsimTimer = new MobsimTimer(scenario.getConfig());
		AgentCounter agentCounter = new AgentCounterImpl();
		ActiveQSimBridge activeQSimModule = new ActiveQSimBridge();
		
		return createDefaultQSim(scenario, eventsManager, mobsimTimer, agentCounter, activeQSimModule);
	}
	
	public static QSim createDefaultQSimWithOverrides( final Scenario scenario, final EventsManager eventsManager, 
			Collection<AbstractModule> overrides ) {
		/*
		 * Not sure what happened here. This should work now but it doesn't seem that the 
		 * previous version was actually doing what it was supposed to.
		 * 
		 * shoerl, feb18
		 */
		
		AbstractModule module = new StandaloneQSimModule(scenario, eventsManager);

		for ( AbstractModule override : overrides ) {
			module = org.matsim.core.controler.AbstractModule.override(Collections.singleton(module), override) ;
		}
		
		Injector injector = org.matsim.core.controler.Injector.createInjector(scenario.getConfig(), module );
		return (QSim) injector.getInstance(Mobsim.class);
	}

	public static QSim createQSim(final Scenario scenario, final EventsManager eventsManager, final Collection<AbstractQSimPlugin> plugins, MobsimTimer mobsimTimer, AgentCounter agentCounter, ActiveQSimBridge activeQSimBridge) {
		Injector injector = org.matsim.core.controler.Injector.createInjector(scenario.getConfig(),
				org.matsim.core.controler.AbstractModule.override(Collections.singleton(new StandaloneQSimModule(scenario, eventsManager)),
				new org.matsim.core.controler.AbstractModule() {
					@Override
					public void install() {
						install(new AuxiliaryQSimModule(agentCounter, mobsimTimer, activeQSimBridge));
						bind(new TypeLiteral<Collection<AbstractQSimPlugin>>() {}).toInstance(plugins);
					}
				}));
		return (QSim) injector.getInstance(Mobsim.class);
	}
	
	private static class AuxiliaryQSimModule extends org.matsim.core.controler.AbstractModule {
		final private AgentCounter agentCounter;
		final private MobsimTimer mobsimTimer;
		final private ActiveQSimBridge activeQSimBridge;
		
		public AuxiliaryQSimModule(AgentCounter agentCounter, MobsimTimer mobsimTimer, ActiveQSimBridge activeQSimBridge) {
			this.agentCounter = agentCounter;
			this.mobsimTimer = mobsimTimer;
			this.activeQSimBridge = activeQSimBridge;
		}
		
		@Override
		public void install() {
			bind(AgentCounter.class).toInstance(agentCounter);
			bind(MobsimTimer.class).toInstance(mobsimTimer);
			bind(ActiveQSimBridge.class).toInstance(activeQSimBridge);
		}
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
