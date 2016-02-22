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

package playground.wrashid.parkingSearch.planLevel;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.wrashid.parkingChoice.ParkingConfigModule;
import playground.wrashid.parkingSearch.planLevel.scenario.ParkingUtils;

public class ParkingIntegrationTestSmall {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	// just to test, that the system runs without errors.
	@Test
	public void testScenario(){
		Config config = ConfigUtils.loadConfig( utils.getPackageInputDirectory() + "chessConfig2.xml", new ParkingConfigModule()) ;
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		Controler controler = new Controler(scenario);

		ParkingUtils.initializeParking(controler);

		controler.run();
	}





}
