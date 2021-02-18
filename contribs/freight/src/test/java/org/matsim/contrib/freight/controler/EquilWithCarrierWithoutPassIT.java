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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReader;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.contrib.freight.mobsim.TimeScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.matsim.contrib.freight.controler.EquilWithCarrierWithPassIT.addDummyVehicleType;

public class EquilWithCarrierWithoutPassIT {
	
	Controler controler;
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	private FreightConfigGroup freightConfigGroup;

	@Before
	public void setUp() throws Exception{
		String NETWORK_FILENAME = testUtils.getClassInputDirectory() + "network.xml";
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
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.controler().setWritePlansInterval(1);
		config.controler().setCreateGraphs(false);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.network().setInputFile(NETWORK_FILENAME);

		freightConfigGroup = new FreightConfigGroup();
		config.addModule(freightConfigGroup);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Carriers carriers = FreightUtils.addOrGetCarriers(scenario );
		new CarrierPlanXmlReader(carriers).readFile(testUtils.getClassInputDirectory() + "carrierPlansEquils.xml" );
		addDummyVehicleType( carriers, "default") ;

		controler = new Controler(scenario);

	}

	@Test
	public void testMobsimWithCarrierRunsWithoutException() {

		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();
	}

	@Test
	public void testScoringInMeters(){
		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();

		Carrier carrier1 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-170000.0, carrier1.getSelectedPlan().getScore().doubleValue(), 0.0);

		Carrier carrier2 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assert.assertEquals(-85000.0, carrier2.getSelectedPlan().getScore().doubleValue(), 0.0);
	}

	@Test
	public void testScoringInSecondsWoTimeWindowEnforcement(){
		if ( false ){
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );
		}
		controler.addOverridingModule( new CarrierModule( ) );
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(TimeScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();

		Carrier carrier1 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-240.0, carrier1.getSelectedPlan().getScore(), 2.0);

		Carrier carrier2 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assert.assertEquals(0.0, carrier2.getSelectedPlan().getScore(), 0.0 );

	}

	@Test
	public void testScoringInSecondsWTimeWindowEnforcement(){
		if ( true ){
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );
		}
		final CarrierModule carrierModule = new CarrierModule( );
		controler.addOverridingModule( carrierModule );
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(TimeScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();

		Carrier carrier1 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-4873.0, carrier1.getSelectedPlan().getScore(), 2.0);

		Carrier carrier2 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assert.assertEquals(0.0, carrier2.getSelectedPlan().getScore(), 0.0 );

	}

	@Test
	public void testScoringInSecondsWithWithinDayRescheduling(){
		if ( true ){
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );
		}
		CarrierModule carrierControler = new CarrierModule();
		controler.addOverridingModule(carrierControler);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(TimeScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();

		Carrier carrier1 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-4871.0, carrier1.getSelectedPlan().getScore(), 2.0);
	}

	
	
	
}
