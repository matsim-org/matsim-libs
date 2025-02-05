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

import java.net.URL;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.passenger.PassengerModule;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.contrib.zone.skims.FreeSpeedTravelTimeMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import jakarta.inject.Provider;

/**
 * This module initialises generic (i.e. not taxi or drt-specific) AND global (not mode-specific) dvrp objects.
 * <p>
 * Some of the initialised objects will become modal at some point in the future.
 *
 * @author michalm
 */
public final class DvrpModule extends AbstractModule {
	@Inject
	private DvrpConfigGroup dvrpConfigGroup;

	private final AbstractModule dvrpTravelTimeEstimationModule;

	public DvrpModule() {
		this(new DvrpTravelTimeModule());
	}

	public DvrpModule(AbstractModule dvrpTravelTimeEstimationModule) {
		this.dvrpTravelTimeEstimationModule = dvrpTravelTimeEstimationModule;
	}

	@Override
	public void install() {
		// Visualisation of schedules for DVRP DynAgents
		bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

		install(dvrpTravelTimeEstimationModule);

		//lazily initialised because:
		// 1. we may have only mode-filtered subnetworks
		// 2. optimisers may not use it
		bind(TravelTimeMatrix.class).toProvider(new Provider<>() {
			@Inject
			@Named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)
			private Network network;

			@Inject
			private QSimConfigGroup qSimConfigGroup;

			@Override
			public TravelTimeMatrix get() {
				var numberOfThreads = getConfig().global().getNumberOfThreads();
				var params = dvrpConfigGroup.getTravelTimeMatrixParams();
				DvrpTravelTimeMatrixParams matrixParams = dvrpConfigGroup.getTravelTimeMatrixParams();
				ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(getConfig().getContext(), network,
					matrixParams.getZoneSystemParams(), getConfig().global().getCoordinateSystem(), zone -> true);
				
				if (params.cachePath == null) {
					return FreeSpeedTravelTimeMatrix.createFreeSpeedMatrix(network, zoneSystem, params, numberOfThreads,
						qSimConfigGroup.getTimeStepSize());
				} else {
					URL cachePath = ConfigGroup.getInputFileURL(getConfig().getContext(), params.cachePath);
					return FreeSpeedTravelTimeMatrix.createFreeSpeedMatrixFromCache(network, zoneSystem, params, numberOfThreads,
						qSimConfigGroup.getTimeStepSize(), cachePath);
				}
			}
		}).in(Singleton.class);

		bind(Network.class).annotatedWith(Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING))
				.toProvider(DvrpGlobalRoutingNetworkProvider.class)
				.asEagerSingleton();

		installQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding(DynActivityEngine.COMPONENT_NAME).to(DynActivityEngine.class);
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				bind(DvrpVehicleLookup.class).toProvider(DvrpVehicleLookup.DvrpVehicleLookupProvider.class)
						.asEagerSingleton();
			}
		});

		install(new PassengerModule());
	}
}
