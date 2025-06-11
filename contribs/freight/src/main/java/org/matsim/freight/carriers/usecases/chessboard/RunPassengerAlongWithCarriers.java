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
import java.net.URL;
import java.util.Map;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.controller.*;
import org.matsim.freight.carriers.usecases.analysis.CarrierScoreStats;
import org.matsim.freight.carriers.usecases.analysis.LegHistogram;

final class RunPassengerAlongWithCarriers {

	final static URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");

	public static void main(String[] args) {
		new RunPassengerAlongWithCarriers().run();
	}

	public void run() {
		Config config = prepareConfig();

		Scenario scenario = prepareScenario(config);

		Controller controller = ControllerUtils.createController(scenario);

		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readURL( IOUtils.extendUrl(url, "vehicleTypes.xml" ) );

		final Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, types ).readURL( IOUtils.extendUrl(url, "carrierPlans.xml" ) );

		controller.addOverridingModule( new CarrierModule() );

		controller.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.bind( CarrierStrategyManager.class ).toProvider( new MyCarrierPlanStrategyManagerFactory(types) );
				this.bind( CarrierScoringFunctionFactory.class ).toInstance(carrier -> {
					SumScoringFunction sf = new SumScoringFunction();
					sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversLegScoring( carrier, scenario.getNetwork() ) );
					sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleVehicleEmploymentScoring( carrier ) );
					sf.addScoringFunction( new CarrierScoringFunctionFactoryImpl.SimpleDriversActivityScoring() );
					return sf;
				} );
			}
		} );


		prepareFreightOutputDataAndStats(scenario, controller.getEvents(), controller, carriers);

		controller.run();
	}


	public Config prepareConfig() {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(url, "config.xml"));
		config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		config.controller().setLastIteration(5);
		config.global().setRandomSeed(4177);
		config.controller().setOutputDirectory("./output/");
		return config;
	}

	public Scenario prepareScenario(Config config) {
		Gbl.assertNotNull( config );
		Scenario scenario = ScenarioUtils.loadScenario(config);
		CarriersUtils.addOrGetCarriers(scenario);
		return scenario;
	}


	private static void prepareFreightOutputDataAndStats(Scenario scenario, EventsManager eventsManager, MatsimServices controller, final Carriers carriers) {
		final LegHistogram freightOnly = new LegHistogram(900);
		freightOnly.setPopulation(scenario.getPopulation());
		freightOnly.setInclPop(false);
		final LegHistogram withoutFreight = new LegHistogram(900);
		withoutFreight.setPopulation(scenario.getPopulation());

		CarrierScoreStats scores = new CarrierScoreStats(carriers, scenario.getConfig().controller().getOutputDirectory()+ "carrier_scores", true);

		eventsManager.addHandler(withoutFreight);
		eventsManager.addHandler(freightOnly);
		controller.addControlerListener(scores);
		controller.addControlerListener((IterationEndsListener) event -> {
			//write plans
			String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());
			CarriersUtils.writeCarriers(carriers, dir, "carrierPlans.xml", String.valueOf(event.getIteration()));

			//write stats
			freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
			freightOnly.reset(event.getIteration());

			withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
			withoutFreight.reset(event.getIteration());
		});
	}


	private static class MyCarrierPlanStrategyManagerFactory implements Provider<CarrierStrategyManager>{
		@Inject private Network network;
		@Inject private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
		@Inject private Map<String, TravelTime> modeTravelTimes;
		private final CarrierVehicleTypes types;
		public MyCarrierPlanStrategyManagerFactory(CarrierVehicleTypes types) {
			this.types = types;
		}

		@Override public CarrierStrategyManager get() {
			final TravelDisutility travelDisutility = CarrierTravelDisutilities.createBaseDisutility(types, modeTravelTimes.get(TransportMode.car ) );
			final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network, travelDisutility, modeTravelTimes.get(TransportMode.car));

			final CarrierStrategyManager carrierStrategyManager = CarrierControllerUtils.createDefaultCarrierStrategyManager();
			carrierStrategyManager.setMaxPlansPerAgent(5);

			carrierStrategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 0.95);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new KeepSelected<>());
				strategy.addStrategyModule( new CarrierTimeAllocationMutator.Factory().build() );
				strategy.addStrategyModule( new CarrierReRouteVehicles.Factory(router, network, modeTravelTimes.get(TransportMode.car ) ).build() );
				carrierStrategyManager.addStrategy(strategy, null, 0.5);
			}

//            carrierStrategyManager.addStrategy(new SelectBestPlanAndOptimizeItsVehicleRouteFactory(network, types, modeTravelTimes.get(TransportMode.car)).createStrategy(), null, 0.05);

			return carrierStrategyManager;
		}
	}

}
