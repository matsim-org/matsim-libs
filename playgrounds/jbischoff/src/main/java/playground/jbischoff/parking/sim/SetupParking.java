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

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import playground.jbischoff.parking.manager.LinkLengthBasedParkingManagerWithRandomInitialUtilisation;
import playground.jbischoff.parking.manager.ParkingManager;
import playground.jbischoff.parking.manager.WalkLegFactory;
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

	static public void installParkingModules(Controler controler){
		controler.addOverridingModule(new AbstractModule() {
			
			
			@Override
			public void install() {
			bind(WalkLegFactory.class).asEagerSingleton();
			bind(ParkingManager.class).to(LinkLengthBasedParkingManagerWithRandomInitialUtilisation.class);
			this.install(new ParkingSearchQSimModule());
			bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
			}
		});
		
	}
	
}
