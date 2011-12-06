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

package playground.wrashid.parkingSearch.withinday;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import playground.wrashid.parkingSearch.mobsim.ParkingQSimFactory;

public class WithinDayParkingController extends WithinDayController implements SimulationInitializedListener, 
	StartupListener, ReplanningListener {

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 8;

	protected RandomSearchIdentifier randomSearchIdentifier;
	protected RandomSearchReplanner randomSearchReplanner;

	protected LegModeChecker legModeChecker;
	protected ParkingAgentsTracker parkingAgentsTracker;
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure parkingInfrastructure;
	
	public WithinDayParkingController(String[] args) {
		super(args);
		
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}

	protected void initIdentifiers() {

		this.randomSearchIdentifier = new RandomSearchIdentifier(parkingAgentsTracker, parkingInfrastructure); 
		this.getFixedOrderSimulationListener().addSimulationListener(this.randomSearchIdentifier);
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanners(QSim sim) {

		TravelTimeCollector travelTime = super.getTravelTimeCollector();
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sim.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, factory, routeFactory);
	
		this.randomSearchReplanner = new RandomSearchReplannerFactory(router, 1.0, this.scenarioData, parkingAgentsTracker).createReplanner();
		this.randomSearchReplanner.addAgentsToReplanIdentifier(this.randomSearchIdentifier);
		new SearchParkingAgentsIdentifier(parkingAgentsTracker);		
		this.getReplanningManager().addDuringLegReplanner(this.randomSearchReplanner);
	}
	
	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
				
		// connect facilities to network
		new WorldConnectLocations(this.config).connectFacilitiesWithLinks(getFacilities(), getNetwork());
		
		super.createAndInitReplanningManager(numReplanningThreads);
		super.createAndInitTravelTimeCollector();
		super.createAndInitLinkReplanningMap();
		
		// ensure that all agents' plans have valid mode chains
		legModeChecker = new LegModeChecker(this.scenarioData, this.createRoutingAlgorithm());
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.run(this.scenarioData.getPopulation());
		
		parkingInfrastructure = new ParkingInfrastructure(this.scenarioData);
		this.getEvents().addHandler(this.parkingInfrastructure);
		
		parkingAgentsTracker = new ParkingAgentsTracker(this.scenarioData, 2000.0);
		this.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
		this.getEvents().addHandler(this.parkingAgentsTracker);
		
		insertParkingActivities = new InsertParkingActivities(scenarioData, this.createRoutingAlgorithm(), parkingInfrastructure);
		
		MobsimFactory mobsimFactory = new ParkingQSimFactory(insertParkingActivities, parkingInfrastructure);
		this.setMobsimFactory(mobsimFactory);
		
		this.initIdentifiers();
	}

	@Override
	public void notifyReplanning(ReplanningEvent event) {
		/*
		 * During the replanning the mode chain of the agents' selcted plans
		 * might have been changed. Therefore, we have to ensure that the 
		 * chains are still valid.
		 */
		for (Person person : this.scenarioData.getPopulation().getPersons().values()) {
			legModeChecker.run(person.getSelectedPlan());			
		}
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		QSim sim = (QSim) e.getQueueSimulation();
		initReplanners(sim);
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
//			args=new String[]{"test/input/playground/wrashid/parkingSearch/withinday/config_plans1.xml"};
			args=new String[]{"test/input/playground/wrashid/parkingSearch/withinday/chessboard/config.xml"};
		}
		final WithinDayParkingController controller = new WithinDayParkingController(args);
		controller.setOverwriteFiles(true);
		controller.run();
		
		System.exit(0);
	}
}
