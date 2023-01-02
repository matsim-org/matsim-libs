/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.codeexamples.extensions.freight;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.*;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.contrib.freight.usecases.chessboard.CarrierTravelDisutilities;
import org.matsim.contrib.freight.controler.FreightUtils;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * @see org.matsim.contrib.freight
 */
public class RunFreightWithIterationsExample{

	public static void main(String[] args) throws ExecutionException, InterruptedException{
		run( args, true );
	}

	public static void run( String[] args, boolean runWithOTFVis ) throws ExecutionException, InterruptedException{

		// ### config stuff: ###
		Config config;
		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ) );
			config.plans().setInputFile( null ); // remove passenger input
			config.controler().setOutputDirectory( "./output/freight" );
			config.controler().setLastIteration( 1 );

			FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class );
			freightConfigGroup.setCarriersFile( "singleCarrierFiveActivitiesWithoutRoutes.xml" );
			freightConfigGroup.setCarriersVehicleTypesFile( "vehicleTypes.xml" );
		} else {
			config = ConfigUtils.loadConfig( args, new FreightConfigGroup() );
		}

		// load scenario (this is not loading the freight material):
		Scenario scenario = ScenarioUtils.loadScenario( config );

		//load carriers according to freight config
		FreightUtils.loadCarriersAccordingToFreightConfig( scenario );

		// how to set the capacity of the "light" vehicle type to "1":
//		FreightUtils.getCarrierVehicleTypes( scenario ).getVehicleTypes().get( Id.create("light", VehicleType.class ) ).getCapacity().setOther( 1 );

		// output before jsprit run (not necessary)
		new CarrierPlanWriter(FreightUtils.getCarriers( scenario )).write( "output/jsprit_unplannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		// Solving the VRP (generate carrier's tour plans)
		FreightUtils.runJsprit( scenario );

		// Output after jsprit run (not necessary)
		new CarrierPlanWriter(FreightUtils.getCarriers( scenario )).write( "output/jsprit_plannedCarriers.xml" ) ;
		// (this will go into the standard "output" directory.  note that this may be removed if this is also used as the configured output dir.)

		// ## MATSim configuration:  ##
		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new CarrierModule() );
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( CarrierStrategyManager.class ).toProvider( new MyCarrierPlanStrategyManagerProvider( ) );
				bind( CarrierScoringFunctionFactory.class ).to( MyCarrierScoringFunctionFactory.class );

			}
		} );

		if ( runWithOTFVis ){
			controler.addOverridingModule( new OTFVisLiveModule() );
		}

		// ## Start of the MATSim-Run: ##
		controler.run();
	}

	private static class MyCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {
		@Inject private Network network;
		@Override public ScoringFunction createScoringFunction( Carrier carrier ) {
			SumScoringFunction sf = new SumScoringFunction();
			sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversLegScoring(carrier, network) );
			sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleVehicleEmploymentScoring(carrier) );
			sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversActivityScoring() );
			return sf;
		}
	}

	/**
	 * See {@link CarrierStrategyManager} for some explanation of how this currently works.  kai, jan'23
	 */
	private static class MyCarrierPlanStrategyManagerProvider implements Provider<CarrierStrategyManager>{
		@Inject private Network network;
		@Inject private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
		@Inject private Map<String, TravelTime> modeTravelTimes;
		@Inject private Scenario scenario;
		@Override
		public CarrierStrategyManager get() {
			final CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
			strategyManager.setMaxPlansPerAgent(5);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger.Factory<CarrierPlan,Carrier>().build() );
				strategyManager.addStrategy(strategy, null, 1.0);
			}
			{
				final TravelDisutility travelDisutility = CarrierTravelDisutilities.createBaseDisutility( FreightUtils.getCarrierVehicleTypes( scenario ), modeTravelTimes.get( TransportMode.car ) );
				final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network, travelDisutility, modeTravelTimes.get(TransportMode.car ) );

				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new KeepSelected<>());
				strategy.addStrategyModule(new CarrierTimeAllocationMutator.Factory().build() );
				strategy.addStrategyModule(new CarrierReRouteVehicles.Factory(router, network, modeTravelTimes.get(TransportMode.car ) ).build() );
				strategyManager.addStrategy(strategy, null, 0.5);
			}
			return strategyManager;
		}
	}

}
