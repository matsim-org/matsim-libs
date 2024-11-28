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

/**
 *
 */
package org.matsim.contrib.parking.parkingsearch.sim;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingStatsWriter;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controller;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.TravelTime;

/**
 * @author jbischoff
 */

public class SetupParking {
	// TODO: create config group and make this all configurable?

	static public void installParkingModules(Controller controller) {
		// No need to route car routes in Routing module in advance, as they are
		// calculated on the fly
		if (!controller.getConfig().getModules().containsKey(DvrpConfigGroup.GROUP_NAME)) {
			controller.getConfig().addModule(new DvrpConfigGroup());
		}

		controller.addOverridingModule(new DvrpTravelTimeModule());
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TravelTime.class).annotatedWith(DvrpModes.mode(TransportMode.car))
									  .to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
				bind(TravelDisutilityFactory.class).annotatedWith(DvrpModes.mode(TransportMode.car))
												   .toInstance(TimeAsTravelDisutility::new);
				bind(Network.class).annotatedWith(DvrpModes.mode(TransportMode.car))
								   .to(Key.get(Network.class, Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)));
				install(new DvrpModeRoutingModule(TransportMode.car, new SpeedyALTFactory()));
				bind(Network.class).annotatedWith(Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING))
								   .to(Network.class)
								   .asEagerSingleton();
				bind(ParkingSearchManager.class).to(FacilityBasedParkingManager.class).asEagerSingleton();
				bind(ParkingStatsWriter.class);
				this.install(new ParkingSearchQSimModule());
				addControlerListenerBinding().to(ParkingSearchManager.class);
				bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
				bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
			}
		});

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				QSimComponentsConfig components = new QSimComponentsConfig();

				new StandardQSimComponentConfigurator(controller.getConfig()).configure(components);
				components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
				components.addNamedComponent(ParkingSearchPopulationModule.COMPONENT_NAME);

				bind(QSimComponentsConfig.class).toInstance(components);
			}
		});

	}

}
