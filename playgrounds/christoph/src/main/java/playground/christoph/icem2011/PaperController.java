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
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.utils.misc.Counter;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
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
	/*package*/ int numReplanningThreads = 4;

	/*
	 * File that contains a list of all links where agents
	 * will replan their plans.
	 */
	/*package*/ String replanningLinksFile = "";
	private Set<Id> replanningLinks;
	private Charset charset = Charset.forName("UTF-8");
	
	/*
	 * Define when the Replanning is en- and disabled.
	 */
	protected int tWithinDayEnabled = 7*3600 + 45*60;	// 07:45
	protected int tWithinDayDisabled = 11*3600 + 45*60;	// 11:45
	protected boolean enabled = false;
	
//	protected InitialIdentifier initialIdentifier;
//	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
//	protected WithinDayInitialReplanner initialReplanner;
//	protected WithinDayDuringActivityReplanner duringActivityReplanner;
	protected WithinDayDuringLegReplanner duringLegReplanner;

	protected SelectHandledAgentsByProbability selector;

	private static final Logger log = Logger.getLogger(ExampleWithinDayController.class);

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
	 * By doing this every person can use a personalised Router.
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
		 * Use the FilteredDuringLegIdentifier to remove those agents that are not within
		 * a given distance to the affected links.
		 */
		parseReplanningLinks(this.replanningLinksFile);
		LinkReplanningMap linkReplanningMap = super.getLinkReplanningMap();
		DuringLegIdentifier identifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.duringLegIdentifier = new FilteredDuringLegIdentifier(identifier, this.replanningLinks);
		this.selector.addIdentifier(duringLegIdentifier, pDuringLegReplanning);
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
	}
	
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		initReplanners((QSim)e.getQueueSimulation());
	}
	
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
		boolean currentState = this.enabled;
		int time = (int) e.getSimulationTime();
		if (time >= this.tWithinDayEnabled && time <= this.tWithinDayDisabled) this.getReplanningManager().doDuringLegReplanning(true);
		else this.getReplanningManager().doDuringLegReplanning(false);
		
		// if state has changed
		if (enabled != currentState) {
			if (currentState) log.info("Within-Day Replanning has been enabled at t = " + e.getSimulationTime());
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
	
	
	/*
	 * Wrapper for a DuringLegIdentifier. Checks for all agents that are identified by
	 * the delegate DuringLegIdentifier whether they are currently on a Link on which
	 * replanning shall be performed.
	 */
	private static class FilteredDuringLegIdentifier extends DuringLegIdentifier {

		private DuringLegIdentifier delegate;
		private Set<Id> replanningLinks;
		
		public FilteredDuringLegIdentifier(DuringLegIdentifier identifier, Set<Id> replanningLinks) {
			this.delegate = identifier;
			this.replanningLinks = replanningLinks;
		}	
		
		@Override
		public Set<WithinDayAgent> getAgentsToReplan(double time) {
			Set<WithinDayAgent> agentsFromDelegate = delegate.getAgentsToReplan(time);
			Set<WithinDayAgent> filteredAgents = new TreeSet<WithinDayAgent>();
			for (WithinDayAgent agent : agentsFromDelegate) {
				if (replanningLinks.contains(agent.getCurrentLinkId())) filteredAgents.add(agent);
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
	private final static String REPLANNINGLINKSFILE = "-replanninglinksfile";

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
				}  else if (args[i].equalsIgnoreCase(REPLANNINGLINKSFILE)) {
					i++;
					controller.replanningLinksFile = args[i];
					log.info("leg replanning links file: " + args[i]);
				} else log.warn("Unknown Parameter: " + args[i]);
			}
			controller.run();
		}
		System.exit(0);
	}
	
}