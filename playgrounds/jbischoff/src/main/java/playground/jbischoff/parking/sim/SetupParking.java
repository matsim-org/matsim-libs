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
package playground.jbischoff.parking.sim;

import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import playground.jbischoff.parking.evaluation.ParkingListener;
import playground.jbischoff.parking.manager.FacilityBasedParkingManager;
import playground.jbischoff.parking.manager.LinkLengthBasedParkingManagerWithRandomInitialUtilisation;
import playground.jbischoff.parking.manager.ParkingSearchManager;
import playground.jbischoff.parking.manager.WalkLegFactory;
import playground.jbischoff.parking.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import playground.jbischoff.parking.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import playground.jbischoff.parking.routing.ParkingRouter;
import playground.jbischoff.parking.routing.WithinDayParkingRouter;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class SetupParking {
//TODO: create config group and make this all configurable
	static public void installParkingModules(Controler controler){
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
		controler.addOverridingModule(new AbstractModule() {
			
			
			@Override
			public void install() {
			bind(ParkingSearchManager.class).to(FacilityBasedParkingManager.class).asEagerSingleton();;
			bind(WalkLegFactory.class).asEagerSingleton();
			
			this.install(new ParkingSearchQSimModule());
			addControlerListenerBinding().to(ParkingListener.class);
			bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
			bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
			}
		});
		
	}
	
}
