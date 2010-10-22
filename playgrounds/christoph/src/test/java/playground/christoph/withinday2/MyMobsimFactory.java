/* *********************************************************************** *
 * project: org.matsim.*
 * MyMobsimFactory.java
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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.Mobsim;

import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.withinday.mobsim.DuringActivityReplanningModule;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.InitialReplanningModule;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayAgentFactory;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.WithinDayReplanner;
import playground.christoph.withinday.replanning.modules.ReplanningModule;
import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;

/**
 * @author nagel
 *
 */
public class MyMobsimFactory implements MobsimFactory {
	private static final Logger log = Logger.getLogger("dummy");
	private ParallelInitialReplanner parallelInitialReplanner;
	private ParallelDuringActivityReplanner parallelActEndReplanner;
	private ParallelDuringLegReplanner parallelLeaveLinkReplanner;
	private PersonalizableTravelCost travCostCalc;
	private PersonalizableTravelTime travTimeCalc;
	
	MyMobsimFactory( PersonalizableTravelCost travelCostCalculator, PersonalizableTravelTime travelTimeCalculator ) {
		this.travCostCalc = travelCostCalculator ;
		this.travTimeCalc = travelTimeCalculator ;
	}

	@Override
	public Simulation createMobsim(Scenario sc, EventsManager eventsManager) {
		int numReplanningThreads = 1;

		
		Mobsim mobsim = new QSim( sc, eventsManager ) ;
		
		AgentFactory agentFactory = new WithinDayAgentFactory( mobsim ) ;
		mobsim.setAgentFactory(agentFactory) ;
		
		ReplanningManager replanningManager = new ReplanningManager();
		
		// Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		// ensure that they are started in the needed order.
		FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);
		mobsim.addQueueSimulationListeners(foqsl);
		// (essentially, can just imagine the replanningManager as a regular MobsimListener)
		
		List<SimulationListener> simulationListenerList = new ArrayList<SimulationListener>() ;

		log.info("Initialize Parallel Replanning Modules");
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads, simulationListenerList );
		this.parallelActEndReplanner = new ParallelDuringActivityReplanner(numReplanningThreads, simulationListenerList );
		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads, simulationListenerList );
		// these are containers, but they don't do anything by themselves
		
		for ( SimulationListener siml : simulationListenerList ) {
			mobsim.addQueueSimulationListeners( siml ) ;
		}

		log.info("Initialize Replanning Routers");
		initReplanningRouter(sc, mobsim);

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
		
		return mobsim ;
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	private void initReplanningRouter(Scenario sc, Mobsim mobsim ) {

		//		PlansCalcRoute dijkstraRouter = new PlansCalcRoute(new PlansCalcRouteConfigGroup(), network, this.createTravelCostCalculator(), travelTime, new DijkstraFactory());
		AbstractMultithreadedModule routerModule = 
			new ReplanningModule(sc.getConfig(), sc.getNetwork(), this.travCostCalc, this.travTimeCalc, new DijkstraFactory());
		// (ReplanningModule is a wrapper that either returns PlansCalcRoute or MultiModalPlansCalcRoute)



		// replanning while at activity:

		WithinDayReplanner duringActivityReplanner = new OldPeopleReplanner(ReplanningIdGenerator.getNextId(), sc);
		// defines a "doReplanning" method which contains the core of the work
		// as a piece, it re-routes a _future_ leg.  
		
		duringActivityReplanner.setAbstractMultithreadedModule(routerModule);
		// this pretends being a general Plan Algorithm, but I wonder if it can reasonably be anything else but a router?

		duringActivityReplanner.addAgentsToReplanIdentifier(new OldPeopleIdentifier(mobsim));
		// which persons to replan
		
		this.parallelActEndReplanner.addWithinDayReplanner(duringActivityReplanner);
		// I think this just adds the stuff to the threads mechanics (can't say why it is not enough to use the multithreaded
		// module).  kai, oct'10

		

		// replanning while on leg:
		
		WithinDayReplanner duringLegReplanner = new YoungPeopleReplanner(ReplanningIdGenerator.getNextId(), sc);
		// defines a "doReplanning" method which contains the core of the work
		// it replaces the next activity
		// in order to get there, it re-routes the current route
		
		duringLegReplanner.setAbstractMultithreadedModule(routerModule);
		// this pretends being a general Plan Algorithm, but I wonder if it can reasonably be anything else but a router?

		duringLegReplanner.addAgentsToReplanIdentifier(new YoungPeopleIdentifier(mobsim));
		// persons identifier added to replanner

		this.parallelLeaveLinkReplanner.addWithinDayReplanner(duringLegReplanner);
		// I think this just adds the stuff to the threads mechanics (can't say why it is not enough to use the multithreaded
		// module).  kai, oct'10
	}



}
