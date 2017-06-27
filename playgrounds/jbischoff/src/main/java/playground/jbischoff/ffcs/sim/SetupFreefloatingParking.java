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
package playground.jbischoff.ffcs.sim;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.contrib.zone.Zones;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.StageActivityTypes;

import playground.jbischoff.ffcs.FFCSConfigGroup;
import playground.jbischoff.ffcs.FFCSUtils;
import playground.jbischoff.ffcs.data.CarsharingData;
import playground.jbischoff.ffcs.data.CarsharingVehiclesReader;
import playground.jbischoff.ffcs.manager.FreefloatingCarsharingManager;
import playground.jbischoff.ffcs.manager.ShapeBasedFreeFloatingCarsharingManager;
import playground.jbischoff.ffcs.manager.SimpleFreeFloatingCarsharingManagerImpl;
import playground.jbischoff.ffcs.parking.FacilityBasedFreefloatingParkingManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class SetupFreefloatingParking {
	// TODO: create config group and make this all configurable
	static public void installFreefloatingParkingModules(Controler controler, FFCSConfigGroup ffcsconfig) {
		final CarsharingData data = new CarsharingData();
		final boolean useZones;
		if (ffcsconfig.getZonesShapeFile() != null) {
			data.setZones(Zones.readZones(ffcsconfig.getZonesXMLFileUrl(controler.getConfig().getContext()).getFile(),
					ffcsconfig.getZonesShapeFileUrl(controler.getConfig().getContext()).getFile()));
			useZones = true;
		} else {
			useZones = false;
		}
		final DynRoutingModule routingModule = new DynRoutingModule(FFCSUtils.FREEFLOATINGMODE);
		StageActivityTypes stageActivityTypes = new StageActivityTypes() {
			@Override
			public boolean isStageActivity(String activityType) {

				return (activityType.equals(FFCSUtils.FREEFLOATINGPARKACTIVITYTYPE));
			}
		};
		routingModule.setStageActivityTypes(stageActivityTypes);

		// No need to route car routes in Routing module
		final DynRoutingModule routingModuleCar = new DynRoutingModule(TransportMode.car);
		StageActivityTypes stageActivityTypesCar = new StageActivityTypes() {
			@Override
			public boolean isStageActivity(String activityType) {

				return (activityType.equals(ParkingUtils.PARKACTIVITYTYPE));
			}
		};
		routingModuleCar.setStageActivityTypes(stageActivityTypesCar);
		new CarsharingVehiclesReader(data).parse(ffcsconfig.getVehiclesFileUrl(controler.getConfig().getContext()));
		controler.addOverridingModule(new DvrpTravelTimeModule());
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addRoutingModuleBinding(FFCSUtils.FREEFLOATINGMODE).toInstance(routingModule);
				addRoutingModuleBinding(TransportMode.car).toInstance(routingModuleCar);
				bind(CarsharingData.class).toInstance(data);
				if (useZones) {
					bind(FreefloatingCarsharingManager.class).to(ShapeBasedFreeFloatingCarsharingManager.class)
							.asEagerSingleton();
				} else {
					bind(FreefloatingCarsharingManager.class).to(SimpleFreeFloatingCarsharingManagerImpl.class)
							.asEagerSingleton();
				}
				bind(ParkingSearchManager.class).to(FacilityBasedFreefloatingParkingManager.class).asEagerSingleton();
				bind(WalkLegFactory.class).asEagerSingleton();
				this.install(new FreefloatingParkingSearchQSimModule());
				addControlerListenerBinding().to(CarsharingParkingListener.class);
				bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
				bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
			}
		});

	}

}
