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

package org.matsim.contrib.dvrp.benchmark;

import java.util.Collection;
import java.util.function.Function;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpQSimComponentsConfigurator;
import org.matsim.contrib.dvrp.run.DvrpQSimPluginsProvider;
import org.matsim.contrib.dvrp.run.DvrpQSimPluginsProvider.DvrpQSimPluginsProviderFactory;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class DvrpBenchmarkModule extends AbstractModule {
	private final DvrpQSimPluginsProviderFactory qSimPluginProviderFactory;

	public DvrpBenchmarkModule(Function<Config, AbstractQSimModule> moduleCreator,
			Collection<Class<? extends MobsimListener>> listeners) {
		this(config -> new DvrpQSimPluginsProvider(config, moduleCreator).addListeners(listeners));
	}

	public DvrpBenchmarkModule(DvrpQSimPluginsProviderFactory qSimPluginProviderFactory) {
		this.qSimPluginProviderFactory = qSimPluginProviderFactory;
	}

	@Provides
	@Singleton
	public QSimComponents provideQSimComponents(Config config) {
		QSimComponents components = new QSimComponents();
		new StandardQSimComponentsConfigurator(config).configure(components);
		new DvrpQSimComponentsConfigurator().configure(components);
		return components;
	}

	@Override
	public void install() {
		install(new DvrpBenchmarkTravelTimeModule());// fixed travel times

		bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))
				.toProvider(DvrpRoutingNetworkProvider.class).asEagerSingleton();

		bind(new TypeLiteral<Collection<AbstractQSimModule>>() {
		}).toProvider(qSimPluginProviderFactory.create(getConfig()));
	}
}
