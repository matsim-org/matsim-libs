/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayParkingController.java
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

package playground.wrashid.parkingSearch.withinDay_v_STRC;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayControlerListener;

import playground.christoph.parking.core.ParkingCostCalculatorImpl;
import playground.christoph.parking.core.mobsim.InsertParkingActivities;
import playground.christoph.parking.core.mobsim.ParkingQSimFactory;
import playground.christoph.parking.core.utils.LegModeChecker;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplannerFactory;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;
import playground.wrashid.parkingSearch.withinDay_v_STRC.analysis.ParkingAnalysisHandlerChessBoard;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.identifier.ParkingSearchIdentifier_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.replanner.ParkingSearchReplannerFactoryWithStrategySwitching;
import playground.wrashid.parkingSearch.withinDay_v_STRC.scoring.ParkingScoreManager;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.FullParkingSearchStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.GarageParkingStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.StreetParkingStrategy;
import playground.wrashid.parkingSearch.withinDay_v_STRC.strategies.manager.ParkingStrategyManager;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;
import playground.wrashid.parkingSearch.withindayFW.controllers.kti.HUPCControllerKTIzh;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

public class WithinDayParkingController implements StartupListener, ReplanningListener {

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 6;

	/*
	 * How many nodes should be checked when adapting an existing route
	 */
	protected int nodesToCheck = 3;
	
	/*
	 * Capacity for parkings where no capacity limit is set.
	 */
	protected int parkingCapacity = 10;
	
	/*
	 * The distance to the agent's destination when an agent starts
	 * its parking search.
	 */
	protected double searchRadius = 1000;
	
	protected ParkingSearchIdentifier_v2 parkingSearchIdentifier;
	protected ParkingSearchReplannerFactory searchReplannerFactory;

	protected WithinDayControlerListener withinDayControlerListener;
	protected MultiModalControlerListener multiModalControlerListener;
	protected ParkingRouterFactory parkingRouterFactory;
	protected Scenario scenario;
	protected LegModeChecker legModeChecker;
	protected ParkingScoreManager parkingAgentsTracker;
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure_v2 parkingInfrastructure;
	
	public WithinDayParkingController(Controler controler, MultiModalControlerListener multiModalControlerListener) {
		
		this.scenario = controler.getScenario();		
		this.multiModalControlerListener = multiModalControlerListener;
		this.withinDayControlerListener = new WithinDayControlerListener();
	}

	protected void initIdentifiers(EventsManager eventsManager) {
 
		LinkedList<FullParkingSearchStrategy> linkedList = new LinkedList<FullParkingSearchStrategy>();
		this.parkingSearchIdentifier = new ParkingSearchIdentifier_v2((ParkingAgentsTracker_v2) parkingAgentsTracker, parkingInfrastructure, 
				linkedList, eventsManager); 
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.parkingSearchIdentifier);
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanners() {
						
		LinkedList<FullParkingSearchStrategy> allStrategies = getParkingStrategiesForScenario(parkingRouterFactory);
				
		ParkingStrategyManager parkingStrategyManager=new ParkingStrategyManager(allStrategies);
		parkingAgentsTracker.setParkingStrategyManager(parkingStrategyManager);
		
		this.searchReplannerFactory = new ParkingSearchReplannerFactoryWithStrategySwitching(this.withinDayControlerListener.getWithinDayEngine(), this.scenario, 
				parkingAgentsTracker, parkingInfrastructure, parkingRouterFactory);
		this.searchReplannerFactory.addIdentifier(this.parkingSearchIdentifier);		
		this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(this.searchReplannerFactory);
	}

	private LinkedList<FullParkingSearchStrategy> getParkingStrategiesForScenario(ParkingRouterFactory parkingRouterFactory) {
		LinkedList<FullParkingSearchStrategy> strategies=new LinkedList<FullParkingSearchStrategy>();
		strategies.add(new GarageParkingStrategy((ParkingInfrastructure_v2) parkingInfrastructure, (ScenarioImpl) this.scenario));
		strategies.add(new StreetParkingStrategy((ParkingInfrastructure_v2) parkingInfrastructure, (ScenarioImpl) this.scenario));
		//strategies.add(new OptimalParkingStrategy(parkingRouterFactory.createParkingRouter(), this.scenarioData, parkingAgentsTracker,  parkingInfrastructure));
		//strategies.add(new FakeStrategy());
		return strategies;
	}

	private HashMap<Id, String> getParkingTypesForScenario() {
		HashMap<Id, String> parkingTypes = new HashMap<Id, String>();
		
		Random random = MatsimRandom.getLocalInstance();
		
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {
			
			ActivityOption parkingOption;

			parkingOption = facility.getActivityOptions().get("parking");
			
			if (parkingOption != null) {
				if (random.nextBoolean()){
					parkingTypes.put(facility.getId(), "streetParking");
					parkingOption.setCapacity(3.0);
				} else {
					parkingTypes.put(facility.getId(), "garageParking");
					parkingOption.setCapacity(20.0);
				}
			}
		}
		
		return parkingTypes;
	}
	

	@Override
	public void notifyStartup(StartupEvent event) {
		
		// ensure that network is multi-modal
		if (!NetworkUtils.isMultimodal(event.getControler().getNetwork())) {
			MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) event.getControler().getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
			new MultiModalNetworkCreator(multiModalConfigGroup).run(event.getControler().getNetwork());
		}
		
		// connect facilities to network
		new WorldConnectLocations(this.scenario.getConfig()).
			connectFacilitiesWithLinks(((ScenarioImpl) scenario).getActivityFacilities(), (NetworkImpl) scenario.getNetwork());

		
		this.checkModeChains(event.getControler(), this.multiModalControlerListener.getMultiModalTravelTimes());
		
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = 
				new TripRouterFactoryBuilderWithDefaults().createDefaultLeastCostPathCalculatorFactory(event.getControler().getScenario());
		
		// This is a workaround since the controler does not return its TripRouterFactory
		TripRouterFactory tripRouterFactoryWrapper = new TripRouterFactoryWrapper(event.getControler().getScenario(), 
				event.getControler().getTripRouterFactory(), leastCostPathCalculatorFactory);
		
		// we can use the TripRouterFactory that has been initialized by the MultiModalControlerListener
		this.withinDayControlerListener.setWithinDayTripRouterFactory(tripRouterFactoryWrapper);
				
		/*
		 * notifyStartup has to be called after the WithinDayTripRouterFactory has been set.
		 * After this call, the setters cannot be called anymore.
		 */
		this.withinDayControlerListener.notifyStartup(event);

		/*
		 * Create a copy of the multiModalTravelTimes map and set the TravelTimeCollector for car mode.
		 * After the withinDayControlerListener has handled the startup event, its TravelTimeCollector
		 * has been initialized. Therefore, we can add it now to the multiModalTravelTimes map.
		 */
		Map<String, TravelTime> multiModalTravelTimes = new HashMap<String, TravelTime>(this.multiModalControlerListener.getMultiModalTravelTimes());
		multiModalTravelTimes.put(TransportMode.car, this.withinDayControlerListener.getTravelTimeCollector());
		
		ParkingCostCalculatorImpl parkingCostCalculator = new ParkingCostCalculatorImpl(this.initParkingTypes());
		//TODO: this the parking type is not initialized here (give types to different parkings)
		HashMap<Id, String> parkingTypes = getParkingTypesForScenario();
		
		parkingInfrastructure = new ParkingInfrastructure_v2(this.scenario, parkingCostCalculator, parkingTypes);
		
		ParkingPersonalBetas parkingPersonalBetas = new ParkingPersonalBetas((ScenarioImpl) this.scenario, HUPCControllerKTIzh.getHouseHoldIncomeCantonZH((ScenarioImpl) this.scenario));
		parkingAgentsTracker = new ParkingScoreManager(this.scenario, parkingInfrastructure, searchRadius, event.getControler(), parkingPersonalBetas);
		parkingAgentsTracker.setParkingAnalysisHandler(new ParkingAnalysisHandlerChessBoard(event.getControler(), parkingInfrastructure));
		
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		event.getControler().getEvents().addHandler(this.parkingAgentsTracker);
		event.getControler().addControlerListener(parkingAgentsTracker);
		
		this.parkingRouterFactory = new ParkingRouterFactory(this.scenario, multiModalTravelTimes, 
				event.getControler().getTravelDisutilityFactory(), tripRouterFactoryWrapper, nodesToCheck);
				
		MobsimFactory mobsimFactory = new ParkingQSimFactory(parkingInfrastructure, parkingRouterFactory, 
				this.withinDayControlerListener.getWithinDayEngine(), this.parkingAgentsTracker);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		this.initIdentifiers(event.getControler().getEvents());
		this.initReplanners();
	}
	
	private void checkModeChains(Controler controler, Map<String, TravelTime> multiModalTravelTimes) {
		
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = 
				new TripRouterFactoryBuilderWithDefaults().createDefaultLeastCostPathCalculatorFactory(controler.getScenario());
		
		TripRouterFactory defaultTripRouterFactory = new TripRouterFactoryBuilderWithDefaults().build(controler.getScenario());
		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes, 
				controler.getTravelDisutilityFactory(), defaultTripRouterFactory, leastCostPathCalculatorFactory);
		
		// ensure that all agents' plans have valid mode chains
		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = controler.getTravelDisutilityFactory().createTravelDisutility(travelTime, 
				controler.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);
		TripRouter tripRouter = multiModalTripRouterFactory.instantiateAndConfigureTripRouter(routingContext);
		PlanAlgorithm planAlgorithm = new PlanRouter(tripRouter);
		legModeChecker = new LegModeChecker(this.scenario, planAlgorithm);
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenario.getPopulation());
	}
	
	@Override
	public void notifyReplanning(ReplanningEvent event) {
		/*
		 * During the replanning the mode chain of the agents' selected plans
		 * might have been changed. Therefore, we have to ensure that the 
		 * chains are still valid.
		 */
		for (Person person : event.getControler().getPopulation().getPersons().values()) {
			legModeChecker.run(person.getSelectedPlan());			
		}
	}
	
	
	// TODO: change this!!!!
	private HashMap<String, HashSet<Id>> initParkingTypes() {
		
		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();

		HashSet<Id> streetParking = new HashSet<Id>();
		HashSet<Id> garageParking = new HashSet<Id>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);
		
		for (ActivityFacility facility : ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().values()) {

			// if the facility offers a parking activity
			ActivityOption activityOption = facility.getActivityOptions().get("parking");
			if (activityOption != null) {
				if (MatsimRandom.getRandom().nextBoolean()){
					streetParking.add(facility.getId());
				} else {
					garageParking.add(facility.getId());
				}
				
				// If no capacity is set, set one.
				if (activityOption.getCapacity() > 100000) {
					activityOption.setCapacity((double) this.parkingCapacity);
				}
			}
		}
		return parkingTypes;
	}
	
	private static class TripRouterFactoryWrapper implements TripRouterFactory {

		private final TripRouterFactoryInternal internalFactory;
		private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		private final PopulationFactory populationFactory;
		private final ModeRouteFactory modeRouteFactory;
		private final Network subNetwork;
		
		public TripRouterFactoryWrapper(Scenario scenario, TripRouterFactoryInternal internalFactory,
				LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
			this.internalFactory = internalFactory;
			this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
		
			this.populationFactory = scenario.getPopulation().getFactory();
			this.modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
			
			this.subNetwork = NetworkImpl.createNetwork();
			Set<String> restrictions = new HashSet<String>();
			restrictions.add(TransportMode.car);
			TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(scenario.getNetwork());
			networkFilter.filter(subNetwork, restrictions);
		}
		
		@Override
		public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
			TripRouter tripRouter = internalFactory.instantiateAndConfigureTripRouter();
			
	        LeastCostPathCalculator leastCostPathCalculator = leastCostPathCalculatorFactory.createPathCalculator(
	        		this.subNetwork, routingContext.getTravelDisutility(), routingContext.getTravelTime());

	        LegRouter networkLegRouter = new NetworkLegRouter(this.subNetwork, leastCostPathCalculator, this.modeRouteFactory);
			RoutingModule legRouterWrapper = new LegRouterWrapper(TransportMode.car, populationFactory, networkLegRouter); 
			tripRouter.setRoutingModule(TransportMode.car, legRouterWrapper);
	        
			return tripRouter;
		}
		
	}
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("using default config");
		}
		Config config = ConfigUtils.loadConfig(args[0], new MultiModalConfigGroup());
		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		
		/*
		 * Controler listeners are called in reverse order. Since the parkingControlerListener
		 * depends on the outcomes of the multiModalControlerListener, we add the later last.
		 */
		MultiModalControlerListener multiModalControlerListener = new MultiModalControlerListener();
		controler.addControlerListener(new WithinDayParkingController(controler, multiModalControlerListener));
		controler.addControlerListener(multiModalControlerListener);
				
		GeneralLib.controler=controler;
		
		controler.run();
		
		System.exit(0);
	}

}
