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

package playground.wrashid.parkingSearch.withinday;

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.controler.WithinDayControler;
import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.withinday.mobsim.DuringActivityReplanningModule;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.InitialReplanningModule;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;
import playground.christoph.withinday.replanning.WithinDayInitialReplanner;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;

public class MyWithinDayControler extends Controler {

	public MyWithinDayControler(String configFileName) {
		super(configFileName);
	}

	public static void start(String configFilePath){
		final MyWithinDayControler controler = new MyWithinDayControler(configFilePath);
		controler.setOverwriteFiles(true);
		controler.run();
	}


	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 1;

	protected TravelTime travelTime;

	protected ParallelInitialReplanner parallelInitialReplanner;
	protected ParallelDuringActivityReplanner parallelActEndReplanner;
	protected ParallelDuringLegReplanner parallelLeaveLinkReplanner;

	protected InitialIdentifier initialIdentifier;
	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayInitialReplanner initialReplanner;
	protected WithinDayDuringActivityReplanner duringActivityReplanner;
	protected WithinDayDuringLegReplanner duringLegReplanner;

	protected ReplanningManager replanningManager;
	protected WithinDayQSim sim;
	protected FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();

	private static final Logger log = Logger.getLogger(WithinDayControler.class);

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {

		// use dijkstra for replanning (routing)
		travelTime=this.getTravelTimeCalculator();
		PlansCalcRoute dijkstraRouter = new PlansCalcRoute(new PlansCalcRouteConfigGroup(), network, this.createTravelCostCalculator(), travelTime, new DijkstraFactory());




//		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
//		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId());
//		this.initialReplanner.setReplanner(dijkstraRouter);
//		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
//		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);
//

		// use replanning during activity
		this.duringActivityIdentifier = new OldPeopleIdentifier(this.sim);
		this.duringActivityReplanner = new ReplannerOldPeople(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.duringActivityReplanner.setReplanner(dijkstraRouter);
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.parallelActEndReplanner.addWithinDayReplanner(this.duringActivityReplanner);

		this.duringLegIdentifier = new YoungPeopleIdentifier(this.sim);
		this.duringLegReplanner = new ReplannerYoungPeople(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.duringLegReplanner.setReplanner(dijkstraRouter);
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.parallelLeaveLinkReplanner.addWithinDayReplanner(this.duringLegReplanner);
	}

	/*
	 * Initializes the ParallelReplannerModules
	 */
	protected void initParallelReplanningModules()
	{
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads);
		this.parallelActEndReplanner = new ParallelDuringActivityReplanner(numReplanningThreads);
		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads);
	}

	/*
	 * Creates the Handler and Listener Object so that they can be handed over to the Within Day
	 * Replanning Modules (TravelCostWrapper, etc).
	 *
	 * The full initialization of them is done later (we don't have all necessary Objects yet).
	 */
	protected void createHandlersAndListeners()
	{
		replanningManager = new ReplanningManager();
	}

	@Override
	protected void runMobSim()
	{
		createHandlersAndListeners();

		sim = new WithinDayQSim(this.scenarioData, this.events);

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);

		sim.addQueueSimulationListeners(foqsl);

		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();

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





}
