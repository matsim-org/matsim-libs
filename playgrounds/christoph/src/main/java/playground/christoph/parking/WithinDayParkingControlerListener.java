/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayParkingControlerListener.java
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

package playground.christoph.parking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.multimodal.MultiModalControlerListener;
import org.matsim.contrib.multimodal.MultimodalQSimFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.network.NetworkImpl;
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
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayControlerListener;

import playground.christoph.parking.core.ParkingCostCalculatorImpl;
import playground.christoph.parking.core.mobsim.InsertParkingActivities;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.core.mobsim.ParkingQSimFactory;
import playground.christoph.parking.core.utils.LegModeChecker;
import playground.christoph.parking.withinday.identifier.ParkingSearchIdentifier;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplannerFactory;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;

public class WithinDayParkingControlerListener implements StartupListener, ReplanningListener, IterationEndsListener {

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 8;

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
	protected double searchRadius = 1.0;

	protected ParkingSearchIdentifier randomSearchIdentifier;
	protected ParkingSearchReplannerFactory randomSearchReplannerFactory;

	protected LegModeChecker legModeChecker;
	protected ParkingAgentsTracker parkingAgentsTracker;
	protected ParkingRouterFactory parkingRouterFactory;
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure parkingInfrastructure;
	
	private Scenario scenario;
	private WithinDayControlerListener withinDayControlerListener;
	private MultiModalControlerListener multiModalControlerListener;
	
	public WithinDayParkingControlerListener(Controler controler, MultiModalControlerListener multiModalControlerListener) {
		
		this.scenario = controler.getScenario();
		this.multiModalControlerListener = multiModalControlerListener;
		
		init(controler);
	}
	
	private void init(Controler controler) {
			
		/*
		 * Create a WithinDayControlerListener but do NOT register it as ControlerListener.
		 * It implements the StartupListener interface as this class also does. The
		 * StartupEvent is passed over to it when this class handles the event. 
		 */
		this.withinDayControlerListener = new WithinDayControlerListener();
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
	
	private void initIdentifiers() {

		this.randomSearchIdentifier = new ParkingSearchIdentifier(parkingAgentsTracker, parkingInfrastructure, 
				this.withinDayControlerListener.getMobsimDataProvider());
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	private void initReplanners() {
		
		this.randomSearchReplannerFactory = new ParkingSearchReplannerFactory(
				this.withinDayControlerListener.getWithinDayEngine(), this.scenario, 
				parkingAgentsTracker, parkingInfrastructure, parkingRouterFactory);
		this.randomSearchReplannerFactory.addIdentifier(this.randomSearchIdentifier);		
		this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(this.randomSearchReplannerFactory);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
//		this.checkModeChains(event.getControler(), this.multiModalControlerListener.getMultiModalTravelTimes());
		
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = 
				new TripRouterFactoryBuilderWithDefaults().createDefaultLeastCostPathCalculatorFactory(event.getControler().getScenario());
		
		// This is a workaround since the controler does not return its TripRouterFactory
		TripRouterFactory tripRouterFactoryWrapper = new TripRouterFactoryWrapper(event.getControler().getScenario(), 
				event.getControler().getTripRouterFactory(), leastCostPathCalculatorFactory);
		
		// we can use the TripRouterFactory that has been initialized by the MultiModalControlerListener
		this.withinDayControlerListener.setWithinDayTripRouterFactory(tripRouterFactoryWrapper);
		
		/*
		 * Since we use the multi-modal simulation, we have to ensure that the travel
		 * time collector only takes car trips into account.
		 */
		this.withinDayControlerListener.setModesAnalyzedByTravelTimeCollector(CollectionUtils.stringToSet(TransportMode.car));
		
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
		
		this.parkingRouterFactory = new ParkingRouterFactory(this.scenario, multiModalTravelTimes, 
				event.getControler().getTravelDisutilityFactory(), tripRouterFactoryWrapper, nodesToCheck);
		
		ParkingCostCalculatorImpl parkingCostCalculator = new ParkingCostCalculatorImpl(this.initParkingTypes());
		this.parkingInfrastructure = new ParkingInfrastructure(this.scenario, parkingCostCalculator);
		
		this.parkingAgentsTracker = new ParkingAgentsTracker(this.scenario, parkingInfrastructure, 
				this.withinDayControlerListener.getMobsimDataProvider() , searchRadius);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		event.getControler().getEvents().addHandler(this.parkingAgentsTracker);
		
		/*
		 * Replace the WithinDayQSimFactory which has been set by the withinDayControlerListener.
		 * Then, wrap a MultimodalQSimFactory around it to also use the multi-modal simulation.
		 */
		MobsimFactory mobsimFactory = new ParkingQSimFactory(parkingInfrastructure, parkingRouterFactory, 
				this.withinDayControlerListener.getWithinDayEngine(), this.parkingAgentsTracker);
		mobsimFactory = new MultimodalQSimFactory(this.multiModalControlerListener.getMultiModalTravelTimes(), mobsimFactory);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		this.initIdentifiers();
		this.initReplanners();
	}
		
	@Override
	public void notifyReplanning(ReplanningEvent event) {
//		/*
//		 * During the replanning the mode chain of the agents' selected plans
//		 * might have been changed. Therefore, we have to ensure that the 
//		 * chains are still valid.
//		 */
//		for (Person person : this.scenario.getPopulation().getPersons().values()) {
//			legModeChecker.run(person.getSelectedPlan());
//		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		this.parkingInfrastructure.printStatistics();
		this.parkingInfrastructure.resetParkingFacilityForNewIteration();
	}
	
	private HashMap<String, HashSet<Id>> initParkingTypes() {
		
		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();

		HashSet<Id> streetParking = new HashSet<Id>();
		HashSet<Id> garageParking = new HashSet<Id>();
		parkingTypes.put(ParkingTypes.STREETPARKING, streetParking);
		parkingTypes.put(ParkingTypes.GARAGEPARKING, garageParking);
		
		for (ActivityFacility facility : ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().values()) {

			// if the facility offers a parking activity
			ActivityOption activityOption = facility.getActivityOptions().get(ParkingTypes.PARKING);
			if (activityOption != null) {
				if (MatsimRandom.getRandom().nextBoolean()) {
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
}