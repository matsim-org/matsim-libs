/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayControllerListener.java
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
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.withinday.controller.ReplanningFlagInitializer;
import org.matsim.withinday.mobsim.DuringActivityReplanningModule;
import org.matsim.withinday.mobsim.DuringLegReplanningModule;
import org.matsim.withinday.mobsim.InitialReplanningModule;
import org.matsim.withinday.mobsim.ReplanningManager;
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

public class WithinDayControllerListener implements StartupListener, BeforeMobsimListener, AfterMobsimListener, SimulationInitializedListener {

	/**
	 * Define the probability that an Agent uses the replanning 
	 * strategy. It is possible to assign multiple strategies 
	 * to the agents.
	 */
	private double pInitialReplanning = 0.0;
	private double pDuringActivityReplanning = 1.0;
	private double pDuringLegReplanning = 1.0;
	
	private int lastIteration = 0;
	
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

	private ActivityReplanningMap activityReplanningMap;
	private LinkReplanningMap linkReplanningMap;
	private PersonalizableTravelTime travelTime;
	private OnlyTimeDependentTravelCostCalculator travelCost;
	private ReplanningManager replanningManager;
	private ReplanningFlagInitializer rfi;
	private FixedOrderSimulationListener fosl;
	
	private static final Logger log = Logger.getLogger(WithinDayControllerListener.class);
	
	public WithinDayControllerListener(Controler controller) {
		setControllerParameters(controller);
		
		/*
		 * Add this as a ControllerListener
		 */
		controller.addControlerListener(this);
		// yyyy this is aside effect.  Is this what we want?
		// It seems to me that we had agreed to avoid side effects.  kai, apr'11

	}

	private void setControllerParameters(Controler controller) {
		/*
		 * Use WithinDayLinkImpls. They can carry some additional information
		 * like their current TravelTime.
		 */
		controller.getNetwork().getFactory().setLinkFactory(new WithinDayLinkFactoryImpl());

		/*
		 * Use a Scoring Function, that only scores the travel times!
		 */
		controller.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
		
		/*
		 * Use a WithinDayQSimFactory
		 */
		controller.setMobsimFactory(new WithinDayQSimFactory());
		
		/*
		 * Create and register a FixedOrderQueueSimulationListener
		 */
		fosl = new FixedOrderSimulationListener();
		controller.getQueueSimulationListener().add(fosl);
		
		/*
		 * Register this as SimulationListener
		 */
		fosl.addSimulationListener(this);		
	}
	
	/*
	 * At this point, the Controller has already initialized its EventsHandler.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		
		/*
		 * Set the number of iterations
		 */
		event.getControler().getConfig().controler().setLastIteration(lastIteration);
		
		/*
		 * Throw an Exception if an unsupported EventsManager is used
		 */
		if (event.getControler().getEvents() instanceof ParallelEventsManagerImpl) {
			throw new RuntimeException("Using a ParallelEventsManagerImpl is not supported by the WithinDay Replanning Code. Please use an EventsMangerImpl or a SimStepParallelEventsManagerImpl.");
		}
		
		/*
		 * Create and initialize an ActivityReplanningMap and a LinkReplanningMap.
		 * They identify, when an agent is going to end an activity and when an
		 * agent might be able to leave a link.
		 */
		activityReplanningMap = new ActivityReplanningMap(event.getControler().getEvents());
		fosl.addSimulationListener(activityReplanningMap);
		linkReplanningMap = new LinkReplanningMap(event.getControler().getEvents());
		fosl.addSimulationListener(linkReplanningMap);
		
		/*
		 * Create and initialize the travel time calculator which is used by
		 * the replanning modules.
		 * Here we use a TravelTimeCollector which collects the travel times for
		 * each link and returns the average travel time from the past few minutes.
		 * It has to be registered as SimulationListener and as an EventsHandler.
		 */
		travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(event.getControler().getScenario());
		fosl.addSimulationListener((TravelTimeCollector)travelTime);
		event.getControler().getEvents().addHandler((TravelTimeCollector)travelTime);

		/*
		 * Create and initialize a travel cost calculator which takes the travel
		 * times as travel costs.
		 */
		travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
	}
	
	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
		/*
		 * Crate and initialize a ReplanningManager and a ReplaningFlagInitializer.
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the correct order.
		 */
		replanningManager = new ReplanningManager();
		fosl.addSimulationListener(replanningManager);
		rfi = new ReplanningFlagInitializer(replanningManager);
		fosl.addSimulationListener(rfi);
		
		/*
		 * Create ParallelReplanners. They are registered as SimulationListeners.
		 */
		log.info("Initialize Parallel Replanning Modules");
		int numReplanningThreads = event.getControler().getConfig().global().getNumberOfThreads();
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads, event.getControler().getQueueSimulationListener());
		this.parallelActEndReplanner = new ParallelDuringActivityReplanner(numReplanningThreads, event.getControler().getQueueSimulationListener());
		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads, event.getControler().getQueueSimulationListener());
		
		/*
		 * We create and set the Replanning Modules for each iteration.
		 * This could be varied, e.g. using the WithinDay Modules only
		 * in the first iteration.
		 */
		InitialReplanningModule initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		DuringActivityReplanningModule actEndReplanning = new DuringActivityReplanningModule(parallelActEndReplanner);
		DuringLegReplanningModule leaveLinkReplanning = new DuringLegReplanningModule(parallelLeaveLinkReplanner);

		replanningManager.setInitialReplanningModule(initialReplanningModule);
		replanningManager.setActEndReplanningModule(actEndReplanning);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		
		log.info("Initialize Replanning Routers");
		QSim sim = (QSim) e.getQueueSimulation();
				
		/**
		 * Create a within day replanning module which is based on an
		 * AbstractMultithreadedModule.
		 */
		LeastCostPathCalculatorFactory factory = new DijkstraFactory();
		AbstractMultithreadedModule router = new ReplanningModule(sim.getScenario().getConfig(), sim.getScenario().getNetwork(), travelCost, travelTime, factory);
		
		/**
		 * Create and identify the within day replanners and identifiers which select
		 * the agents that should adapt their plans.
		 */
		this.initialIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
		this.initialReplanner = new InitialReplannerFactory(sim.getScenario(), sim.getAgentCounter(), router, 1.0).createReplanner();
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);
		this.rfi.addInitialReplanner(this.initialReplanner.getId(), this.pInitialReplanning);
		
		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
		this.duringActivityReplanner = new NextLegReplannerFactory(sim.getScenario(), sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.parallelActEndReplanner.addWithinDayReplanner(this.duringActivityReplanner);
		this.rfi.addDuringActivityReplanner(this.duringActivityReplanner.getId(), this.pDuringActivityReplanning);
		
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.duringLegReplanner = new CurrentLegReplannerFactory(sim.getScenario(), sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.parallelLeaveLinkReplanner.addWithinDayReplanner(this.duringLegReplanner);
		this.rfi.addDuringLegReplanner(this.duringLegReplanner.getId(), this.pDuringLegReplanning);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		/*
		 * Remove some SimulationListeners
		 */
		fosl.removeSimulationListener(replanningManager);
		fosl.removeSimulationListener(rfi);
		replanningManager = null;
		rfi = null;
	}

}
