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
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestCase;

public class EquilWithCarrierWithPassTest extends MatsimTestCase {
	
	Controler controler;

	private String planFile;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		String NETWORK_FILENAME = getClassInputDirectory() + "network.xml";
		String PLANS_FILENAME = getClassInputDirectory() + "plans100.xml";
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
		config.plans().setInputFile(PLANS_FILENAME);
		StrategySettings bestScore = new StrategySettings(Id.create("1", StrategySettings.class));
		bestScore.setStrategyName("BestScore");
		bestScore.setWeight(1.0);
		StrategySettings reRoute = new StrategySettings(Id.create("2", StrategySettings.class));
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.0);
		reRoute.setDisableAfter(300);
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(bestScore);
		config.strategy().addStrategySettings(reRoute);
//		
		controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(1);
        controler.getConfig().controler().setCreateGraphs(false);
        //		CarrierConfig carrierConfig = new CarrierConfig();
//		carrierConfig.addCoreModules();
		planFile = getClassInputDirectory() + "carrierPlansEquils.xml";
//		carrierConfig.plans().setInputFile(planFile);
//		carrierConfig.setWithinDayReScheduling(true);
		
	}

	
	public void testScoringInMeters(){
//		try{
        CarrierModule carrierControler = new CarrierModule(planFile,new StrategyManagerFactoryForTests(controler),new DistanceScoringFunctionFactoryForTests(controler.getScenario().getNetwork()));
		
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

//		}
//		catch(Exception e){
//			assertTrue(false);
//		}
	}

//	public void testScoringInSeconds_carrier1(){
////		try{
//		carrierControler = new CarrierController(planFile,new StrategyManagerFactoryForTests(),new TimeScoringFunctionFactoryForTests(controler.getNetwork()));
//		carrierControler.setEnableWithinDayActivityReScheduling(true);
//		controler.addControlerListener(carrierControler);
//		controler.setOverwriteFiles(true);
//		controler.run();	
//
//		Carrier carrier1 = carrierControler.getCarriers().get(Id.create("carrier1"));
//		assertEquals(-8040.0,carrier1.getSelectedPlan().getScore());
//
//		
////		}
////		catch(Exception e){
////			assertTrue(false);
////		}
//	}
//	
//	public void testScoringInSeconds_carrier2(){
////		try{
//		carrierControler = new CarrierController(planFile,new StrategyManagerFactoryForTests(),new TimeScoringFunctionFactoryForTests(controler.getNetwork()));
//		carrierControler.setEnableWithinDayActivityReScheduling(true);
//		controler.addControlerListener(carrierControler);
//		controler.setOverwriteFiles(true);
//		controler.run();	
//
//		Carrier carrier2 = carrierControler.getCarriers().get(Id.create("carrier2"));
//		assertEquals(-6572.0,carrier2.getSelectedPlan().getScore());
//
//
//	}
//	
//	public void testScoringInSeconds_carrier3(){
////		try{
//		carrierControler = new CarrierController(planFile,new StrategyManagerFactoryForTests(),new TimeScoringFunctionFactoryForTests(controler.getNetwork()));
//		carrierControler.setEnableWithinDayActivityReScheduling(true);
//		controler.addControlerListener(carrierControler);
//		controler.setOverwriteFiles(true);
//		controler.run();	
//
//		Carrier carrier3 = carrierControler.getCarriers().get(Id.create("carrier3"));
//		assertEquals(-7701.0,carrier3.getSelectedPlan().getScore());
//
////		}
////		catch(Exception e){
////			assertTrue(false);
////		}
//	}
}
