/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.passenger;

import com.google.inject.TypeLiteral;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.ControlerDefaultsWithMultiModalModule;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.TransitTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.LinkSlopesReader;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.pt.router.TransitRouter;
import org.matsim.withinday.controller.WithinDayControlerListener;
import playground.christoph.evacuation.mobsim.LegModeChecker;

import javax.inject.Provider;
import java.util.Map;

public class PassengerRunner {

	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("");
		} else {
			Config config = ConfigUtils.loadConfig(args[0], new MultiModalConfigGroup());
			Scenario scenario = ScenarioUtils.loadScenario(config);
			
//			config.getQSimConfigGroup().setVehicleBehavior("teleport");
			
//			PassengerDepartureHandler.passengerMode = "ride_passenger";
			
			// if multi-modal simulation is enabled
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) config.getModule(MultiModalConfigGroup.GROUP_NAME);
			if (multiModalConfigGroup != null && multiModalConfigGroup.isMultiModalSimulationEnabled()) {
				/*
				 * If the network is not multi-modal but multi-modal simulation is enabled,
				 * convert it to multi-modal.
				 */
				if (!NetworkUtils.isMultimodal(scenario.getNetwork())) {
					new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
				}				
			}
			
			/*
			 * Using a LegModeChecker to ensure that all agents' plans have valid mode chains.
			 * With the new demand, this should not be necessary anymore!
			 */
			checkLegModes(scenario, createTripRouterInstance(scenario, createTripRouterFactory(scenario)));
			
			Controler controler = new Controler(scenario);
            controler.setModules(new AbstractModule() {
                @Override
                public void install() {
                    install(new ControlerDefaultsWithMultiModalModule());
                    Provider<Map<String, TravelTime>> multiModalTravelTimes = getProvider(new TypeLiteral<Map<String, TravelTime>>(){});
                    WithinDayControlerListener withinDayControlerListener = new WithinDayControlerListener();
                    PassengerControlerHandler passengerControlerHandler = new PassengerControlerHandler(withinDayControlerListener, multiModalTravelTimes);
					addControlerListenerBinding().toInstance(passengerControlerHandler);
					addControlerListenerBinding().toInstance(withinDayControlerListener);
				}
            });

			controler.getConfig().controler().setOverwriteFileSetting(
					true ?
							OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
							OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
			controler.run();
			
			System.exit(0);			
		}		
	}
	
	private static void checkLegModes(Scenario scenario, TripRouter tripRouter) {
		
		LegModeChecker legModeChecker = new LegModeChecker(tripRouter, scenario.getNetwork());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk, TransportMode.bike, TransportMode.pt});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(scenario.getPopulation());
		legModeChecker.printStatistics();	
	}
	
	private static TripRouter createTripRouterInstance(Scenario scenario, Provider<TripRouter> tripRouterFactory) {
		TravelDisutilityFactory travelDisutilityFactory = new TravelTimeAndDistanceBasedTravelDisutilityFactory();
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(travelTime, scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);
		return tripRouterFactory.get();
	}
	
	private static Provider<TripRouter> createTripRouterFactory(Scenario scenario) {
		
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);		
		Map<Id<Link>, Double> linkSlopes = new LinkSlopesReader().getLinkSlopes(multiModalConfigGroup, scenario.getNetwork());
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig(), linkSlopes);
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();
		
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = builder.createDefaultLeastCostPathCalculatorFactory(scenario);
		Provider<TransitRouter> transitRouterFactory = null;
		if (scenario.getConfig().transit().isUseTransit()) transitRouterFactory = builder.createDefaultTransitRouter(scenario);
		
		TravelDisutilityFactory travelDisutilityFactory = new TravelTimeAndDistanceBasedTravelDisutilityFactory();
		Provider<TripRouter> defaultDelegateFactory = new DefaultDelegateFactory(scenario, leastCostPathCalculatorFactory);
		Provider<TripRouter> multiModalTripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes,
				travelDisutilityFactory, defaultDelegateFactory, new FastDijkstraFactory());
		Provider<TripRouter> transitTripRouterFactory = new TransitTripRouterFactory(scenario, multiModalTripRouterFactory,
				transitRouterFactory);
		
		return transitTripRouterFactory;
	}
}
