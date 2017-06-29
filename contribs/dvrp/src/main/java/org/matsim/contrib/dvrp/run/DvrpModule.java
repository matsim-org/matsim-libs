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

import java.util.*;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.run.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.*;
import com.google.inject.name.Named;

public final class DvrpModule extends AbstractModule {
	public static final String DVRP_ROUTING = "dvrp_routing";// TODO ==> dvrp_optimizer???

	@Inject
	private DvrpConfigGroup dvrpCfg;

	private final Module module;
	private final List<Class<? extends MobsimListener>> listeners;

	@SuppressWarnings("unchecked")
	public DvrpModule(final Class<? extends VrpOptimizer> vrpOptimizerClass,
			final Class<? extends PassengerRequestCreator> passengerRequestCreatorClass,
			final Class<? extends DynActionCreator> dynActionCreatorClass) {

		module = new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(VrpOptimizer.class).to(vrpOptimizerClass).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(passengerRequestCreatorClass).asEagerSingleton();
				bind(DynActionCreator.class).to(dynActionCreatorClass).asEagerSingleton();
			}
		};

		listeners = new ArrayList<>();
		if (MobsimListener.class.isAssignableFrom(vrpOptimizerClass)) {
			listeners.add((Class<? extends MobsimListener>)vrpOptimizerClass);
		}
	}

	@SafeVarargs
	public DvrpModule(Module module, Class<? extends MobsimListener>... listeners) {
		this.module = module;
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
			Collection<Module> result = new ArrayList<>();
			result.add(module);
			return result;
		}

		@Override
		public Collection<Class<? extends MobsimListener>> listeners() {
			Collection<Class<? extends MobsimListener>> result = new ArrayList<>();
			for (Class<? extends MobsimListener> l : listeners) {
				result.add(l);
			}
			return result;
		}
	}
}
