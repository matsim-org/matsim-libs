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
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
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

import playground.christoph.router.CostNavigationRouteFactory;
import playground.christoph.router.CostNavigationTravelTimeLogger;
import playground.christoph.router.NonLearningCostNavigationTravelTimeLogger;
import playground.christoph.router.traveltime.LookupNetwork;
import playground.christoph.router.traveltime.LookupNetworkFactory;
import playground.christoph.router.traveltime.LookupTravelTime;

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

	/* 
	 * Command line parameters
	 */
	/*package*/ boolean agentsLearn = true;
	/*package*/ double gamma = 0.50;
	/*package*/ double tau = 1.10;
	/*package*/ double tauplus = 0.75;
	/*package*/ double tauminus = 1.25;
		
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayDuringLegReplanner duringLegReplanner;

	protected SelectHandledAgentsByProbability selector;

	protected CostNavigationTravelTimeLogger costNavigationTravelTimeLogger;
	
	protected LookupNetwork lookupNetwork;
	protected LookupTravelTime lookupTravelTime;
	protected int updateLookupTravelTimeInterval = 15;
	
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
//		PersonalizableTravelTime travelTime = super.getTravelTimeCollector();
		PersonalizableTravelTime travelTime = this.lookupTravelTime;
		
		OnlyTimeDependentTravelCostCalculatorFactory travelCostFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
		LeastCostPathCalculatorFactory factory = new FastAStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCostFactory.createTravelCostCalculator(travelTime, this.config.planCalcScore()), travelTime, factory, routeFactory);
		
		if (agentsLearn) {
			costNavigationTravelTimeLogger = new CostNavigationTravelTimeLogger(this.scenarioData.getPopulation(), this.lookupNetwork, travelTime);			
		} else {
			costNavigationTravelTimeLogger = new NonLearningCostNavigationTravelTimeLogger(this.scenarioData.getPopulation(), this.lookupNetwork, travelTime, this.gamma);
			log.info("Agents learning is disabled - using constant gamma of " + this.gamma + "!");
		}
		costNavigationTravelTimeLogger.toleranceSlower = tauminus;
		costNavigationTravelTimeLogger.toleranceFaster = tauplus;
		costNavigationTravelTimeLogger.toleranceAlternativeRoute = tau;
		
		this.events.addHandler(costNavigationTravelTimeLogger);
		
		LinkReplanningMap linkReplanningMap = super.getLinkReplanningMap();
		this.duringLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.selector.addIdentifier(duringLegIdentifier, pDuringLegReplanning);
		this.duringLegReplanner = new CostNavigationRouteFactory(this.scenarioData, this.lookupNetwork, sim.getAgentCounter(), router, 1.0, costNavigationTravelTimeLogger, travelCostFactory, travelTime, this.getLeastCostPathCalculatorFactory()).createReplanner();
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
		
		this.lookupNetwork = new LookupNetworkFactory().createLookupNetwork(this.network);
		this.lookupTravelTime = new LookupTravelTime(lookupNetwork, this.getTravelTimeCollector(), scenarioData.getConfig().global().getNumberOfThreads());
		this.lookupTravelTime.setUpdateInterval(this.updateLookupTravelTimeInterval);
		this.getFixedOrderSimulationListener().addSimulationListener(lookupTravelTime);
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
	
	private final static String NUMOFTHREADS = "-numofthreads";
	private final static String REPLANNINGSHARE = "-replanningshare";
	private final static String LOOKUPTABLEUPDATEINTERVAL = "-lookuptableupdateinterval";
	private final static String AGENTSLEARN = "-agentslearn";
	private final static String GAMMA = "-gamma";
	private final static String TAU = "-tau";
	private final static String TAUPLUS = "-tauplus";
	private final static String TAUMINUS = "-tauminus";
	
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: CostNavigationRouteController config-file parameters");
		} else {
			final CostNavigationRouteController controller = new CostNavigationRouteController(args);

			// do not dump plans, network and facilities and the end
			controller.setDumpDataAtEnd(false);
						
			// set parameter from command line
			for (int i = 1; i < args.length; i++) {
				if (args[i].equalsIgnoreCase(NUMOFTHREADS)) {
					i++;
					int threads = Integer.parseInt(args[i]);
					if (threads < 1) threads = 1;
					controller.numReplanningThreads = threads;
					log.info("number of replanning threads: " + threads);
				} else if (args[i].equalsIgnoreCase(REPLANNINGSHARE)) {
					i++;
					double share = Double.parseDouble(args[i]);
					if (share > 1.0) share = 1.0;
					else if (share < 0.0) share = 0.0;
					controller.pDuringLegReplanning = share;
					log.info("share of leg replanning agents: " + share);
				} else if (args[i].equalsIgnoreCase(LOOKUPTABLEUPDATEINTERVAL)) {
					i++;
					int interval = Integer.parseInt(args[i]);
					if (interval < 1) interval = 1;
					controller.updateLookupTravelTimeInterval = interval;
					log.info("travel time lookup table update interval: " + interval);
				} else if (args[i].equalsIgnoreCase(AGENTSLEARN)) {
					i++;
					boolean agentsLearn = true;
					if (args[i].toLowerCase().equals("false")) agentsLearn = false;
					controller.agentsLearn = agentsLearn;
					log.info("agents learn: " + agentsLearn);
				} else if (args[i].equalsIgnoreCase(GAMMA)) {
					i++;
					double gamma = Double.parseDouble(args[i]);
					if (gamma > 1.0) gamma = 1.0;
					else if (gamma < 0.0) gamma = 0.0;
					controller.gamma = gamma;
					log.info("gamma: " + gamma);
				} else if (args[i].equalsIgnoreCase(TAU)) {
					i++;
					double tau = Double.parseDouble(args[i]);
					if (tau < 1.0) tau = 1.0;
					controller.tau = tau;
					log.info("tau: " + tau);
				} else if (args[i].equalsIgnoreCase(TAUPLUS)) {
					i++;
					double tauplus = Double.parseDouble(args[i]);
					if (tauplus > 1.0) tauplus = 1.0;
					else if (tauplus < 0.0) tauplus = 0.0;
					controller.tauplus = tauplus;
					log.info("tauplus: " + tauplus);
				} else if (args[i].equalsIgnoreCase(TAUMINUS)) {
					i++;
					double tauminus = Double.parseDouble(args[i]);
					if (tauminus < 1.0) tauminus = 1.0;
					controller.tauminus = tauminus;
					log.info("tauminus: " + tauminus);
				} else log.warn("Unknown Parameter: " + args[i]);
			}
			
			controller.run();
		}
		System.exit(0);
	}
}
