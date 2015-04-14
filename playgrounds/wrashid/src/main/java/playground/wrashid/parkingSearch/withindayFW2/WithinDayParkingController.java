/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayParkingController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.router.util.BikeTravelTime;
import org.matsim.contrib.multimodal.router.util.UnknownTravelTime;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import playground.wrashid.parkingSearch.withinday.LegModeChecker;
import playground.wrashid.parkingSearch.withinday.WithinDayController;
import playground.wrashid.parkingSearch.withindayFW.core.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.core.mobsim.ParkingQSimFactory;

import java.util.HashMap;
import java.util.Map;

public class WithinDayParkingController extends WithinDayController implements ReplanningListener {

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 8;

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
	@Override
	protected void initReplanners(QSim sim) {

		initIdentifiers();
		
		// create a copy of the MultiModalTravelTimeWrapperFactory and set the TravelTimeCollector for car mode
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.walk, new WalkTravelTime(this.config.plansCalcRoute()));
		travelTimes.put(TransportMode.bike, new BikeTravelTime(this.config.plansCalcRoute()));
		travelTimes.put(TransportMode.ride, new UnknownTravelTime(TransportMode.ride, this.config.plansCalcRoute()));
		travelTimes.put(TransportMode.pt, new UnknownTravelTime(TransportMode.pt, this.config.plansCalcRoute()));
		travelTimes.put(TransportMode.car, super.getTravelTimeCollector());
		
		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelDisutilityFactory();
		
		RoutingContext routingContext = new RoutingContextImpl(costFactory, super.getTravelTimeCollector(), this.config.planCalcScore());
		
		this.randomSearchReplannerFactory = new ParkingSearchReplannerFactory(this.getWithinDayEngine(), this.scenarioData, parkingAgentsTracker,
				this.getWithinDayTripRouterFactory(), routingContext);
		this.randomSearchReplannerFactory.addIdentifier(this.randomSearchIdentifier);		
		this.getWithinDayEngine().addDuringLegReplannerFactory(this.randomSearchReplannerFactory);
	}
	
	protected void setUp() {
		super.setUp();


		// connect facilities to network
		new WorldConnectLocations(this.config).connectFacilitiesWithLinks(getScenario().getActivityFacilities(), (NetworkImpl) getScenario().getNetwork());

		super.initWithinDayEngine(numReplanningThreads);
		super.createAndInitTravelTimeCollector();
		super.createAndInitLinkReplanningMap();

		// ensure that all agents' plans have valid mode chains
		legModeChecker = new LegModeChecker(this.scenarioData, new PlanRouter(
				this.getTripRouterProvider().get(),
				this.getScenario().getActivityFacilities()
				));
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenarioData.getPopulation());

		parkingInfrastructure = new ParkingInfrastructure(this.scenarioData,null,null);

		// init parking facility capacities
		IntegerValueHashMap<Id> facilityCapacities = new IntegerValueHashMap<Id>();
		parkingInfrastructure.setFacilityCapacities(facilityCapacities);
		for (ActivityFacility parkingFacility : parkingInfrastructure.getParkingFacilities()) {
			facilityCapacities.incrementBy(parkingFacility.getId(), 10);
		}


		//		Set<Id> parkingFacilityIds = parkingInfrastructure.getFacilityCapacities().getKeySet();
		//		for (Id id : parkingFacilityIds) parkingInfrastructure.getFacilityCapacities().incrementBy(id, 1000);

		parkingAgentsTracker = new ParkingAgentsTracker(this.scenarioData, 2000.0);
		this.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		this.getEvents().addHandler(this.parkingAgentsTracker);

		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory(), super.getTravelTimeCollector(), this.config.planCalcScore());

		insertParkingActivities = new InsertParkingActivities(scenarioData, this.getWithinDayTripRouterFactory().instantiateAndConfigureTripRouter(routingContext), parkingInfrastructure);

		MobsimFactory mobsimFactory = new ParkingQSimFactory(insertParkingActivities, parkingInfrastructure, this.getWithinDayEngine());
		this.setMobsimFactory(mobsimFactory);
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
			args=new String[]{"test/input/playground/wrashid/parkingSearch/withinday/chessboard/config_plans1.xml"};
//			args=new String[]{"test/input/playground/wrashid/parkingSearch/withinday/chessboard/config.xml"};
		}
		final WithinDayParkingController controller = new WithinDayParkingController(args);
		controller.setOverwriteFiles(true);
		
		
		controller.run();
		
		System.exit(0);
	}
}
