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

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.multimodal.MultimodalQSimFactory;
import org.matsim.contrib.multimodal.router.util.WalkTravelTimeFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.*;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.controller.ExperiencedPlansWriter;
import org.matsim.withinday.controller.WithinDayControlerListener;
import playground.christoph.parking.core.ParkingCostCalculatorImpl;
import playground.christoph.parking.core.interfaces.ParkingCostCalculator;
import playground.christoph.parking.core.mobsim.InitialParkingSelector;
import playground.christoph.parking.core.mobsim.InsertParkingActivities;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.core.mobsim.ParkingQSimFactory;
import playground.christoph.parking.core.utils.LegModeChecker;
import playground.christoph.parking.withinday.identifier.ParkingSearchIdentifier;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplannerFactory;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WithinDayParkingControlerListener implements StartupListener, ReplanningListener, IterationEndsListener {

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 4;

	/*
	 * How many nodes should be checked when adapting an existing route
	 */
	protected int nodesToCheck = 3;
	
	/*
	 * Capacity for parkings where no capacity limit is set.
	 */
//	protected int parkingCapacity = 10;
	
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
	protected ParkingCostCalculator parkingCostCalculator;
	
	private final Scenario scenario;

    @Inject(optional = true)
    Map<String, TravelTime> multiModalTravelTimes;

    private final Set<String> initialParkingTypes;
	private final Set<String> allParkingTypes;
	private final double capacityFactor;
	
	private final WithinDayControlerListener withinDayControlerListener;
	private ExperiencedPlansWriter experiencedPlansWriter;
	
	public WithinDayParkingControlerListener(Scenario scenario, Set<String> initialParkingTypes, Set<String> allParkingTypes, double capacityFactor) {
		
		this.scenario = scenario;
		this.initialParkingTypes = initialParkingTypes;
		this.allParkingTypes = allParkingTypes;
		this.capacityFactor = capacityFactor;
		
		/*
		 * Create a WithinDayControlerListener but do NOT register it as ControlerListener.
		 * It implements the StartupListener interface as this class also does. The
		 * StartupEvent is passed over to it when this class handles the event. 
		 */
		this.withinDayControlerListener = new WithinDayControlerListener();
	}
	
	private void initIdentifiers() {

		this.randomSearchIdentifier = new ParkingSearchIdentifier(this.parkingAgentsTracker, this.parkingInfrastructure, 
				this.parkingCostCalculator, this.withinDayControlerListener.getMobsimDataProvider());
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	private void initReplanners() {
		
		this.randomSearchReplannerFactory = new ParkingSearchReplannerFactory(
				this.withinDayControlerListener.getWithinDayEngine(), this.scenario, 
				this.parkingAgentsTracker, this.parkingInfrastructure, this.parkingRouterFactory);
		this.randomSearchReplannerFactory.addIdentifier(this.randomSearchIdentifier);		
		this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(this.randomSearchReplannerFactory);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		// connect facilities to links
		new WorldConnectLocations(event.getControler().getConfig()).connectFacilitiesWithLinks(scenario.getActivityFacilities(), 
				(NetworkImpl) scenario.getNetwork());
		
//		this.checkModeChains(event.getControler(), this.multiModalControlerListener.getMultiModalTravelTimes());
		
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = 
				new TripRouterFactoryBuilderWithDefaults().createDefaultLeastCostPathCalculatorFactory(event.getControler().getScenario());
		
		// This is a workaround since the controler does not return its TripRouterFactory
		TripRouterFactory tripRouterFactoryWrapper = new TripRouterFactoryWrapper(event.getControler().getScenario(), 
				event.getControler().getTripRouterProvider(), leastCostPathCalculatorFactory);
		
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
		 * After the withinDayControlerListener has been initialized, also the experiencedPlansWriter can be created.
		 */
		this.experiencedPlansWriter = new ExperiencedPlansWriter(this.withinDayControlerListener.getMobsimDataProvider());
		event.getControler().addControlerListener(this.experiencedPlansWriter);
		
		/*
		 * The ParkingRouter is used to create on-the-fly routes, therefore use the TravelTimeCollector
		 * for car trips. For walk trips use either the TravelTime object used by the multi-modal simulation
		 * or a walk travel time object.
		 */
		TravelTime carTravelTime = this.withinDayControlerListener.getTravelTimeCollector();
//		TravelTime carTravelTime = new FreeSpeedTravelTime();
		TravelTime walkTravelTime;

		if (this.multiModalTravelTimes != null && this.multiModalTravelTimes.containsKey(TransportMode.walk)) {
			walkTravelTime = this.multiModalTravelTimes.get(TransportMode.walk);
		} else walkTravelTime = new WalkTravelTimeFactory(event.getControler().getConfig().plansCalcRoute()).createTravelTime();
		
		this.parkingRouterFactory = new ParkingRouterFactory(this.scenario, carTravelTime, walkTravelTime, 
				event.getControler().getTravelDisutilityFactory(), tripRouterFactoryWrapper, nodesToCheck);
		
		parkingCostCalculator = new ParkingCostCalculatorImpl(this.initParkingTypes());
		this.parkingInfrastructure = new ParkingInfrastructure(this.scenario, parkingCostCalculator, this.allParkingTypes, this.capacityFactor);
		
		this.parkingAgentsTracker = new ParkingAgentsTracker(this.scenario, this.parkingInfrastructure, 
				this.withinDayControlerListener.getMobsimDataProvider() , searchRadius);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		event.getControler().getEvents().addHandler(this.parkingAgentsTracker);
		
		
		/*
		 * Select agent's initial parking locations. They will not be changed anymore
		 * during the simulation.
		 */
		InitialParkingSelector initialParkingSelector = new InitialParkingSelector(this.scenario, 
				this.initialParkingTypes, this.parkingInfrastructure);
		Counter counter = new Counter("# initial parking locations selected: ");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			initialParkingSelector.run(person);
			counter.incCounter();
		}
		counter.printCounter();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			String idString = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), 
					InitialParkingSelector.INITIALPARKINGFACILITY);
			Id<ActivityFacility> facilityId = Id.create(idString, ActivityFacility.class);
			Id<Vehicle> vehicleId = this.parkingInfrastructure.getVehicleId(person);
			this.parkingInfrastructure.unReserveParking(vehicleId, facilityId);
		}
		this.parkingInfrastructure.resetParkingFacilityForNewIteration();
		
		/*
		 * Replace the WithinDayQSimFactory which has been set by the withinDayControlerListener.
		 * If the multi-modal simulation is used, also wrap a MultimodalQSimFactory around it 
		 * to also use the multi-modal simulation.
		 */
		MobsimFactory mobsimFactory = new ParkingQSimFactory(this.parkingInfrastructure, this.parkingRouterFactory, 
				this.withinDayControlerListener.getWithinDayEngine(), this.parkingAgentsTracker);
		if (this.multiModalTravelTimes != null) {
			mobsimFactory = new MultimodalQSimFactory(this.multiModalTravelTimes, mobsimFactory);
		}
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
		streetParking.addAll(this.scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingTypes.STREETPARKING).keySet());
		parkingTypes.put(ParkingTypes.STREETPARKING, streetParking);

		HashSet<Id> garageParking = new HashSet<Id>();
		garageParking.addAll(this.scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingTypes.GARAGEPARKING).keySet());
		parkingTypes.put(ParkingTypes.GARAGEPARKING, garageParking);

		HashSet<Id> privateInsideParking = new HashSet<Id>();
		privateInsideParking.addAll(this.scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingTypes.PRIVATEINSIDEPARKING).keySet());
		parkingTypes.put(ParkingTypes.PRIVATEINSIDEPARKING, privateInsideParking);
		
		HashSet<Id> privateOutsideParking = new HashSet<Id>();
		privateOutsideParking.addAll(this.scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingTypes.PRIVATEOUTSIDEPARKING).keySet());
		parkingTypes.put(ParkingTypes.PRIVATEOUTSIDEPARKING, privateOutsideParking);
		
//		for (ActivityFacility facility : ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().values()) {
//
//			// if the facility offers a parking activity
//			ActivityOption activityOption = facility.getActivityOptions().get(ParkingTypes.PARKING);
//			if (activityOption != null) {
//				if (MatsimRandom.getRandom().nextBoolean()) {
//					streetParking.add(facility.getId());
//				} else {
//					garageParking.add(facility.getId());
//				}
//				
//				// If no capacity is set, set one.
//				if (activityOption.getCapacity() > 100000) {
//					activityOption.setCapacity((double) this.parkingCapacity);
//				}
//			}
//		}
		return parkingTypes;
	}
	
	private static class TripRouterFactoryWrapper implements TripRouterFactory {

		private final TripRouterProvider internalFactory;
		private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		private final PopulationFactory populationFactory;
		private final ModeRouteFactory modeRouteFactory;
		private final Network subNetwork;
		
		public TripRouterFactoryWrapper(Scenario scenario, TripRouterProvider internalFactory,
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
			TripRouter tripRouter = internalFactory.get();
			
	        LeastCostPathCalculator leastCostPathCalculator = leastCostPathCalculatorFactory.createPathCalculator(
	        		this.subNetwork, routingContext.getTravelDisutility(), routingContext.getTravelTime());

	        LegRouter networkLegRouter = new NetworkLegRouter(this.subNetwork, leastCostPathCalculator, this.modeRouteFactory);
			RoutingModule legRouterWrapper = new LegRouterWrapper(TransportMode.car, populationFactory, networkLegRouter); 
			tripRouter.setRoutingModule(TransportMode.car, legRouterWrapper);
	        
			return tripRouter;
		}
		
	}
}