/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleRoutingControler.java
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
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.events.algorithms.ParallelInitialReplanner;
import playground.christoph.knowledge.container.MapKnowledgeDB;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.mobsim.ReplanningManager;
import playground.christoph.mobsim.ReplanningQueueSimulation;
import playground.christoph.network.MyLinkFactoryImpl;
import playground.christoph.replanning.MyStrategyManagerConfigLoader;
import playground.christoph.router.CompassRoute;
import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.RandomCompassRoute;
import playground.christoph.router.RandomRoute;
import playground.christoph.router.TabuRoute;
import playground.christoph.router.costcalculators.KnowledgeTravelTimeCalculator;
import playground.christoph.router.util.SimpleRouterFactory;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;

/**
 * This Controler should give an Example what is needed to run
 * Simulations with Simple Routing Strategies.
 * 
 * The Path to a Config File is needed as Argument to run the
 * Simulation.
 * 
 * By default "test/scenarios/berlin/config.xml" should work.
 * 
 * @author Christoph Dobler
 */

//mysimulations/kt-zurich/configIterative.xml

public class SimpleRouterControler extends Controler {
	
	private static final Logger log = Logger.getLogger(SimpleRouterControler.class);
	
	/*
	 * Select which size the known Areas should have in the Simulation.
	 * The needed Parameter is the Name of the Table in the MYSQL Database.
	 */
	static
	{
		MapKnowledgeDB.setTableName("BatchTable1_05");
		log.info("Selected Knowledge Database Table");
	}
	
	protected ArrayList<PlanAlgorithm> replanners;
	protected ArrayList<SelectNodes> nodeSelectors;

	/*
	 * Each Person can only use one Router at a time.
	 * The sum of the probabilities should not be higher
	 * than 1.0 (100%). If its lower, all remaining agents
	 * will do no Replanning.
	 */
	protected double pRandomRouter = 0.5;
	protected double pTabuRouter = 0.5;
	protected double pCompassRouter = 0.0;
	protected double pRandomCompassRouter = 0.0;
	
	protected int noReplanningCounter = 0;
	protected int initialReplanningCounter = 0;
	
	protected int randomRouterCounter = 0;
	protected int tabuRouterCounter = 0;
	protected int compassRouterCounter = 0;
	protected int randomCompassRouterCounter = 0;
		
	/*
	 * How many parallel Threads shall do the Replanning.
	 * The results should stay deterministic as long as the same
	 * number of Threads is used in the Simulations. Each Thread
	 * gets his own Random Number Generator so the results will
	 * change if the number of Threads is changed. 
	 */
	protected int numReplanningThreads = 2;
	
	/*
	 * Should the Agents use their Knowledge? If not, they use
	 * the entire Network.
	 */
	protected boolean useKnowledge = true;
	
	protected KnowledgeTravelTimeCalculator knowledgeTravelTime;
	protected ParallelInitialReplanner parallelInitialReplanner;
	protected ReplanningManager replanningManager;
	protected ReplanningQueueSimulation sim;

	public SimpleRouterControler(String[] args)
	{
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public SimpleRouterControler(Config config)
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
		this.getNetworkFactory().setLinkFactory(new MyLinkFactoryImpl());

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {
		replanners = new ArrayList<PlanAlgorithm>();
		
		// BasicReplanners (Random, Tabu, Compass, ...)
		// each replanner can handle an arbitrary number of persons
		KnowledgePlansCalcRoute randomRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new RandomRoute(this.network)));
		replanners.add(randomRouter);

		KnowledgePlansCalcRoute tabuRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new TabuRoute(this.network)));
		replanners.add(tabuRouter);

		KnowledgePlansCalcRoute compassRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new CompassRoute(this.network)));
		replanners.add(compassRouter);

		KnowledgePlansCalcRoute randomCompassRouter = new KnowledgePlansCalcRoute(new PlansCalcRouteConfigGroup(), network, null, null, new SimpleRouterFactory(new RandomCompassRoute(this.network)));
		replanners.add(randomCompassRouter);
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
	}
	
	@Override
	protected void runMobSim() 
	{
		sim = new ReplanningQueueSimulation(this.network, this.population, this.events);
		
		if (useKnowledge)
		{
			log.info("Set Knowledge Storage Type");
			setKnowledgeStorageHandler();			
		}
		
		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();

		log.info("Set Replanners for each Person");
		setReplanners();

		log.info("do initial Replanning");
		doInitialReplanning();	
				
		sim.run();
	}

	/*
	 * Assigns a replanner to every Person of the population. 
	 * 
	 * At the moment: Replanning Modules are assigned hard coded. Later: Modules
	 * are assigned based on probabilities from config files.
	 */
	protected void setReplanners() 
	{
		Iterator<PersonImpl> PersonIterator = this.getPopulation().getPersons().values().iterator();
		while (PersonIterator.hasNext())
		{
			PersonImpl p = PersonIterator.next();

			Map<String, Object> customAttributes = p.getCustomAttributes();
					
			double probability;
			probability = MatsimRandom.getRandom().nextDouble();
			
			double pRandomRouterLow = 0.0;
			double pRandomRouterHigh = pRandomRouter;
			double pTabuRouterLow = pRandomRouter;
			double pTabuRouterHigh = pRandomRouter + pTabuRouter;
			double pCompassRouterLow = pRandomRouter + pTabuRouter;
			double pCompassRouterHigh = pRandomRouter + pTabuRouter + pCompassRouter;
			double pRandomCompassRouterLow = pRandomRouter + pTabuRouter + pCompassRouter;
			double pRandomCompassRouterHigh = pRandomRouter + pTabuRouter + pCompassRouter + pRandomCompassRouter;
			
			// Random Router
			if (pRandomRouterLow <= probability && probability < pRandomRouterHigh)
			{
				customAttributes.put("Replanner", replanners.get(0)); // Random
				customAttributes.put("initialReplanning", new Boolean(true));
				initialReplanningCounter++;
				randomRouterCounter++;
			}
			// Tabu Router
			else if (pTabuRouterLow <= probability && probability < pTabuRouterHigh)
			{
				customAttributes.put("Replanner", replanners.get(1)); // Tabu
				customAttributes.put("initialReplanning", new Boolean(true));
				initialReplanningCounter++;
				tabuRouterCounter++;
			}
			// Compass Router
			else if (pCompassRouterLow <= probability && probability < pCompassRouterHigh)
			{
				customAttributes.put("Replanner", replanners.get(2)); // Compass
				customAttributes.put("initialReplanning", new Boolean(true));
				initialReplanningCounter++;
				compassRouterCounter++;
			}
			// Random Compass Router
			else if (pRandomCompassRouterLow <= probability && probability < pRandomCompassRouterHigh)
			{
				customAttributes.put("Replanner", replanners.get(3)); // RandomCompass
				customAttributes.put("initialReplanning", new Boolean(true));
				initialReplanningCounter++;
				randomCompassRouterCounter++;
			}
			// No Router
			else
			{
				customAttributes.put("initialReplanning", new Boolean(false));
				noReplanningCounter++;
			}
		}
		
		log.info("Random Routing Probability: " + pRandomRouter);
		log.info("Tabu Routing Probability: " + pTabuRouter);
		log.info("Compass Routing Probability: " + pCompassRouter);
		log.info("Random Compass Routing Probability: " + pRandomCompassRouter);

		double numPersons = this.population.getPersons().size();
		log.info(randomRouterCounter + " persons use a Random Router (" + randomRouterCounter / numPersons * 100.0 + "%)");
		log.info(tabuRouterCounter + " persons use a Tabu Router (" + tabuRouterCounter / numPersons * 100.0 + "%)");
		log.info(compassRouterCounter + " persons use a Compass Router (" + compassRouterCounter / numPersons * 100.0 + "%)");
		log.info(randomCompassRouterCounter + " persons use a Random Compass Router (" + randomCompassRouterCounter / numPersons * 100.0 + "%)");			
		
		log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
		log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
	}
	
	protected void doInitialReplanning()
	{
		ArrayList<PersonImpl> personsToReplan = new ArrayList<PersonImpl>();

		for (PersonImpl person : this.getPopulation().getPersons().values()) {
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

	/*
	 * How to store the known Nodes of the Agents?
	 * Currently we store them in a Database.
	 */
	private void setKnowledgeStorageHandler()
	{		
		for(Person person : population.getPersons().values())
		{			
			Map<String, Object> customAttributes = person.getCustomAttributes();
			
			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());
			
			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(network);

			customAttributes.put("NodeKnowledge", mapKnowledgeDB);
		}
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
			final SimpleRouterControler controler = new SimpleRouterControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

}
