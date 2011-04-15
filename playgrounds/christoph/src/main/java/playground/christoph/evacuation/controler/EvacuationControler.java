/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.OnlyTimeDependentScoringFunctionFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalControler;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalMobsimFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.BufferedTravelTime;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.TravelTimeCalculatorWithBuffer;
import org.matsim.withinday.mobsim.DuringActivityReplanningModule;
import org.matsim.withinday.mobsim.DuringLegReplanningModule;
import org.matsim.withinday.mobsim.InitialReplanningModule;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.mobsim.WithinDayQSim;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.SelectHandledAgentsByProbability;
import org.matsim.withinday.replanning.modules.ReplanningModule;
import org.matsim.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import org.matsim.withinday.replanning.parallel.ParallelDuringLegReplanner;
import org.matsim.withinday.replanning.parallel.ParallelInitialReplanner;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.EvacuationDuringActivityReplanningModule;
import playground.christoph.evacuation.mobsim.EvacuationDuringLegReplanningModule;
import playground.christoph.evacuation.withinday.replanning.identifiers.InsecureActivityPerformingIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.InsecureLegPerformingIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.SecureActivityPerformingIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.identifiers.SecureLegPerformingIdentifierFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToRescueFacilityReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToSecureFacilityReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.EndActivityAndEvacuateReplannerFactory;
import playground.christoph.evacuation.withinday.replanning.replanners.ExtendCurrentActivityReplannerFactory;

public class EvacuationControler extends MultiModalControler {

	/*
	 * If more than one iteration are simulated, within day replanning is
	 * deactivated (replanning is performed only one when the evacuation
	 * starts).
	 */
	public boolean doIterations = true;

//	public boolean multiModal = true;

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	protected double pInitialReplanning = 0.0;
	protected double pDuringActivityReplanning = 1.0;
	protected double pDuringLegReplanning = 1.0;

	/*
	 * Probability, that an agent really performs a replanning.
	 * Should be 1.0 for within day runs and ~ 0.1 for iterative runs.
	 */
	protected double pReplanning = 0.1;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 6;

	/*
	 * The EventsFile which is used to get the initial TravelTimes for the
	 * TravelTimeCalculator.
	 */
	protected String initialEventsFile = "../../matsim/mysimulations/multimodal/input_10pct_zrhCutC/events_initial_traveltimes.txt.gz";

	protected PersonalizableTravelTime travelTime;

	protected ParallelInitialReplanner parallelInitialReplanner;
	protected ParallelDuringActivityReplanner parallelDuringActivityReplanner;
	protected ParallelDuringLegReplanner parallelDuringLegReplanner;
	protected InitialIdentifier initialIdentifier;
	protected DuringActivityIdentifier duringSecureActivityIdentifier;
	protected DuringActivityIdentifier duringInsecureActivityIdentifier;
	protected DuringLegIdentifier duringSecureLegIdentifier;
	protected DuringLegIdentifier duringInsecureLegIdentifier;
	protected DuringLegIdentifier currentInsecureLegIdentifier;
	protected WithinDayInitialReplanner initialReplanner;
	protected WithinDayDuringActivityReplanner duringSecureActivityReplanner;
	protected WithinDayDuringActivityReplanner duringInsecureActivityReplanner;
	protected WithinDayDuringLegReplanner duringSecureLegReplanner;
	protected WithinDayDuringLegReplanner duringInsecureLegReplanner;
	protected WithinDayDuringLegReplanner currentInsecureLegReplanner;

	protected SelectHandledAgentsByProbability selector;
	protected ReplanningManager replanningManager;
	protected WithinDayQSim sim;
	protected FixedOrderSimulationListener fosl;
	
	static final Logger log = Logger.getLogger(EvacuationControler.class);

	public EvacuationControler(String[] args) {
		super(args);

		setConstructorParameters();
	}

//	// only for Batch Runs
//	public EvacuationControler(Config config) {
//		super(config);
//
//		setConstructorParameters();
//	}

	private void setConstructorParameters() {

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());

//		already set in Multi Modal Controler
//		/*
//		 * Use a TravelTimeCalculator that buffers the TravelTimes form the
//		 * previous Iteration.
//		 */
//		setTravelTimeCalculatorFactory(new TravelTimeCalculatorWithBufferFactory());
	}

	@Override
	protected void loadData() {
		super.loadData();

		/*
		 * both only for WithinDay without iterations
		 */
//		// Add Rescue Links to Network
//		new AddExitLinksToNetwork(this.scenarioData).createExitLinks();

//		// Add secure Facilities to secure Links.
//		new AddSecureFacilitiesToNetwork(this.scenarioData).createSecureFacilities();
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	protected void initReplanningRouter() {

		// iterative
		if (doIterations) {
			travelTime = new BufferedTravelTime((TravelTimeCalculatorWithBuffer) this.getTravelTimeCalculator());

			((TravelTimeCalculatorWithBuffer) this.getTravelTimeCalculator()).initTravelTimes(initialEventsFile);
		}
		// within day
		else {
			travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(this.scenarioData);
			fosl.addSimulationBeforeSimStepListener((TravelTimeCollector) travelTime);	// for TravelTimeCollector
			fosl.addSimulationAfterSimStepListener((TravelTimeCollector) travelTime);	// for TravelTimeCollector
			this.events.addHandler((TravelTimeCollector) travelTime);	// for TravelTimeCollector
		}

		// without social costs
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);

//		CloneablePlansCalcRoute router = new CloneablePlansCalcRoute(config.plansCalcRoute(), network, travelCost, travelTime, new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.charyparNagelScoring())));
//		MultiModalPlansCalcRoute router = new MultiModalPlansCalcRoute(config.plansCalcRoute(), network, travelCost, travelTime, new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.charyparNagelScoring())));
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, factory);

//		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
//		this.selector.addIdentifier(initialIdentifier, pInitialReplanning);
//		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
//		this.initialReplanner.setReplanner(router);
//		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
//		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);

		/*
		 * Create ActivityReplanningMap here and reuse it for both duringActivityReplanners!
		 */
		ActivityReplanningMap activityReplanningMap = new ActivityReplanningMap();
		this.getEvents().addHandler(activityReplanningMap);
		fosl.addSimulationListener(activityReplanningMap);

		this.duringSecureActivityIdentifier = new SecureActivityPerformingIdentifierFactory(activityReplanningMap, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.selector.addIdentifier(this.duringSecureActivityIdentifier, this.pDuringActivityReplanning);
		this.duringSecureActivityReplanner = new ExtendCurrentActivityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringSecureActivityReplanner.addAgentsToReplanIdentifier(this.duringSecureActivityIdentifier);
		this.duringSecureActivityReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringActivityReplanner.addWithinDayReplanner(this.duringSecureActivityReplanner);

		this.duringInsecureActivityIdentifier = new InsecureActivityPerformingIdentifierFactory(activityReplanningMap, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.selector.addIdentifier(this.duringInsecureActivityIdentifier, this.pDuringActivityReplanning);
		this.duringInsecureActivityReplanner = new EndActivityAndEvacuateReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringInsecureActivityReplanner.addAgentsToReplanIdentifier(this.duringInsecureActivityIdentifier);
		this.duringInsecureActivityReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringActivityReplanner.addWithinDayReplanner(this.duringInsecureActivityReplanner);

		/*
		 * Create LegReplanningMap here and reuse it for all three duringLegReplanners!
		 */
		LinkReplanningMap linkReplanningMap = new LinkReplanningMap();
		this.getEvents().addHandler(linkReplanningMap);
		fosl.addSimulationListener(linkReplanningMap);

		this.duringSecureLegIdentifier = new SecureLegPerformingIdentifierFactory(linkReplanningMap, network, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.selector.addIdentifier(this.duringSecureLegIdentifier, this.pDuringLegReplanning);
		this.duringSecureLegReplanner = new CurrentLegToSecureFacilityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringSecureLegReplanner.addAgentsToReplanIdentifier(this.duringSecureLegIdentifier);
		this.duringSecureLegReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringLegReplanner.addWithinDayReplanner(this.duringSecureLegReplanner);

		this.duringInsecureLegIdentifier = new InsecureLegPerformingIdentifierFactory(linkReplanningMap, network, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.selector.addIdentifier(this.duringInsecureLegIdentifier, this.pDuringLegReplanning);
		this.duringInsecureLegReplanner = new CurrentLegToRescueFacilityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringInsecureLegReplanner.addAgentsToReplanIdentifier(this.duringInsecureLegIdentifier);
		this.duringInsecureLegReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringLegReplanner.addWithinDayReplanner(this.duringInsecureLegReplanner);

		this.currentInsecureLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.selector.addIdentifier(this.currentInsecureLegIdentifier, this.pDuringLegReplanning);
		this.currentInsecureLegReplanner = new CurrentLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.currentInsecureLegReplanner.addAgentsToReplanIdentifier(this.currentInsecureLegIdentifier);
		this.currentInsecureLegReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringLegReplanner.addWithinDayReplanner(this.currentInsecureLegReplanner);
	}

	/*
	 * Initializes the ParallelReplannerModules
	 */
	protected void initParallelReplanningModules() {
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads);
		this.parallelDuringActivityReplanner = new ParallelDuringActivityReplanner(numReplanningThreads);
		this.parallelDuringLegReplanner = new ParallelDuringLegReplanner(numReplanningThreads);
		
		this.getQueueSimulationListener().add(this.parallelInitialReplanner);
		this.getQueueSimulationListener().add(this.parallelDuringActivityReplanner);
		this.getQueueSimulationListener().add(this.parallelDuringLegReplanner);
	}

	@Override
	protected void setUp() {
		/*
		 * SimStepParallelEventsManagerImpl might be moved to org.matsim.
		 * Then this piece of code could be placed in the controller.
		 */
		if (this.events instanceof ParallelEventsManagerImpl) {
			log.info("Replacing ParallelEventsManagerImpl with SimStepParallelEventsManagerImpl. This is needed for Within-Day Replanning.");
			SimStepParallelEventsManagerImpl manager = new SimStepParallelEventsManagerImpl();
			this.fosl.addSimulationAfterSimStepListener(manager);
			this.events = manager;
		}
		
		super.setUp();

		replanningManager = new ReplanningManager();

		selector = new SelectHandledAgentsByProbability();
		fosl.addSimulationListener(selector);

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		fosl.addSimulationInitializedListener(replanningManager);
		fosl.addSimulationBeforeSimStepListener(replanningManager);

		this.getQueueSimulationListener().add(fosl);

		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();

		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		InitialReplanningModule initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		DuringActivityReplanningModule actEndReplanning = new EvacuationDuringActivityReplanningModule(parallelDuringActivityReplanner);
		DuringLegReplanningModule leaveLinkReplanning = new EvacuationDuringLegReplanningModule(parallelDuringLegReplanner);

		replanningManager.setInitialReplanningModule(initialReplanningModule);
		replanningManager.setActEndReplanningModule(actEndReplanning);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);

		log.info("Creating additional Plans");
		clonePlans();
	}

	/*
	 * Ensure that each person holds the maximum amount of allowed Plans.
	 * This is necessary because the replanning is done within day where
	 * no additional plans are created.
	 */
	protected void clonePlans() {
		if (doIterations) {
			for (Person person : this.scenarioData.getPopulation().getPersons().values()) {
				while (person.getPlans().size() < this.config.strategy().getMaxAgentPlanMemorySize()) {
					((PersonImpl) person).copySelectedPlan();
				}
			}
		}
	}

	/*
	 * Always use a EvacuationQSimFactory - it will return
	 * a (Parallel)QSim using a MultiModalQNetwork.
	 */
	@Override
	public MobsimFactory getMobsimFactory() {
		return new MultiModalMobsimFactory(super.getMobsimFactory(), travelTime);
	}

	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	public static void main(final String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final EvacuationControler controler = new EvacuationControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

}
