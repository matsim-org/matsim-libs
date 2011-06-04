/* *********************************************************************** *
 * project: org.matsim.*
 * PaperController.java
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

package playground.christoph.icem2011;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.utils.misc.Counter;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.withinday.controller.ExampleWithinDayController;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.SelectHandledAgentsByProbability;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

/**
 * Controller for the simulation runs presented in the ICEM 2011 paper. 
 * 
 * @author cdobler
 */
public class PaperController extends WithinDayController implements StartupListener,
		SimulationInitializedListener, SimulationBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(ExampleWithinDayController.class);

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
//	/*package*/ double pInitialReplanning = 0.0;
//	/*package*/ double pDuringActivityReplanning = 0.0;
	/*package*/ double pDuringLegReplanning = 1.0;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	/*package*/ int numReplanningThreads = 8;

	/*
	 * File that contains a list of all links where agents
	 * will replan their plans.
	 */
	/*package*/ String noEventEventsFile = "";
	/*package*/ String replanningAgentsFile = "";
	/*package*/ String replanningLinksFile = "";
	private Set<Id> replanningAgents;
	private Set<Id> replanningLinks;
	private Charset charset = Charset.forName("UTF-8");
	
	/*
	 * Define when the affected agents start and end being collected.
	 */
	/*package*/ int tBeginEvent = 7*3600;	// 07:00
	/*package*/ int tEndEvent = 9*3600;		// 09:00
	
	/*
	 * Define when the Replanning is en- and disabled.
	 */
	/*package*/ int tWithinDayEnabled = 7*3600;		// 07:00
	/*package*/ int tWithinDayDisabled = 12*3600;	// 12:00
	/*package*/ boolean enabled = false;
	
	/*package*/ String cityZurichSHPFile = "../../matsim/mysimulations/ICEM2011/input/GIS/Zurich_City.shp";
	/*package*/ String cantonZurichSHPFile = "../../matsim/mysimulations/ICEM2011/input/GIS/Zurich_Canton.shp";
	
//	protected InitialIdentifier initialIdentifier;
//	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
//	protected WithinDayInitialReplanner initialReplanner;
//	protected WithinDayDuringActivityReplanner duringActivityReplanner;
	protected WithinDayDuringLegReplanner duringLegReplanner;

	protected SelectHandledAgentsByProbability selector;


	public PaperController(String[] args) {
		super(args);

		init();
	}

	private void init() {
		// Use a Scoring Function, that only scores the travel times!
//		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
		
		// register this as a Controller and Simulation Listener
		super.getFixedOrderSimulationListener().addSimulationListener(this);
		super.addControlerListener(this);
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanners(QSim sim) {

		TravelTimeCollector travelTime = super.getTravelTimeCollector();
		PersonalizableTravelCost travelCost = super.getTravelCostCalculatorFactory().createTravelCostCalculator(travelTime, this.config.planCalcScore());
//		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
//		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		LeastCostPathCalculatorFactory factory = new DijkstraFactory(); 
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, factory);
		
//		this.initialIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
//		this.selector.addIdentifier(initialIdentifier, pInitialReplanning);
//		this.initialReplanner = new InitialReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
//		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
//		this.getReplanningManager().addIntialReplanner(this.initialReplanner);
		
//		ActivityReplanningMap activityReplanningMap = super.getActivityReplanningMap();
//		this.duringActivityIdentifier = new ActivityEndIdentifierFactory(activityReplanningMap).createIdentifier();
//		this.selector.addIdentifier(duringActivityIdentifier, pDuringActivityReplanning);
//		this.duringActivityReplanner = new NextLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
//		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
//		this.getReplanningManager().addDuringActivityReplanner(this.duringActivityReplanner);
		
		/*
		 * Use the LinkFilteredDuringLegIdentifier to remove those agents that are not within
		 * a given distance to the affected links.
		 * Use the AgentFilteredDuringLegIdentifier to remove those agents who do not cross a
		 * certain area when no event occurs and they do not replan their plans.
		 */
		getReplanningAgents(this.noEventEventsFile);
		parseReplanningLinks(this.replanningLinksFile);
		LinkReplanningMap linkReplanningMap = super.getLinkReplanningMap();
		DuringLegIdentifier identifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.selector.addIdentifier(identifier, pDuringLegReplanning);
		this.duringLegIdentifier = new AgentFilteredDuringLegIdentifier(new LinkFilteredDuringLegIdentifier(identifier, this.replanningLinks), this.replanningAgents);
		this.duringLegReplanner = new CurrentLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
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
//		super.createAndInitActivityReplanningMap();
		super.createAndInitLinkReplanningMap();
		
		this.getReplanningManager().doInitialReplanning(false);
		this.getReplanningManager().doDuringActivityReplanning(false);
		this.getReplanningManager().doDuringLegReplanning(false);
		
		// Module to analyze expected travel time on a single link
		Collection<Link> changedLinks = new HashSet<Link>();
		for (NetworkChangeEvent networkChangeEvent : super.getNetwork().getNetworkChangeEvents()) {
			changedLinks.addAll(networkChangeEvent.getLinks());
		}
		LogLinkTravelTime logLinkTravelTime = new LogLinkTravelTime(changedLinks, super.getTravelTimeCollector(), super.getTravelTimeCalculator());
		super.addControlerListener(logLinkTravelTime);
		super.getFixedOrderSimulationListener().addSimulationListener(logLinkTravelTime);
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		initReplanners((QSim)e.getQueueSimulation());
		
		// Module to analyze the travel times
		AnalyzeTravelTimes analyzeTravelTimes = new AnalyzeTravelTimes(this.scenarioData, cityZurichSHPFile, cantonZurichSHPFile, replanningAgents);
		super.addControlerListener(analyzeTravelTimes);
		super.getEvents().addHandler(analyzeTravelTimes);
	}
	
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		boolean currentState = this.enabled;
		int time = (int) e.getSimulationTime();
		if (time >= this.tWithinDayEnabled && time <= this.tWithinDayDisabled) {
			this.enabled = true;
			this.getReplanningManager().doDuringLegReplanning(true);
		} else {
			this.enabled = false;
			this.getReplanningManager().doDuringLegReplanning(false);
		}
		
		// if state has changed
		if (enabled != currentState) {
			if (enabled) log.info("Within-Day Replanning has been enabled at t = " + e.getSimulationTime());
			else log.info("Within-Day Replanning has been disabled at t = " + e.getSimulationTime());
		}
	}
	
	@Override
	protected void runMobSim() {
		
		selector = new SelectHandledAgentsByProbability();
		super.getFixedOrderSimulationListener().addSimulationListener(selector);
		
		super.runMobSim();
	}


	private void parseReplanningLinks(String replanningLinksFile) {
		replanningLinks = new HashSet<Id>();
	    	    
	    try {
	    	Counter lineCounter = new Counter("Parsed replanning link ids ");

	    	FileInputStream fis = null;
	    	InputStreamReader isr = null;
	    	BufferedReader br = null;

	    	log.info("start parsing...");
	    	fis = new FileInputStream(replanningLinksFile);
	    	isr = new InputStreamReader(fis, charset);
	    	br = new BufferedReader(isr);

	    	// skip first Line which is only a header
	    	br.readLine();
	    	
	    	String line;
	    	while((line = br.readLine()) != null) {
	    		
	    		replanningLinks.add(this.scenarioData.createId(line));
	    		lineCounter.incCounter();
	    	}	    	

	    	br.close();
	    	isr.close();
	    	fis.close();
	    	
	    	log.info("done.");
	    	log.info("Found " + replanningLinksFile.length() + " replanning links.");
	    } catch (IOException e) {
	    	log.error("Error when trying to parse the replanning links file. No replanning links identified!");
	    }
	}
	
	private void getReplanningAgents(String eventsFile) {
		
		IdentifyAffectedAgents iaa = new IdentifyAffectedAgents(scenarioData, tWithinDayEnabled, tWithinDayDisabled, replanningAgentsFile);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(iaa);
		
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		
		this.replanningAgents = iaa.getAffectedAgents();
	}
	
	/*
	 * Wrapper for a DuringLegIdentifier. Checks for all agents that are identified by
	 * the delegate DuringLegIdentifier whether they are currently on a Link on which
	 * replanning shall be performed.
	 */
	private static class LinkFilteredDuringLegIdentifier extends DuringLegIdentifier {

		private DuringLegIdentifier delegate;
		private Set<Id> replanningLinks;
		
		public LinkFilteredDuringLegIdentifier(DuringLegIdentifier identifier, Set<Id> replanningLinks) {
			this.delegate = identifier;
			this.replanningLinks = replanningLinks;
		}	
		
		@Override
		public Set<WithinDayAgent> getAgentsToReplan(double time) {
			Set<WithinDayAgent> agentsFromDelegate = delegate.getAgentsToReplan(time);
			Set<WithinDayAgent> filteredAgents = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
			for (WithinDayAgent agent : agentsFromDelegate) {
				if (replanningLinks.contains(agent.getCurrentLinkId())) filteredAgents.add(agent);
			}
			return filteredAgents;
		}
	}
	
	/*
	 * Wrapper for a DuringLegIdentifier. Checks for all agents that are identified by
	 * the delegate DuringLegIdentifier whether they are currently on a Link on which
	 * replanning shall be performed.
	 */
	private static class AgentFilteredDuringLegIdentifier extends DuringLegIdentifier {

		private DuringLegIdentifier delegate;
		private Set<Id> replanningAgents;
		
		public AgentFilteredDuringLegIdentifier(DuringLegIdentifier identifier, Set<Id> replanningAgents) {
			this.delegate = identifier;
			this.replanningAgents = replanningAgents;
		}	
		
		@Override
		public Set<WithinDayAgent> getAgentsToReplan(double time) {
			Set<WithinDayAgent> agentsFromDelegate = delegate.getAgentsToReplan(time);
			Set<WithinDayAgent> filteredAgents = new TreeSet<WithinDayAgent>(new PersonAgentComparator());
			for (WithinDayAgent agent : agentsFromDelegate) {
				if (replanningAgents.contains(agent.getId())) filteredAgents.add(agent);
			}
			return filteredAgents;
		}
	}
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	private final static String NUMOFTHREADS = "-numofthreads";
	private final static String LEGREPLANNINGSHARE = "-legreplanningshare";
	private final static String NOEVENTEVENTSFILE = "-noeventeventsfile";
	private final static String REPLANNINGAGENTSFILE = "-replanningagentsfile";
	private final static String REPLANNINGLINKSFILE = "-replanninglinksfile";
	private final static String STARTEVENT = "-startevent";
	private final static String ENDEVENT = "-endevent";
	private final static String STARTREPLANNING = "-startreplanning";
	private final static String ENDREPLANNING = "-endreplanning";
	private final static String ZURICHCITYSHP = "-cityshp";
	private final static String ZURICHCANTONSHP = "-cantonshp";

	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {		
			final PaperController controller = new PaperController(new String[]{args[0]});
			
			// set parameter from command line
			for (int i = 1; i < args.length; i++) {
				if (args[i].equalsIgnoreCase(NUMOFTHREADS)) {
					i++;
					controller.numReplanningThreads = Integer.parseInt(args[i]);
					log.info("number of replanning threads: " + args[i]);
				} else if (args[i].equalsIgnoreCase(LEGREPLANNINGSHARE)) {
					i++;
					double share = Double.parseDouble(args[i]);
					if (share > 1.0) share = 1.0;
					else if (share < 0.0) share = 0.0;
					controller.pDuringLegReplanning = share;
					log.info("share of leg replanning agents: " + args[i]);
				} else if (args[i].equalsIgnoreCase(NOEVENTEVENTSFILE)) {
					i++;
					controller.noEventEventsFile = args[i];
					log.info("no event occuring events file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(REPLANNINGAGENTSFILE)) {
					i++;
					controller.replanningAgentsFile = args[i];
					log.info("leg replanning agents file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(REPLANNINGLINKSFILE)) {
					i++;
					controller.replanningLinksFile = args[i];
					log.info("leg replanning links file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(STARTEVENT)) {
					i++;
					controller.tBeginEvent = Integer.parseInt(args[i]);
					log.info("start event: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ENDEVENT)) {
					i++;
					controller.tEndEvent = Integer.parseInt(args[i]);
					log.info("end event: " + args[i]);
				}  else if (args[i].equalsIgnoreCase(STARTREPLANNING)) {
					i++;
					controller.tWithinDayEnabled = Integer.parseInt(args[i]);
					log.info("start within-day replanning: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ENDREPLANNING)) {
					i++;
					controller.tWithinDayDisabled = Integer.parseInt(args[i]);
					log.info("end within-day replanning: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ZURICHCITYSHP)) {
					i++;
					controller.cityZurichSHPFile = args[i];
					log.info("city of zurich shp file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ZURICHCANTONSHP)) {
					i++;
					controller.cantonZurichSHPFile = args[i];
					log.info("canton of zurich shp file: " + args[i]);
				} else log.warn("Unknown Parameter: " + args[i]);
			}
			controller.run();
		}
		System.exit(0);
	}
	
}