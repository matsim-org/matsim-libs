/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel.init;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.facilities.ActivityFacilitiesImpl;

import playground.wrashid.lib.GlobalRegistry;

public class InitializeParkings implements StartupListener {

	public void notifyStartup(StartupEvent event) {
		GlobalRegistry.controler=event.getControler();
        ParkingRoot.init((ActivityFacilitiesImpl) event.getControler().getScenario().getActivityFacilities(), (NetworkImpl) event.getControler().getScenario().getNetwork(), event.getControler());
		
		//ParkingRoot.getParkingOccupancyMaintainer().performInitializationsAfterLoadingControlerData();
		
		
		
	}

}
