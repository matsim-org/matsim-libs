/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareEvacuationScenarioListener.java
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

package playground.christoph.evacuation.controler;

import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
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
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.pt.router.FastTransitRouterImplFactory;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.LegModeChecker;
import playground.christoph.evacuation.network.AddExitLinksToNetwork;
import playground.christoph.evacuation.pt.TransitRouterNetworkReaderMatsimV1;
import playground.christoph.evacuation.vehicles.AssignVehiclesToPlans;
import playground.christoph.evacuation.vehicles.CreateVehiclesForHouseholds;
import playground.christoph.evacuation.vehicles.HouseholdVehicleAssignmentReader;

public class PrepareEvacuationScenarioListener {

	private static final Logger log = Logger.getLogger(PrepareEvacuationScenarioListener.class);
	
	private final TravelDisutilityFactory travelDisutilityFactory = new TravelTimeAndDistanceBasedTravelDisutilityFactory();
	private final TravelTime travelTime = new FreeSpeedTravelTime();
	
	private TransitRouterNetwork transitRouterNetwork;
	private TripRouterFactory tripRouterFactory;
	
	public void prepareScenario(Scenario scenario) {

		/*
		 * Read transit router network from file. 
		 */
//		readTransitRouterNetwork(scenario);
		
		/*
		 * Create trip router factory.
		 */
		createTripRouterFactory(scenario);
		
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
		 * Remove households without assigned members. They should not exist at all but
		 * still some are there and confuse the analysis code.
		 */
		removeEmptyHouseholds(scenario);
		
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
	
	private void readTransitRouterNetwork(Scenario scenario) {
		transitRouterNetwork = new TransitRouterNetwork();
		new TransitRouterNetworkReaderMatsimV1(scenario, transitRouterNetwork).parse(EvacuationConfig.transitRouterFile);
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
	
	private void removeEmptyHouseholds(Scenario scenario) {
		
		Households households = ((ScenarioImpl) scenario).getHouseholds();
		
		int removed = 0;
		Iterator<Household> iter = households.getHouseholds().values().iterator();
		while (iter.hasNext()) {
			Household household = iter.next();
			if (household.getMemberIds().size() == 0) {
				iter.remove();
				removed++;
			}
		}
		log.info("Removed " + removed + " households without any assigned member.");
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
	
	private void createTripRouterFactory(Scenario scenario) {
		
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);		
		Map<Id<Link>, Double> linkSlopes = new LinkSlopesReader().getLinkSlopes(multiModalConfigGroup, scenario.getNetwork());
		MultiModalTravelTimeFactory multiModalTravelTimeFactory = new MultiModalTravelTimeFactory(scenario.getConfig(), linkSlopes);
		Map<String, TravelTime> multiModalTravelTimes = multiModalTravelTimeFactory.createTravelTimes();
		
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = builder.createDefaultLeastCostPathCalculatorFactory(scenario);
		TransitRouterFactory transitRouterFactory = null;
		if (scenario.getConfig().scenario().isUseTransit()) {
//			transitRouterFactory = builder.createDefaultTransitRouter(scenario);
			Config config = scenario.getConfig();
	        TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(),
	        		config.transitRouter(), config.vspExperimental());
//	        throw new RuntimeException("This feature is not yet implemented in TransitRouterImplFactory!");
//	        transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, this.transitRouterNetwork);
	        transitRouterFactory = new FastTransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, this.transitRouterNetwork);
		}
		
		TripRouterFactory defaultDelegateFactory = new DefaultDelegateFactory(scenario, leastCostPathCalculatorFactory);
		TripRouterFactory multiModalTripRouterFactory = new MultimodalTripRouterFactory(scenario, multiModalTravelTimes, 
				travelDisutilityFactory, defaultDelegateFactory, new FastDijkstraFactory());
		TripRouterFactory transitTripRouterFactory = new TransitTripRouterFactory(scenario, multiModalTripRouterFactory, 
				transitRouterFactory);
		
		this.tripRouterFactory = transitTripRouterFactory;
	}
	
	private TripRouter createTripRouterInstance(Scenario scenario, TripRouterFactory tripRouterFactory) {
		TravelDisutility travelDisutility = this.travelDisutilityFactory.createTravelDisutility(travelTime, scenario.getConfig().planCalcScore());
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);
		return tripRouterFactory.instantiateAndConfigureTripRouter(routingContext);
	}

}
