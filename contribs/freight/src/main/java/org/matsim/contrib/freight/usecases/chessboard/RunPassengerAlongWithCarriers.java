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
import org.matsim.contrib.freight.carrier.*;
import org.matsim.contrib.freight.controler.*;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversActivityScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.DriversLegScoring;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl.VehicleEmploymentScoring;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.IterationEndsListener;
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

import javax.inject.Inject;
import java.net.URL;
import java.util.Map;

final class RunPassengerAlongWithCarriers {

	final static URL url = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");

	private Config config ;
	private Scenario scenario ;

	public static void main(String[] args) {
		new RunPassengerAlongWithCarriers().run();
	}

	public void run() {
		if ( scenario==null ) {
			prepareScenario() ;
		}

		Controler controler = new Controler(scenario);

		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readURL( IOUtils.extendUrl(url, "vehicleTypes.xml" ) );

		final Carriers carriers = new Carriers();
		new CarrierPlanXmlReader(carriers, types ).readURL( IOUtils.extendUrl(url, "carrierPlans.xml" ) );

		controler.addOverridingModule( new CarrierModule() );

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				this.bind( CarrierStrategyManager.class ).toProvider( new MyCarrierPlanStrategyManagerFactory(types) );
				this.bind( CarrierScoringFunctionFactory.class ).toInstance( createScoringFunctionFactory( scenario.getNetwork() ) );
			}
		} );


		prepareFreightOutputDataAndStats(scenario, controler.getEvents(), controler, carriers);

		controler.run();
	}


	public final Config prepareConfig() {
		config = ConfigUtils.loadConfig(IOUtils.extendUrl(url, "config.xml"));
		config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
		config.global().setRandomSeed(4177);
		config.controler().setOutputDirectory("./output/");
		return config;
	}

	public final Scenario prepareScenario() {
		if ( config==null ) {
			prepareConfig() ;
		}
		scenario = ScenarioUtils.loadScenario( config ) ;
		FreightUtils.addOrGetCarriers( scenario );
		return scenario ;
	}


	private static void prepareFreightOutputDataAndStats(Scenario scenario, EventsManager eventsManager, MatsimServices controler, final Carriers carriers) {
		final LegHistogram freightOnly = new LegHistogram(900);
		freightOnly.setPopulation(scenario.getPopulation());
		freightOnly.setInclPop(false);
		final LegHistogram withoutFreight = new LegHistogram(900);
		withoutFreight.setPopulation(scenario.getPopulation());

		CarrierScoreStats scores = new CarrierScoreStats(carriers, scenario.getConfig().controler().getOutputDirectory()+ "carrier_scores", true);

		eventsManager.addHandler(withoutFreight);
		eventsManager.addHandler(freightOnly);
		controler.addControlerListener(scores);
		controler.addControlerListener((IterationEndsListener) event -> {
			//write plans
			String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());
			new CarrierPlanWriter(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

			//write stats
			freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
			freightOnly.reset(event.getIteration());

			withoutFreight.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_withoutFreight.png");
			withoutFreight.reset(event.getIteration());
		});
	}


	private static CarrierScoringFunctionFactory createScoringFunctionFactory(final Network network) {
		return carrier -> {
			SumScoringFunction sf = new SumScoringFunction();
			DriversLegScoring driverLegScoring = new DriversLegScoring(carrier, network);
			VehicleEmploymentScoring vehicleEmploymentScoring = new VehicleEmploymentScoring(carrier);
			DriversActivityScoring actScoring = new DriversActivityScoring();
			sf.addScoringFunction(driverLegScoring);
			sf.addScoringFunction(vehicleEmploymentScoring);
			sf.addScoringFunction(actScoring);
			return sf;
		};
	}

	private static class MyCarrierPlanStrategyManagerFactory implements Provider<CarrierStrategyManager>{

		@Inject
		private Network network;

		@Inject
		private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		@Inject
		private Map<String, TravelTime> modeTravelTimes;

		private final CarrierVehicleTypes types;

		public MyCarrierPlanStrategyManagerFactory(CarrierVehicleTypes types) {
			this.types = types;
		}

		@Override
		public CarrierStrategyManager get() {
			TravelDisutility travelDisutility = TravelDisutilities.createBaseDisutility(types, modeTravelTimes.get(TransportMode.car));
			final LeastCostPathCalculator router = leastCostPathCalculatorFactory.createPathCalculator(network,
					travelDisutility, modeTravelTimes.get(TransportMode.car));

//            final GenericStrategyManagerImpl<CarrierPlan, Carrier> strategyManager = new GenericStrategyManagerImpl<>();
			final CarrierStrategyManager strategyManager = new CarrierStrategyManagerImpl();
			strategyManager.setMaxPlansPerAgent(5);

			strategyManager.addStrategy(new GenericPlanStrategyImpl<>(new BestPlanSelector<>()), null, 0.95);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<CarrierPlan, Carrier>());
				strategy.addStrategyModule(new TimeAllocationMutator());
				strategy.addStrategyModule(new ReRouteVehicles(router, network, modeTravelTimes.get(TransportMode.car), 1.));
				strategyManager.addStrategy(strategy, null, 0.5);
			}

//            strategyManager.addStrategy(new SelectBestPlanAndOptimizeItsVehicleRouteFactory(network, types, modeTravelTimes.get(TransportMode.car)).createStrategy(), null, 0.05);

			return (CarrierStrategyManager) strategyManager;
		}
	}

}
