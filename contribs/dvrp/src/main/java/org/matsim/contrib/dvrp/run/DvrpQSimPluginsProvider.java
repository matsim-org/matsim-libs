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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.inject.Provider;

import org.matsim.contrib.dvrp.passenger.PassengerEngineModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author michalm
 */
public class DvrpQSimPluginsProvider implements Provider<Collection<AbstractQSimModule>> {
	public interface DvrpQSimPluginsProviderFactory {
		DvrpQSimPluginsProvider create(Config config);
	}

	private final Config config;
	private final Function<Config, AbstractQSimModule> moduleCreator;
	private final List<Class<? extends MobsimListener>> listeners = new ArrayList<>();

	private boolean addPassengerEnginePlugin = true;

	public DvrpQSimPluginsProvider(Config config, Function<Config, AbstractQSimModule> moduleCreator) {
		this.config = config;
		this.moduleCreator = moduleCreator;
	}

	public DvrpQSimPluginsProvider addListener(Class<? extends MobsimListener> listener) {
		listeners.add(listener);
		return this;
	}

	public DvrpQSimPluginsProvider addListeners(Collection<Class<? extends MobsimListener>> listener) {
		listeners.addAll(listener);
		return this;
	}

	public DvrpQSimPluginsProvider setAddPassengerEnginePlugin(boolean addPassengerEnginePlugin) {
		this.addPassengerEnginePlugin = addPassengerEnginePlugin;
		return this;
	}

	@Override
	public Collection<AbstractQSimModule> get() {
		final List<AbstractQSimModule> modules = new ArrayList<>(
				DynQSimModule.provideQSimPlugins(config, VrpAgentSource.class));
		if (addPassengerEnginePlugin) {
			modules.add(new PassengerEngineModule(DvrpConfigGroup.get(config).getMode()));
		}
		modules.add(new ListenerModule());
		modules.add(moduleCreator.apply(config));
		return Collections.unmodifiableList(modules);
	}

	private class ListenerModule extends AbstractQSimModule {
		@Override
		protected void configureQSim() {
			int index = 0;

			for (Class<? extends MobsimListener> listener : listeners) {
				String listenerName = String.format("DVRP_%d_%s", index, listener.getClass().toString());
				addMobsimListener(listenerName).to(listener);
				index++;
			}

			// Attention! Right now listeners are not filtered by QSimComponents.
			// Later on, they will be, so we need a name here. The best would then
			// be to create a decorator for all the listeners registered here. On
			// the other side, with the new QSimModule's its probably easier to
			// define them from outside anyway than thrugh this way.
		}
	}
}
