/* *********************************************************************** *
 * project: org.matsim.*
 * EquilWithCarrierTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.controler;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.contrib.freight.mobsim.TimeScoringFunctionFactoryForTests;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestCase;

public class EquilWithCarrierWithoutPassTest extends MatsimTestCase {
	
	Controler controler;
	
	private String planFile;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		String NETWORK_FILENAME = getClassInputDirectory() + "network.xml";
		Config config = new Config();
		config.addCoreModules();
		
		ActivityParams workParams = new ActivityParams("w");
		workParams.setTypicalDuration(60 * 60 * 8);
		config.planCalcScore().addActivityParams(workParams);
		ActivityParams homeParams = new ActivityParams("h");
		homeParams.setTypicalDuration(16 * 60 * 60);
		config.planCalcScore().addActivityParams(homeParams);
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(2);
		config.network().setInputFile(NETWORK_FILENAME);

		controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(1);
        controler.getConfig().controler().setCreateGraphs(false);

        planFile = getClassInputDirectory() + "carrierPlansEquils.xml";
	}

	
	public void testMobsimWithCarrierRunsWithoutException() {
		try{
			CarrierModule carrierControler = new CarrierModule(planFile,new StrategyManagerFactoryForTests(controler), new DistanceScoringFunctionFactoryForTests(controler.getScenario().getNetwork()));
//			carrierControler.setEnableWithinDayActivityReScheduling(true);
			controler.addOverridingModule(carrierControler);
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
			controler.run();
//			assertTrue(true);
		}
		catch(Exception e){
			assertTrue(false);
		}
	}
	
	public void testScoringInMeters(){
		try{
            CarrierModule carrierControler = new CarrierModule(planFile,new StrategyManagerFactoryForTests(controler), new DistanceScoringFunctionFactoryForTests(controler.getScenario().getNetwork()));
//			carrierControler.setEnableWithinDayActivityReScheduling(true);
			controler.addOverridingModule(carrierControler);
			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
			controler.run();
			
			Carrier carrier1 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier1", Carrier.class));
			assertEquals(-170000.0,carrier1.getSelectedPlan().getScore());
			
			Carrier carrier2 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier2", Carrier.class));
			assertEquals(-85000.0,carrier2.getSelectedPlan().getScore());
			
		}
		catch(Exception e){
			assertTrue(false);
		}
	}
	
	public void testScoringInSeconds(){

		CarrierModule carrierControler = new CarrierModule(planFile,new StrategyManagerFactoryForTests(controler), new TimeScoringFunctionFactoryForTests(controler.getScenario().getNetwork()));

		controler.addOverridingModule(carrierControler);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();

		Carrier carrier1 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier1", Carrier.class));
		assertEquals(-240.0, carrier1.getSelectedPlan().getScore(),2.0);

		Carrier carrier2 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier2", Carrier.class));
		assertEquals(0.0,carrier2.getSelectedPlan().getScore());

	}
	
	public void testScoringInSecondsWithWithinDayRescheduling(){
        CarrierModule carrierControler = new CarrierModule(planFile,new StrategyManagerFactoryForTests(controler), new TimeScoringFunctionFactoryForTests(controler.getScenario().getNetwork()));
		carrierControler.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(carrierControler);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();

		Carrier carrier1 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier1", Carrier.class));
		assertEquals(-4871.0, carrier1.getSelectedPlan().getScore(),2.0);
	}

	
	
	
}
