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
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.RideTravelTime;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.modules.ReplanningModule;

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
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.replanner.ParkingSearchReplannerFactoryWithStrategySwitching;

public class WithinDayParkingController extends WithinDayController implements ReplanningListener {

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
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure parkingInfrastructure;
	
	public WithinDayParkingController(String[] args) {
		super(args);
		
		// register this as a Controller Listener
		super.addControlerListener(this);
	}

	protected void initIdentifiers() {

		this.randomSearchIdentifier = new ParkingSearchIdentifier(parkingAgentsTracker, parkingInfrastructure); 
		this.getFixedOrderSimulationListener().addSimulationListener(this.randomSearchIdentifier);
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanners() {

		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.scenarioData.getPopulation().getFactory()).getModeRouteFactory();

		// create a copy of the MultiModalTravelTimeWrapperFactory and set the TravelTimeCollector for car mode
		Map<String, TravelTime> times = new HashMap<String, TravelTime>();
		times.put(TransportMode.walk, new WalkTravelTime(this.config.plansCalcRoute()));
		times.put(TransportMode.bike, new BikeTravelTime(this.config.plansCalcRoute()));
		times.put(TransportMode.ride, new RideTravelTime(this.getLinkTravelTimes(), new WalkTravelTime(this.config.plansCalcRoute())));
		times.put(TransportMode.pt, new PTTravelTime(this.config.plansCalcRoute(), this.getLinkTravelTimes(), new WalkTravelTime(this.config.plansCalcRoute())));
		times.put(TransportMode.car, super.getTravelTimeCollector());
		
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		
		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, times, factory, routeFactory);

		// Use a the TravelTimeCollector here for within-day routes replanning!
		ParkingRouterFactory parkingRouterFactory = new ParkingRouterFactory(this.scenarioData, times, 
				this.createTravelCostCalculator(), this.getTripRouterFactory(), nodesToCheck);
		
		this.randomSearchReplannerFactory = new ParkingSearchReplannerFactoryWithStrategySwitching(this.getWithinDayEngine(), router, 1.0, this.scenarioData, 
				parkingAgentsTracker, parkingInfrastructure, parkingRouterFactory);
		this.randomSearchReplannerFactory.addIdentifier(this.randomSearchIdentifier);		
		this.getWithinDayEngine().addDuringLegReplannerFactory(this.randomSearchReplannerFactory);
	}
	
	@Override
	protected void setUp() {
		super.setUp();
		
		// replace travel time calculators for walk and bike
		Map<String, TravelTime> times = this.getMultiModalTravelTimes();
		times.put(TransportMode.walk, new WalkTravelTime(this.config.plansCalcRoute()));
		times.put(TransportMode.bike, new BikeTravelTime(this.config.plansCalcRoute()));
		
		// connect facilities to network
		new WorldConnectLocations(this.config).connectFacilitiesWithLinks(getFacilities(), (NetworkImpl) getNetwork());
		
		super.initWithinDayEngine(numReplanningThreads);
		super.createAndInitTravelTimeCollector();
		super.createAndInitLinkReplanningMap();
		
		// ensure that all agents' plans have valid mode chains
		legModeChecker = new LegModeChecker(this.scenarioData, this.createRoutingAlgorithm());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenarioData.getPopulation());
		
		ParkingCostCalculatorImpl parkingCostCalculator = new ParkingCostCalculatorImpl(this.initParkingTypes());
		//TODO: this the parking type is not initialized here (give types to different parkings)
		parkingInfrastructure = new ParkingInfrastructure_v2(this.scenarioData, parkingCostCalculator, new HashMap<Id, String>());
		
		parkingAgentsTracker = new ParkingAgentsTracker(this.scenarioData, parkingInfrastructure, searchRadius);
		this.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		this.getEvents().addHandler(this.parkingAgentsTracker);
		
		ParkingRouterFactory parkingRouterFactory = new ParkingRouterFactory(this.scenarioData, times, 
				this.createTravelCostCalculator(), this.getTripRouterFactory(), nodesToCheck);
		
		MobsimFactory mobsimFactory = new ParkingQSimFactory(parkingInfrastructure, parkingRouterFactory, this.getWithinDayEngine());
		this.setMobsimFactory(mobsimFactory);
		
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
		for (Person person : this.scenarioData.getPopulation().getPersons().values()) {
			legModeChecker.run(person.getSelectedPlan());			
		}
	}
	
	private HashMap<String, HashSet<Id>> initParkingTypes() {
		
		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();

		HashSet<Id> streetParking = new HashSet<Id>();
		HashSet<Id> garageParking = new HashSet<Id>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);
		
		for (ActivityFacility facility : scenarioData.getActivityFacilities().getFacilities().values()) {

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
		final WithinDayParkingController controller = new WithinDayParkingController(args);
		controller.setOverwriteFiles(true);
		
		controller.run();
		
		System.exit(0);
	}
}
