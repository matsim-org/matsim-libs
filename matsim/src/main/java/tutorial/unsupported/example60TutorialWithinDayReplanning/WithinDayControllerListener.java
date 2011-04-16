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
	private SelectHandledAgentsByProbability selector;
	private FixedOrderSimulationListener fosl;
	
	private static final Logger log = Logger.getLogger(WithinDayControllerListener.class);
	
	public WithinDayControllerListener() {
		log.info("Please call setControllerParameters(Controler controller) so configure the Controller.");
	}

	public void setControllerParameters(Controler controller) {
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
		 * Add this as a ControlerListener
		 */
		controller.addControlerListener(this);
		
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
		activityReplanningMap = new ActivityReplanningMap();
		event.getControler().getEvents().addHandler(activityReplanningMap);
		fosl.addSimulationListener(activityReplanningMap);
		linkReplanningMap = new LinkReplanningMap();
		event.getControler().getEvents().addHandler(linkReplanningMap);
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
		int numReplanningThreads = event.getControler().getConfig().global().getNumberOfThreads();
		replanningManager = new ReplanningManager(numReplanningThreads);
		fosl.addSimulationListener(replanningManager);
		selector = new SelectHandledAgentsByProbability();
		fosl.addSimulationListener(selector);
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
		this.selector.addIdentifier(this.initialIdentifier, this.pInitialReplanning);
		this.initialReplanner = new InitialReplannerFactory(sim.getScenario(), sim.getAgentCounter(), router, 1.0).createReplanner();
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.replanningManager.addIntialReplanner(this.initialReplanner);
		
		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
		this.selector.addIdentifier(this.duringActivityIdentifier, this.pDuringActivityReplanning);
		this.duringActivityReplanner = new NextLegReplannerFactory(sim.getScenario(), sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.replanningManager.addDuringActivityReplanner(this.duringActivityReplanner);
		
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.selector.addIdentifier(this.duringLegIdentifier, this.pDuringLegReplanning);
		this.duringLegReplanner = new CurrentLegReplannerFactory(sim.getScenario(), sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.replanningManager.addDuringLegReplanner(this.duringLegReplanner);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		/*
		 * Remove some SimulationListeners
		 */
		fosl.removeSimulationListener(replanningManager);
		fosl.removeSimulationListener(selector);
		replanningManager = null;
		selector = null;
	}

}
