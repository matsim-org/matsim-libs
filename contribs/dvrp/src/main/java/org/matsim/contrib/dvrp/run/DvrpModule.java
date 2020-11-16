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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.passenger.PassengerModule;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.contrib.zone.skims.DvrpGlobalTravelTimesMatrixProvider;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

/**
 * This module initialises generic (i.e. not taxi or drt-specific) AND global (not mode-specific) dvrp objects.
 * <p>
 * Some of the initialised objects will become modal at some point in the future. E.g. VehicleType or TravelTime
 * are likely to be provided separately per each mode in the future.
 *
 * @author michalm
 */
public final class DvrpModule extends AbstractModule {
	@Inject
	private DvrpConfigGroup dvrpConfigGroup;

	@Override
	public void install() {
		// Visualisation of schedules for DVRP DynAgents
		bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

		bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSourceQSimModule.DVRP_VEHICLE_TYPE))
				.toInstance(VehicleUtils.getDefaultVehicleType());

		install(new DvrpTravelTimeModule());

		//lazily initialised:
		// 1. we have only mode-filtered subnetworks
		// 2. optimisers may do not use it
		bind(DvrpTravelTimeMatrix.class).toProvider(new DvrpGlobalTravelTimesMatrixProvider(getConfig().global(),
				dvrpConfigGroup.getTravelTimeMatrixParams())).in(Singleton.class);

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
