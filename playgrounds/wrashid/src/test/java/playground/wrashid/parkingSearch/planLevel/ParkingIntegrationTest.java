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

import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseControlerScenario;

public class ParkingIntegrationTest extends MatsimTestCase {
 
	// just to test, that the system runs without errors.
	public void testScenario(){   
		Controler controler;
		String configFilePath="test/input/playground/wrashid/parkingSearch/planLevel/chessConfig2.xml";
		controler = new Controler(configFilePath);
		
		new BaseControlerScenario(controler);
		
		controler.run();
	}
	
	
	
	

}
