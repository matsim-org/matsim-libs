/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.run;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.matsim.contrib.dvrp.passenger.PassengerEngineModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

/**
 * @author michalm
 */
public class DvrpQSimModuleBuilder {
	private final Function<Config, AbstractQSimModule> moduleCreator;
	private final Map<String, Class<? extends MobsimListener>> listeners = new HashMap<>();
	
	private int listenerIndex = 0;
	private boolean addPassengerEngineModule = true;

	public DvrpQSimModuleBuilder(Function<Config, AbstractQSimModule> moduleCreator) {
		this.moduleCreator = moduleCreator;
	}
	
	private String createNextListenerName(Class<? extends MobsimListener> listener) {
		return String.format("DVRP_%d_%s", listenerIndex++, listener.getClass().toString());
	}

	public DvrpQSimModuleBuilder addListener(Class<? extends MobsimListener> listener) {
		listeners.put(createNextListenerName(listener), listener);
		return this;
	}

	public DvrpQSimModuleBuilder addListeners(Collection<Class<? extends MobsimListener>> listener) {
		listener.forEach(this::addListener);
		return this;
	}

	public DvrpQSimModuleBuilder setAddPassengerEnginePlugin(boolean addPassengerEnginePlugin) {
		this.addPassengerEngineModule = addPassengerEnginePlugin;
		return this;
	}

	public AbstractQSimModule build(Config config) {
		return new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				install(new DynQSimModule(VrpAgentSource.class));
				
				if (addPassengerEngineModule) {
					install(new PassengerEngineModule(DvrpConfigGroup.get(config).getMode()));
				}
				
				install(new ListenerModule());
				install(moduleCreator.apply(config));
			}
		};
	}

	private class ListenerModule extends AbstractQSimModule {
		@Override
		protected void configureQSim() {
			for (Map.Entry<String, Class<? extends MobsimListener>> entry : listeners.entrySet()) {
				bindMobsimListener(entry.getKey()).to(entry.getValue());
			}
		}
	}
	
	public void configureComponents(QSimComponents components) {
		new DvrpQSimComponentsConfigurator(addPassengerEngineModule).configure(components);
		components.activeMobsimListeners.addAll(listeners.keySet());
	}
}
