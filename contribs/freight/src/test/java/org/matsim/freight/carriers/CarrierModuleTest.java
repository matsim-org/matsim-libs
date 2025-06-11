/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.controller.CarrierModule;
import org.matsim.freight.carriers.controller.CarrierScoringFunctionFactory;
import org.matsim.freight.carriers.controller.CarrierStrategyManager;
import org.matsim.freight.carriers.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.freight.carriers.mobsim.StrategyManagerFactoryForTests;
import org.matsim.testcases.MatsimTestUtils;

public class CarrierModuleTest {

    Controler controler;

    FreightCarriersConfigGroup freightCarriersConfigGroup;

    @RegisterExtension
	public MatsimTestUtils testUtils = new MatsimTestUtils();

    @BeforeEach
    public void setUp(){
        Config config = ConfigUtils.createConfig() ;
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
        ScoringConfigGroup.ActivityParams workParams = new ScoringConfigGroup.ActivityParams("w");
        workParams.setTypicalDuration(60 * 60 * 8);
        config.scoring().addActivityParams(workParams);
        ScoringConfigGroup.ActivityParams homeParams = new ScoringConfigGroup.ActivityParams("h");
        homeParams.setTypicalDuration(16 * 60 * 60);
        config.scoring().addActivityParams(homeParams);
        config.global().setCoordinateSystem("EPSG:32632");
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory(testUtils.getOutputDirectory());
        config.network().setInputFile( testUtils.getClassInputDirectory() + "network.xml" );
        config.plans().setInputFile( testUtils.getClassInputDirectory() + "plans100.xml" );
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setWritePlansInterval(1);
        config.controller().setCreateGraphsInterval(0);
        freightCarriersConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class ) ;
        freightCarriersConfigGroup.setCarriersFile( testUtils.getClassInputDirectory() + "carrierPlansEquils.xml");
        freightCarriersConfigGroup.setCarriersVehicleTypesFile( testUtils.getPackageInputDirectory() + "vehicleTypes_v2.xml");

        Scenario scenario = ScenarioUtils.loadScenario( config );

	    CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

	    controler = new Controler(scenario);
    }


	//using this constructor does not work at the moment, as the module would need to derive the carriers out of the scenario.
	// to me, it is currently not clear how to do that, tschlenther oct 10 '19
	@Test
	void test_ConstructorWOParameters(){
		// note setUp method!
        controler.addOverridingModule(new CarrierModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
                bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
            }
        });
        controler.run();
    }

	@Test
	void test_ConstructorWithOneParameter(){
	    // note setUp method!
        controler.addOverridingModule(new CarrierModule());
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
                bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
            }
        });
        controler.run();
    }

//    @Test
//    public void test_ConstructorWithThreeParameters(){
//	    // note setUp method!
//        controler.addOverridingModule(new CarrierModule(new StrategyManagerFactoryForTests(),
//		    new DistanceScoringFunctionFactoryForTests()));
//        controler.run();
//    }
    // this syntax is no longer allowed.  kai, jul'22

}
