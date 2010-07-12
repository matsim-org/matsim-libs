package playground.wrashid.parkingSearch.planLevel;

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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.GlobalRegistry;

public class MainTest extends MatsimTestCase {
 
	public void testScenario(){   
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig2.xml";
		controler = new Controler(configFilePath);
		
		controler.setOverwriteFiles(true);
		
		// add controler for initialization
		controler.addControlerListener(new InitializeParkings());
		
		// add handlers (e.g. parking book keeping)
		EventHandlerAtStartupAdder eventHandlerAdder=new EventHandlerAtStartupAdder();
		eventHandlerAdder.addEventHandler(new ParkingBookKeeper(controler));
		controler.addControlerListener(eventHandlerAdder);
		
		controler.run();
	}
	

}
