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

import org.matsim.core.controler.Controler;

import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.parkingSearch.planLevel.init.InitializeParkings;
import playground.wrashid.parkingSearch.planLevel.occupancy.FinishParkingOccupancyMaintainer;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingBookKeeper;

public class BaseControlerScenario {

	public ParkingBookKeeper parkingBookKeeper;

	public BaseControlerScenario(Controler controler) {
		controler.setOverwriteFiles(true);

		// add controler for initialization
		controler.addControlerListener(new InitializeParkings());

		// add handlers (e.g. parking book keeping)
		EventHandlerAtStartupAdder eventHandlerAdder = new EventHandlerAtStartupAdder();
		this.parkingBookKeeper = new ParkingBookKeeper(controler);
		eventHandlerAdder.addEventHandler(parkingBookKeeper);
		controler.addControlerListener(eventHandlerAdder);

		controler.addControlerListener(new FinishParkingOccupancyMaintainer());


	}

}
