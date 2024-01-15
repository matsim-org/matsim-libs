/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * LeastCostPathCalculatorModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.parking.parkingsearch.sim;

import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.LinkLengthBasedParkingManagerWithRandomInitialUtilisation;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.ZoneParkingManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

public class ParkingManagerModule extends AbstractModule {

    @Override
    public void install() {
		Config config = getConfig();

		ParkingSearchConfigGroup psConfigGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);

		if (psConfigGroup.getParkingSearchManagerType().equals(ParkingSearchConfigGroup.ParkingSearchManagerType.FacilityBasedParkingManager)){
			bind(ParkingSearchManager.class).to(FacilityBasedParkingManager.class).asEagerSingleton();
		} else if (psConfigGroup.getParkingSearchManagerType().equals(ParkingSearchConfigGroup.ParkingSearchManagerType.ZoneParkingManager)) {
			bind(ParkingSearchManager.class).to(ZoneParkingManager.class).asEagerSingleton();
		} else if (psConfigGroup.getParkingSearchManagerType().equals(ParkingSearchConfigGroup.ParkingSearchManagerType.LinkLengthBasedParkingManagerWithRandomInitialUtilisation)) {
			bind(ParkingSearchManager.class).to(LinkLengthBasedParkingManagerWithRandomInitialUtilisation.class).asEagerSingleton();
		}
    }

}
