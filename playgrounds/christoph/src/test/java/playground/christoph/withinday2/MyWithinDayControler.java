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

package playground.christoph.withinday2;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.DijkstraFactory;

import playground.christoph.controler.WithinDayControler;
import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.withinday.mobsim.DuringActivityReplanningModule;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.InitialReplanningModule;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.modules.ReplanningModule;
import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;

class MyWithinDayControler extends Controler {

	static void start(Config config) {
		final MyWithinDayControler controler = new MyWithinDayControler(config);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	static void start(String configFilePath) {
		final MyWithinDayControler controler = new MyWithinDayControler(configFilePath);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 1;

	protected ParallelInitialReplanner parallelInitialReplanner;


	protected ParallelDuringActivityReplanner parallelActEndReplanner;

	protected ParallelDuringLegReplanner parallelLeaveLinkReplanner;
	protected ReplanningManager replanningManager;
	protected WithinDayQSim sim;

	private static final Logger log = Logger.getLogger(WithinDayControler.class);
	MyWithinDayControler(Config config) {
		super(config);
	}

	MyWithinDayControler(String configFileName) {
		super(configFileName);
	}

	@Override
	protected void runMobSim()
	{
		createHandlersAndListeners();
		// initializes "replanningManager"

		sim = new WithinDayQSim(this.scenarioData, this.events);
		// a QSim with two differences:
		// (1) uses WithinDayAgentFactory instead of the regular agent factory
		// (2) offers "rescheduleActivityEnd" although I am not sure that this is still needed (after some modifications
		//     that happened in the meantime)

		// Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		// ensure that they are started in the needed order.
		FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);
		sim.addQueueSimulationListeners(foqsl);
		// essentially, can just imagine the replanningManager as a regular MobsimListener

		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();
		// these are containers, but they don't do anything by themselves

		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		InitialReplanningModule initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		DuringActivityReplanningModule actEndReplanning = new DuringActivityReplanningModule(parallelActEndReplanner);
		DuringLegReplanningModule leaveLinkReplanning = new DuringLegReplanningModule(parallelLeaveLinkReplanner);

		replanningManager.setInitialReplanningModule(initialReplanningModule);
		replanningManager.setActEndReplanningModule(actEndReplanning);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);

		//just activitate replanning during an activity
		replanningManager.doActEndReplanning(true);
		replanningManager.doInitialReplanning(false);
		replanningManager.doLeaveLinkReplanning(true);

		sim.run();
	}
	
	// ============================================================================================================================
	// private method only below here

	/*
	 * Creates the Handler and Listener Object so that they can be handed over to the Within Day
	 * Replanning Modules (TravelCostWrapper, etc).
	 *
	 * The full initialization of them is done later (we don't have all necessary Objects yet).
	 */
	private void createHandlersAndListeners()
	{
		replanningManager = new ReplanningManager();
	}

	/*
	 * Initializes the ParallelReplannerModules
	 */
	private void initParallelReplanningModules()
	{
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads, this);
		this.parallelActEndReplanner = new ParallelDuringActivityReplanner(numReplanningThreads, this);
		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads, this);
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	private void initReplanningRouter() {

		//		PlansCalcRoute dijkstraRouter = new PlansCalcRoute(new PlansCalcRouteConfigGroup(), network, this.createTravelCostCalculator(), travelTime, new DijkstraFactory());
		AbstractMultithreadedModule router = 
			new ReplanningModule(config, network, this.createTravelCostCalculator(), this.getTravelTimeCalculator(), new DijkstraFactory());
		// ReplanningModule is a wrapper that either returns PlansCalcRoute or MultiModalPlansCalcRoute

//		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
//		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId());
//		this.initialReplanner.setReplanner(dijkstraRouter);
//		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
//		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);


		// replanning while at activity:

		WithinDayReplanner duringActivityReplanner = new ReplannerOldPeople(ReplanningIdGenerator.getNextId(), this.scenarioData);
		// defines a "doReplanning" method which contains the core of the work
		// as a piece, it re-routes a _future_ leg.  
		
		duringActivityReplanner.setAbstractMultithreadedModule(router);
		// this pretends being a general Plan Algorithm, but I wonder if it can reasonably be anything else but a router?

		duringActivityReplanner.addAgentsToReplanIdentifier(new OldPeopleIdentifier(this.sim));
		// which persons to replan
		
		this.parallelActEndReplanner.addWithinDayReplanner(duringActivityReplanner);
		// I think this just adds the stuff to the threads mechanics (can't say why it is not enough to use the multithreaded
		// module).  kai, oct'10

		
		// replanning while on leg:
		
		WithinDayReplanner duringLegReplanner = new ReplannerYoungPeople(ReplanningIdGenerator.getNextId(), this.scenarioData);
		// defines a "doReplanning" method which contains the core of the work
		// it replaces the next activity
		// in order to get there, it re-routes the current route
		
		duringLegReplanner.setAbstractMultithreadedModule(router);
		// this pretends being a general Plan Algorithm, but I wonder if it can reasonably be anything else but a router?

		duringLegReplanner.addAgentsToReplanIdentifier(new YoungPeopleIdentifier(this.sim));
		// persons identifier added to replanner

		this.parallelLeaveLinkReplanner.addWithinDayReplanner(duringLegReplanner);
		// I think this just adds the stuff to the threads mechanics (can't say why it is not enough to use the multithreaded
		// module).  kai, oct'10
	}





}
