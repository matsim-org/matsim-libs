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
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.withinday.WithindayControler;
import org.matsim.withinday.mobsim.WithindayQueueSimulation;
import org.matsim.withinday.trafficmanagement.TrafficManagement;

/**
 * @author illenberger
 *
 */
public class EUTController extends WithindayControler {

	private static final String CONFIG_MODULE_NAME = "eut";

//	private static final Logger log = Logger.getLogger(EUTController.class);

	private TwoStateTTKnowledge ttmemory;

	private EstimReactiveLinkTT reactTTs;

	private EUTRouterAnalyzer routerAnalyzer;

	private double incidentProba;

	private double equipmentFraction;

	private double replanningFraction;

	private double ttLearningRate;

	private int maxMemorySlots;

	private double rho;

//	private RandomIncidentSimulator incidentSimulator;

	private EUTReRoute2 eutReRoute;

	private BenefitAnalyzer bAnalyzer;

	private SummaryWriter summaryWriter;

	private double capReduction;

	private List<Link> riskyLinks;


	/**
	 * @param args
	 */
	public EUTController(String[] args) {
		super(args);
		setOverwriteFiles(true);


	}

	@Override
	protected StrategyManager loadStrategyManager() {
		/*
		 * Initialize the travel time memory for day2day re-planning.
		 */
		this.ttmemory = new TwoStateTTKnowledge();
		this.ttmemory.setLearningRate(this.ttLearningRate);
		this.ttmemory.setMaxMemorySlots(this.maxMemorySlots);
		TimevariantTTStorage storage = this.ttmemory.makeTTStorage(getTravelTimeCalculator(), this.network, config.travelTimeCalculator().getTraveltimeBinSize(), 0, 86400);
		this.ttmemory.appendNewStorage(storage);
		/*
		 * Load the strategy manager.
		 */
		StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(1);
		PlanSelector selector = new BestPlanSelector();
		/*
		 * Add one EUTRouter and one empty module.
		 */
		PlanStrategy strategy = new PlanStrategy(selector);
		this.eutReRoute = new EUTReRoute2(getNetwork(), this.ttmemory, this.rho);
		strategy.addStrategyModule(this.eutReRoute);
		manager.addStrategy(strategy, this.replanningFraction);
		/*
		 * Do nothing...
		 */
		strategy = new PlanStrategy(selector);
		manager.addStrategy(strategy, 1 - this.replanningFraction);
		/*
		 * Create a router analyzer...
		 */
		this.routerAnalyzer = new EUTRouterAnalyzer(this.eutReRoute.getUtilFunction(), this.summaryWriter);
		this.eutReRoute.setRouterAnalyzer(this.routerAnalyzer);
		addControlerListener(this.routerAnalyzer);

		return manager;
	}

	@Override
	protected void setUp() {
		this.equipmentFraction = string2Double(getConfig().getParam(CONFIG_MODULE_NAME, "equipmentFraction"));
		this.replanningFraction = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "replanFraction"));
		this.incidentProba = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "incidentProba"));

		this.ttLearningRate = string2Double(getConfig().findParam(CONFIG_MODULE_NAME, "ttLearningRate"));
		this.maxMemorySlots = Integer.parseInt(getConfig().findParam(CONFIG_MODULE_NAME, "maxMemorySlots"));

		this.rho = Integer.parseInt(getConfig().findParam(CONFIG_MODULE_NAME, "rho"));


		this.summaryWriter = new SummaryWriter(getConfig().findParam(CONFIG_MODULE_NAME, "summaryFile"));
		addControlerListener(this.summaryWriter);
		super.setUp();

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
			Link link = getNetwork().getLinks().get(new IdImpl(id));
			riskyLinks.add(link);

		}
		/*
		 * Count agents traversed risky links...
		 */
		TraversedRiskyLink travRiskyLink = new TraversedRiskyLink(getPopulation(), riskyLinks, this.summaryWriter);


		TripAndScoreStats stats = new TripAndScoreStats(this.routerAnalyzer, travRiskyLink, this.summaryWriter);
		addControlerListener(stats);
		this.events.addHandler(stats);
		addControlerListener(travRiskyLink);
		/*
		 * Analyze benefits...
		 */
		this.bAnalyzer = new BenefitAnalyzer(stats, this.routerAnalyzer, this.ttmemory, this.eutReRoute.getUtilFunction(), this.summaryWriter, this.network);
		addControlerListener(this.bAnalyzer);
		/*
		 *
		 */
		String personsFile = getConfig().findParam(CONFIG_MODULE_NAME, "guidedPersons");
		addControlerListener(new CEAnalyzer(personsFile, this.population, stats, this.eutReRoute.getUtilFunction()));
		/*
		 *
		 */
		addControlerListener(new RemoveDuplicatePlans(this.network));
		addControlerListener(new RemoveScores());
	}

	@Override
	protected void runMobSim() {

		this.config.withinday().addParam("contentThreshold", "1");
		this.config.withinday().addParam("replanningInterval", "1");

		WithindayQueueSimulation sim = new WithindayQueueSimulation(this.scenarioData, this.events, this);
		this.trafficManagement = new TrafficManagement();
		sim.setTrafficManagement(this.trafficManagement);
		/*
		 * Initialize the reactive travel times.
		 */
//		this.reactTTs = new EstimReactiveLinkTT(sim.getQueueNetworkLayer());
		this.reactTTs = new EstimReactiveLinkTT(1, this.network); // TODO: Think about that!!!
		this.events.addHandler(this.reactTTs);
		this.reactTTs.reset(getIterationNumber());

		RandomIncidentSimulator simulator = new RandomIncidentSimulator(network, incidentProba, this.getControlerIO());
		simulator.setCapReduction(capReduction);
		for(Link link : riskyLinks)
//			simulator.addLink(sim.getQueueNetworkLayer().getQueueLink(link.getId()));
			simulator.addLink(null); //FIXME
		simulator.notifyIterationStarts(this.getIterationNumber());

		sim.run();

//		simulator.notifyIterationEnds(null);
		events.removeHandler(reactTTs);

	}

	private class TTMemotyUpdater implements IterationEndsListener {

		public void notifyIterationEnds(IterationEndsEvent event) {
			TimevariantTTStorage storage = EUTController.this.ttmemory.makeTTStorage(getTravelTimeCalculator(), EUTController.this.network, config.travelTimeCalculator().getTraveltimeBinSize(), 0, 86400);
			EUTController.this.ttmemory.appendNewStorage(storage);

		}

	}

	private class WithindayControlerListener implements StartupListener, IterationStartsListener {

		public void notifyStartup(StartupEvent event) {
			EUTController.this.factory = new GuidedAgentFactory(EUTController.this.network, EUTController.this.config.charyparNagelScoring(), EUTController.this.reactTTs, EUTController.this.equipmentFraction, config.global().getRandomSeed(), EUTController.this.getControlerIO());
			((GuidedAgentFactory)EUTController.this.factory).setRouteAnalyzer(EUTController.this.routerAnalyzer);
			((GuidedAgentFactory)EUTController.this.factory).setBenefitAnalyzer(EUTController.this.bAnalyzer);

		}

		public void notifyIterationStarts(IterationStartsEvent event) {
			((GuidedAgentFactory)EUTController.this.factory).reset(event.getIteration());
		}

	}

	public static void main(String args[]) {
		EUTController controller = new EUTController(args);
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
