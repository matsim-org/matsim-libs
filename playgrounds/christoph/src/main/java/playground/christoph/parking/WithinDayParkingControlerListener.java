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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayControlerListener;

import playground.christoph.evacuation.trafficmonitoring.BikeTravelTime;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTime;
import playground.christoph.parking.core.ParkingCostCalculatorImpl;
import playground.christoph.parking.core.mobsim.InsertParkingActivities;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.christoph.parking.core.mobsim.ParkingQSimFactory;
import playground.christoph.parking.core.utils.LegModeChecker;
import playground.christoph.parking.withinday.identifier.ParkingSearchIdentifier;
import playground.christoph.parking.withinday.replanner.ParkingSearchReplannerFactory;
import playground.christoph.parking.withinday.utils.ParkingAgentsTracker;
import playground.christoph.parking.withinday.utils.ParkingRouterFactory;

public class WithinDayParkingControlerListener implements StartupListener, ReplanningListener {

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

	protected TripRouterFactory tripRouterFactory;
	protected ParkingSearchIdentifier randomSearchIdentifier;
	protected ParkingSearchReplannerFactory randomSearchReplannerFactory;

	protected LegModeChecker legModeChecker;
	protected ParkingAgentsTracker parkingAgentsTracker;
	protected ParkingRouterFactory parkingRouterFactory;
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure parkingInfrastructure;
	
	private Scenario scenario;
	private WithinDayControlerListener withinDayControlerListener;
	
	public WithinDayParkingControlerListener(Controler controler) {
		
		this.scenario = controler.getScenario();
		
		init(controler);
	}
	
	private void init(Controler controler) {
			
		/*
		 * Create a WithinDayControlerListener but do NOT register it as ControlerListener.
		 * It implements the StartupListener interface as this class also does. The
		 * StartupEvent is passed over to it when this class handles the event. 
		 */
		this.withinDayControlerListener = new WithinDayControlerListener();
		
		// workaround
		this.withinDayControlerListener.setLeastCostPathCalculatorFactory(new DijkstraFactory());
	}
	
	private void initIdentifiers() {

		this.randomSearchIdentifier = new ParkingSearchIdentifier(parkingAgentsTracker, parkingInfrastructure); 
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.randomSearchIdentifier);
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
		
		// create a copy of the MultiModalTravelTimeWrapperFactory and set the TravelTimeCollector for car mode
		Map<String, TravelTime> multiModalTravelTimes = new HashMap<String, TravelTime>();
		multiModalTravelTimes.put(TransportMode.walk, new WalkTravelTime(this.scenario.getConfig().plansCalcRoute()));
		multiModalTravelTimes.put(TransportMode.bike, new BikeTravelTime(this.scenario.getConfig().plansCalcRoute()));
		
		this.tripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes);
//		this.withinDayControlerListener.setWithinDayTripRouterFactory(tripRouterFactory);
		// workaround
		TripRouterFactoryWrapper tripRouterFactoryWrapper = new TripRouterFactoryWrapper(
				this.tripRouterFactory, event.getControler().getTravelDisutilityFactory(),
				this.withinDayControlerListener.getTravelTimeCollector(), event.getControler().getConfig().planCalcScore());
		this.withinDayControlerListener.setWithinDayTripRouterFactory(tripRouterFactoryWrapper);
		
		/*
		 * notifyStartup has to be called after the WithinDayTripRouterFactory has been set.
		 * After this call, the setters cannot be called anymore.
		 */
		this.withinDayControlerListener.notifyStartup(event);

		/*
		 * After the withinDayControlerListener has handled the startup event, its TravelTimeCollector
		 * has been initialized. Therefore, we can add it now to the multiModalTravelTimes map.
		 */
		multiModalTravelTimes.put(TransportMode.car, this.withinDayControlerListener.getTravelTimeCollector());
		
		// ensure that all agents' plans have valid mode chains
		RoutingContext routingContext = new RoutingContextImpl(event.getControler().getTravelDisutilityFactory(), 
				new FreeSpeedTravelTime(), event.getControler().getConfig().planCalcScore());
		TripRouter tripRouter = this.tripRouterFactory.instantiateAndConfigureTripRouter(routingContext);
		PlanAlgorithm planAlgorithm = new PlanRouter(tripRouter);
		legModeChecker = new LegModeChecker(this.scenario, planAlgorithm);
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenario.getPopulation());	
		
//		this.parkingRouterFactory = new ParkingRouterFactory(this.scenario, multiModalTravelTimes, 
//				controler.createTravelDisutilityCalculator(), tripRouterFactory, nodesToCheck);
		//workaround
		this.parkingRouterFactory = new ParkingRouterFactory(this.scenario, multiModalTravelTimes, 
				event.getControler().getTravelDisutilityFactory(), tripRouterFactoryWrapper, nodesToCheck);
		
		ParkingCostCalculatorImpl parkingCostCalculator = new ParkingCostCalculatorImpl(this.initParkingTypes());
		this.parkingInfrastructure = new ParkingInfrastructure(this.scenario, parkingCostCalculator);
		
		this.parkingAgentsTracker = new ParkingAgentsTracker(this.scenario, parkingInfrastructure, searchRadius);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		event.getControler().getEvents().addHandler(this.parkingAgentsTracker);
		
		// replace the WithinDayQSimFactory which has been set by the withinDayControlerListener.
		MobsimFactory mobsimFactory = new ParkingQSimFactory(parkingInfrastructure, parkingRouterFactory, 
				this.withinDayControlerListener.getWithinDayEngine(), this.parkingAgentsTracker);
		event.getControler().setMobsimFactory(mobsimFactory);
		
		this.initIdentifiers();
		this.initReplanners();
	}
	
	@Override
	public void notifyReplanning(ReplanningEvent event) {
		/*
		 * During the replanning the mode chain of the agents' selected plans
		 * might have been changed. Therefore, we have to ensure that the 
		 * chains are still valid.
		 */
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			legModeChecker.run(person.getSelectedPlan());
		}
	}
	
	private HashMap<String, HashSet<Id>> initParkingTypes() {
		
		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();

		HashSet<Id> streetParking = new HashSet<Id>();
		HashSet<Id> garageParking = new HashSet<Id>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);
		
		for (ActivityFacility facility : ((ScenarioImpl) this.scenario).getActivityFacilities().getFacilities().values()) {

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
	
	private static class TripRouterFactoryWrapper implements TripRouterFactoryInternal {

		private final TripRouterFactory tripRouterFactory;
		private final TravelDisutilityFactory travelDisutilityFactory;
		private final TravelTime travelTime;
		private final PlanCalcScoreConfigGroup cnScoringGroup;
		
		public TripRouterFactoryWrapper(TripRouterFactory tripRouterFactory, TravelDisutilityFactory travelDisutilityFactory,
				TravelTime travelTime, PlanCalcScoreConfigGroup cnScoringGroup) {
			this.tripRouterFactory = tripRouterFactory;
			this.travelDisutilityFactory = travelDisutilityFactory;
			this.travelTime = travelTime;
			this.cnScoringGroup = cnScoringGroup;
		}
		
		@Override
		public TripRouter instantiateAndConfigureTripRouter() {
			RoutingContext routingContext = new RoutingContextImpl(this.travelDisutilityFactory, 
					this.travelTime, this.cnScoringGroup);
			TripRouter tripRouter = this.tripRouterFactory.instantiateAndConfigureTripRouter(routingContext);
			return tripRouter;
		}
		
	}
}