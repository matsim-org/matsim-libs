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

import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.network.Link;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

/**
 * @author illenberger
 *
 */
public class EUTController2 extends WithindayControler {

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
	/**
	 * @param args
	 */
	public EUTController2(String[] args) {
		super(args);
		setOverwriteFiles(true);


	}

	@Override
	protected void setup() {
		this.equipmentFraction = string2Double(getConfig().getParam(CONFIG_MODULE_NAME, "equipmentFraction"));
//		replanningFraction = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "replanFraction"));
		this.incidentProba = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "incidentProba"));

		this.rho = Integer.parseInt(getConfig().findParam(CONFIG_MODULE_NAME, "rho"));


		this.summaryWriter = new SummaryWriter(getConfig().findParam(CONFIG_MODULE_NAME, "summaryFile"));
		addControlerListener(this.summaryWriter);
		super.setup();
		/*
		 * Create a new factory for our withinday agents.
		 */
		addControlerListener(new WithindayControlerListener());
		/*
		 * Trip stats...
		 */
		CARAFunction utilFunction = new CARAFunction(this.rho);
		this.analyzer = new EUTRouterAnalyzer(utilFunction, this.summaryWriter);
		addControlerListener(this.analyzer);
		/*
		 * Link stats...
		 */
		LinkTTVarianceStats linkStats = new LinkTTVarianceStats(getTravelTimeCalculator(), 25200, 32400, 60, this.summaryWriter);
		addControlerListener(linkStats);
		/*
		 * Create incident simulator...
		 */
		String linkIds = getConfig().findParam(CONFIG_MODULE_NAME, "links");
		capReduction = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "capReduction"));
		riskyLinks = new LinkedList<Link>();
		for(String id : linkIds.split(" ")) {
			Link link = getNetwork().getLink(id);
			riskyLinks.add(link);
			
		}
		/*
		 * Count agents traversed risky links...
		 */
		TraversedRiskyLink travRiskyLink = new TraversedRiskyLink(getPopulation(), riskyLinks, this.summaryWriter);


		TripAndScoreStats stats = new TripAndScoreStats(this.analyzer, travRiskyLink, this.summaryWriter);
		addControlerListener(stats);
		this.events.addHandler(stats);
		addControlerListener(travRiskyLink);
		/*
		 *
		 */
		String personsFile = getConfig().findParam(CONFIG_MODULE_NAME, "guidedPersons");
		addControlerListener(new CEAnalyzer(personsFile, this.population, stats, utilFunction));
		/*
		 *
		 */
		this.scoringFunctionFactory = new EUTScoringFactory(utilFunction);
		/*
		 *
		 */
		addControlerListener(new RemoveDuplicatePlans());
		addControlerListener(new RemoveScores());
	}

	@Override
	protected void runMobSim() {

		this.config.withinday().addParam("contentThreshold", "1");
		this.config.withinday().addParam("replanningInterval", "1");

		WithindayQueueSimulation sim = new WithindayQueueSimulation(this.network, this.population, this.events, this);
		this.trafficManagement = new TrafficManagement();
		sim.setTrafficManagement(this.trafficManagement);
		/*
		 * Initialize the reactive travel times.
		 */
//		this.reactTTs = new EstimReactiveLinkTT(sim.getQueueNetworkLayer());
		this.reactTTs = new EstimReactiveLinkTT(null);
		this.events.addHandler(this.reactTTs);
		this.reactTTs.reset(getIteration());
		
		RandomIncidentSimulator simulator = new RandomIncidentSimulator(incidentProba);
		simulator.setCapReduction(capReduction);
		for(Link link : riskyLinks)
//			simulator.addLink(sim.getQueueNetworkLayer().getQueueLink(link.getId()));
			simulator.addLink(null);
		simulator.notifyIterationStarts(EUTController.getIteration());
		
		sim.run();
		
		simulator.notifyIterationEnds(null);
		events.removeHandler(reactTTs);

	}


	private class WithindayControlerListener implements StartupListener, IterationStartsListener {

		public void notifyStartup(StartupEvent event) {
			EUTController2.this.factory = new GuidedAgentFactory(EUTController2.this.network, EUTController2.this.config.charyparNagelScoring(), EUTController2.this.reactTTs, EUTController2.this.equipmentFraction);
			((GuidedAgentFactory)EUTController2.this.factory).setRouteAnalyzer(EUTController2.this.analyzer);

		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			((GuidedAgentFactory)EUTController2.this.factory).reset();
		}

	}

	public static void main(String args[]) {
//		EUTController controller = new EUTController(new String[]{"/Users/fearonni/vsp-work/eut/corridor/config/config.xml"});
		EUTController2 controller = new EUTController2(args);
		long time = System.currentTimeMillis();
		controller.run();
		System.out.println("Controller took " + (System.currentTimeMillis() - time) +" ms.");
	}

	private double string2Double(String str) {
		if(str.endsWith("%"))
			return Integer.parseInt(str.substring(0, str.length()-1))/100.0;
		else
			return Double.parseDouble(str);

	}
}
