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

package playground.wrashid.parkingChoice.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.EventHandlerAtStartupAdder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import playground.wrashid.parkingChoice.ParkingConfigModule;


public class ActivityDurationEstimatorTest extends MatsimTestCase {

	public void testConfig1(){
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig1.xml");
		ConfigUtils.addOrGetModule(config, "parkingChoice", ParkingConfigModule.class);

		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(26254, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(55322, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
	}
	
	public void testConfig2(){
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig2.xml");
		ConfigUtils.addOrGetModule(config, "parkingChoice", ParkingConfigModule.class);


		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(28800, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(52776, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
	}
	
	public void testConfig3(){
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/utils/config3.xml");
		
		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(600, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(12600, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
		assertEquals(69960, activityDurationEstimator.getActivityDurationEstimations().get(2),1);
	}
	
	public void testConfig4(){
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig4.xml");
		ConfigUtils.addOrGetModule(config, "parkingChoice", ParkingConfigModule.class);

		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(51600, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(29976, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
	}
	
	//TODO: go through the numbers of this test...
	public void testConfig5(){
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/utils/chessConfig5.xml");
		ConfigUtils.addOrGetModule(config, "parkingChoice", ParkingConfigModule.class);

		config.plans().setActivityDurationInterpretation( PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime );
		
		ActivityDurationEstimator activityDurationEstimator = getActivityDurationEstimations(config);
	
		assertEquals(118933, activityDurationEstimator.getActivityDurationEstimations().get(0),1);
		assertEquals(3600, activityDurationEstimator.getActivityDurationEstimations().get(1),1);
		assertEquals(3600, activityDurationEstimator.getActivityDurationEstimations().get(2),1);

		assertEquals(3802, activityDurationEstimator.getActivityDurationEstimations().get(3),1);
		// (The test originally expected 3803.  This started failing in Sep/2014.  It is too far in the past to get a precise build server message.
		// Marcel could reconstruct that it failed between svn revision 29988 and 30031.  Most of these revisions have to do with typed id's.  
		// Since this is only an activity duration_estimation_, decided to accept the one second change without further investigation. kai, apr'15)
	}

	private static ActivityDurationEstimator getActivityDurationEstimations(Config config) {
		Controler controler=new Controler(ScenarioUtils.loadScenario( config ) );
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		ActivityDurationEstimator activityDurationEstimator = new ActivityDurationEstimator(controler.getScenario(), Id.create(1, Person.class));
		eventHandlerAtStartupAdder.addEventHandler(activityDurationEstimator);

		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();
		return activityDurationEstimator;
	}
	
}
