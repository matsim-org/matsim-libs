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

import java.util.Collection;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpQSimPluginsProvider.DvrpQSimPluginsProviderFactory;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public final class DvrpModule extends AbstractModule {
	private final DvrpQSimPluginsProviderFactory qSimPluginProviderFactory;

	public DvrpModule(Function<Config, Module> moduleCreator, Collection<Class<? extends MobsimListener>> listeners) {
		this(config -> new DvrpQSimPluginsProvider(config, moduleCreator).addListeners(listeners));
	}

	public DvrpModule(DvrpQSimPluginsProviderFactory qSimPluginProviderFactory) {
		this.qSimPluginProviderFactory = qSimPluginProviderFactory;
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
				.toProvider(qSimPluginProviderFactory.create(getConfig()));
	}

	public static class MobsimTimerProvider implements Provider<MobsimTimer> {
		@Inject
		private QSim qsim;

		public MobsimTimer get() {
			return qsim.getSimTimer();
		}
	}

	public static void bindTravelDisutilityForOptimizer(Binder binder, String optimizerType) {
		binder.bind(TravelDisutility.class).annotatedWith(Names.named(optimizerType))
				.toProvider(new TravelDisutilityProvider(optimizerType)).asEagerSingleton();
	}

	public static class TravelDisutilityProvider implements Provider<TravelDisutility> {
		@Inject
		private Injector injector;

		@Inject
		@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
		private TravelTime travelTime;

		private final String optimizerName;

		public TravelDisutilityProvider(String optimizerType) {
			this.optimizerName = optimizerType;
		}

		@Override
		public TravelDisutility get() {
			return injector.getInstance(Key.get(TravelDisutilityFactory.class, Names.named(optimizerName)))
					.createTravelDisutility(travelTime);
		}
	}
}
