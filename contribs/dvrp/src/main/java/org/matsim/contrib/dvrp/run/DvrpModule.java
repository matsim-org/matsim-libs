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

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public final class DvrpModule extends AbstractModule {
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

	private final Function<Config, Provider<Collection<AbstractQSimPlugin>>> qSimPluginProviderCreator;

	@SafeVarargs
	public DvrpModule(Module module, Class<? extends MobsimListener>... listeners) {
		this(config -> new DvrpQSimPluginsProvider(config, cfg -> module).addListeners(Arrays.asList(listeners)));
	}

	@SafeVarargs
	public DvrpModule(Function<Config, Module> moduleCreator, Class<? extends MobsimListener>... listeners) {
		this(config -> new DvrpQSimPluginsProvider(config, moduleCreator).addListeners(Arrays.asList(listeners)));
	}

	public DvrpModule(Function<Config, Provider<Collection<AbstractQSimPlugin>>> qSimPluginProviderCreator) {
		this.qSimPluginProviderCreator = qSimPluginProviderCreator;
	}

	@Override
	public void install() {
		String mode = DvrpConfigGroup.get(getConfig()).getMode();
		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

		// Visualisation of schedules for DVRP DynAgents
		bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

		// VrpTravelTimeEstimator
		install(new DvrpTravelTimeModule());

		bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))
				.toProvider(DvrpRoutingNetworkProvider.class).asEagerSingleton();

		bind(new TypeLiteral<Collection<AbstractQSimPlugin>>() {})
				.toProvider(qSimPluginProviderCreator.apply(getConfig()));
	}
}
