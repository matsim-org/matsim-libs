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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.passenger.PassengerModule;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.contrib.dynagent.run.DynActivityEngine;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.name.Names;

/**
 * @author michalm
 */
public class DvrpBenchmarkModule extends AbstractModule {
	@Override
	public void install() {
		bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSourceQSimModule.DVRP_VEHICLE_TYPE))
				.toInstance(VehicleUtils.getDefaultVehicleType());

		install(new DvrpBenchmarkTravelTimeModule());// fixed travel times

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
