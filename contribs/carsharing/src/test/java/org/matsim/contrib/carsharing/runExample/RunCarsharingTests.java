/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.carsharing.runExample;

import static org.junit.Assert.fail;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.carsharing.config.FreeFloatingConfigGroup;
import org.matsim.contrib.carsharing.config.OneWayCarsharingConfigGroup;
import org.matsim.contrib.carsharing.config.TwoWayCarsharingConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RunCarsharingTests {
	private final static Logger log = Logger.getLogger( RunCarsharingTests.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	@Ignore
	public final void test() {
		log.info( "class input dir: " + utils.getClassInputDirectory() ) ;
		
		Config config = ConfigUtils.createConfig( new FreeFloatingConfigGroup(), new OneWayCarsharingConfigGroup(), new TwoWayCarsharingConfigGroup() ) ;
		
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		
		config.network().setInputFile( utils.getClassInputDirectory()+"/network.xml" );
		
		config.plans().setInputFile( utils.getClassInputDirectory()+"/10persons.xml");
		config.plans().setInputPersonAttributeFile( utils.getClassInputDirectory()+"/1000desiresAttributes.xml");
		
		config.facilities().setInputFile( utils.getClassInputDirectory()+"/facilities.xml" );
		
		FreeFloatingConfigGroup ffConfig = (FreeFloatingConfigGroup) config.getModule( FreeFloatingConfigGroup.GROUP_NAME ) ;
		ffConfig.setvehiclelocations( utils.getClassInputDirectory()+"/Stations.txt");
		
		OneWayCarsharingConfigGroup oneWayConfig = (OneWayCarsharingConfigGroup) config.getModule( OneWayCarsharingConfigGroup.GROUP_NAME ) ;
		oneWayConfig.setvehiclelocations( utils.getClassInputDirectory()+"/Stations.txt");

		TwoWayCarsharingConfigGroup twoWayConfig = (TwoWayCarsharingConfigGroup) config.getModule( TwoWayCarsharingConfigGroup.GROUP_NAME ) ;
		twoWayConfig.setvehiclelocations( utils.getClassInputDirectory()+"/Stations.txt");
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

		// ---
		
		fail("Not yet implemented"); // TODO
	}

}
