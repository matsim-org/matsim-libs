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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentAnnotationsRegistry;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.router.StageActivityTypes;

import com.google.inject.name.Names;


/**
 * @author  jbischoff
 *
 */

public class SetupParking {
	// TODO: create config group and make this all configurable?

	static public void installParkingModules(Controler controler) {
		// No need to route car routes in Routing module in advance, as they are
		// calculated on the fly
		if (!controler.getConfig().getModules().containsKey(DvrpConfigGroup.GROUP_NAME)){
		controler.getConfig().addModule(new DvrpConfigGroup());
		}
		final DynRoutingModule routingModuleCar = new DynRoutingModule(TransportMode.car);
		StageActivityTypes stageActivityTypesCar = new StageActivityTypes() {
			@Override
			public boolean isStageActivity(String activityType) {

				return (activityType.equals(ParkingUtils.PARKACTIVITYTYPE));
			}
		};
		routingModuleCar.setStageActivityTypes(stageActivityTypesCar);
		controler.addOverridingModule(new DvrpTravelTimeModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addRoutingModuleBinding(TransportMode.car).toInstance(routingModuleCar);
				bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
				bind(ParkingSearchManager.class).to(FacilityBasedParkingManager.class).asEagerSingleton();
				bind(WalkLegFactory.class).asEagerSingleton();
				bind(PrepareForSim.class).to(ParkingSearchPrepareForSimImpl.class);
				this.install(new ParkingSearchQSimModule());
				addControlerListenerBinding().to(ParkingListener.class);
				bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
				bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
			}
		});
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				QSimComponentAnnotationsRegistry components = new QSimComponentAnnotationsRegistry();
				
				new StandardQSimComponentConfigurator(controler.getConfig()).configure(components);
				components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
				components.addNamedAnnotation(ParkingSearchPopulationModule.COMPONENT_NAME );
				
				bind( QSimComponentAnnotationsRegistry.class ).toInstance(components );
			}
		});

	}

}
