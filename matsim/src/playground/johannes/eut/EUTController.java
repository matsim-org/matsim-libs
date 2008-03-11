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

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.interfaces.networks.basicNet.BasicLinkI;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.replanning.selectors.PlanSelectorI;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.WithindayCreateVehiclePersonAlgorithm;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

/**
 * @author illenberger
 *
 */
public class EUTController extends WithindayControler {
	
	private static final String CONFIG_MODULE_NAME = "eut";
	
//	private static final Logger log = Logger.getLogger(EUTController.class);
	
	private TravelTimeMemory ttmemory;
	
	private EstimReactiveLinkTT reactTTs;
	
	private EUTRouterAnalyzer routerAnalyzer;

	private double incidentProba;
	
	private double equipmentFraction;
	
	private double replanningFraction;
	
	private double ttLearningRate;
	
	private int maxMemorySlots;
	
	private double rho;
	
	private IterationStartsListener incidentSimulator;
	
	/**
	 * @param args
	 */
	public EUTController(String[] args) {
		super(args);
		setOverwriteFiles(true);
		
		
	}

//	@Override
//	public void run() {
//		// TODO Auto-generated method stub
//		super.run();
//		
//	}

	@Override
	protected StrategyManager loadStrategyManager() {
		/*
		 * Initialize the travel time memory for day2day re-planning.
		 */
		ttmemory = new TravelTimeMemory();
		ttmemory.setLearningRate(ttLearningRate);
		ttmemory.setMaxMemorySlots(maxMemorySlots);
		TimevariantTTStorage storage = ttmemory.makeTTStorage(getTravelTimeCalculator(), network, getTraveltimeBinSize(), 0, 86400);
		ttmemory.appendNewStorage(storage);
		/*
		 * Load the strategy manager.
		 */
		StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(1);
		PlanSelectorI selector = new KeepSelected();
		/*
		 * Add one EUTRouter and one empty module.
		 */
		PlanStrategy strategy = new PlanStrategy(selector);
		EUTReRoute eutReRoute = new EUTReRoute(getNetwork(), ttmemory, rho);
		strategy.addStrategyModule(eutReRoute);
		manager.addStrategy(strategy, replanningFraction);
		
		strategy = new PlanStrategy(selector);
		manager.addStrategy(strategy, 1 - replanningFraction);
		/*
		 * Create a router analyzer...
		 */
		routerAnalyzer = new EUTRouterAnalyzer(eutReRoute.getUtilFunction());
		eutReRoute.setRouterAnalyzer(routerAnalyzer);
		addControlerListener(routerAnalyzer);
		
		return manager;
	}

	@Override
	protected void setup() {
		equipmentFraction = string2Double(getConfig().getParam(CONFIG_MODULE_NAME, "equipmentFraction"));
		replanningFraction = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "replanFraction"));
		incidentProba = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "incidentProba"));
		
		ttLearningRate = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "ttLearningRate"));
		maxMemorySlots = Integer.parseInt(getConfig().findParam(CONFIG_MODULE_NAME, "maxMemorySlots"));
		
		rho = Integer.parseInt(getConfig().findParam(CONFIG_MODULE_NAME, "rho"));
		/*
		 * Dunno exactly where to place this...
		 */
		setTraveltimeBinSize(60);
		
		super.setup();
		/*
		 * Initialize the reactive travel times.
		 */
		reactTTs = new EstimReactiveLinkTT();
		events.addHandler((EstimReactiveLinkTT)reactTTs);
		/*
		 * Add the ttmemory updater for day2day re-replanning.
		 */
		addControlerListener(new TTMemotyUpdater());
		/*
		 * Create a new factory for our withinday agents.
		 */
		addControlerListener(new WithindayControlerListener());
		/*
		 * Trip stats...
		 */
		TripAndScoreStats stats = new TripAndScoreStats(routerAnalyzer); 
		addControlerListener(stats);
		events.addHandler(stats);
		/*
		 * Link stats...
		 */
		LinkTTVarianceStats linkStats = new LinkTTVarianceStats(getTravelTimeCalculator(), 25200, 32400, 60);
		addControlerListener(linkStats);
		/*
		 * Create incident simulator...
		 */
		incidentSimulator = new RandomIncidentSimulator((QueueNetworkLayer) getNetwork(), incidentProba);
		addControlerListener(incidentSimulator);
		/*
		 * Count agents traversed risky links...
		 */
		List<BasicLinkI> riskyLinks = new LinkedList<BasicLinkI>();
		riskyLinks.add(getNetwork().getLink("800"));
		riskyLinks.add(getNetwork().getLink("1100"));
		riskyLinks.add(getNetwork().getLink("1400"));
		addControlerListener(new TraversedRiskyLink(getPopulation(), riskyLinks));
	}

	@Override
	protected void runMobSim() {
		
		config.withinday().addParam("contentThreshold", "1");
		config.withinday().addParam("replanningInterval", "1");
		WithindayCreateVehiclePersonAlgorithm vehicleAlgo = new WithindayCreateVehiclePersonAlgorithm(this);

		//build the queuesim
		WithindayQueueSimulation sim = new WithindayQueueSimulation((QueueNetworkLayer)this.network, this.population, this.events, this);
		sim.setVehicleCreateAlgo(vehicleAlgo);
		trafficManagement = new TrafficManagement();
		sim.setTrafficManagement(trafficManagement);
		//run the simulation
//		long time = System.currentTimeMillis();
//		QueueSimulation sim = new QueueSimulation((QueueNetworkLayer)this.network, this.population, this.events);
		sim.run();
//		System.err.println("Mobsim took " + (System.currentTimeMillis() - time) +" ms.")
	}

	private class TTMemotyUpdater implements IterationEndsListener {

		public void notifyIterationEnds(IterationEndsEvent event) {
			TimevariantTTStorage storage = ttmemory.makeTTStorage(getTravelTimeCalculator(), network, getTraveltimeBinSize(), 0, 86400);
			ttmemory.appendNewStorage(storage);
			
		}
		
	}
	
	private class WithindayControlerListener implements StartupListener, IterationStartsListener {

		public void notifyStartup(StartupEvent event) {
			EUTController.this.factory = new GuidedAgentFactory(network, config.charyparNagelScoring(), reactTTs, equipmentFraction);
			((GuidedAgentFactory)factory).setRouteAnalyzer(routerAnalyzer);
		
		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			((GuidedAgentFactory)factory).reset();
		}
		
	}
		
	public static void main(String args[]) {
//		EUTController controller = new EUTController(new String[]{"/Users/fearonni/vsp-work/eut/corridor/config/config.xml"});
		EUTController controller = new EUTController(args);
		controller.run();
	}

	private double string2Double(String str) {
		if(str.endsWith("%"))
			return Integer.parseInt(str.substring(0, str.length()-1))/100.0;
		else
			return Double.parseDouble(str);
		
	}
}
