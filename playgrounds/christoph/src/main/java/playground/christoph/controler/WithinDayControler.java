/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.controler;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.interfaces.QSimI;

import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.replanning.MyStrategyManagerConfigLoader;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;
import playground.christoph.withinday.mobsim.DuringActivityReplanningModule;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.InitialReplanningModule;
import playground.christoph.withinday.mobsim.KnowledgeWithinDayQSim;
import playground.christoph.withinday.mobsim.KnowledgeWithinDayQSimFactory;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.network.WithinDayLinkFactoryImpl;
import playground.christoph.withinday.replanning.CurrentLegReplanner;
import playground.christoph.withinday.replanning.InitialReplanner;
import playground.christoph.withinday.replanning.NextLegReplanner;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.TravelTimeCollector;
import playground.christoph.withinday.replanning.WithinDayDuringActivityReplanner;
import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;
import playground.christoph.withinday.replanning.WithinDayInitialReplanner;
import playground.christoph.withinday.replanning.identifiers.ActivityEndIdentifier;
import playground.christoph.withinday.replanning.identifiers.InitialIdentifierImpl;
import playground.christoph.withinday.replanning.identifiers.LeaveLinkIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import playground.christoph.withinday.replanning.modules.ReplanningModule;
import playground.christoph.withinday.replanning.parallel.ParallelDuringActivityReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;

/**
 * This Controler should give an Example what is needed to run
 * Simulations with WithinDayReplanning.
 *
 * The Path to a Config File is needed as Argument to run the
 * Simulation.
 *
 * By default "../matsim/test/scenarios/berlin/config.xml" should work.
 *
 * @author Christoph Dobler
 */

public class WithinDayControler extends Controler {

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	protected double pInitialReplanning = 0.0;
	protected double pActEndReplanning = 1.0;
	protected double pLeaveLinkReplanning = 0.0;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 6;

	protected PersonalizableTravelTime travelTime;

	protected ParallelInitialReplanner parallelInitialReplanner;
	protected ParallelDuringActivityReplanner parallelActEndReplanner;
	protected ParallelDuringLegReplanner parallelLeaveLinkReplanner;
	protected InitialIdentifier initialIdentifier;
	protected DuringActivityIdentifier duringActivityIdentifier;
	protected DuringLegIdentifier duringLegIdentifier;
	protected WithinDayInitialReplanner initialReplanner;
	protected WithinDayDuringActivityReplanner duringActivityReplanner;
	protected WithinDayDuringLegReplanner duringLegReplanner;

	protected ReplanningManager replanningManager;
	protected KnowledgeWithinDayQSim sim;
	protected FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();

	private static final Logger log = Logger.getLogger(WithinDayControler.class);

	public WithinDayControler(String[] args) {
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public WithinDayControler(Config config) {
		super(config);

		setConstructorParameters();
	}

	private void setConstructorParameters() {
		/*
		 * Use MyLinkImpl. They can carry some additional Information like their
		 * TravelTime or VehicleCount.
		 * This is needed to be able to use a LinkVehiclesCounter.
		 */
//		this.getNetwork().getFactory().setLinkFactory(new MyLinkFactoryImpl());
		this.getNetwork().getFactory().setLinkFactory(new WithinDayLinkFactoryImpl());

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {

		travelTime = new TravelTimeCollector(network);
		foqsl.addQueueSimulationInitializedListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		foqsl.addQueueSimulationAfterSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		this.events.addHandler((TravelTimeCollector)travelTime);	// for TravelTimeCollector

//		travelTime = new FreeSpeedTravelTime();

		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);

//		CloneablePlansCalcRoute dijkstraRouter = new CloneablePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCost, travelTime);
		LeastCostPathCalculatorFactory factory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config.charyparNagelScoring()));
		AbstractMultithreadedModule router = new ReplanningModule(config, network, travelCost, travelTime, factory);

		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
		this.initialReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.initialReplanner.setAbstractMultithreadedModule(router);
		this.initialReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.initialReplanner);

		this.duringActivityIdentifier = new ActivityEndIdentifier(this);
		this.duringActivityReplanner = new NextLegReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData, this.events);
		this.duringActivityReplanner.setAbstractMultithreadedModule(router);
		this.duringActivityReplanner.addAgentsToReplanIdentifier(this.duringActivityIdentifier);
		this.parallelActEndReplanner.addWithinDayReplanner(this.duringActivityReplanner);

		this.duringLegIdentifier = new LeaveLinkIdentifier(this);
		this.duringLegReplanner = new CurrentLegReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData, this.getEvents());
		this.duringLegReplanner.setAbstractMultithreadedModule(router);
		this.duringLegReplanner.addAgentsToReplanIdentifier(this.duringLegIdentifier);
		this.parallelLeaveLinkReplanner.addWithinDayReplanner(this.duringLegReplanner);
	}

	/*
	 * Initializes the ParallelReplannerModules
	 */
	protected void initParallelReplanningModules() {
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads, this);
		this.parallelActEndReplanner = new ParallelDuringActivityReplanner(numReplanningThreads, this);
		this.parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(numReplanningThreads, this);
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
	protected void runMobSim() {
		createHandlersAndListeners();

		sim = new KnowledgeWithinDayQSimFactory().createMobsim(this.scenarioData, this.events);

		ReplanningFlagInitializer rfi = new ReplanningFlagInitializer(this);
		foqsl.addQueueSimulationInitializedListener(rfi);

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);

		sim.addQueueSimulationListeners(foqsl);

		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();

		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		InitialReplanningModule initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		DuringActivityReplanningModule actEndReplanning = new DuringActivityReplanningModule(parallelActEndReplanner);
		DuringLegReplanningModule leaveLinkReplanning = new DuringLegReplanningModule(parallelLeaveLinkReplanner);

		replanningManager.setInitialReplanningModule(initialReplanningModule);
		replanningManager.setActEndReplanningModule(actEndReplanning);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);

		sim.run();
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		MyStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}


	public static class ReplanningFlagInitializer implements SimulationInitializedListener {

		protected WithinDayControler withinDayControler;
		protected Map<Id, WithinDayPersonAgent> withinDayPersonAgents;

		protected int noReplanningCounter = 0;
		protected int initialReplanningCounter = 0;
		protected int actEndReplanningCounter = 0;
		protected int leaveLinkReplanningCounter = 0;

		public ReplanningFlagInitializer(WithinDayControler controler) {
			this.withinDayControler = controler;
		}

		@Override
		public void notifySimulationInitialized(SimulationInitializedEvent e) {
			collectAgents((QSimI) e.getQueueSimulation());
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

				probability = random.nextDouble();
				if (probability > withinDayControler.pInitialReplanning) ;
				else {
					withinDayPersonAgent.addWithinDayReplanner(withinDayControler.initialReplanner);
					noReplanning = false;
					initialReplanningCounter++;
				}

				// No Replanning
				probability = random.nextDouble();
				if (probability > withinDayControler.pActEndReplanning);
				else {
					withinDayPersonAgent.addWithinDayReplanner(withinDayControler.duringActivityReplanner);
					noReplanning = false;
					actEndReplanningCounter++;
				}

				// No Replanning
				probability = random.nextDouble();
				if (probability > withinDayControler.pLeaveLinkReplanning) ;
				else {
					withinDayPersonAgent.addWithinDayReplanner(withinDayControler.duringLegReplanner);
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
			log.info("Act End Replanning Probability: " + withinDayControler.pActEndReplanning);
			log.info("Leave Link Replanning Probability: " + withinDayControler.pLeaveLinkReplanning);

			double numPersons = withinDayControler.population.getPersons().size();
			log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
			log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
			log.info(actEndReplanningCounter + " persons replan their plans after an activity (" + actEndReplanningCounter / numPersons * 100.0 + "%)");
			log.info(leaveLinkReplanningCounter + " persons replan their plans at each node (" + leaveLinkReplanningCounter / numPersons * 100.0 + "%)");
		}

		protected void collectAgents(QSimI sim) {
			this.withinDayPersonAgents = new TreeMap<Id, WithinDayPersonAgent>();

			for (PersonAgent personAgent : ((WithinDayQSim) sim).getPersonAgents().values()) {
				withinDayPersonAgents.put(personAgent.getPerson().getId(), (WithinDayPersonAgent) personAgent);
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
			final WithinDayControler controler = new WithinDayControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

	public static class FreeSpeedTravelTime implements PersonalizableTravelTime, Cloneable {

		@Override
		public double getLinkTravelTime(Link link, double time) {
			return link.getFreespeed(time);
		}

		@Override
		public FreeSpeedTravelTime clone() {
			return new FreeSpeedTravelTime();
		}

		@Override
		public void setPerson(Person person) {
			// nothing to do here
		}
	}
}
