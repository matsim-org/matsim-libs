/* *********************************************************************** *
 * project: org.matsim.*
 * EUTController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.johannes.eut;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

/**
 * @author illenberger
 *
 */
public class EUTController2 extends WithindayControler {
	
	//==============================================================================
	// private fields
	//==============================================================================

	private static final String CONFIG_MODULE_NAME = "eut";

//	private static final Logger log = Logger.getLogger(EUTController.class);

	private EstimReactiveLinkTT reactTTs;

	private double incidentProba;

	private double equipmentFraction;

	private double rho;

	private SummaryWriter summaryWriter;

	private EUTRouterAnalyzer analyzer;
	
	private double capReduction;
	
	private List<Link> riskyLinks;

	//==============================================================================
	// 
	//==============================================================================
	
	public EUTController2(String[] args) {
		super(args);
		setOverwriteFiles(true);
	}

	@Override
	protected void setUp() {
		this.equipmentFraction = string2Double(getConfig().getParam(CONFIG_MODULE_NAME, "equipmentFraction"));
		this.incidentProba = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "incidentProba"));
		this.rho = Integer.parseInt(getConfig().findParam(CONFIG_MODULE_NAME, "rho"));
		this.capReduction = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "capReduction"));
		/*
		 * Output writer for summary...
		 */
		this.summaryWriter = new SummaryWriter(getConfig().findParam(CONFIG_MODULE_NAME, "summaryFile"));
		addControlerListener(this.summaryWriter);
		super.setUp();
		/*
		 * Create a new factory for our withinday agents.
		 */
		addControlerListener(new WithindayControlerListener());
		/*
		 * Create a new utility function...
		 */
		CARAFunction utilFunction = new CARAFunction(this.rho);
		/*
		 * Create a router analyser...
		 */
		this.analyzer = new EUTRouterAnalyzer(utilFunction, this.summaryWriter);
		addControlerListener(this.analyzer);
		/*
		 * Link variance statistics...
		 */
		LinkTTVarianceStats linkStats = new LinkTTVarianceStats(getTravelTimeCalculator(), 25200, 32400, 60, this.summaryWriter);
		addControlerListener(linkStats);
		/*
		 * Get the "risky" links...
		 */
		String linkIds = getConfig().findParam(CONFIG_MODULE_NAME, "links");
		riskyLinks = new LinkedList<Link>();
		for(String id : linkIds.split(" ")) {
			Link link = getNetwork().getLinks().get(new IdImpl(id));
			riskyLinks.add(link);
			
		}
		/*
		 * Count agents traversed risky links...
		 */
		TraversedRiskyLink travRiskyLink = new TraversedRiskyLink(getPopulation(), riskyLinks, this.summaryWriter);
		/*
		 * Trip and score statistics...
		 */
		TripAndScoreStats stats = new TripAndScoreStats(this.analyzer, travRiskyLink, this.summaryWriter);
		addControlerListener(stats);
		this.events.addHandler(stats);
		addControlerListener(travRiskyLink);
		/*
		 * Currently disabled...
		 */
		// String personsFile = getConfig().findParam(CONFIG_MODULE_NAME, "guidedPersons");
		// addControlerListener(new CEAnalyzer(personsFile, this.population, stats, utilFunction));
		/*
		 * Replace the default scoring function with the "risk"-scoring function.
		 */
		this.scoringFunctionFactory = new EUTScoringFactory(utilFunction, config.charyparNagelScoring());
		/*
		 * Remove all scores in the 0-th iteration.
		 * We have to remove duplicate plans, i.e., plans with the same route and departure time!
		 */
		addControlerListener(new RemoveDuplicatePlans());
		addControlerListener(new RemoveScores());
		/*
		 * Travel time provider for reactive travel times.
		 * FIXME: Need changes from Gregor!
		 */
//		this.reactTTs = new EstimReactiveLinkTT(null);
//		this.events.addHandler(this.reactTTs);
//		this.reactTTs.reset(getIteration());
		/*
		 * Create a new incident simulator...
		 */
		RandomIncidentSimulator simulator = new RandomIncidentSimulator(network, incidentProba);
		simulator.setCapReduction(capReduction);
		for(Link link : riskyLinks)
			simulator.addLink(link);
		
		addControlerListener(simulator);

	}

	@Override
	protected void runMobSim() {

		this.config.withinday().addParam("contentThreshold", "1");
		this.config.withinday().addParam("replanningInterval", "1");

		WithindayQueueSimulation sim = new WithindayQueueSimulation(this.scenarioData, this.events, this);
		this.trafficManagement = new TrafficManagement();
		sim.setTrafficManagement(this.trafficManagement);
		
		sim.run();
	}


	private class WithindayControlerListener implements StartupListener, IterationStartsListener {

		public void notifyStartup(StartupEvent event) {
			EUTController2.this.factory = new GuidedAgentFactory(EUTController2.this.network, EUTController2.this.config.charyparNagelScoring(), EUTController2.this.reactTTs, EUTController2.this.equipmentFraction, config.global().getRandomSeed(), EUTController2.this.getControlerIO());
			((GuidedAgentFactory)EUTController2.this.factory).setRouteAnalyzer(EUTController2.this.analyzer);

		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			((GuidedAgentFactory)EUTController2.this.factory).reset(event.getControler().getIterationNumber());
		}

	}

	public static void main(String args[]) {
		EUTController2 controller = new EUTController2(args);
		long time = System.currentTimeMillis();
		controller.run();
		System.out.println("Controller took " + (System.currentTimeMillis() - time) +" ms.");
	}

	private double string2Double(String str) {
		if(str.endsWith("%")) {
			return Integer.parseInt(str.substring(0, str.length()-1))/100.0;
		}
		return Double.parseDouble(str);

	}
}
