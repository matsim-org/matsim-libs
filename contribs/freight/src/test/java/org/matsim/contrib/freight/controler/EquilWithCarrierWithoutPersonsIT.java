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
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.mobsim.StrategyManagerFactoryForTests;
import org.matsim.contrib.freight.mobsim.TimeScoringFunctionFactoryForTests;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

public class EquilWithCarrierWithoutPersonsIT {

	private Controler controler;
	
	@Rule public MatsimTestUtils testUtils = new MatsimTestUtils();

	public void setUp() {
		Config config = EquilWithCarrierWithPersonsIT.commonConfig( testUtils );
		Scenario scenario = EquilWithCarrierWithPersonsIT.commonScenario( config, testUtils );
		controler = new Controler(scenario);
	}

	@Test
	public void testMobsimWithCarrierRunsWithoutException() {
		setUp();
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

	@Test(expected = IllegalStateException.class )
	public void testWithoutCarrierRoutes() {
		Config config = EquilWithCarrierWithPersonsIT.commonConfig( testUtils );
		Scenario scenario = EquilWithCarrierWithPersonsIT.commonScenario( config, testUtils );

		// set the routes to null:
		for( Carrier carrier : FreightUtils.getCarriers( scenario ).getCarriers().values() ){
			for( ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours() ){
				for( Tour.TourElement tourElement : tour.getTour().getTourElements() ){
					if ( tourElement instanceof Tour.Leg ) {
						((Tour.Leg) tourElement).setRoute( null );
					}
				}
			}
		}

		controler = new Controler(scenario);
		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});

		// this fails in CarrierAgent#createDriverPlans(...).  Could be made pass there, but then does not seem to drive on network.  Would
		// need carrier equivalent to PersonPrepareForSim.  Could then adapt this test accordingly. kai, jul'22
		controler.run();
	}

	@Test
	public void testScoringInMeters(){
		setUp();
		controler.addOverridingModule(new CarrierModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();

		Carrier carrier1 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-170000.0, carrier1.getSelectedPlan().getScore(), 0.0 );

		Carrier carrier2 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assert.assertEquals(-85000.0, carrier2.getSelectedPlan().getScore(), 0.0 );
	}

	@Test
	public void testScoringInSecondsWoTimeWindowEnforcement(){
		setUp();
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( controler.getConfig(), FreightConfigGroup.class );
		if ( false ){
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightConfigGroup.setTimeWindowHandling( FreightConfigGroup.TimeWindowHandling.ignore );
		}
		controler.addOverridingModule( new CarrierModule( ) );
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
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
		setUp();
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( controler.getConfig(), FreightConfigGroup.class );
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
				bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
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
		setUp();
		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( controler.getConfig(), FreightConfigGroup.class );
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
				bind( CarrierStrategyManager.class ).toProvider(StrategyManagerFactoryForTests.class ).asEagerSingleton();
				bind(CarrierScoringFunctionFactory.class).to(TimeScoringFunctionFactoryForTests.class).asEagerSingleton();
			}
		});
		controler.run();

		Carrier carrier1 = FreightUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assert.assertEquals(-4871.0, carrier1.getSelectedPlan().getScore(), 2.0);
	}

	
	
	
}
