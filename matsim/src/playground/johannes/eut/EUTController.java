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

import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
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
		EUTReRoute eutReRoute = new EUTReRoute(getNetwork(), ttmemory);
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
		equipmentFraction = Double.parseDouble(getConfig().getParam(CONFIG_MODULE_NAME, "equipmentFraction"));
		replanningFraction = Double.parseDouble(getConfig().findParam(CONFIG_MODULE_NAME, "replanFraction"));
		incidentProba = Double.parseDouble(getConfig().findParam(CONFIG_MODULE_NAME, "incidentProba"));
		
		ttLearningRate = Double.parseDouble(getConfig().findParam(CONFIG_MODULE_NAME, "ttLearningRate"));
		maxMemorySlots = Integer.parseInt(getConfig().findParam(CONFIG_MODULE_NAME, "maxMemorySlots"));
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
		factory = new GuidedAgentFactory(network, config.charyparNagelScoring(), reactTTs, equipmentFraction);
		((GuidedAgentFactory)factory).setRouteAnalyzer(routerAnalyzer);
		addControlerListener(((GuidedAgentFactory)factory));
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
		
		incidentSimulator = new RandomIncidentSimulator((QueueNetworkLayer) getNetwork(), incidentProba);
		addControlerListener(incidentSimulator);
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
		
	public static void main(String args[]) {
		EUTController controller = new EUTController(new String[]{"/Users/fearonni/vsp-work/eut/corridor/config/config.xml"});
		controller.run();
	}

}
