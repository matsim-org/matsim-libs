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

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.mobsim.WithinDayQSim;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.network.WithinDayLinkFactoryImpl;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.InitialIdentifierImplFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.SelectHandledAgentsByProbability;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.InitialReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

/**
 * This Controller should give an Example what is needed to run
 * Simulations with WithinDayReplanning.
 * 
 * The Path to a Config File is needed as Argument to run the
 * Simulation.
 * 
 * Additional Parameters have to be set in the WithinDayControler
 * Class. Here we just add the Knowledge Component.
 * 
 * By default "test/scenarios/berlin/config.xml" should work.
 * 
 * @author Christoph Dobler
 */
public class WithinDayControler extends Controler {

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	protected double pInitialReplanning = 0.0;
	protected double pActEndReplanning = 1.0;
	protected double pLeaveLinkReplanning = 1.0;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 2;

	protected PersonalizableTravelTime travelTime;

	protected InitialIdentifier initialIdentifier;
	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayInitialReplanner initialReplanner;
	protected WithinDayDuringActivityReplanner duringActivityReplanner;
	protected WithinDayDuringLegReplanner duringLegReplanner;

	protected SelectHandledAgentsByProbability selector;
	protected ReplanningManager replanningManager;
	protected WithinDayQSim sim;
	protected FixedOrderSimulationListener fosl;
	
	static final Logger log = Logger.getLogger(WithinDayControler.class);

	public WithinDayControler(String[] args) {
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public WithinDayControler(Config config) {
		super(config);

		setConstructorParameters();
	}

	private void setConstructorParameters() {
		/*
		 * Use MyLinkImpl. They can carry some additional Information like their
		 * TravelTime or VehicleCount.
		 * This is needed to be able to use a LinkVehiclesCounter.
		 */
		this.getNetwork().getFactory().setLinkFactory(new WithinDayLinkFactoryImpl());

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {

		travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(this.scenarioData);
		fosl.addSimulationInitializedListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		fosl.addSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		fosl.addSimulationAfterSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		this.events.addHandler((TravelTimeCollector)travelTime);	// for TravelTimeCollector

//		travelTime = new FreeSpeedTravelTime();

		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);

//		CloneablePlansCalcRoute dijkstraRouter = new CloneablePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCost, travelTime);
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, factory);

		this.initialIdentifier = new InitialIdentifierImplFactory(this.sim).createIdentifier();
		this.selector.addIdentifier(this.initialIdentifier, this.pInitialReplanning);
		this.initialReplanner = new InitialReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.replanningManager.addIntialReplanner(this.initialReplanner);

		ActivityReplanningMap activityReplanningMap = new ActivityReplanningMap();
		this.getEvents().addHandler(activityReplanningMap);
		fosl.addSimulationListener(activityReplanningMap);
		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
		this.selector.addIdentifier(this.duringActivityIdentifier, this.pActEndReplanning);
		this.duringActivityReplanner = new NextLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.replanningManager.addDuringActivityReplanner(this.duringActivityReplanner);

		LinkReplanningMap linkReplanningMap = new LinkReplanningMap();
		this.getEvents().addHandler(linkReplanningMap);
		fosl.addSimulationListener(linkReplanningMap);
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.selector.addIdentifier(this.duringLegIdentifier, this.pLeaveLinkReplanning);
		this.duringLegReplanner = new CurrentLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.replanningManager.addDuringLegReplanner(this.duringLegReplanner);
	}

	@Override
	protected void setUp() {
		/*
		 * SimStepParallelEventsManagerImpl might be moved to org.matsim.
		 * Then this piece of code could be placed in the controller.
		 */
		if (this.events instanceof ParallelEventsManagerImpl) {
			log.info("Replacing ParallelEventsManagerImpl with SimStepParallelEventsManagerImpl. This is needed for Within-Day Replanning.");
			SimStepParallelEventsManagerImpl manager = new SimStepParallelEventsManagerImpl();
			this.fosl.addSimulationAfterSimStepListener(manager);
			this.events = manager;
		}
		
		super.setUp();
	}
	
	@Override
	protected void runMobSim() {
		
		this.replanningManager = new ReplanningManager(numReplanningThreads);

		sim = new WithinDayQSimFactory().createMobsim(this.scenarioData, this.events);

		selector = new SelectHandledAgentsByProbability();
		fosl.addSimulationListener(selector);
		
		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		fosl.addSimulationInitializedListener(replanningManager);
		fosl.addSimulationBeforeSimStepListener(replanningManager);

		sim.addQueueSimulationListeners(fosl);
	
		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		sim.run();
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