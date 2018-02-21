/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEnginePlugin;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourcePlugin;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public final class DvrpModule extends AbstractModule {
	public static final String DVRP_ROUTING = "dvrp_routing";// TODO ==> dvrp_optimizer???

	@SuppressWarnings("unchecked")
	public static DvrpModule create(final Class<? extends VrpOptimizer> vrpOptimizerClass,
			final Class<? extends PassengerRequestCreator> passengerRequestCreatorClass,
			final Class<? extends DynActionCreator> dynActionCreatorClass) {
		Module module = new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(VrpOptimizer.class).to(vrpOptimizerClass).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(passengerRequestCreatorClass).asEagerSingleton();
				bind(DynActionCreator.class).to(dynActionCreatorClass).asEagerSingleton();
			}
		};

		return MobsimListener.class.isAssignableFrom(vrpOptimizerClass)
				? new DvrpModule(module, (Class<? extends MobsimListener>)vrpOptimizerClass) : new DvrpModule(module);
	}

	@Inject
	private DvrpConfigGroup dvrpCfg;

	private final Function<Config, Module> moduleCreator;
	private final List<Class<? extends MobsimListener>> listeners;

	@SafeVarargs
	public DvrpModule(Module module, Class<? extends MobsimListener>... listeners) {
		this(cfg -> module, listeners);
	}

	@SafeVarargs
	public DvrpModule(Function<Config, Module> moduleCreator, Class<? extends MobsimListener>... listeners) {
		this.moduleCreator = moduleCreator;
		this.listeners = Arrays.asList(listeners);
	}

	@Override
	public void install() {
		String mode = dvrpCfg.getMode();
		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

		// Visualisation of schedules for DVRP DynAgents
		bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

		// VrpTravelTimeEstimator
		install(new DvrpTravelTimeModule());
	}

	@Provides
	@Singleton
	@Named(DvrpModule.DVRP_ROUTING)
	private Network provideDvrpRoutingNetwork(Network network, DvrpConfigGroup dvrpCfg) {
		if (dvrpCfg.getNetworkMode() == null) { // no mode filtering
			return network;
		}

		Network dvrpNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(dvrpNetwork, Collections.singleton(dvrpCfg.getNetworkMode()));
		return dvrpNetwork;
	}

	@Provides
	private Collection<AbstractQSimPlugin> provideQSimPlugins(Config config) {
		final Collection<AbstractQSimPlugin> plugins = DynQSimModule.createQSimPlugins(config);
		plugins.add(new PassengerEnginePlugin(config, dvrpCfg.getMode()));
		plugins.add(new VrpAgentSourcePlugin(config));
		plugins.add(new QSimPlugin(config));
		return plugins;
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
