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

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.events.LinkReplanningMap;
import playground.christoph.events.LinkVehiclesCounter2;
import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.events.algorithms.ParallelActEndReplanner;
import playground.christoph.events.algorithms.ParallelInitialReplanner;
import playground.christoph.events.algorithms.ParallelLeaveLinkReplanner;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.mobsim.ActEndReplanningModule;
import playground.christoph.mobsim.LeaveLinkReplanningModule;
import playground.christoph.mobsim.ReplanningManager;
import playground.christoph.mobsim.ReplanningQueueSimulation;
import playground.christoph.network.MyLinkFactoryImpl;
import playground.christoph.replanning.MyStrategyManagerConfigLoader;
import playground.christoph.replanning.TravelTimeCollector;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;

/**
 * This Controler should give an Example what is needed to run
 * Simulations with WithinDayReplanning.
 *
 * The Path to a Config File is needed as Argument to run the
 * Simulation.
 *
 * By default "test/scenarios/berlin/config.xml" should work.
 *
 * @author Christoph Dobler
 */

//mysimulations/kt-zurich/configIterative.xml

public class WithinDayControler extends Controler {

	protected ArrayList<PlanAlgorithm> replanners;
	protected ArrayList<SelectNodes> nodeSelectors;

	/*
	 * Define the Probability that an Agent uses the
	 * Replanning Strategy. It is possible to assign
	 * multiple Strategies to the Agents.
	 */
	protected double pInitialReplanning = 0.0;
	protected double pActEndReplanning = 1.0;
	protected double pLeaveLinkReplanning = 0.0;

	protected int noReplanningCounter = 0;
	protected int initialReplanningCounter = 0;
	protected int actEndReplanningCounter = 0;
	protected int leaveLinkReplanningCounter = 0;

	/*
	 * How many parallel Threads shall do the Replanning.
	 */
	protected int numReplanningThreads = 8;

	protected TravelTime travelTime;
	protected ParallelInitialReplanner parallelInitialReplanner;
	protected ParallelActEndReplanner parallelActEndReplanner;
	protected ParallelLeaveLinkReplanner parallelLeaveLinkReplanner;
	protected LinkVehiclesCounter2 linkVehiclesCounter;
	protected LinkReplanningMap linkReplanningMap;
	protected ReplanningManager replanningManager;
	protected ReplanningQueueSimulation sim;
	protected FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();

	private static final Logger log = Logger.getLogger(WithinDayControler.class);

	public WithinDayControler(String[] args)
	{
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public WithinDayControler(Config config)
	{
		super(config);

		setConstructorParameters();
	}

	private void setConstructorParameters()
	{
		/*
		 * Use MyLinkImpl. They can carry some additional Information like their
		 * TravelTime or VehicleCount.
		 * This is needed to be able to use a LinkVehiclesCounter.
		 */
		this.getNetwork().getFactory().setLinkFactory(new MyLinkFactoryImpl());

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {
		replanners = new ArrayList<PlanAlgorithm>();

		/*
		 * Calculate the TravelTime based on the actual load of the links. Use only
		 * the TravelTime to find the LeastCostPath.
		 */
//		travelTime = new KnowledgeTravelTimeCalculator(sim.getQueueNetwork());
		// fully initialize & add LinkVehiclesCounter
//		linkVehiclesCounter.setQueueNetwork(sim.getQueueNetwork());	// for KnowledgeTravelTimeCalculator
//		foqsl.addQueueSimulationInitializedListener(linkVehiclesCounter);	// for KnowledgeTravelTimeCalculator
//		foqsl.addQueueSimulationAfterSimStepListener(linkVehiclesCounter);	// for KnowledgeTravelTimeCalculator
//		this.events.addHandler(linkVehiclesCounter);	// for KnowledgeTravelTimeCalculator

//		TravelTimeEstimatorHandlerAndListener handlerAndListener = new TravelTimeEstimatorHandlerAndListener(population, network, 60, 86400 * 2);
//		travelTime = new TravelTimeEstimator(handlerAndListener);
//		foqsl.addQueueSimulationInitializedListener((TravelTimeEstimatorHandlerAndListener)handlerAndListener); // for TravelTimeEstimator
//		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeEstimatorHandlerAndListener)handlerAndListener);	// for TravelTimeEstimator
//		this.events.addHandler((TravelTimeEstimatorHandlerAndListener)handlerAndListener);	// for TravelTimeEstimator

		travelTime = new TravelTimeCollector(network);
		foqsl.addQueueSimulationBeforeSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		foqsl.addQueueSimulationAfterSimStepListener((TravelTimeCollector)travelTime);	// for TravelTimeCollector
		this.events.addHandler((TravelTimeCollector)travelTime);	// for TravelTimeCollector

		OnlyTimeDependentTravelCostCalculator travelCost = new OnlyTimeDependentTravelCostCalculator(travelTime);
//		SystemOptimalTravelCostCalculator travelCost = new SystemOptimalTravelCostCalculator(travelTime);

		KnowledgePlansCalcRoute dijkstraRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, travelCost, travelTime);
		replanners.add(dijkstraRouter);
	}

	/*
	 * Hands over the ArrayList to the ParallelReplannerModules
	 */
	protected void initParallelReplanningModules()
	{
		this.parallelInitialReplanner = new ParallelInitialReplanner();
		this.parallelInitialReplanner.setNumberOfThreads(numReplanningThreads);
		this.parallelInitialReplanner.setReplannerArrayList(replanners);
		this.parallelInitialReplanner.createReplannerArray();
		this.parallelInitialReplanner.init();

		this.parallelActEndReplanner = new ParallelActEndReplanner();
		this.parallelActEndReplanner.setNumberOfThreads(numReplanningThreads);
		this.parallelActEndReplanner.setReplannerArrayList(replanners);
		this.parallelActEndReplanner.setReplannerArray(parallelInitialReplanner.getReplannerArray());
		this.parallelActEndReplanner.init();

		this.parallelLeaveLinkReplanner = new ParallelLeaveLinkReplanner(network);
		this.parallelLeaveLinkReplanner.setNumberOfThreads(numReplanningThreads);
		this.parallelLeaveLinkReplanner.setReplannerArrayList(replanners);
		this.parallelLeaveLinkReplanner.setReplannerArray(parallelInitialReplanner.getReplannerArray());
		this.parallelLeaveLinkReplanner.init();
	}

	/*
	 * Creates the Handler and Listener Object so that they can be handed over to the Within Day
	 * Replanning Modules (TravelCostWrapper, etc).
	 *
	 * The full initialization of them is done later (we don't have all necessary Objects yet).
	 */
	protected void createHandlersAndListeners()
	{
		linkVehiclesCounter = new LinkVehiclesCounter2();
		linkReplanningMap = new LinkReplanningMap();
		replanningManager = new ReplanningManager();
	}

	@Override
	protected void runMobSim()
	{
		sim = new ReplanningQueueSimulation(this.network, this.population, this.events);

		createHandlersAndListeners();

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);

		sim.addQueueSimulationListeners(foqsl);

		// fully initialize & add LinkReplanningMap
		linkReplanningMap.setQueueNetwork(sim.getQueueNetwork());
		this.events.addHandler(linkReplanningMap);

		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();

		log.info("Set Replanning flags");
		setReplanningFlags();

		log.info("Set Replanners for each Person");
		setReplanners();

		log.info("do initial Replanning");
		doInitialReplanning();

		ActEndReplanningModule actEndReplanning = new ActEndReplanningModule(parallelActEndReplanner, sim);
		LeaveLinkReplanningModule leaveLinkReplanning = new LeaveLinkReplanningModule(parallelLeaveLinkReplanner, linkReplanningMap);

		replanningManager.setActEndReplanningModule(actEndReplanning);
		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);

		sim.run();
	}

	/*
	 * Add three boolean variables to each Person. They are used to indicate, if
	 * the plans of this person should be replanned each time if an activity
	 * ends, each time a link is left, before the simulation starts or never
	 * during an iteration.
	 */
	protected void setReplanningFlags() {
		noReplanningCounter = 0;
		initialReplanningCounter = 0;
		actEndReplanningCounter = 0;
		leaveLinkReplanningCounter = 0;

		for (Person person : this.getPopulation().getPersons().values())
		{
			// get Person's Custom Attributes
			Map<String, Object> customAttributes = person.getCustomAttributes();

			double probability;
			boolean noReplanning = true;

			probability = MatsimRandom.getRandom().nextDouble();
			if (probability > pInitialReplanning)customAttributes.put("initialReplanning", new Boolean(false));
			else
			{
				customAttributes.put("initialReplanning", new Boolean(true));
				noReplanning = false;
				initialReplanningCounter++;
			}

			// No Replanning
			probability = MatsimRandom.getRandom().nextDouble();
			if (probability > pActEndReplanning) customAttributes.put("endActivityReplanning", new Boolean(false));
			else
			{
				customAttributes.put("endActivityReplanning", new Boolean(true));
				noReplanning = false;
				actEndReplanningCounter++;
			}

			// No Replanning
			probability = MatsimRandom.getRandom().nextDouble();
			if (probability > pLeaveLinkReplanning) customAttributes.put("leaveLinkReplanning", new Boolean(false));
			else
			{
				customAttributes.put("leaveLinkReplanning", new Boolean(true));
				noReplanning = false;
				leaveLinkReplanningCounter++;
			}

			// if non of the Replanning Modules was activated
			if (noReplanning) noReplanningCounter++;

			// (de)activate replanning if they are not needed
			if (actEndReplanningCounter == 0) replanningManager.doActEndReplanning(false);
			else replanningManager.doActEndReplanning(true);

			if (leaveLinkReplanningCounter == 0) replanningManager.doLeaveLinkReplanning(false);
			else replanningManager.doLeaveLinkReplanning(true);
		}

		log.info("Initial Replanning Probability: " + pInitialReplanning);
		log.info("Act End Replanning Probability: " + pActEndReplanning);
		log.info("Leave Link Replanning Probability: " + pLeaveLinkReplanning);

		double numPersons = this.population.getPersons().size();
		log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
		log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
		log.info(actEndReplanningCounter + " persons replan their plans after an activity (" + actEndReplanningCounter / numPersons * 100.0 + "%)");
		log.info(leaveLinkReplanningCounter + " persons replan their plans at each node (" + leaveLinkReplanningCounter / numPersons * 100.0 + "%)");
	}

	/*
	 * Assigns a replanner to every Person of the population.
	 *
	 * At the moment: Replanning Modules are assigned hard coded. Later: Modules
	 * are assigned based on probabilities from config files.
	 */
	protected void setReplanners()
	{
		for (Person p : this.getPopulation().getPersons().values()) {
			Map<String, Object> customAttributes = p.getCustomAttributes();
			customAttributes.put("Replanner", replanners.get(0));
		}
	}

	protected void doInitialReplanning()
	{
		ArrayList<Person> personsToReplan = new ArrayList<Person>();

		for (Person person : this.getPopulation().getPersons().values()) {
			boolean replanning = (Boolean) person.getCustomAttributes().get("initialReplanning");

			if (replanning)
			{
				personsToReplan.add(person);
			}
		}

		double time = 0.0;

		// Run Replanner.
		this.parallelInitialReplanner.run(personsToReplan, time);

	} // doInitialReplanning

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		MyStrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}

	/*
	 * =================================================================== main
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

}
