/* *********************************************************************** *
 * project: org.matsim.*
 * PaperRunner.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.misc.Counter;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.SelectHandledAgentsByProbability;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import playground.christoph.tools.PersonAgentComparator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Code for the simulation runs presented in the ICEM 2011 (respectively TRB 2012) paper. 
 * 
 * @author cdobler
 */
public class PaperRunner implements StartupListener, MobsimBeforeSimStepListener {

	private static final Logger log = Logger.getLogger(PaperRunner.class);

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
	/*package*/ String affectedAgentsFile = "";
	/*package*/ String replanningAgentsFile = "";
	/*package*/ String replanningLinksFile = "";
	private Set<Id> affectedAgents;
	private Set<Id> replanningAgents;
	private Set<Id<Link>> replanningLinks;
	private Charset charset = Charset.forName("UTF-8");
	
	/*
	 * Define when the affected agents start and end being collected.
	 */
	/*package*/ int tBeginEvent = -1;
	/*package*/ int tEndEvent = -1;
	
	/*
	 * Define when the Replanning is en- and disabled.
	 */
	/*package*/ int tWithinDayEnabled = -1;
	/*package*/ int tWithinDayDisabled = -1;
	/*package*/ boolean enabled = false;
	
	/*package*/ String cityZurichSHPFile = "";
	/*package*/ String cantonZurichSHPFile = "";
	
//	protected InitialIdentifier initialIdentifier;
//	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegAgentSelector duringLegIdentifier;
//	protected WithinDayInitialReplanner initialReplanner;
//	protected WithinDayDuringActivityReplanner duringActivityReplanner;
	protected WithinDayDuringLegReplannerFactory duringLegReplannerFactory;

	protected Scenario scenario;
	protected WithinDayControlerListener withinDayControlerListener;
	protected SelectHandledAgentsByProbability selector;

	public PaperRunner(Controler controler) {

		this.scenario = controler.getScenario();
		this.withinDayControlerListener = new WithinDayControlerListener();
		
		// Use a Scoring Function, that only scores the travel times!
//		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanners() {
		
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
		getAffectedAgents(this.noEventEventsFile);
		getReplanningAgents(this.noEventEventsFile);
		parseReplanningLinks(this.replanningLinksFile);
		
		selector = new SelectHandledAgentsByProbability();
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(selector);
		
		TravelDisutility travelDisutility = this.withinDayControlerListener.getTravelDisutilityFactory().createTravelDisutility(
				this.withinDayControlerListener.getTravelTimeCollector(), this.scenario.getConfig().planCalcScore()); 
		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, this.withinDayControlerListener.getTravelTimeCollector());
		
		LinkReplanningMap linkReplanningMap = this.withinDayControlerListener.getLinkReplanningMap();
		MobsimDataProvider mobsimDataProvider = this.withinDayControlerListener.getMobsimDataProvider();
		DuringLegAgentSelector identifier = new LeaveLinkIdentifierFactory(linkReplanningMap, mobsimDataProvider).createIdentifier();
		this.selector.addIdentifier(identifier, pDuringLegReplanning);
		this.duringLegIdentifier = new AgentFilteredDuringLegIdentifier(new LinkFilteredDuringLegIdentifier(identifier, this.replanningLinks), this.replanningAgents);
		this.duringLegReplannerFactory = new CurrentLegReplannerFactory(this.scenario, 
				this.withinDayControlerListener.getWithinDayEngine(),
				this.withinDayControlerListener.getWithinDayTripRouterFactory(), routingContext);
		this.duringLegReplannerFactory.addIdentifier(this.duringLegIdentifier);
		this.withinDayControlerListener.getWithinDayEngine().addDuringLegReplannerFactory(this.duringLegReplannerFactory);
	}

	/*
	 * When the Controller Startup Event is created, the EventsManager
	 * has already been initialized. Therefore we can initialize now
	 * all Objects, that have to be registered at the EventsManager.
	 */
	@Override
	public void notifyStartup(StartupEvent event) {
		
		this.withinDayControlerListener.notifyStartup(event);
				
		this.withinDayControlerListener.getWithinDayEngine().doInitialReplanning(false);
		this.withinDayControlerListener.getWithinDayEngine().doDuringActivityReplanning(false);
		this.withinDayControlerListener.getWithinDayEngine().doDuringLegReplanning(false);

		initReplanners();
		
		// Module to analyze the travel times
		AnalyzeTravelTimes analyzeTravelTimes = new AnalyzeTravelTimes(this.scenario, cityZurichSHPFile, cantonZurichSHPFile, affectedAgents, replanningAgents);
		event.getControler().addControlerListener(analyzeTravelTimes);
		event.getControler().getEvents().addHandler(analyzeTravelTimes);

		// Module to analyze expected travel time on a single link
		Collection<Link> changedLinks = new HashSet<Link>();
        for (NetworkChangeEvent networkChangeEvent : ((NetworkImpl) event.getControler().getScenario().getNetwork()).getNetworkChangeEvents()) {
			changedLinks.addAll(networkChangeEvent.getLinks());
		}
		LogLinkTravelTime logLinkTravelTime = new LogLinkTravelTime(changedLinks, 
				this.withinDayControlerListener.getTravelTimeCollector());
		event.getControler().addControlerListener(logLinkTravelTime);
		this.withinDayControlerListener.getFixedOrderSimulationListener().addSimulationListener(logLinkTravelTime);			
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		boolean currentState = this.enabled;
		int time = (int) e.getSimulationTime();
		if (time >= this.tWithinDayEnabled && time <= this.tWithinDayDisabled) {
			this.enabled = true;
			this.withinDayControlerListener.getWithinDayEngine().doDuringLegReplanning(true);
		} else {
			this.enabled = false;
			this.withinDayControlerListener.getWithinDayEngine().doDuringLegReplanning(false);
		}
		
		// if state has changed
		if (enabled != currentState) {
			if (enabled) log.info("Within-Day Replanning has been enabled at t = " + e.getSimulationTime());
			else log.info("Within-Day Replanning has been disabled at t = " + e.getSimulationTime());
		}
	}
	
	private void parseReplanningLinks(String replanningLinksFile) {
		replanningLinks = new HashSet<>();
	    	    
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
	    		
	    		replanningLinks.add(Id.create(line, Link.class));
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
	
	private void getAffectedAgents(String eventsFile) {
		IdentifyAffectedAgents iaa = new IdentifyAffectedAgents(scenario, tBeginEvent, tEndEvent, affectedAgentsFile);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(iaa);
		
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		
		this.affectedAgents = iaa.getAffectedAgents();
	}
	
	private void getReplanningAgents(String eventsFile) {
		
		IdentifyAffectedAgents iaa = new IdentifyAffectedAgents(scenario, tBeginEvent, tEndEvent, replanningAgentsFile);
		
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
	private static class LinkFilteredDuringLegIdentifier extends DuringLegAgentSelector {

		private DuringLegAgentSelector delegate;
		private Set<Id<Link>> replanningLinks;
		
		public LinkFilteredDuringLegIdentifier(DuringLegAgentSelector identifier, Set<Id<Link>> replanningLinks) {
			this.delegate = identifier;
			this.replanningLinks = replanningLinks;
		}	
		
		@Override
		public Set<MobsimAgent> getAgentsToReplan(double time) {
			Set<MobsimAgent> agentsFromDelegate = delegate.getAgentsToReplan(time);
			Set<MobsimAgent> filteredAgents = new TreeSet<MobsimAgent>(new PersonAgentComparator());
			for (MobsimAgent agent : agentsFromDelegate) {
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
	private static class AgentFilteredDuringLegIdentifier extends DuringLegAgentSelector {

		private DuringLegAgentSelector delegate;
		private Set<Id> replanningAgents;
		
		public AgentFilteredDuringLegIdentifier(DuringLegAgentSelector identifier, Set<Id> replanningAgents) {
			this.delegate = identifier;
			this.replanningAgents = replanningAgents;
		}	
		
		@Override
		public Set<MobsimAgent> getAgentsToReplan(double time) {
			Set<MobsimAgent> agentsFromDelegate = delegate.getAgentsToReplan(time);
			Set<MobsimAgent> filteredAgents = new TreeSet<MobsimAgent>(new PersonAgentComparator());
			for (MobsimAgent agent : agentsFromDelegate) {
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
	private final static String AFFECTEDAGENTSFILE = "-affectedagentsfile";
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
			final Controler controler = new Controler(args[0]);

			// create a PaperRunner and register it as a Controller and Simulation Listener
			PaperRunner paperRunner = new PaperRunner(controler);
			controler.addControlerListener(paperRunner);
			controler.getMobsimListeners().add(paperRunner);
			
			// do not dump plans, network and facilities and the end
//			controller.setDumpDataAtEnd(false);
			
			// set parameter from command line
			for (int i = 1; i < args.length; i++) {
				if (args[i].equalsIgnoreCase(NUMOFTHREADS)) {
					i++;
					paperRunner.numReplanningThreads = Integer.parseInt(args[i]);
					log.info("number of replanning threads: " + args[i]);
				} else if (args[i].equalsIgnoreCase(LEGREPLANNINGSHARE)) {
					i++;
					double share = Double.parseDouble(args[i]);
					if (share > 1.0) share = 1.0;
					else if (share < 0.0) share = 0.0;
					paperRunner.pDuringLegReplanning = share;
					log.info("share of leg replanning agents: " + args[i]);
				} else if (args[i].equalsIgnoreCase(NOEVENTEVENTSFILE)) {
					i++;
					paperRunner.noEventEventsFile = args[i];
					log.info("no event occuring events file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(AFFECTEDAGENTSFILE)) {
					i++;
					paperRunner.affectedAgentsFile = args[i];
					log.info("affected agents file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(REPLANNINGAGENTSFILE)) {
					i++;
					paperRunner.replanningAgentsFile = args[i];
					log.info("leg replanning agents file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(REPLANNINGLINKSFILE)) {
					i++;
					paperRunner.replanningLinksFile = args[i];
					log.info("leg replanning links file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(STARTEVENT)) {
					i++;
					paperRunner.tBeginEvent = Integer.parseInt(args[i]);
					log.info("start event: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ENDEVENT)) {
					i++;
					paperRunner.tEndEvent = Integer.parseInt(args[i]);
					log.info("end event: " + args[i]);
				}  else if (args[i].equalsIgnoreCase(STARTREPLANNING)) {
					i++;
					paperRunner.tWithinDayEnabled = Integer.parseInt(args[i]);
					log.info("start within-day replanning: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ENDREPLANNING)) {
					i++;
					paperRunner.tWithinDayDisabled = Integer.parseInt(args[i]);
					log.info("end within-day replanning: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ZURICHCITYSHP)) {
					i++;
					paperRunner.cityZurichSHPFile = args[i];
					log.info("city of zurich shp file: " + args[i]);
				} else if (args[i].equalsIgnoreCase(ZURICHCANTONSHP)) {
					i++;
					paperRunner.cantonZurichSHPFile = args[i];
					log.info("canton of zurich shp file: " + args[i]);
				} else log.warn("Unknown Parameter: " + args[i]);
			}
			controler.run();
		}
		System.exit(0);
	}
	
}