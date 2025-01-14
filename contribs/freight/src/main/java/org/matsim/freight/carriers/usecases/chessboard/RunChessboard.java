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

package org.matsim.freight.carriers.usecases.chessboard;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import java.util.Map;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
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
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.*;
import org.matsim.freight.carriers.usecases.analysis.CarrierScoreStats;
import org.matsim.freight.carriers.usecases.analysis.LegHistogram;

public final class RunChessboard {

	public static void main(String[] args){
		Config config;
		if ( args ==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ) );
			config.controller().setLastIteration( 1 );
			config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule( config, FreightCarriersConfigGroup.class );
		freightCarriersConfigGroup.setCarriersFile("carrierPlans.xml");
		freightCarriersConfigGroup.setCarriersVehicleTypesFile("vehicleTypes.xml");

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		CarriersUtils.loadCarriersAccordingToFreightConfig( scenario );

		Carriers carriers = CarriersUtils.addOrGetCarriers( scenario );
		CarrierVehicleTypes types = CarriersUtils.getCarrierVehicleTypes( scenario );

		Controller controller = ControllerUtils.createController( scenario );

		controller.addOverridingModule(new CarrierModule() );

		controller.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind( CarrierStrategyManager.class ).toProvider( new MyCarrierPlanStrategyManagerProvider( types ) );
				bind( CarrierScoringFunctionFactory.class).toInstance( new MyCarrierScoringFunctionFactory() );

				final LegHistogram freightOnly = new LegHistogram(900).setInclPop( false );
				addEventHandlerBinding().toInstance(freightOnly);

				final LegHistogram withoutFreight = new LegHistogram(900);
				addEventHandlerBinding().toInstance(withoutFreight);

				addControlerListenerBinding().toInstance( new CarrierScoreStats(carriers, config.controller().getOutputDirectory() +"/carrier_scores", true) );
				addControlerListenerBinding().toInstance( new IterationEndsListener() {

					@Inject private OutputDirectoryHierarchy controlerIO;

					@Override public void notifyIterationEnds(IterationEndsEvent event) {
						String dir = controlerIO.getIterationPath(event.getIteration());

						//write plans
						CarriersUtils.writeCarriers(carriers, dir, "carrierPlans.xml", String.valueOf(event.getIteration()));

						//write stats
						freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
						freightOnly.reset(event.getIteration());

						withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
						withoutFreight.reset(event.getIteration());
					}
				});
			}
		});

		controller.run();

	}

	private static class MyCarrierScoringFunctionFactory implements CarrierScoringFunctionFactory {
		@Inject private Network network;
		@Override public ScoringFunction createScoringFunction(Carrier carrier) {
			SumScoringFunction sf = new SumScoringFunction();
			sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversLegScoring(carrier, network) );
			sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleVehicleEmploymentScoring(carrier) );
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
			final CarrierStrategyManager strategyManager = CarrierControllerUtils.createDefaultCarrierStrategyManager();
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
