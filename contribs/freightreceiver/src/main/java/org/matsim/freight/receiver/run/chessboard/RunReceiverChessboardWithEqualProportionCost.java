/**
 * ********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 * *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 * LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 * *
 * *********************************************************************** *
 * *
 * This program is free software; you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation; either version 2 of the License, or     *
 * (at your option) any later version.                                   *
 * See also COPYING, LICENSE and WARRANTY file                           *
 * *
 * ***********************************************************************
 */

package org.matsim.freight.receiver.run.chessboard;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.*;
import org.matsim.freight.carriers.usecases.analysis.CarrierScoreStats;
import org.matsim.freight.carriers.usecases.analysis.LegHistogram;
import org.matsim.freight.carriers.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.freight.carriers.usecases.chessboard.CarrierTravelDisutilities;
import org.matsim.freight.carriers.usecases.chessboard.RunChessboard;
import org.matsim.freight.receiver.ReceiverModule;
import org.matsim.freight.receiver.ReceiverReplanningType;
import org.matsim.freight.receiver.ReceiverUtils;

import java.io.File;
import java.util.Locale;
import java.util.Map;

public class RunReceiverChessboardWithEqualProportionCost {
	private final static Logger LOG = LogManager.getLogger(RunReceiverChessboardWithEqualProportionCost.class);
	private final static int FIRST_RUN = 1;
	private final static int LAST_RUN = 1;

	public static void main(String[] args) {
		if (args.length > 0) {
			throw new IllegalArgumentException("This class should be self-contained and run without any arguments.");
		}
		for (int run = FIRST_RUN; run <= LAST_RUN; run++) {
			run(run);
		}
	}

	static void run(int run) {
		LOG.info("Executing run {}...", run);

		/* Prepare the scenario. */
		String folder = String.format(Locale.US, "%s/run_%03d/", ReceiverChessboardParameters.OUTPUT_FOLDER, run);
		boolean created = new File(folder).mkdirs();
		if (!created && !new File(folder).exists()) {
			LOG.error("Could not create output folder. This may cause code to crash down the line.");
		}
		Scenario scenario = ReceiverChessboardScenario.createChessboardScenario(
			run * ReceiverChessboardParameters.SEED_BASE,
			ReceiverChessboardParameters.NUMBER_OF_RECEIVERS,
			folder,
			true);

		Controller controler = new Controler(scenario);

		/* We assume, since you are using the receiver module, you will
		 * (have to) use and load the carrier module. */
		CarrierModule carrierModule = new CarrierModule();
		controler.addOverridingModule(carrierModule);

		/* Set up the receiver module. */
		CarrierVehicleTypes types = CarriersUtils.getCarrierVehicleTypes(scenario);
		ReceiverModule receiverModule = new ReceiverModule(ReceiverUtils.createEqualProportionCostAllocation());
		receiverModule.setReplanningType(ReceiverChessboardParameters.RECEIVER_REPLANNING);
		receiverModule.setScoringFunctionFactory(new MyCarrierScoringFunctionFactory());
		receiverModule.setCarrierStrategyManagerProvider(new MyCarrierPlanStrategyManagerProvider(types));
		receiverModule.setCarrierScoreStats(new CarrierScoreStats(CarriersUtils.getCarriers(scenario), scenario.getConfig().controller().getOutputDirectory() + "/carrier_scores", true));

		controler.addOverridingModule(receiverModule);



		/* Carrier statistics. */
//		prepareFreightOutputDataAndStats(controler);

		controler.run();
	}

	/**
	 * This method ensures that the {@link Carriers}' scores are also updated in the output.
	 * TODO This is less elegant than the embedded score stats for the receiver.
	 * FIXME This also does not seem to work (Feb'25) as it only produces NAs.
	 */
	static void prepareFreightOutputDataAndStats(MatsimServices controler) {
		CarrierScoreStats scoreStats = new CarrierScoreStats(CarriersUtils.getCarriers(controler.getScenario()), controler.getScenario().getConfig().controller().getOutputDirectory() + "/carrier_scores", true);
		controler.addControlerListener(scoreStats);
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

	private static class MyCarrierPlanStrategyManagerProvider implements Provider<CarrierStrategyManager> {
		@Inject
		private Network network;
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
