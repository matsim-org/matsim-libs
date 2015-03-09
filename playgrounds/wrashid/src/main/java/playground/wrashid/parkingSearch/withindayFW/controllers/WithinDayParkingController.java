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

package playground.wrashid.parkingSearch.withindayFW.controllers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.Desires;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.withinday.controller.WithinDayController;

import playground.wrashid.parkingSearch.withindayFW.core.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.core.LegModeChecker;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingAgentsTracker;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.core.mobsim.ParkingQSimFactory;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingCostCalculatorFW;

import java.util.HashMap;
import java.util.HashSet;

public abstract class WithinDayParkingController extends WithinDayController implements ReplanningListener  {

	protected static int currentScenarioId=2;
	
	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	// TODO: set this parameter from config!
	protected int numReplanningThreads = 3;

	// protected RandomSearchIdentifier randomSearchIdentifier;
	// protected RandomSearchReplannerFactory randomSearchReplannerFactory;

	protected LegModeChecker legModeChecker;
	protected ParkingAgentsTracker parkingAgentsTracker;
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure parkingInfrastructure;

	public WithinDayParkingController(String[] args) {
		super(args);

		// register this as a Controller Listener
		super.addControlerListener(this);
	}

	protected void startUpFinishing() {
		/*
		ParkingPersonalBetas parkingPersonalBetas=new ParkingPersonalBetas(this.scenarioData, null);
		
		ParkingStrategyActivityMapperFW parkingStrategyActivityMapperFW = new ParkingStrategyActivityMapperFW();
		Collection<ParkingStrategy> parkingStrategies = new LinkedList<ParkingStrategy>();
		ParkingStrategyManager parkingStrategyManager = new ParkingStrategyManager(parkingStrategyActivityMapperFW,
				parkingStrategies,parkingPersonalBetas);
		parkingAgentsTracker.setParkingStrategyManager(parkingStrategyManager);

		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeAndDisutility(
				this.config.planCalcScore()));
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) this.scenarioData.getPopulation().getFactory())
				.getModeRouteFactory();

		
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
		TravelTimeFactoryWrapper travelTimeCollectorWrapperFactory = new TravelTimeFactoryWrapper(this.getTravelTimeCollector());

		// create a copy of the MultiModalTravelTimeWrapperFactory and set the
		// TravelTimeCollector for car mode
		MultiModalTravelTimeWrapperFactory timeFactory = new MultiModalTravelTimeWrapperFactory();
		for (Entry<String, PersonalizableTravelTimeFactory> entry : this.getMultiModalTravelTimeWrapperFactory()
				.getPersonalizableTravelTimeFactories().entrySet()) {
			timeFactory.setPersonalizableTravelTimeFactory(entry.getKey(), entry.getValue());
		}

		timeFactory.setPersonalizableTravelTimeFactory(TransportMode.car, travelTimeCollectorWrapperFactory);

		TravelDisutilityFactory costFactory = new OnlyTimeDependentTravelCostCalculatorFactory();

		AbstractMultithreadedModule router = new ReplanningModule(config, network, costFactory, timeFactory, factory,
				routeFactory);

		// adding random steet search
		RandomSearchReplannerFactory randomSearchReplannerFactory = new RandomSearchReplannerFactory(this.getReplanningManager(),
				router, 1.0, this.scenarioData, parkingAgentsTracker);
		RandomSearchIdentifier randomSearchIdentifier = new RandomSearchIdentifier(parkingAgentsTracker, parkingInfrastructure);
		this.getFixedOrderSimulationListener().addSimulationListener(randomSearchIdentifier);
		randomSearchReplannerFactory.addIdentifier(randomSearchIdentifier);
		ParkingStrategy parkingStrategy = new ParkingStrategy(randomSearchIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getReplanningManager().addDuringLegReplannerFactory(randomSearchReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "home", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work", parkingStrategy);

		// adding garage parking strategy
		GPSNIReplannerFactory gpsniReplannerFactory = new GPSNIReplannerFactory(this.getReplanningManager(), router, 1.0,
				this.scenarioData, parkingAgentsTracker);
		GPSNIIdentifier gpsniIdentifier = new GPSNIIdentifier(parkingAgentsTracker, parkingInfrastructure);
		this.getFixedOrderSimulationListener().addSimulationListener(gpsniIdentifier);
		gpsniReplannerFactory.addIdentifier(gpsniIdentifier);
		parkingStrategy = new ParkingStrategy(gpsniIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getReplanningManager().addDuringLegReplannerFactory(gpsniReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "shopping", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "leisure", parkingStrategy);

		
		this.addControlerListener(parkingStrategyManager);
		this.getFixedOrderSimulationListener().addSimulationListener(parkingStrategyManager);
*/
	}
	
	protected void setUp(){
//		setUseTripRouting(false);		
		
		super.setUp();
		
		startUpBegin();
		//TODO: clean this out! => nothing from test/zürich scenario should be here.
		HashMap<String, HashSet<Id>> parkingTypes = initParkingTypes(this);
		
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
		legModeChecker.setValidNonCarModes(new String[] { TransportMode.walk });
		legModeChecker.setToCarProbability(0.5);
		ParallelPersonAlgorithmRunner.run(this.scenarioData.getPopulation(), numReplanningThreads, legModeChecker);
		//legModeChecker.run(this.scenarioData.getPopulation());

		if (parkingInfrastructure==null){
			parkingInfrastructure = new ParkingInfrastructure(this.scenarioData,parkingTypes, new ParkingCostCalculatorFW(parkingTypes));
		}

		parkingAgentsTracker = new ParkingAgentsTracker(this.scenarioData, 10000.0, parkingInfrastructure);
		this.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		this.getEvents().addHandler(this.parkingAgentsTracker);
		this.addControlerListener(parkingAgentsTracker);

		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory(), super.getTravelTimeCollector(), this.config.planCalcScore());
		
		insertParkingActivities = new InsertParkingActivities(scenarioData, this.getWithinDayTripRouterFactory().instantiateAndConfigureTripRouter(routingContext), parkingInfrastructure);

		this.getWithinDayEngine().initializeReplanningModules(numReplanningThreads);
		MobsimFactory mobsimFactory = new ParkingQSimFactory(insertParkingActivities, parkingInfrastructure, this.getWithinDayEngine());
		this.setMobsimFactory(mobsimFactory);

		setDesiresIfApplicable();
		
		// this.initIdentifiers();
		// this.initReplanners();
		startUpFinishing();
		
		throw new RuntimeException("setting useTripRouting to false (see at beginning of method) " +
				"no longer possible since this only affects initialization and my thus be " +
		"inconsistent.  kai, may'13.  aborting ... ") ;


	}

	private void setDesiresIfApplicable() {
		for (Person p:scenarioData.getPopulation().getPersons().values()){
			PersonImpl person=(PersonImpl) p;
			Desires desires = person.getDesires();
			if (desires!=null){
				// setting typical parking duration
				// if missing, this causes score to become Infinity (e.g. kti scenario)
				desires.putActivityDuration("parking", 180);
			}
		}
	}

	protected void startUpBegin() {
		
	}

	private HashMap<String, HashSet<Id>> initParkingTypes(Controler controler) {
		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();

		HashSet<Id> streetParking=new HashSet<Id>();
		HashSet<Id> garageParking=new HashSet<Id>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);
		
		for (ActivityFacility facility : ((ScenarioImpl) controler.getScenario()).getActivityFacilities()
				.getFacilities().values()) {

			// if the facility offers a parking activity
			if (facility.getActivityOptions().containsKey("parking")) {
				if (MatsimRandom.getRandom().nextBoolean()){
					streetParking.add(facility.getId());
				} else {
					garageParking.add(facility.getId());
				}
			}
		}
		return parkingTypes;
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		/*
		 * During the replanning the mode chain of the agents' selected plans
		 * might have been changed. Therefore, we have to ensure that the chains
		 * are still valid.
		 */
		//for (Person person : this.scenarioData.getPopulation().getPersons().values()) {
		//	legModeChecker.run(person.getSelectedPlan());
		//}
		
		ParallelPersonAlgorithmRunner.run(this.scenarioData.getPopulation(), numReplanningThreads, legModeChecker);
	}

	/*
	 * =================================================================== main
	 * ===================================================================
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("using default config");
			// args=new
			// String[]{"test/input/playground/wrashid/parkingSearch/withinday/chessboard/config_plans1.xml"};
			args = new String[] { "test/input/playground/wrashid/parkingSearch/withinday/chessboard/config.xml" };
		}
		//final WithinDayParkingController controller = new WithinDayParkingController(args);
		//controller.setOverwriteFiles(true);

		//controller.run();

		System.exit(0);
	}


}
