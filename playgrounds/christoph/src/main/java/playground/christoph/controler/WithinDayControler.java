/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.controler;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.mobsim.queuesim.QueueLink;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.mobsim.queuesim.QueueVehicle;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;
//import org.matsim.core.events.parallelEventsHandler.WithinDayParallelEventsManagerImpl;

import playground.christoph.events.LinkReplanningMap;
import playground.christoph.events.LinkVehiclesCounter2;
import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.network.MyLinkFactoryImpl;
import playground.christoph.replanning.MyStrategyManagerConfigLoader;
import playground.christoph.replanning.TravelTimeCollector;
import playground.christoph.router.FastDijkstraFactory;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.router.costcalculators.SystemOptimalTravelCostCalculator;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;
import playground.christoph.withinday.mobsim.DuringActivityReplanningModule;
import playground.christoph.withinday.mobsim.InitialReplanningModule;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.ReplanningQueueSimulation;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.CurrentLegReplanner;
import playground.christoph.withinday.replanning.InitialReplanner;
import playground.christoph.withinday.replanning.NextLegReplanner;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;
import playground.christoph.withinday.replanning.WithinDayInitialReplanner;
import playground.christoph.withinday.replanning.identifiers.ActivityEndIdentifier;
import playground.christoph.withinday.replanning.identifiers.InitialIdentifierImpl;
import playground.christoph.withinday.replanning.identifiers.LeaveLinkIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;

/**
 * This Controler should give an Example what is needed to run
 * Simulations with WithinDayReplanning.
 *
 * The Path to a Config File is needed as Argument to run the
 * Simulation.
 *
 * By default "../matsim/test/scenarios/berlin/config.xml" should work.
 *
 * @author Christoph Dobler
 */

//mysimulations/kt-zurich/configIterative.xml

public class WithinDayControler extends Controler {

	protected ArrayList<SelectNodes> nodeSelectors;

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	protected double pInitialReplanning = 0.0;
	protected double pActEndReplanning = 0.0;
	protected double pLeaveLinkReplanning = 0.0;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 8;

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
	
	protected LinkVehiclesCounter2 linkVehiclesCounter;
	protected LinkReplanningMap linkReplanningMap;
	protected ReplanningManager replanningManager;
	protected ReplanningQueueSimulation sim;
	protected FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();

	private static final Logger log = Logger.getLogger(WithinDayControler.class);

	public WithinDayControler(String[] args)
	{
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public WithinDayControler(Config config)
	{
		super(config);

		setConstructorParameters();
	}

	private void setConstructorParameters()
	{
		/*
		 * Use MyLinkImpl. They can carry some additional Information like their
		 * TravelTime or VehicleCount.
		 * This is needed to be able to use a LinkVehiclesCounter.
		 */
		this.getNetwork().getFactory().setLinkFactory(new MyLinkFactoryImpl());

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {

		/*
		 * Calculate the TravelTime based on the actual load of the links. Use only
		 * the TravelTime to find the LeastCostPath.
		 */
//		travelTime = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
//		// fully initialize & add LinkVehiclesCounter
//		linkVehiclesCounter.setQueueNetwork(sim.getQueueNetwork());	// for KnowledgeTravelTimeCalculator
//		foqsl.addQueueSimulationInitializedListener(linkVehiclesCounter);	// for KnowledgeTravelTimeCalculator
//		foqsl.addQueueSimulationAfterSimStepListener(linkVehiclesCounter);	// for KnowledgeTravelTimeCalculator
//		this.events.addHandler(linkVehiclesCounter);	// for KnowledgeTravelTimeCalculator

//		TravelTimeEstimatorHandlerAndListener handlerAndListener = new TravelTimeEstimatorHandlerAndListener(population, network, 60, 86400 * 2);
//		travelTime = new TravelTimeEstimator(handlerAndListener);
//		foqsl.addQueueSimulationInitializedListener((TravelTimeEstimatorHandlerAndListener)handlerAndListener); // for TravelTimeEstimator
//		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeEstimatorHandlerAndListener)handlerAndListener);	// for TravelTimeEstimator
//		this.events.addHandler((TravelTimeEstimatorHandlerAndListener)handlerAndListener);	// for TravelTimeEstimator

		travelTime = new TravelTimeCollector(network);
		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		foqsl.addQueueSimulationAfterSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		this.events.addHandler((TravelTimeCollector)travelTime);	// for TravelTimeCollector

		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
//		SystemOptimalTravelCostCalculator travelCost = new SystemOptimalTravelCostCalculator(travelTime);

		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCost, travelTime, new FastDijkstraFactory());
//		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCost, travelTime);
		
		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId());
		this.initialReplanner.setReplanner(dijkstraRouter);
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);
		
		this.duringActivityIdentifier = new ActivityEndIdentifier(this.sim);
		this.duringActivityReplanner = new NextLegReplanner(ReplanningIdGenerator.getNextId());
		this.duringActivityReplanner.setReplanner(dijkstraRouter);
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.parallelActEndReplanner.addWithinDayReplanner(this.duringActivityReplanner);
		
		this.duringLegIdentifier = new LeaveLinkIdentifier(this.linkReplanningMap);
		this.duringLegReplanner = new CurrentLegReplanner(ReplanningIdGenerator.getNextId(), this.network);
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
		linkVehiclesCounter = new LinkVehiclesCounter2();
		linkReplanningMap = new LinkReplanningMap();
		replanningManager = new ReplanningManager();
	}

	@Override
	protected void runMobSim()
	{	
		createHandlersAndListeners();
		
		sim = new ReplanningQueueSimulation(this.network, this.population, this.events);
	
		ReplanningFlagInitializer rfi = new ReplanningFlagInitializer(this);
		foqsl.addQueueSimulationInitializedListener(rfi);
				
//		if (this.events instanceof WithinDayParallelEventsManagerImpl)
//		{
//			foqsl.addQueueSimulationAfterSimStepListener((WithinDayParallelEventsManagerImpl)this.events);			
//		}

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);
		
		sim.addQueueSimulationListeners(foqsl);

		// fully initialize & add LinkReplanningMap
		linkReplanningMap.setQueueNetwork(sim.getQueueNetwork());
		this.events.addHandler(linkReplanningMap);

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

		sim.run();
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		MyStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}


	public static class ReplanningFlagInitializer implements SimulationInitializedListener{

		protected WithinDayControler withinDayControler;
		protected Map<Id, WithinDayPersonAgent> withinDayPersonAgents;
				
		protected int noReplanningCounter = 0;
		protected int initialReplanningCounter = 0;
		protected int actEndReplanningCounter = 0;
		protected int leaveLinkReplanningCounter = 0;
		
		public ReplanningFlagInitializer(WithinDayControler controler)
		{
			this.withinDayControler = controler;
		}
		
		@Override
		public void notifySimulationInitialized(SimulationInitializedEvent e)
		{
			collectAgents((QueueSimulation)e.getQueueSimulation());
			setReplanningFlags();
			
		}
		
		protected void setReplanningFlags()
		{
			noReplanningCounter = 0;
			initialReplanningCounter = 0;
			actEndReplanningCounter = 0;
			leaveLinkReplanningCounter = 0;

			Random random = MatsimRandom.getLocalInstance();
			
			for (WithinDayPersonAgent withinDayPersonAgent : this.withinDayPersonAgents.values())
			{
				double probability;
				boolean noReplanning = true;

				probability = random.nextDouble();
				if (probability > withinDayControler.pInitialReplanning) ;
				else
				{
					withinDayPersonAgent.addWithinDayReplanner(withinDayControler.initialReplanner);
					noReplanning = false;
					initialReplanningCounter++;
				}

				// No Replanning
				probability = random.nextDouble();
				if (probability > withinDayControler.pActEndReplanning) ;
				else
				{
					withinDayPersonAgent.addWithinDayReplanner(withinDayControler.duringActivityReplanner);
					noReplanning = false;
					actEndReplanningCounter++;
				}

				// No Replanning
				probability = random.nextDouble();
				if (probability > withinDayControler.pLeaveLinkReplanning) ;
				else
				{
					withinDayPersonAgent.addWithinDayReplanner(withinDayControler.duringLegReplanner);
					noReplanning = false;
					leaveLinkReplanningCounter++;
				}

				// if non of the Replanning Modules was activated
				if (noReplanning) noReplanningCounter++;

				// (de)activate replanning if they are not needed
				if (initialReplanningCounter == 0) withinDayControler.replanningManager.doInitialReplanning(false);
				else withinDayControler.replanningManager.doInitialReplanning(true);
				
				if (actEndReplanningCounter == 0) withinDayControler.replanningManager.doActEndReplanning(false);
				else withinDayControler.replanningManager.doActEndReplanning(true);

				if (leaveLinkReplanningCounter == 0) withinDayControler.replanningManager.doLeaveLinkReplanning(false);
				else withinDayControler.replanningManager.doLeaveLinkReplanning(true);
			}

			log.info("Initial Replanning Probability: " + withinDayControler.pInitialReplanning);
			log.info("Act End Replanning Probability: " + withinDayControler.pActEndReplanning);
			log.info("Leave Link Replanning Probability: " + withinDayControler.pLeaveLinkReplanning);

			double numPersons = withinDayControler.population.getPersons().size();
			log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
			log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
			log.info(actEndReplanningCounter + " persons replan their plans after an activity (" + actEndReplanningCounter / numPersons * 100.0 + "%)");
			log.info(leaveLinkReplanningCounter + " persons replan their plans at each node (" + leaveLinkReplanningCounter / numPersons * 100.0 + "%)");
		}
		
		protected void collectAgents(QueueSimulation sim)
		{
			this.withinDayPersonAgents = new TreeMap<Id, WithinDayPersonAgent>();
			
			for (QueueLink queueLink : sim.getQueueNetwork().getLinks().values())
			{
				for (QueueVehicle queueVehicle : queueLink.getAllVehicles())
				{
					WithinDayPersonAgent withinDayPersonAgent = (WithinDayPersonAgent) queueVehicle.getDriver();
					this.withinDayPersonAgents.put(withinDayPersonAgent.getPerson().getId(), withinDayPersonAgent);
				}
			}
		}
		
	}
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final WithinDayControler controler = new WithinDayControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

}
