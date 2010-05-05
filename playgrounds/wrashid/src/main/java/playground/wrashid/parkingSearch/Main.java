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

package playground.wrashid.parkingSearch;

import org.matsim.core.controler.Controler;

import playground.wrashid.lib.EventHandlerAtStartupAdder;

public class Main {
	public static void main(String[] args) {
		Controler controler;
		String configFilePath="C:\\data\\workspace\\matsim\\test\\scenarios\\equil\\config_plans1.xml";
		controler = new Controler(configFilePath);

		controler.setOverwriteFiles(true);

		EventHandlerAtStartupAdder eventHandlerAdder=new EventHandlerAtStartupAdder();
		eventHandlerAdder.addEventHandler(new ReplanParkingSearchRoute(controler));
		controler.addControlerListener(eventHandlerAdder);

		controler.run();

	}
}
