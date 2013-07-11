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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.CollectionAgentFilter;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilter;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.SelectHandledAgentsByProbability;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;

import playground.christoph.router.AnalyzeTravelTimes;
import playground.christoph.router.CostNavigationRouteFactory;
import playground.christoph.router.CostNavigationTravelTimeLogger;
import playground.christoph.router.NonLearningCostNavigationTravelTimeLogger;
import playground.christoph.router.traveltime.LookupNetwork;
import playground.christoph.router.traveltime.LookupNetworkFactory;
import playground.christoph.router.traveltime.LookupTravelTime;

public class CostNavigationRouteController extends WithinDayController implements MobsimInitializedListener, StartupListener  {
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
	/*package*/ int followedAndAccepted = 1;
	/*package*/ int followedAndNotAccepted = 1;
	/*package*/ int notFollowedAndAccepted = 1;
	/*package*/ int notFollowedAndNotAccepted = 1;
	/*package*/ String eventsFile = null;
	
	protected int handledAgents;
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayDuringLegReplannerFactory duringLegReplannerFactory;

	protected Set<Id> replannedAgents;
	protected AnalyzeTravelTimes analyzeTravelTimes; 
	protected SelectHandledAgentsByProbability selector;
	protected CostNavigationTravelTimeLogger costNavigationTravelTimeLogger;
		
	protected LookupNetwork lookupNetwork;
	protected LookupTravelTime lookupTravelTime;
	protected int updateLookupTravelTimeInterval = 15;
		
	private static final Logger log = Logger.getLogger(CostNavigationRouteController.class);

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
//		PersonalizableTravelTime travelTime = super.getTravelTimeCollector();
		TravelTime travelTime = this.lookupTravelTime;
		
		OnlyTimeDependentTravelCostCalculatorFactory travelCostFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
				
		if (agentsLearn) {
			costNavigationTravelTimeLogger = new CostNavigationTravelTimeLogger(this.scenarioData.getPopulation(), this.lookupNetwork, travelTime,
					followedAndAccepted, followedAndNotAccepted, notFollowedAndAccepted, notFollowedAndNotAccepted);			
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
		this.duringLegReplannerFactory = new CostNavigationRouteFactory(this.scenarioData, this.lookupNetwork, this.getWithinDayEngine(), 
				costNavigationTravelTimeLogger, travelCostFactory, travelTime, this.getLeastCostPathCalculatorFactory());
		this.duringLegReplannerFactory.addIdentifier(this.duringLegIdentifier);
		this.getWithinDayEngine().addDuringLegReplannerFactory(this.duringLegReplannerFactory);
	}

	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		super.initWithinDayEngine(numReplanningThreads);
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
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		initReplanners((QSim)e.getQueueSimulation());
		
		this.selector.notifyMobsimInitialized(e);
		
		replannedAgents = new HashSet<Id>();
		
		for (AgentFilter agentFilter : this.duringLegIdentifier.getAgentFilters()) {
			if (agentFilter instanceof CollectionAgentFilter) {
				replannedAgents.addAll(((CollectionAgentFilter) agentFilter).getIncludedAgents());
				handledAgents = replannedAgents.size();
			}
		}
		this.analyzeTravelTimes = new AnalyzeTravelTimes(replannedAgents, scenarioData.getPopulation().getPersons().size());
		this.getEvents().addHandler(analyzeTravelTimes);
	}
	
	@Override
	protected void runMobSim() {
		
		selector = new SelectHandledAgentsByProbability();
		
		/*
		 * If no events file is given, the simulation is run...
		 */
		if (eventsFile == null) {
			super.runMobSim();
		}
		
		/*
		 * ... otherwise the events file is parsed and analyzed.
		 */
		else {
			QSim qSim1 = new QSim(scenarioData, events);
			ActivityEngine activityEngine = new ActivityEngine();
			qSim1.addMobsimEngine(activityEngine);
			qSim1.addActivityHandler(activityEngine);
			QNetsimEngine netsimEngine = new DefaultQSimEngineFactory().createQSimEngine(qSim1);
			qSim1.addMobsimEngine(netsimEngine);
			qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
			TeleportationEngine teleportationEngine = new TeleportationEngine();
			qSim1.addMobsimEngine(teleportationEngine);
			QSim qSim = qSim1;
			ExperimentalBasicWithindayAgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
			PopulationAgentSource agentSource = new PopulationAgentSource(scenarioData.getPopulation(), agentFactory, qSim);
			qSim.addAgentSource(agentSource);
			agentSource.insertAgentsIntoMobsim();
			
			/*
			 * To create consistent results, the same random numbers have to be used. When running
			 * a real simulation, for each node in the network, an additional random object is created.
			 * Since those objects are not created when the QSim does not run, we create them by our own.
			 */
			for (int i = 0; i < this.scenarioData.getNetwork().getNodes().size(); i++) MatsimRandom.getLocalInstance();
			
				throw new RuntimeException("Events are no longer settable in this place after some refactoring. Sorry."); // mrieser, 11Sep2012
//			this.events = (EventsManagerImpl) EventsUtils.createEventsManager();
//						
//			this.notifyMobsimInitialized(new MobsimInitializedEventImpl(qSim));
//			log.info("Agents to be handled: " + handledAgents);
//			new EventsReaderXMLv1(getEvents()).parse(eventsFile);
		}		
		
		printStatistics();
		this.analyzeTravelTimes.printStatistics();
	}

	private void printStatistics() {
		log.info("Persons using CostNavigationRoute: " + handledAgents);
		
		double sumTrust = 0.0;
		double followedAndAccepted = 0;
		double followedAndNotAccepted = 0;
		double notFollowedAndAccepted = 0;
		double notFollowedAndNotAccepted = 0;
		for (Id agentId : replannedAgents) {
			sumTrust += costNavigationTravelTimeLogger.getTrust(agentId);
			followedAndAccepted += costNavigationTravelTimeLogger.getFollowedAndAccepted(agentId);
			followedAndNotAccepted += costNavigationTravelTimeLogger.getFollowedAndNotAccepted(agentId);
			notFollowedAndAccepted += costNavigationTravelTimeLogger.getNotFollowedAndAccepted(agentId);
			notFollowedAndNotAccepted += costNavigationTravelTimeLogger.getNotFollowedAndNotAccepted(agentId);
		}
		double totalObservations = followedAndAccepted + followedAndNotAccepted + notFollowedAndAccepted + notFollowedAndNotAccepted;
		double overAllTrust = (followedAndAccepted + notFollowedAndAccepted) / totalObservations;
		log.info("Mean total trust (over all observations): \t" + overAllTrust);
		log.info("Mean trust per person (over all persons): \t" + sumTrust / handledAgents);
		log.info("Mean followed and accepted choices per person: \t" + followedAndAccepted / handledAgents);
		log.info("Mean followed and not accepted choices per person: \t" + followedAndNotAccepted / handledAgents);
		log.info("Mean not followed and accepted choices per person: \t" + notFollowedAndAccepted / handledAgents);
		log.info("Mean not followed and not accepted choices per person: \t" + notFollowedAndNotAccepted / handledAgents);
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
	private final static String FOLLOWEDANDACCEPTED = "-followedandaccepted";
	private final static String FOLLOWEDANDNOTACCEPTED = "-followedandnotaccepted";
	private final static String NOTFOLLOWEDANDACCEPTED = "-notfollowedandaccepted";
	private final static String NOTFOLLOWEDANDNOTACCEPTED = "-notfollowedandnotaccepted";
	
	private final static String EVENTSFILE = "-eventsfile";
	
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
				} else if (args[i].equalsIgnoreCase(FOLLOWEDANDACCEPTED)) {
					i++;
					int value = Integer.parseInt(args[i]);
					if (value < 0) value = 0;
					controller.followedAndAccepted = value;
					log.info("followedAndAccepted: " + value);
				} else if (args[i].equalsIgnoreCase(FOLLOWEDANDNOTACCEPTED)) {
					i++;
					int value = Integer.parseInt(args[i]);
					if (value < 0) value = 0;
					controller.followedAndNotAccepted = value;
					log.info("followedAndNotAccepted: " + value);
				} else if (args[i].equalsIgnoreCase(NOTFOLLOWEDANDACCEPTED)) {
					i++;
					int value = Integer.parseInt(args[i]);
					if (value < 0) value = 0;
					controller.notFollowedAndAccepted = value;
					log.info("notFollowedAndAccepted: " + value);
				} else if (args[i].equalsIgnoreCase(NOTFOLLOWEDANDNOTACCEPTED)) {
					i++;
					int value = Integer.parseInt(args[i]);
					if (value < 0) value = 0;
					controller.notFollowedAndNotAccepted = value;
					log.info("notFollowedAndNotAccepted: " + value);
				}  else if (args[i].equalsIgnoreCase(EVENTSFILE)) {
					i++;
					String value = args[i];
					controller.eventsFile = value;
					log.info("events file to analyze: " + value);
				} else log.warn("Unknown Parameter: " + args[i]);
			}

			controller.run();
		}
		System.exit(0);
	}
}
