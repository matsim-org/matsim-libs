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

import org.matsim.contrib.dvrp.passenger.PassengerEnginePlugin;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.contrib.dynagent.run.DynAgentSourcePlugin;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;

import com.google.inject.Module;

/**
 * @author michalm
 */
public class DvrpQSimPluginsProvider implements Provider<Collection<AbstractQSimPlugin>> {
	public interface DvrpQSimPluginsProviderFactory {
		DvrpQSimPluginsProvider create(Config config);
	}

	private final Config config;
	private final Function<Config, Module> moduleCreator;
	private final List<Class<? extends MobsimListener>> listeners = new ArrayList<>();

	private boolean addPassengerEnginePlugin = true;

	public DvrpQSimPluginsProvider(Config config, Function<Config, Module> moduleCreator) {
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
	public Collection<AbstractQSimPlugin> get() {
		final List<AbstractQSimPlugin> plugins = new ArrayList<>(DynQSimModule.provideQSimPlugins(config));
		if (addPassengerEnginePlugin) {
			plugins.add(new PassengerEnginePlugin(config, DvrpConfigGroup.get(config).getMode()));
		}
		plugins.add(new DynAgentSourcePlugin(config, VrpAgentSource.class));
		plugins.add(new QSimPlugin(config));
		return Collections.unmodifiableList(plugins);
	}

	private class QSimPlugin extends AbstractQSimPlugin {
		public QSimPlugin(Config config) {
			super(config);
		}

		@Override
		public Collection<? extends Module> modules() {
			return Collections.singletonList(moduleCreator.apply(getConfig()));
		}

		@Override
		public Collection<Class<? extends MobsimListener>> listeners() {
			return new ArrayList<>(listeners);
		}
	}
}
