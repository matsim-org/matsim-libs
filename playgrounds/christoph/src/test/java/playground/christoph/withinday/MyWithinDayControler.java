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
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeFactoryWrapper;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.christoph.controler.WithinDayControler;

class MyWithinDayControler extends Controler {

	MyWithinDayControler(String configFileName) {
		super(configFileName);
	}

	MyWithinDayControler(Config config) {
		super(config);
	}

	static void start(String configFilePath) {
		final MyWithinDayControler controler = new MyWithinDayControler(configFilePath);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	static void start(Config config) {
		final MyWithinDayControler controler = new MyWithinDayControler(config);
		controler.setOverwriteFiles(true);
		controler.run();
	}


	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 5;

	protected ReplanningManager replanningManager;
	protected QSim sim;

	private static final Logger log = Logger.getLogger(WithinDayControler.class);

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	private void initReplanningRouter() {

		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sim.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		
		TravelTimeFactoryWrapper wrapper = new TravelTimeFactoryWrapper(this.getTravelTimeCalculator());
		AbstractMultithreadedModule router = new ReplanningModule(config, network, this.getTravelDisutilityFactory(), wrapper, new DijkstraFactory(), routeFactory);
		// ReplanningModule is a wrapper that either returns PlansCalcRoute or MultiModalPlansCalcRoute
		// this pretends being a general Plan Algorithm, but I wonder if it can reasonably be anything else but a router?

//		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
//		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId());
//		this.initialReplanner.setReplanner(dijkstraRouter);
//		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
//		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);


		// replanning while at activity:

		WithinDayDuringActivityReplannerFactory duringActivityReplannerFactory = new ReplannerOldPeopleFactory(this.scenarioData, replanningManager, router, 1.0); 
		// defines a "doReplanning" method which contains the core of the work
		// as a piece, it re-routes a _future_ leg.  
		
		duringActivityReplannerFactory.addIdentifier(new OldPeopleIdentifierFactory(this.sim).createIdentifier());
		// which persons to replan
		
		this.replanningManager.addDuringActivityReplannerFactory(duringActivityReplannerFactory);
		// I think this just adds the stuff to the threads mechanics (can't say why it is not enough to use the multithreaded
		// module).  kai, oct'10

		
		// replanning while on leg:
		
		WithinDayDuringLegReplannerFactory duringLegReplannerFactory = new ReplannerYoungPeopleFactory(this.scenarioData, replanningManager, router, 1.0);
		// defines a "doReplanning" method which contains the core of the work
		// it replaces the next activity
		// in order to get there, it re-routes the current route
		
		duringLegReplannerFactory.addIdentifier(new YoungPeopleIdentifierFactory(this.sim).createIdentifier());
		// persons identifier added to replanner

		this.replanningManager.addDuringLegReplannerFactory(duringLegReplannerFactory);
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
		replanningManager = new ReplanningManager();
		replanningManager.initializeReplanningModules(numReplanningThreads);
	}

	@Override
	protected void runMobSim() {
		createHandlersAndListeners();
		// initializes "replanningManager"

		sim = WithinDayQSimFactory.createWithinDayQSim(this.scenarioData, this.events);
		// a QSim with two differences:
		// (1) uses WithinDayAgentFactory instead of the regular agent factory
		// (2) offers "rescheduleActivityEnd" although I am not sure that this is still needed (after some modifications
		//     that happened in the meantime)

		// Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		// ensure that they are started in the needed order.
		FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
		fosl.addSimulationBeforeSimStepListener(replanningManager);
		sim.addQueueSimulationListeners(fosl);
		// essentially, can just imagine the replanningManager as a regular MobsimListener

		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		//just activitate replanning during an activity
		replanningManager.doDuringActivityReplanning(true);
		replanningManager.doInitialReplanning(false);
		replanningManager.doDuringLegReplanning(true);

		sim.run();
	}

}