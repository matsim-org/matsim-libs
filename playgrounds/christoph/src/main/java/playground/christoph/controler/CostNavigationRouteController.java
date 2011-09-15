/* *********************************************************************** *
 * project: org.matsim.*
 * CostNavigationRouteController.java
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

package playground.christoph.controler;

import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.withinday.controller.ExampleWithinDayController;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.SelectHandledAgentsByProbability;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import playground.christoph.router.CostNavigationRouteFactory;
import playground.christoph.router.CostNavigationTravelTimeLogger;

public class CostNavigationRouteController extends WithinDayController implements SimulationInitializedListener, StartupListener  {
	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	/*package*/ double pDuringLegReplanning = 0.00;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	/*package*/ int numReplanningThreads = 1;

	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayDuringLegReplanner duringLegReplanner;

	protected SelectHandledAgentsByProbability selector;

	protected CostNavigationTravelTimeLogger costNavigationTravelTimeLogger;
	
	private static final Logger log = Logger.getLogger(ExampleWithinDayController.class);

	public CostNavigationRouteController(String[] args) {
		super(args);

		init();
	}

	private void init() {	
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanners(QSim sim) {
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sim.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		TravelTimeCollector travelTime = super.getTravelTimeCollector();
		OnlyTimeDependentTravelCostCalculatorFactory travelCostFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCostFactory.createTravelCostCalculator(travelTime, this.config.planCalcScore()), travelTime, factory, routeFactory);
		costNavigationTravelTimeLogger = new CostNavigationTravelTimeLogger(this.scenarioData, travelTime);
		this.events.addHandler(costNavigationTravelTimeLogger);
		
		LinkReplanningMap linkReplanningMap = super.getLinkReplanningMap();
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.selector.addIdentifier(duringLegIdentifier, pDuringLegReplanning);
		this.duringLegReplanner = new CostNavigationRouteFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0, costNavigationTravelTimeLogger, travelCostFactory, travelTime).createReplanner();
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.getReplanningManager().addDuringLegReplanner(this.duringLegReplanner);
	}

	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		super.createAndInitReplanningManager(numReplanningThreads);
		super.createAndInitTravelTimeCollector();
		super.createAndInitLinkReplanningMap();
		
		checkNetwork();
	}
	
	private void checkNetwork() {
		for (Link link : this.network.getLinks().values()) {
			if (link.getLength() == 0) {
				double distance = CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
				if (distance == 0.0) distance = 1.0;
				link.setLength(distance);
				log.info("Changed length of link " + link.getId() + " from 0.0 to " + distance);
			}
		}
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		initReplanners((QSim)e.getQueueSimulation());
	}
	
	@Override
	protected void runMobSim() {
		
		selector = new SelectHandledAgentsByProbability();
		super.getFixedOrderSimulationListener().addSimulationListener(selector);
		
		super.runMobSim();
		
		printStatistics();
	}

	private void printStatistics() {
		Set<PlanBasedWithinDayAgent> handledAgents = duringLegIdentifier.getHandledAgents();
		log.info("Persons using CostNavigationRoute: " + handledAgents.size());
		
		double sumTrust = 0.0;
		double followedAndAccepted = 0;
		double followedAndNotAccepted = 0;
		double notFollowedAndAccepted = 0;
		double notFollowedAndNotAccepted = 0;
		for (PlanBasedWithinDayAgent agent : handledAgents) {
			sumTrust += costNavigationTravelTimeLogger.getTrust(agent.getId());
			followedAndAccepted += costNavigationTravelTimeLogger.getFollowedAndAccepted(agent.getId());
			followedAndNotAccepted += costNavigationTravelTimeLogger.getFollowedAndNotAccepted(agent.getId());
			notFollowedAndAccepted += costNavigationTravelTimeLogger.getNotFollowedAndAccepted(agent.getId());
			notFollowedAndNotAccepted += costNavigationTravelTimeLogger.getNotFollowedAndNotAccepted(agent.getId());
		}
		log.info("Mean trust per person: " + sumTrust / handledAgents.size());
		log.info("Mean followed and accepted choices per person: " + followedAndAccepted / handledAgents.size());
		log.info("Mean followed and not accepted choices per person: " + followedAndNotAccepted / handledAgents.size());
		log.info("Mean not followed and accepted choices per person: " + notFollowedAndAccepted / handledAgents.size());
		log.info("Mean not followed and not accepted choices per person: " + notFollowedAndNotAccepted / handledAgents.size());
	}
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: CostNavigationRouteController config-file rerouting-share rerouting-threads");
			System.out.println();
		} else if (args.length != 3) {
			log.error("Unexpected number of input arguments!");
			log.error("Expected path to a config file (String), rerouting share (double, 0.0 ... 1.0) and number of rerouting threads (1..n).");
			System.exit(0);
		} else {
			final CostNavigationRouteController controller = new CostNavigationRouteController(args);
			controller.pDuringLegReplanning = Double.valueOf(args[1]);
			controller.numReplanningThreads = Integer.valueOf(args[2]);
			controller.run();
		}
		System.exit(0);
	}
}
