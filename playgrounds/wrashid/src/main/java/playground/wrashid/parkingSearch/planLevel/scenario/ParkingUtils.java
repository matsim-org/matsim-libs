/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel.scenario;

import org.matsim.contrib.parking.lib.EventHandlerAtStartupAdder;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import playground.wrashid.parkingSearch.planLevel.init.InitializeParkings;
import playground.wrashid.parkingSearch.planLevel.occupancy.FinishParkingOccupancyMaintainer;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingBookKeeper;

public class ParkingUtils {
	private ParkingUtils(){} // do not instantiate

	public static class ParkingConfigGroup extends ReflectiveConfigGroup {

		// Put your parameters here!

		public ParkingConfigGroup() {
			super("parking", true);
		}
	}

	public static ParkingBookKeeper initializeParking(MatsimServices controler) {
		ConfigUtils.addOrGetModule(controler.getConfig(), "parking", ParkingConfigGroup.class);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		// add controler for initialization
		controler.addControlerListener(new InitializeParkings());

		// add handlers (e.g. parking book keeping)
		EventHandlerAtStartupAdder eventHandlerAdder = new EventHandlerAtStartupAdder();
		ParkingBookKeeper parkingBookKeeper = new ParkingBookKeeper(controler);
		eventHandlerAdder.addEventHandler(parkingBookKeeper);
		controler.addControlerListener(eventHandlerAdder);

		controler.addControlerListener(new FinishParkingOccupancyMaintainer());

		return parkingBookKeeper ;
	}

}
