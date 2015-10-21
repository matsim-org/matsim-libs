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

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.QSim;

import playground.wrashid.parkingSearch.withindayFW.core.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;

public class WithinDayParkingController extends WithinDayController implements ReplanningListener {

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 8;

	protected RandomSearchIdentifier randomSearchIdentifier;
	protected RandomSearchReplannerFactory randomSearchReplannerFactory;

	protected LegModeChecker legModeChecker;
	protected ParkingAgentsTracker parkingAgentsTracker;
	protected InsertParkingActivities insertParkingActivities;
	protected ParkingInfrastructure parkingInfrastructure;
	
	public WithinDayParkingController(String[] args) {
		super(args);
		
		// register this as a Controller Listener
		super.addControlerListener(this);
		
		throw new RuntimeException(Gbl.SET_UP_IS_NOW_FINAL) ;
	}

	protected void initIdentifiers() {

		this.randomSearchIdentifier = new RandomSearchIdentifier(parkingAgentsTracker, parkingInfrastructure); 
		this.getFixedOrderSimulationListener().addSimulationListener(this.randomSearchIdentifier);
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	@Override
	protected void initReplanners(QSim sim) {

		throw new RuntimeException();
//		this.initIdentifiers();
//
//		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory().createTravelDisutility(this.getTravelTimeCollector(), this.getConfig().planCalcScore()), this.getTravelTimeCollector());
//
//		this.randomSearchReplannerFactory = new RandomSearchReplannerFactory(this.getWithinDayEngine(), this.getScenario(), parkingAgentsTracker,
//				this.getWithinDayTripRouterFactory(), routingContext);
//		this.randomSearchReplannerFactory.addIdentifier(this.randomSearchIdentifier);
//		this.getWithinDayEngine().addDuringLegReplannerFactory(this.randomSearchReplannerFactory);
	}
	
//	protected void setUp() {
//		super.setUp();
//		
//		
//// connect facilities to network
//        new WorldConnectLocations(this.getConfig()).connectFacilitiesWithLinks(getScenario().getActivityFacilities(), (NetworkImpl) getScenario().getNetwork());
//		
//		super.initWithinDayEngine(numReplanningThreads);
//		super.createAndInitTravelTimeCollector();
//		super.createAndInitLinkReplanningMap();
//		
//// ensure that all agents' plans have valid mode chains
//		legModeChecker = new LegModeChecker(this.getScenario(), new PlanRouter(
//		this.getTripRouterProvider().get(),
//		this.getScenario().getActivityFacilities()
//		));
//		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
//		legModeChecker.setToCarProbability(0.5);
//		legModeChecker.run(this.getScenario().getPopulation());
//		
//		parkingInfrastructure = new ParkingInfrastructure(this.getScenario(),null,null);
//		
//		// init parking facility capacities
//				IntegerValueHashMap<Id> facilityCapacities = new IntegerValueHashMap<Id>();
//				parkingInfrastructure.setFacilityCapacities(facilityCapacities);
//				for (ActivityFacility parkingFacility : parkingInfrastructure.getParkingFacilities()) {
//					facilityCapacities.incrementBy(parkingFacility.getId(), 10);
//				}
//		
//		
////		Set<Id> parkingFacilityIds = parkingInfrastructure.getFacilityCapacities().getKeySet();
////		for (Id id : parkingFacilityIds) parkingInfrastructure.getFacilityCapacities().incrementBy(id, 1000);
//		
//		parkingAgentsTracker = new ParkingAgentsTracker(this.getScenario(), 2000.0);
//		this.getFixedOrderSimulationListener().addSimulationListener(this.parkingAgentsTracker);
//		this.getEvents().addHandler(this.parkingAgentsTracker);
//		
//		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory(), this.getTravelTimeCollector(), this.getConfig().planCalcScore());
//		
//		insertParkingActivities = new InsertParkingActivities(getScenario(), this.getWithinDayTripRouterFactory().get(routingContext), parkingInfrastructure);
//		
//		final MobsimFactory mobsimFactory = new ParkingQSimFactory(insertParkingActivities, parkingInfrastructure, this.getWithinDayEngine());
//		this.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				bindMobsim().toProvider(new Provider<Mobsim>() {
//					@Override
//					public Mobsim get() {
//						return mobsimFactory.createMobsim(getScenario(), getEvents());
//					}
//				});
//			}
//		});
//	}
	
	@Override
	public void notifyReplanning(ReplanningEvent event) {
		/*
		 * During the replanning the mode chain of the agents' selected plans
		 * might have been changed. Therefore, we have to ensure that the 
		 * chains are still valid.
		 */
		for (Person person : this.getScenario().getPopulation().getPersons().values()) {
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
		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );


		controller.run();
		
		System.exit(0);
	}
}
