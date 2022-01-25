/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.carrier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class CarrierModuleTest {

    Controler controler;

    FreightConfigGroup freightConfigGroup;

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Before
    public void setUp(){
        Config config = ConfigUtils.createConfig() ;
        PlanCalcScoreConfigGroup.ActivityParams workParams = new PlanCalcScoreConfigGroup.ActivityParams("w");
        workParams.setTypicalDuration(60 * 60 * 8);
        config.planCalcScore().addActivityParams(workParams);
        PlanCalcScoreConfigGroup.ActivityParams homeParams = new PlanCalcScoreConfigGroup.ActivityParams("h");
        homeParams.setTypicalDuration(16 * 60 * 60);
        config.planCalcScore().addActivityParams(homeParams);
        config.global().setCoordinateSystem("EPSG:32632");
        config.controler().setFirstIteration(0);
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(testUtils.getOutputDirectory());
        config.network().setInputFile( testUtils.getClassInputDirectory() + "network.xml" );
        config.plans().setInputFile( testUtils.getClassInputDirectory() + "plans100.xml" );
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setWritePlansInterval(1);
        config.controler().setCreateGraphs(false);
        freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class ) ;
        freightConfigGroup.setCarriersFile( testUtils.getClassInputDirectory() + "carrierPlansEquils.xml");
        freightConfigGroup.setCarriersVehicleTypesFile( testUtils.getClassInputDirectory() + "vehicleTypes.xml");

        Scenario scenario = ScenarioUtils.loadScenario( config );

	    FreightUtils.loadCarriersAccordingToFreightConfig( scenario );

	    controler = new Controler(scenario);
    }


	@Test
    //using this constructor does not work at the moment, as the module would need to derive the carriers out of the scenario.
    // to me, it is currently not clear how to do that, tschlenther oct 10 '19
    public void test_ConstructorWOParameters(){
		// note setUp method!
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
    public void test_ConstructorWithOneParameter(){
	    // note setUp method!
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
    public void test_ConstructorWithThreeParameters(){
	    // note setUp method!
        controler.addOverridingModule(new CarrierModule(new StrategyManagerFactoryForTests(),
		    new DistanceScoringFunctionFactoryForTests()));
        controler.run();
    }

}
