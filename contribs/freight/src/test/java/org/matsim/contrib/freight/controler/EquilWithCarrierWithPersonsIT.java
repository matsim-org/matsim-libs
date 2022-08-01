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
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class EquilWithCarrierWithPersonsIT {

	private Controler controler;

	@Rule public MatsimTestUtils testUtils = new MatsimTestUtils();

	static Config commonConfig( MatsimTestUtils testUtils ) {
		Config config = ConfigUtils.createConfig();

		config.planCalcScore().addActivityParams( new ActivityParams("w").setTypicalDuration(60 * 60 * 8 ) );
		config.planCalcScore().addActivityParams( new ActivityParams("h").setTypicalDuration(16 * 60 * 60 ) );
		config.global().setCoordinateSystem("EPSG:32632");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(2);

		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.network().setInputFile( testUtils.getClassInputDirectory() + "network.xml" );

		return config;
	}

	@Before
	public void setUp(){
		Config config = commonConfig( testUtils );

		config.plans().setInputFile( testUtils.getClassInputDirectory() + "plans100.xml" );
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName("BestScore" ).setWeight(1.0 ) );
		config.strategy().addStrategySettings( new StrategySettings().setStrategyName("ReRoute" ).setWeight(0.0 ).setDisableAfter(300 ) );

		Scenario scenario = commonScenario( config, testUtils );

		controler = new Controler(scenario);
	}

	static Scenario commonScenario( Config config, MatsimTestUtils testUtils ){
		Scenario scenario = ScenarioUtils.loadScenario( config );

		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader( carrierVehicleTypes ).readFile( testUtils.getPackageInputDirectory() + "vehicleTypes_v2.xml" );

		Carriers carriers = FreightUtils.addOrGetCarriers(scenario );
		new CarrierPlanXmlReader( carriers, carrierVehicleTypes ).readFile( testUtils.getClassInputDirectory() + "carrierPlansEquils.xml" );
		return scenario;
	}

	@Test
	public void testScoringInMeters(){
		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();

		Carrier carrier1 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-170000.0, carrier1.getSelectedPlan().getScore(), 0.0 );

		Carrier carrier2 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assert.assertEquals(-85000.0, carrier2.getSelectedPlan().getScore(), 0.0 );
	}

}
