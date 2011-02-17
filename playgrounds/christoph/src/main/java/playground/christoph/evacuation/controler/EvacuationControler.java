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

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalControler;
import org.matsim.ptproject.qsim.multimodalsimengine.MultiModalMobsimFactory;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.BufferedTravelTime;
import org.matsim.ptproject.qsim.multimodalsimengine.router.costcalculator.TravelTimeCalculatorWithBuffer;

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
import playground.christoph.withinday.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.withinday.events.parallelEventsHandler.SimStepParallelEventsManagerImpl;
import playground.christoph.withinday.mobsim.DuringActivityReplanningModule;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.InitialReplanningModule;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import playground.christoph.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import playground.christoph.withinday.replanning.identifiers.tools.LinkReplanningMap;
import playground.christoph.withinday.replanning.modules.ReplanningModule;
import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;
import playground.christoph.withinday.replanning.replanners.CurrentLegReplannerFactory;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import playground.christoph.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import playground.christoph.withinday.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.withinday.scoring.OnlyTimeDependentScoringFunctionFactory;
import playground.christoph.withinday.trafficmonitoring.TravelTimeCollector;
import playground.christoph.withinday.trafficmonitoring.TravelTimeCollectorFactory;

/*
 * Theimport playground.christoph.withinday.trafficmonitoring.TravelTimeCollector;
 Path to a Config File is needed as Argument to run the
 * Simulation.
 *
 * By default "../matsim/test/scenarios/berlin/config.xml" should work.
 *
 * @author Christoph Dobler
 */

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

	protected ReplanningManager replanningManager;
	protected WithinDayQSim sim;
	protected FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();

	private static final Logger log = Logger.getLogger(EvacuationControler.class);

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
			travelTime = new TravelTimeCollectorFactory().createFreeSpeedTravelTimeCalculator(this.scenarioData);
			foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeCollector) travelTime);	// for TravelTimeCollector
			foqsl.addQueueSimulationAfterSimStepListener((TravelTimeCollector) travelTime);	// for TravelTimeCollector
			this.events.addHandler((TravelTimeCollector) travelTime);	// for TravelTimeCollector
		}

		// without social costs
		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);

//		CloneablePlansCalcRoute router = new CloneablePlansCalcRoute(config.plansCalcRoute(), network, travelCost, travelTime, new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.charyparNagelScoring())));
//		MultiModalPlansCalcRoute router = new MultiModalPlansCalcRoute(config.plansCalcRoute(), network, travelCost, travelTime, new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.charyparNagelScoring())));
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.planCalcScore()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, factory);

//		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
//		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
//		this.initialReplanner.setReplanner(router);
//		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
//		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);

		/*
		 * Create ActivityReplanningMap here and reuse it for both duringActivityReplanners!
		 */
//		ActivityReplanningMap activityReplanningMap = new ActivityReplanningMap(this.getEvents(), this.getQueueSimulationListener());
//		ActivityReplanningMap activityReplanningMap = new ActivityReplanningMap(this.getEvents(), sim);
		ActivityReplanningMap activityReplanningMap = new ActivityReplanningMap(this.getEvents());
		foqsl.addQueueSimulationListener(activityReplanningMap);

		this.duringSecureActivityIdentifier = new SecureActivityPerformingIdentifierFactory(activityReplanningMap, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.duringSecureActivityReplanner = new ExtendCurrentActivityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringSecureActivityReplanner.addAgentsToReplanIdentifier(this.duringSecureActivityIdentifier);
		this.duringSecureActivityReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringActivityReplanner.addWithinDayReplanner(this.duringSecureActivityReplanner);

		this.duringInsecureActivityIdentifier = new InsecureActivityPerformingIdentifierFactory(activityReplanningMap, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.duringInsecureActivityReplanner = new EndActivityAndEvacuateReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringInsecureActivityReplanner.addAgentsToReplanIdentifier(this.duringInsecureActivityIdentifier);
		this.duringInsecureActivityReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringActivityReplanner.addWithinDayReplanner(this.duringInsecureActivityReplanner);

		/*
		 * Create LegReplanningMap here and reuse it for all three duringLegReplanners!
		 */
//		LinkReplanningMap linkReplanningMap = new LinkReplanningMap(this.getEvents(), this.getQueueSimulationListener());
//		LinkReplanningMap linkReplanningMap = new LinkReplanningMap(this.getEvents(), sim);
		LinkReplanningMap linkReplanningMap = new LinkReplanningMap(this.getEvents());
		foqsl.addQueueSimulationListener(linkReplanningMap);

		this.duringSecureLegIdentifier = new SecureLegPerformingIdentifierFactory(linkReplanningMap, network, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.duringSecureLegReplanner = new CurrentLegToSecureFacilityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringSecureLegReplanner.addAgentsToReplanIdentifier(this.duringSecureLegIdentifier);
		this.duringSecureLegReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringLegReplanner.addWithinDayReplanner(this.duringSecureLegReplanner);

		this.duringInsecureLegIdentifier = new InsecureLegPerformingIdentifierFactory(linkReplanningMap, network, EvacuationConfig.centerCoord, EvacuationConfig.innerRadius).createIdentifier();
		this.duringInsecureLegReplanner = new CurrentLegToRescueFacilityReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.duringInsecureLegReplanner.addAgentsToReplanIdentifier(this.duringInsecureLegIdentifier);
		this.duringInsecureLegReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringLegReplanner.addWithinDayReplanner(this.duringInsecureLegReplanner);

		this.currentInsecureLegIdentifier = new LeaveLinkIdentifierFactory(linkReplanningMap).createIdentifier();
		this.currentInsecureLegReplanner = new CurrentLegReplannerFactory(this.scenarioData, sim.getAgentCounter(), router, 1.0).createReplanner();
		this.currentInsecureLegReplanner.addAgentsToReplanIdentifier(this.currentInsecureLegIdentifier);
		this.currentInsecureLegReplanner.setReplanningProbability(pReplanning);
		this.parallelDuringLegReplanner.addWithinDayReplanner(this.currentInsecureLegReplanner);
	}

	/*
	 * Initializes the ParallelReplannerModules
	 */
	protected void initParallelReplanningModules() {
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads, this);
		this.parallelDuringActivityReplanner = new ParallelDuringActivityReplanner(numReplanningThreads, this);
		this.parallelDuringLegReplanner = new ParallelDuringLegReplanner(numReplanningThreads, this);
	}

	/*
	 * Creates the Handler and Listener Object so that they can be handed over to the Within Day
	 * Replanning Modules (TravelCostWrapper, etc).
	 *
	 * The full initialization of them is done later (we don't have all necessary Objects yet).
	 */
	protected void createHandlersAndListeners() {
		replanningManager = new ReplanningManager();
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
			this.foqsl.addQueueSimulationAfterSimStepListener(manager);
			this.events = manager;
		}
		
		super.setUp();

		createHandlersAndListeners();

		ReplanningFlagInitializer rfi = new ReplanningFlagInitializer(this);
		foqsl.addQueueSimulationInitializedListener(rfi);

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);

		this.getQueueSimulationListener().add(foqsl);

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

	public static class ReplanningFlagInitializer implements SimulationInitializedListener {

		protected EvacuationControler withinDayControler;
		protected Map<Id, WithinDayPersonAgent> withinDayPersonAgents;

		protected int noReplanningCounter = 0;
		protected int initialReplanningCounter = 0;
		protected int actEndReplanningCounter = 0;
		protected int leaveLinkReplanningCounter = 0;

		public ReplanningFlagInitializer(EvacuationControler controler) {
			this.withinDayControler = controler;
		}

		@Override
		public void notifySimulationInitialized(SimulationInitializedEvent e) {
			collectAgents((QSim)e.getQueueSimulation());
			setReplanningFlags();
		}

		protected void setReplanningFlags() {
			noReplanningCounter = 0;
			initialReplanningCounter = 0;
			actEndReplanningCounter = 0;
			leaveLinkReplanningCounter = 0;

			Random random = MatsimRandom.getLocalInstance();

			for (WithinDayPersonAgent withinDayPersonAgent : this.withinDayPersonAgents.values()) {
				double probability;
				boolean noReplanning = true;

				// No Replanner
				probability = random.nextDouble();
				if (probability > withinDayControler.pInitialReplanning) ;
				else {
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(withinDayControler.initialReplanner.getId());
					noReplanning = false;
					initialReplanningCounter++;
				}

				// During Activity Replanner
				probability = random.nextDouble();
				if (probability > withinDayControler.pDuringActivityReplanning) ;
				else {
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(withinDayControler.duringSecureActivityReplanner.getId());
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(withinDayControler.duringInsecureActivityReplanner.getId());
					noReplanning = false;
					actEndReplanningCounter++;
				}

				// During Leg Replanner
				probability = random.nextDouble();
				if (probability > withinDayControler.pDuringLegReplanning) ;
				else {
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(withinDayControler.duringSecureLegReplanner.getId());
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(withinDayControler.duringInsecureLegReplanner.getId());
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(withinDayControler.currentInsecureLegReplanner.getId());
					noReplanning = false;
					leaveLinkReplanningCounter++;
				}

				// if non of the Replanning Modules was activated
				if (noReplanning) noReplanningCounter++;

				// (de)activate replanning if they are not needed
				if (initialReplanningCounter == 0) withinDayControler.replanningManager.doInitialReplanning(false);
				else withinDayControler.replanningManager.doInitialReplanning(true);

				if (actEndReplanningCounter == 0) withinDayControler.replanningManager.doActEndReplanning(false);
				else withinDayControler.replanningManager.doActEndReplanning(true);

				if (leaveLinkReplanningCounter == 0) withinDayControler.replanningManager.doLeaveLinkReplanning(false);
				else withinDayControler.replanningManager.doLeaveLinkReplanning(true);
			}

			log.info("Initial Replanning Probability: " + withinDayControler.pInitialReplanning);
			log.info("Act End Replanning Probability: " + withinDayControler.pDuringActivityReplanning);
			log.info("Leave Link Replanning Probability: " + withinDayControler.pDuringLegReplanning);

			//double numPersons = withinDayControler.population.getPersons().size();
			double numPersons = this.withinDayPersonAgents.size();
			log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
			log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
			log.info(actEndReplanningCounter + " persons replan their plans after an activity (" + actEndReplanningCounter / numPersons * 100.0 + "%)");
			log.info(leaveLinkReplanningCounter + " persons replan their plans at each node (" + leaveLinkReplanningCounter / numPersons * 100.0 + "%)");
		}

		protected void collectAgents(QSim sim) {
			this.withinDayPersonAgents = new TreeMap<Id, WithinDayPersonAgent>();

			for (MobsimAgent mobsimAgent : ((WithinDayQSim) sim).getAgents()) {
				if (mobsimAgent instanceof PersonAgent) {
					PersonAgent personAgent = (PersonAgent) mobsimAgent;
					withinDayPersonAgents.put(personAgent.getId(), (WithinDayPersonAgent) personAgent);
				} else {
					log.warn("MobsimAgent was expected to be from type PersonAgent, but was from type " + mobsimAgent.getClass().toString());
				}
			}
		}

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
