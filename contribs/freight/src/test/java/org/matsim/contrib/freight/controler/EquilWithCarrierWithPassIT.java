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
import org.matsim.contrib.freight.Freight;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;

public class EquilWithCarrierWithPassIT {

	Controler controler;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Before
	public void setUp(){
		Config config = ConfigUtils.createConfig() ;
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
		config.network().setInputFile( testUtils.getClassInputDirectory() + "network.xml" );
		config.plans().setInputFile( testUtils.getClassInputDirectory() + "plans100.xml" );
		StrategySettings bestScore = new StrategySettings();
		bestScore.setStrategyName("BestScore");
		bestScore.setWeight(1.0);
		StrategySettings reRoute = new StrategySettings();
		reRoute.setStrategyName("ReRoute");
		reRoute.setWeight(0.0);
		reRoute.setDisableAfter(300);
		config.strategy().setMaxAgentPlanMemorySize(5);
		config.strategy().addStrategySettings(bestScore);
		config.strategy().addStrategySettings(reRoute);

		Scenario scenario = ScenarioUtils.loadScenario( config );
		{
			Carriers carriers = new Carriers();
			new CarrierPlanXmlReader( carriers ).readFile( testUtils.getClassInputDirectory() + "carrierPlansEquils.xml" );
			scenario.addScenarioElement( FreightUtils.CARRIERS, carriers );

			final String idString = "foo";
			addDummyVehicleType( carriers, idString );
		}
		controler = new Controler(scenario);
		controler.getConfig().controler().setWriteEventsInterval(1);
		controler.getConfig().controler().setCreateGraphs(false);
	}

	static void addDummyVehicleType( Carriers carriers, String idString ){
		CarrierVehicleTypes carrierVehicleTypes = new CarrierVehicleTypes() ;
		Id<org.matsim.vehicles.VehicleType> id = Id.create( idString, org.matsim.vehicles.VehicleType.class ) ;
		VehicleType builder = CarrierUtils.CarrierVehicleTypeBuilder.newInstance( id );
		VehicleType result = builder.build();
		carrierVehicleTypes.getVehicleTypes().put( result.getId(), result ) ;
		new CarrierVehicleTypeLoader( carriers ).loadVehicleTypes( carrierVehicleTypes );
	}

	@Test
	public void testScoringInMeters(){
		//		controler.addOverridingModule(new CarrierModule(carriers));
		Freight.configure( controler );
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CarrierPlanStrategyManagerFactory.class).to(StrategyManagerFactoryForTests.class).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.run();

		Carrier carrier1 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-170000.0, carrier1.getSelectedPlan().getScore(), 0.0 );

		Carrier carrier2 = controler.getInjector().getInstance(Carriers.class).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assert.assertEquals(-85000.0, carrier2.getSelectedPlan().getScore(), 0.0 );
	}

}
