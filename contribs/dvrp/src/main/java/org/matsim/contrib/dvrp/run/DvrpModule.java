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

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dynagent.run.DynActivityEngineModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.common.collect.ImmutableList;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public final class DvrpModule extends AbstractModule {
	private final List<String> modes;

	public DvrpModule(String... modes) {
		this.modes = ImmutableList.copyOf(modes);
	}

	@Provides
	@Singleton
	public QSimComponentsConfig provideQSimComponentsConfig(Config config) {
		QSimComponentsConfig components = new QSimComponentsConfig();
		new StandardQSimComponentConfigurator(config).configure(components);
		DynActivityEngineModule.configureComponents(components);
		modes.forEach(m -> components.addComponent(DvrpModes.mode(m)));
		return components;
	}

	@Override
	public void install() {
		// Visualisation of schedules for DVRP DynAgents
		bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

		// VrpTravelTimeEstimator
		install(new DvrpTravelTimeModule());

		bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))
				.toProvider(DvrpRoutingNetworkProvider.class)
				.asEagerSingleton();

		installQSimModule(new DynActivityEngineModule());
	}
}
