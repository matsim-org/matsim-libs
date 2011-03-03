/* *********************************************************************** *
 * project: org.matsim.*
 * TutorialWithinDayController.java
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

package tutorial.unsupported.example60TutorialWithinDayReplanning;

import org.apache.log4j.Logger;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.evacuation.run.EvacuationQSimControler;
import org.matsim.withinday.controller.ReplanningFlagInitializer;
import org.matsim.withinday.mobsim.DuringActivityReplanningModule;
import org.matsim.withinday.mobsim.DuringLegReplanningModule;
import org.matsim.withinday.mobsim.InitialReplanningModule;
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
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import org.matsim.withinday.replanning.parallel.ParallelDuringLegReplanner;
import org.matsim.withinday.replanning.parallel.ParallelInitialReplanner;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.InitialReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

/**
 * This class should give an example what is needed to run
 * simulations with WithinDayReplanning.
 * 
 * The Path to a config file is needed as argument to run the
 * simulation.
 * 
 * By default
 * "./examples/evacuation-tutorial/withinDayEvacuationConf.xml"
 * should work.
 * 
 * @author cdobler
 */
public class TutorialWithinDayController extends EvacuationQSimControler {
	
	/**
	 * Define the probability that an Agent uses the replanning 
	 * strategy. It is possible to assign multiple strategies 
	 * to the agents.
	 */
	private double pInitialReplanning = 0.0;
	private double pDuringActivityReplanning = 0.0;
	private double pDuringLegReplanning = 0.0;

	/**
	 * Define the objects that are needed for the replanning.
	 */
	private ParallelInitialReplanner parallelInitialReplanner;
	private ParallelDuringActivityReplanner parallelActEndReplanner;
	private ParallelDuringLegReplanner parallelLeaveLinkReplanner;
	private InitialIdentifier initialIdentifier;
	private DuringActivityIdentifier duringActivityIdentifier;
	private DuringLegIdentifier duringLegIdentifier;
	private WithinDayInitialReplanner initialReplanner;
	private WithinDayDuringActivityReplanner duringActivityReplanner;
	private WithinDayDuringLegReplanner duringLegReplanner;

	private ReplanningManager replanningManager;
	private ReplanningFlagInitializer rfi;
	private WithinDayQSim sim;
	private FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();

	private static final Logger log = Logger.getLogger(TutorialWithinDayController.class);

	public TutorialWithinDayController(String[] args) {
		super(args);

		setConstructorParameters();
	}

	/**
	 * Use some factories, that have additional within day replanning
	 * capabilities.
	 */
	private void setConstructorParameters() {
		/**
		 * Use WithinDayLinkImpls. They can carry some additional information
		 * like their current TravelTime.
		 */
		this.getNetwork().getFactory().setLinkFactory(new WithinDayLinkFactoryImpl());

		/**
		 * Use a Scoring Function, that only scores the travel times!
		 */
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	/**
	 * Initialize the within day replanning modules.
	 */
	private void initReplanningModules() {
		/**
		 * Create and initialize the travel time calculator which is used by
		 * the replanning modules.
		 * Here we use a TravelTimeCollector which collects the travel times for
		 * each link and returns the average travel time from the past few minutes.
		 * It has to be registered as SimulationListener and as an EventsHandler.
		 */
		PersonalizableTravelTime travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(this.scenarioData);
		fosl.addSimulationListener((TravelTimeCollector)travelTime);
		this.events.addHandler((TravelTimeCollector)travelTime);

		/**
		 * Create and initialize a travel cost calculator which takes the travel
		 * times as travel costs.
		 */
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);

		/**
		 * Create a within day replanning module which is based on an
		 * AbstractMultithreadedModule.
		 */
		LeastCostPathCalculatorFactory factory = new DijkstraFactory();
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, factory);
		
		/**
		 * Create and initialize an ActivityReplanningMap and a LinkReplanningMap.
		 * They identify, when an agent is going to end an activity and when an
		 * agent might be able to leave a link.
		 */
		ActivityReplanningMap activityReplanningMap = new ActivityReplanningMap(this.getEvents());
		fosl.addSimulationListener(activityReplanningMap);
		LinkReplanningMap linkReplanningMap = new LinkReplanningMap(this.getEvents());
		fosl.addSimulationListener(linkReplanningMap);
		
		/**
		 * Create and identify the within day replanners and identifiers which select
		 * the agents that should adapt their plans.
		 */
		this.initialIdentifier = new InitialIdentifierImplFactory(this.sim).createIdentifier();
		this.initialReplanner = new InitialReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);
		this.rfi.addInitialReplanner(this.initialReplanner.getId(), this.pInitialReplanning);
		
		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
		this.duringActivityReplanner = new NextLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.parallelActEndReplanner.addWithinDayReplanner(this.duringActivityReplanner);
		this.rfi.addDuringActivityReplanner(this.duringActivityReplanner.getId(), this.pDuringActivityReplanning);
		
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.duringLegReplanner = new CurrentLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.parallelLeaveLinkReplanner.addWithinDayReplanner(this.duringLegReplanner);
		this.rfi.addDuringLegReplanner(this.duringLegReplanner.getId(), this.pDuringLegReplanning);
	}

	/**
	 * Initializes the ParallelReplannerModules
	 */
	private void initParallelReplanningModules() {
		
		/**
		 * Use the same number of parallel threads for the within day replanning
		 * as for the between-iterations replanning.
		 */
		int numReplanningThreads = this.config.global().getNumberOfThreads();
		
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads, this);
		this.parallelActEndReplanner = new ParallelDuringActivityReplanner(numReplanningThreads, this);
		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads, this);
	}

	@Override
	protected void setUp() {
		/*
		 * This piece of code might be moved to the controler.
		 * 
		 * Parallel events handling cannot be used with within day replanning, because
		 * it does not ensure that all events are processed, when the replanning
		 * modules are executed. Therefore we use a SimStepParallelEventsManagerImpl 
		 * instead. It is an extended version of a parallel events handler which
		 * ensures that all events are handled before the simulation can continue with
		 * a next time step.
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
		/*
		 * Create a new WithinDayQSim and register the FixedOrderSimulationListener
		 * as a SimulationListener.
		 */
		sim = new WithinDayQSimFactory().createMobsim(this.scenarioData, this.events);
		sim.addQueueSimulationListeners(fosl);

		/*
		 * Crate and initialize a ReplanningManager and a ReplaningFlagInitializer.
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		replanningManager = new ReplanningManager();
		fosl.addSimulationListener(replanningManager);
		rfi = new ReplanningFlagInitializer(replanningManager);
		fosl.addSimulationListener(rfi);
		
		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();

		log.info("Initialize Replanning Routers");
		initReplanningModules();

		InitialReplanningModule initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		DuringActivityReplanningModule actEndReplanning = new DuringActivityReplanningModule(parallelActEndReplanner);
		DuringLegReplanningModule leaveLinkReplanning = new DuringLegReplanningModule(parallelLeaveLinkReplanner);

		replanningManager.setInitialReplanningModule(initialReplanningModule);
		replanningManager.setActEndReplanningModule(actEndReplanning);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);

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
			final TutorialWithinDayController controller = new TutorialWithinDayController(args);
			controller.setOverwriteFiles(true);
			controller.run();
		}
		System.exit(0);
	}
	
}