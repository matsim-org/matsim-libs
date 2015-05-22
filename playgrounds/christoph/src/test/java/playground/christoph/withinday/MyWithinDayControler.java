/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.withinday;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

class MyWithinDayControler extends Controler {

	MyWithinDayControler(String configFileName) {
		super(configFileName);
		throw new RuntimeException(Gbl.RUN_MOB_SIM_NO_LONGER_POSSIBLE) ;
	}

	MyWithinDayControler(Config config) {
		super(config);
		throw new RuntimeException(Gbl.RUN_MOB_SIM_NO_LONGER_POSSIBLE) ;
	}

	static void start(String configFilePath) {
		final MyWithinDayControler controler = new MyWithinDayControler(configFilePath);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}

	static void start(Config config) {
		final MyWithinDayControler controler = new MyWithinDayControler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}


	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 5;

	protected WithinDayEngine withinDayEngine;
	protected QSim sim;

	private static final Logger log = Logger.getLogger(MyWithinDayControler.class);

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	private void initReplanningRouter() {

		RoutingContext routingContext = new RoutingContextImpl(this.createTravelDisutilityCalculator(), this.getLinkTravelTimes());

		TripRouterFactory tripRouterFactory = new TripRouterFactoryBuilderWithDefaults().build(this.getScenario());
		// replanning while at activity:

		WithinDayDuringActivityReplannerFactory duringActivityReplannerFactory = new ReplannerOldPeopleFactory(this.getScenario(), withinDayEngine,
				tripRouterFactory, routingContext); 
		// defines a "doReplanning" method which contains the core of the work
		// as a piece, it re-routes a _future_ leg.  
		
		duringActivityReplannerFactory.addIdentifier(new OldPeopleIdentifierFactory(this.sim).createIdentifier());
		// which persons to replan
		
		this.withinDayEngine.addDuringActivityReplannerFactory(duringActivityReplannerFactory);
		// I think this just adds the stuff to the threads mechanics (can't say why it is not enough to use the multithreaded
		// module).  kai, oct'10

		
		// replanning while on leg:
		
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory = new ReplannerYoungPeopleFactory(this.getScenario(), withinDayEngine,
				tripRouterFactory, routingContext);
		// defines a "doReplanning" method which contains the core of the work
		// it replaces the next activity
		// in order to get there, it re-routes the current route
		
		duringLegReplannerFactory.addIdentifier(new YoungPeopleIdentifierFactory(this.sim).createIdentifier());
		// persons identifier added to replanner

		this.withinDayEngine.addDuringLegReplannerFactory(duringLegReplannerFactory);
		// I think this just adds the stuff to the threads mechanics (can't say why it is not enough to use the multithreaded
		// module).  kai, oct'10
	}

	/*
	 * Creates the Handler and Listener Object so that they can be handed over to the Within Day
	 * Replanning Modules (TravelCostWrapper, etc).
	 *
	 * The full initialization of them is done later (we don't have all necessary Objects yet).
	 */
	private void createHandlersAndListeners() {
		withinDayEngine = new WithinDayEngine(this.getEvents());
		withinDayEngine.initializeReplanningModules(numReplanningThreads);
	}

//	@Override
//	protected void runMobSim() {
//		createHandlersAndListeners();
//		// initializes "replanningManager"
//
////		sim = new WithinDayQSimFactory(withinDayEngine).createMobsim(this.scenarioData, this.events);
//		
//		// a QSim with two differences:
//		// (1) uses WithinDayAgentFactory instead of the regular agent factory
//		// (2) offers "rescheduleActivityEnd" although I am not sure that this is still needed (after some modifications
//		//     that happened in the meantime)
//
//		// Use a FixedOrderQueueSimulationListener to bundle the Listeners and
//		// ensure that they are started in the needed order.
//		FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
//		sim.addQueueSimulationListeners(fosl);
//		// essentially, can just imagine the replanningManager as a regular MobsimListener
//
//		log.info("Initialize Replanning Routers");
//		initReplanningRouter();
//
//		//just activitate replanning during an activity
//		withinDayEngine.doDuringActivityReplanning(true);
//		withinDayEngine.doInitialReplanning(false);
//		withinDayEngine.doDuringLegReplanning(true);
//
//		sim.run();
//	}

}