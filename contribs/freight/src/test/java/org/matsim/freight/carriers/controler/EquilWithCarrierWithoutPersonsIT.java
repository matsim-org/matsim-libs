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

package org.matsim.freight.carriers.controler;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.freight.carriers.Tour;
import org.matsim.freight.carriers.mobsim.DistanceScoringFunctionFactoryForTests;
import org.matsim.freight.carriers.mobsim.StrategyManagerFactoryForTests;
import org.matsim.freight.carriers.mobsim.TimeScoringFunctionFactoryForTests;
import org.matsim.testcases.MatsimTestUtils;

public class EquilWithCarrierWithoutPersonsIT {

	private Controler controler;

	@RegisterExtension private MatsimTestUtils testUtils = new MatsimTestUtils();

	public void setUp() {
		Config config = EquilWithCarrierWithPersonsIT.commonConfig( testUtils );
		Scenario scenario = EquilWithCarrierWithPersonsIT.commonScenario( config, testUtils );
		controler = new Controler(scenario);
	}

	@Test
	void testMobsimWithCarrierRunsWithoutException() {
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

	@Test
	void testWithoutCarrierRoutes() {
		assertThrows(IllegalStateException.class, () -> {
			Config config = EquilWithCarrierWithPersonsIT.commonConfig(testUtils);
			Scenario scenario = EquilWithCarrierWithPersonsIT.commonScenario(config, testUtils);

			// set the routes to null:
			for (Carrier carrier : CarriersUtils.getCarriers(scenario).getCarriers().values()) {
				for (ScheduledTour tour : carrier.getSelectedPlan().getScheduledTours()) {
					for (Tour.TourElement tourElement : tour.getTour().getTourElements()) {
						if (tourElement instanceof Tour.Leg) {
							((Tour.Leg) tourElement).setRoute(null);
						}
					}
				}
			}

			controler = new Controler(scenario);
			controler.addOverridingModule(new CarrierModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(CarrierStrategyManager.class).toProvider(StrategyManagerFactoryForTests.class).asEagerSingleton();
					bind(CarrierScoringFunctionFactory.class).to(DistanceScoringFunctionFactoryForTests.class).asEagerSingleton();
				}
			});

			// this fails in CarrierAgent#createDriverPlans(...).  Could be made pass there, but then does not seem to drive on network.  Would
			// need carrier equivalent to PersonPrepareForSim.  Could then adapt this test accordingly. kai, jul'22
			controler.run();
		});
	}

	@Test
	void testScoringInMeters(){
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

		Carrier carrier1 = CarriersUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assertions.assertEquals(-170000.0, carrier1.getSelectedPlan().getScore(), 0.0 );

		Carrier carrier2 = CarriersUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assertions.assertEquals(-85000.0, carrier2.getSelectedPlan().getScore(), 0.0 );
	}

	@Test
	void testScoringInSecondsWoTimeWindowEnforcement(){
		setUp();
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule( controler.getConfig(), FreightCarriersConfigGroup.class );
		if ( false ){
			freightCarriersConfigGroup.setTimeWindowHandling( FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightCarriersConfigGroup.setTimeWindowHandling( FreightCarriersConfigGroup.TimeWindowHandling.ignore );
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

		Carrier carrier1 = CarriersUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assertions.assertEquals(-240.0, carrier1.getSelectedPlan().getScore(), 2.0);

		Carrier carrier2 = CarriersUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assertions.assertEquals(0.0, carrier2.getSelectedPlan().getScore(), 0.0 );

	}

	@Test
	void testScoringInSecondsWTimeWindowEnforcement(){
		setUp();
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule( controler.getConfig(), FreightCarriersConfigGroup.class );
		if ( true ){
			freightCarriersConfigGroup.setTimeWindowHandling( FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightCarriersConfigGroup.setTimeWindowHandling( FreightCarriersConfigGroup.TimeWindowHandling.ignore );
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

		Carrier carrier1 = CarriersUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assertions.assertEquals(-4873.0, carrier1.getSelectedPlan().getScore(), 2.0);

		Carrier carrier2 = CarriersUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier2", Carrier.class));
		Assertions.assertEquals(0.0, carrier2.getSelectedPlan().getScore(), 0.0 );

	}

	@Test
	void testScoringInSecondsWithWithinDayRescheduling(){
		setUp();
		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule( controler.getConfig(), FreightCarriersConfigGroup.class );
		if ( true ){
			freightCarriersConfigGroup.setTimeWindowHandling( FreightCarriersConfigGroup.TimeWindowHandling.enforceBeginnings );
		} else{
			freightCarriersConfigGroup.setTimeWindowHandling( FreightCarriersConfigGroup.TimeWindowHandling.ignore );
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

		Carrier carrier1 = CarriersUtils.getCarriers(controler.getScenario()).getCarriers().get(Id.create("carrier1", Carrier.class));
		Assertions.assertEquals(-4871.0, carrier1.getSelectedPlan().getScore(), 2.0);
	}

	@Test
	void testEventFilessAreEqual(){
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

		String expected = testUtils.getClassInputDirectory() + "/output_events.xml.gz" ;
		String actual = testUtils.getOutputDirectory() + "/output_events.xml.gz" ;
		MatsimTestUtils.assertEqualEventsFiles(expected, actual);

	}


}
