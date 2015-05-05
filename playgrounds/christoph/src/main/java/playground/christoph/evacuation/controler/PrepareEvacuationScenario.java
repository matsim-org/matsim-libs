/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareEvacuationScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.router.DefaultDelegateFactory;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.TransitTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.LinkSlopesReader;
import org.matsim.contrib.multimodal.router.util.MultiModalTravelTimeFactory;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.households.Household;
import org.matsim.pt.router.TransitRouter;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.LegModeChecker;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;
import playground.christoph.evacuation.vehicles.AssignVehiclesToPlans;
import playground.christoph.evacuation.vehicles.CreateVehiclesForHouseholds;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;

import javax.inject.Provider;

/**
 * Prepares a scenario to be used in an evacuation simulation.
 * <ul>
 * 	<li>adapt network capacities and speeds</li>
 * 	<li>connect facilities to network</li>
 *  <li>make network multi-modal</li>
 * 	<li>add exit links to network</li>
 *  <li>add secure facilities</li>
 * 	<li>add pickup and drop off facilities</li>
 *  <li>check mode chains (car availability)</li>
 *  <li>creates vehicles</li>
 *  <li>assign vehicles to population</li>
 * </ul>
 */
public class PrepareEvacuationScenario {
	
	private final TravelDisutilityFactory travelDisutilityFactory = new TravelTimeAndDistanceBasedTravelDisutilityFactory();
	private final TravelTime travelTime = new FreeSpeedTravelTime();
	
	public void prepareScenario(Scenario scenario) {

		TripRouterFactory tripRouterFactory = createTripRouterFactory(scenario);
		
		/*
		 * Adapt network capacities and speeds.
		 * So far we do not support time dependent capacities/speeds.
		 */
		adaptNetworkCapacitiesAndSpeed(scenario);
		
		/*
		 * Connect facilities to links.
		 */
		connectFacilitiesToLinks(scenario);

		/*
		 * Create a multi-modal network
		 */
		createMultiModalNetwork(scenario);
		
		/*
		 * Add rescue links to network.
		 */
		addExitLinks(scenario);

		/*
		 * Add secure facilities to secure links.
		 */
		addSecureFacilities(scenario);
		
		/*
		 * Add pickup facilities to links.
		 */
		addPickupAndDropOffFacilities(scenario);
		
		/*
		 * Using a LegModeChecker to ensure that all agents' plans have valid mode chains.
		 * With the new demand, this should not be necessary anymore!
		 */
		checkLegModes(scenario, createTripRouterInstance(scenario, tripRouterFactory));
		
		/*
		 * Read household vehicles from files. Then create vehicles for the
		 * census households as well as for the crossboarder population.
		 */
		createVehicles(scenario);
		
		/*
		 * Assign vehicles to agent's plans.
		 */
		assignVehiclesToPlans(scenario, createTripRouterInstance(scenario, tripRouterFactory));
	}
	
	private void adaptNetworkCapacitiesAndSpeed(Scenario scenario) {
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (EvacuationConfig.capacityFactor != 1.0) link.setCapacity(link.getCapacity() * EvacuationConfig.capacityFactor);
			if (EvacuationConfig.speedFactor != 1.0) link.setFreespeed(link.getFreespeed() * EvacuationConfig.speedFactor);
		}
	}
	
	private void connectFacilitiesToLinks(Scenario scenario) {
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		new WorldConnectLocations(scenario.getConfig()).connectFacilitiesWithLinks(facilities, (NetworkImpl) scenario.getNetwork());
	}
	
	private void addExitLinks(Scenario scenario) {
		new AddExitLinksToNetwork(scenario).createExitLinks();
	}
	
	private void createMultiModalNetwork(Scenario scenario) {
		
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		if (!NetworkUtils.isMultimodal(scenario.getNetwork())) {
			new MultiModalNetworkCreator(multiModalConfigGroup).run(scenario.getNetwork());
		}
	}
	
	private void addSecureFacilities(Scenario scenario) {
//		new AddSecureFacilitiesToNetwork(scenario).createSecureFacilities();
	}
	
	private void addPickupAndDropOffFacilities(Scenario scenario) {
		
		ActivityFacilities facilities = scenario.getActivityFacilities();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			/*
			 * Create and add the pickup and drop off facility and add activity options ("pickup", "dropoff")
			 */
			String idString = link.getId().toString() + EvacuationConstants.PICKUP_DROP_OFF_FACILITY_SUFFIX;
			ActivityFacility pickupDropOffFacility = ((ActivityFacilitiesImpl) facilities).createAndAddFacility(Id.create(idString, ActivityFacility.class), link.getCoord());
			((ActivityFacilityImpl) pickupDropOffFacility).setLinkId(((LinkImpl)link).getId());
			
			ActivityOption activityOption;
			activityOption = ((ActivityFacilityImpl) pickupDropOffFacility).createActivityOption(EvacuationConstants.PICKUP_ACTIVITY);
			activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);
			
			activityOption = ((ActivityFacilityImpl) pickupDropOffFacility).createActivityOption(EvacuationConstants.DROP_OFF_ACTIVITY);
			activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);
		}
	}
	
	private void checkLegModes(Scenario scenario, TripRouter tripRouter) {
		
		LegModeChecker legModeChecker = new LegModeChecker(tripRouter, scenario.getNetwork());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk, TransportMode.bike, TransportMode.pt});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(scenario.getPopulation());
		legModeChecker.printStatistics();	
	}
	
	private void createVehicles(Scenario scenario) {

		/*
		 * Read household-vehicles-assignment files.
		 */
		HouseholdVehicleAssignmentReader householdVehicleAssignmentReader = new HouseholdVehicleAssignmentReader(scenario);
		for (String file : EvacuationConfig.vehicleFleet) householdVehicleAssignmentReader.parseFile(file);
		householdVehicleAssignmentReader.createVehiclesForCrossboarderHouseholds();
		
		/*
		 * Create vehicles for households and add them to the scenario.
		 * When useVehicles is set to true, the scenario creates a Vehicles container if necessary.
		 */
		scenario.getConfig().scenario().setUseVehicles(true);
		CreateVehiclesForHouseholds createVehiclesForHouseholds = new CreateVehiclesForHouseholds(scenario, 
				householdVehicleAssignmentReader.getAssignedVehicles());
		createVehiclesForHouseholds.run();
//		config.scenario().setUseVehicles(false);
	}
	
	private void assignVehiclesToPlans(Scenario scenario, TripRouter tripRouter) {
		
		AssignVehiclesToPlans assignVehiclesToPlans = new AssignVehiclesToPlans(scenario, tripRouter);
		for (Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
			assignVehiclesToPlans.run(household);
		}
		assignVehiclesToPlans.printStatistics();
	}
	
	private TripRouterFactory createTripRouterFactory(Scenario scenario) {
		
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);		
		Map<Id<Link>, Double> linkSlopes = new LinkSlopesReader().getLinkSlopes(multiModalConfigGroup, scenario.getNetwork());
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig(), linkSlopes);
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();
		
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = builder.createDefaultLeastCostPathCalculatorFactory(scenario);
		Provider<TransitRouter> transitRouterFactory = null;
		if (scenario.getConfig().scenario().isUseTransit()) transitRouterFactory = builder.createDefaultTransitRouter(scenario);
		
		TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(scenario, leastCostPathCalculatorFactory);
		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes, 
				travelDisutilityFactory, defaultDelegateFactory, new FastDijkstraFactory());
		TripRouterFactory transitTripRouterFactory = new TransitTripRouterFactory(scenario, multiModalTripRouterFactory, 
				transitRouterFactory);
		
		return transitTripRouterFactory;
	}
	
	private TripRouter createTripRouterInstance(Scenario scenario, TripRouterFactory tripRouterFactory) {
		TravelDisutility travelDisutility = this.travelDisutilityFactory.createTravelDisutility(travelTime, scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);
		return tripRouterFactory.instantiateAndConfigureTripRouter(routingContext);
	}
}