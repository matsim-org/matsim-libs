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

package org.matsim.contrib.freight.usecases.chessboard;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.*;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.SimpleVehicleEmploymentScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
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
import java.util.Map;

public final class RunChessboard {

	public static void main(String[] args){
		Config config;
		if ( args ==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ) );
			config.controler().setLastIteration( 1 );
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule( config, FreightConfigGroup.class );
		freightConfigGroup.setCarriersFile("carrierPlans.xml");
		freightConfigGroup.setCarriersVehicleTypesFile("vehicleTypes.xml");

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		FreightUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = FreightUtils.addOrGetCarriers( scenario );
		CarrierVehicleTypes types = FreightUtils.getCarrierVehicleTypes( scenario );

		Controler controler = new Controler( scenario);

		controler.addOverridingModule(new CarrierModule() );

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind( CarrierStrategyManager.class ).toProvider( new MyCarrierPlanStrategyManagerProvider( types ) );
				bind( CarrierScoringFunctionFactory.class).toInstance( new MyCarrierScoringFunctionFactory() );

				final LegHistogram freightOnly = new LegHistogram(900).setInclPop( false );
				addEventHandlerBinding().toInstance(freightOnly);

				final LegHistogram withoutFreight = new LegHistogram(900);
				addEventHandlerBinding().toInstance(withoutFreight);

				addControlerListenerBinding().toInstance( new CarrierScoreStats(carriers, config.controler().getOutputDirectory() +"/carrier_scores", true) );
				addControlerListenerBinding().toInstance( new IterationEndsListener() {

					@Inject private OutputDirectoryHierarchy controlerIO;

					@Override public void notifyIterationEnds(IterationEndsEvent event) {
						String dir = controlerIO.getIterationPath(event.getIteration());

						//write plans
						new CarrierPlanWriter(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

						//write stats
						freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
						freightOnly.reset(event.getIteration());

						withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
						withoutFreight.reset(event.getIteration());
					}
				});
			}
		});

		controler.run();

	}

	private static class MyCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {
		@Inject private Network network;
		@Override public ScoringFunction createScoringFunction(Carrier carrier) {
			SumScoringFunction sf = new SumScoringFunction();
			sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversLegScoring(carrier, network) );
			sf.addScoringFunction( new SimpleVehicleEmploymentScoring(carrier) );
			sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversActivityScoring() );
			return sf;
		}
	}

	private static class MyCarrierPlanStrategyManagerProvider implements Provider<CarrierStrategyManager>{
		@Inject private Network network;
		@Inject private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
		@Inject private Map<String, TravelTime> modeTravelTimes;
		private final CarrierVehicleTypes types;
		MyCarrierPlanStrategyManagerProvider( CarrierVehicleTypes types ) {
			this.types = types;
		}

		@Override
		public CarrierStrategyManager get() {
			final CarrierStrategyManager strategyManager = FreightUtils.createDefaultCarrierStrategyManager();
			strategyManager.setMaxPlansPerAgent(5);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger.Factory<CarrierPlan,Carrier>().build() );
				strategyManager.addStrategy(strategy, null, 1.0);
			}
			{
				final TravelDisutility travelDisutility = CarrierTravelDisutilities.createBaseDisutility( types, modeTravelTimes.get( TransportMode.car ) );
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
