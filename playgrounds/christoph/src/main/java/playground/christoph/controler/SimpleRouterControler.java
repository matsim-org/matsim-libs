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
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.QSimI;

import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.knowledge.container.MapKnowledgeDB;
import playground.christoph.knowledge.nodeselection.SelectNodes;
import playground.christoph.replanning.MyStrategyManagerConfigLoader;
import playground.christoph.router.CompassRoute;
import playground.christoph.router.RandomCompassRoute;
import playground.christoph.router.RandomDijkstraRoute;
import playground.christoph.router.RandomRoute;
import playground.christoph.router.TabuRoute;
import playground.christoph.router.costcalculators.OnlyTimeDependentTravelCostCalculator;
import playground.christoph.router.util.SimpleRouterFactory;
import playground.christoph.scoring.OnlyTimeDependentScoringFunctionFactory;
import playground.christoph.withinday.mobsim.InitialReplanningModule;
import playground.christoph.withinday.mobsim.KnowledgeWithinDayQSim;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.replanning.InitialReplanner;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.WithinDayInitialReplanner;
import playground.christoph.withinday.replanning.identifiers.InitialIdentifierImpl;
import playground.christoph.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import playground.christoph.withinday.replanning.modules.ReplanningModule;
import playground.christoph.withinday.replanning.parallel.ParallelInitialReplanner;

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
	protected String tableName = "BatchTable1_1";
	
	protected ArrayList<PlanAlgorithm> replanners;
	protected ArrayList<SelectNodes> nodeSelectors;

	/*
	 * Each Person can only use one Router at a time.
	 * The sum of the probabilities should not be higher
	 * than 1.0 (100%). If its lower, all remaining agents
	 * will do no Replanning.
	 */
	protected double pRandomRouter = 0.1;
	protected double pTabuRouter = 0.1;
	protected double pCompassRouter = 0.1;
	protected double pRandomCompassRouter = 0.1;
	protected double pRandomDijkstraRouter = 0.1;
	
	/*
	 * Weight Factor for the RandomDijkstraRouter.
	 * 0.0 means that the Route is totally Random based
	 * 1.0 means that the Route is totally Dijkstra based  
	 */
	protected double randomDijsktraWeightFactor = 0.5;
	
	protected int noReplanningCounter = 0;
	protected int initialReplanningCounter = 0;
	
	protected int randomRouterCounter = 0;
	protected int tabuRouterCounter = 0;
	protected int compassRouterCounter = 0;
	protected int randomCompassRouterCounter = 0;
	protected int randomDijkstraRouterCounter = 0;
	
	protected InitialIdentifier initialIdentifier;
	protected WithinDayInitialReplanner randomReplanner;
	protected WithinDayInitialReplanner tabuReplanner;
	protected WithinDayInitialReplanner compassReplanner;
	protected WithinDayInitialReplanner randomCompassReplanner;
	protected WithinDayInitialReplanner randomDijkstraReplanner;
	
	protected FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();
	/*
	 * TravelTime and TravelCost for the Dijkstra Router
	 */
	protected TravelTime dijkstraTravelTime = new FreeSpeedTravelTime();
	protected PersonalizableTravelCost dijkstraTravelCost = new OnlyTimeDependentTravelCostCalculator(dijkstraTravelTime);
//	protected PersonalizableTravelCost dijkstraTravelCost = new OnlyDistanceDependentTravelCostCalculator();
	
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
	
	protected ParallelInitialReplanner parallelInitialReplanner;
	protected ReplanningManager replanningManager = new ReplanningManager();
	protected KnowledgeWithinDayQSim sim;

	public SimpleRouterControler(String[] args) {
		super(args);

		setConstructorParameters();
	}

	// only for Batch Runs
	public SimpleRouterControler(Config config) {
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

		// Use a Scoring Function, that only scores the travel times!
		this.setScoringFunctionFactory(new OnlyTimeDependentScoringFunctionFactory());
	}
	
	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalised Router.
	 */
	protected void initReplanningRouter() {
		
		AbstractMultithreadedModule router;
		
		this.initialIdentifier = new InitialIdentifierImpl(this.sim);
		
		// BasicReplanners (Random, Tabu, Compass, ...)
		// each replanner can handle an arbitrary number of persons
		RandomRoute randomRoute = new RandomRoute(this.network);
		router = new ReplanningModule(config, network, randomRoute, null, new SimpleRouterFactory()); 
		this.randomReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.randomReplanner.setAbstractMultithreadedModule(router);
		this.randomReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.randomReplanner);

		TabuRoute tabuRoute = new TabuRoute(this.network);
		router = new ReplanningModule(config, network, tabuRoute, null, new SimpleRouterFactory());
		this.tabuReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.tabuReplanner.setAbstractMultithreadedModule(router);
		this.tabuReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.tabuReplanner);

		CompassRoute compassRoute = new CompassRoute(this.network);
		router = new ReplanningModule(config, network, compassRoute, null, new SimpleRouterFactory());
		this.compassReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.compassReplanner.setAbstractMultithreadedModule(router);
		this.compassReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.compassReplanner);

		RandomCompassRoute randomCompassRoute = new RandomCompassRoute(this.network);
		router = new ReplanningModule(config, network, randomCompassRoute, null, new SimpleRouterFactory());
		this.randomCompassReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.randomCompassReplanner.setAbstractMultithreadedModule(router);
		this.randomCompassReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.randomCompassReplanner);

		RandomDijkstraRoute randomDijkstraRoute = new RandomDijkstraRoute(this.network, dijkstraTravelCost, dijkstraTravelTime);
		randomDijkstraRoute.setDijsktraWeightFactor(randomDijsktraWeightFactor);
		router = new ReplanningModule(config, network, randomDijkstraRoute, null, new SimpleRouterFactory());
		this.randomDijkstraReplanner = new InitialReplanner(ReplanningIdGenerator.getNextId(), this.scenarioData);
		this.randomDijkstraReplanner.setAbstractMultithreadedModule(router);
		this.randomDijkstraReplanner.addAgentsToReplanIdentifier(this.initialIdentifier);
		this.parallelInitialReplanner.addWithinDayReplanner(this.randomDijkstraReplanner);
	}

	/*
	 * Hands over the ArrayList to the ParallelReplannerModules
	 */
	protected void initParallelReplanningModules() {	
		this.parallelInitialReplanner = new ParallelInitialReplanner(numReplanningThreads, this);
		
		InitialReplanningModule initialReplanningModule = new InitialReplanningModule(parallelInitialReplanner);
		replanningManager.setInitialReplanningModule(initialReplanningModule);
	}
	
	@Override
	protected void runMobSim() {
		sim = new KnowledgeWithinDayQSim(this.scenarioData, this.events);
//		sim.addQueueSimulationListeners(replanningManager);
		sim.addQueueSimulationListeners(foqsl);
		
		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		ReplanningFlagInitializer rfi = new ReplanningFlagInitializer(this);
		foqsl.addQueueSimulationInitializedListener(rfi);
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);
		
		
		if (useKnowledge) {
			log.info("Set Knowledge Storage Type");
			setKnowledgeStorageHandler();			
		}
		
		log.info("Initialize Parallel Replanning Modules");
		initParallelReplanningModules();
		
		log.info("Initialize Replanning Routers");
		initReplanningRouter();

		sim.run();
	}

	/*
	 * How to store the known Nodes of the Agents?
	 * Currently we store them in a Database.
	 */
	private void setKnowledgeStorageHandler() {		
		for(Person person : population.getPersons().values())
		{			
			Map<String, Object> customAttributes = person.getCustomAttributes();
			
			customAttributes.put("NodeKnowledgeStorageType", MapKnowledgeDB.class.getName());
			
			MapKnowledgeDB mapKnowledgeDB = new MapKnowledgeDB();
			mapKnowledgeDB.setPerson(person);
			mapKnowledgeDB.setNetwork(network);
			mapKnowledgeDB.setTableName(tableName);

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
	
	public static class ReplanningFlagInitializer implements SimulationInitializedListener<QSimI> {

		protected SimpleRouterControler simpleRouterControler;
		protected Map<Id, WithinDayPersonAgent> withinDayPersonAgents;
				
		protected int noReplanningCounter = 0;
		protected int initialReplanningCounter = 0;
		protected int actEndReplanningCounter = 0;
		protected int leaveLinkReplanningCounter = 0;
		
		public ReplanningFlagInitializer(SimpleRouterControler controler) {
			this.simpleRouterControler = controler;
		}
		
		@Override
		public void notifySimulationInitialized(SimulationInitializedEvent<QSimI> e) {
			collectAgents((QSim)e.getQueueSimulation());
			setReplanningFlags();
		}
		
		/*
		 * Assigns a replanner to every Person of the population. 
		 * 
		 * At the moment: Replanning Modules are assigned hard coded. Later: Modules
		 * are assigned based on probabilities from config files.
		 */
		protected void setReplanningFlags() {
			for (WithinDayPersonAgent withinDayPersonAgent : this.withinDayPersonAgents.values()) {
				double probability;
				Random random = MatsimRandom.getLocalInstance();
				probability = random.nextDouble();
				
				double pRandomRouterLow = 0.0;
				double pRandomRouterHigh = simpleRouterControler.pRandomRouter;
				double pTabuRouterLow = simpleRouterControler.pRandomRouter;
				double pTabuRouterHigh = simpleRouterControler.pRandomRouter + simpleRouterControler.pTabuRouter;
				double pCompassRouterLow = simpleRouterControler.pRandomRouter + simpleRouterControler.pTabuRouter;
				double pCompassRouterHigh = simpleRouterControler.pRandomRouter + simpleRouterControler.pTabuRouter + 
						simpleRouterControler.pCompassRouter;
				double pRandomCompassRouterLow = simpleRouterControler.pRandomRouter + 
						simpleRouterControler.pTabuRouter + simpleRouterControler.pCompassRouter;
				double pRandomCompassRouterHigh = simpleRouterControler.pRandomRouter + simpleRouterControler.pTabuRouter + 
						simpleRouterControler.pCompassRouter + simpleRouterControler.pRandomCompassRouter;
				double pRandomDijkstraRouterLow = simpleRouterControler.pRandomRouter + simpleRouterControler.pTabuRouter + 
						simpleRouterControler.pCompassRouter + simpleRouterControler.pRandomCompassRouter;
				double pRandomDijkstraRouterHigh = simpleRouterControler.pRandomRouter + simpleRouterControler.pTabuRouter + 
						simpleRouterControler.pCompassRouter + simpleRouterControler.pRandomCompassRouter + 
						simpleRouterControler.pRandomDijkstraRouter;

				
				// Random Router
				if (pRandomRouterLow <= probability && probability < pRandomRouterHigh) {
					withinDayPersonAgent.addWithinDayReplanner(simpleRouterControler.randomReplanner);
					initialReplanningCounter++;
					simpleRouterControler.randomRouterCounter++;
				}
				// Tabu Router
				else if (pTabuRouterLow <= probability && probability < pTabuRouterHigh) {
					withinDayPersonAgent.addWithinDayReplanner(simpleRouterControler.tabuReplanner);
					initialReplanningCounter++;
					simpleRouterControler.tabuRouterCounter++;
				}
				// Compass Router
				else if (pCompassRouterLow <= probability && probability < pCompassRouterHigh) {
					withinDayPersonAgent.addWithinDayReplanner(simpleRouterControler.compassReplanner);
					initialReplanningCounter++;
					simpleRouterControler.compassRouterCounter++;
				}
				// Random Compass Router
				else if (pRandomCompassRouterLow <= probability && probability < pRandomCompassRouterHigh) {
					withinDayPersonAgent.addWithinDayReplanner(simpleRouterControler.randomCompassReplanner);
					initialReplanningCounter++;
					simpleRouterControler.randomCompassRouterCounter++;
				}
				// Random Dijkstra Router
				else if (pRandomDijkstraRouterLow <= probability && probability <= pRandomDijkstraRouterHigh) {
					withinDayPersonAgent.addWithinDayReplanner(simpleRouterControler.randomDijkstraReplanner);
					initialReplanningCounter++;
					simpleRouterControler.randomDijkstraRouterCounter++;
				}
				// No Router
				else {
					noReplanningCounter++;
				}

				// (de)activate replanning if they are not needed
				if (initialReplanningCounter == 0) simpleRouterControler.replanningManager.doInitialReplanning(false);
				else simpleRouterControler.replanningManager.doInitialReplanning(true);
			}
			
			log.info("Random Routing Probability: " + simpleRouterControler.pRandomRouter);
			log.info("Tabu Routing Probability: " + simpleRouterControler.pTabuRouter);
			log.info("Compass Routing Probability: " + simpleRouterControler.pCompassRouter);
			log.info("Random Compass Routing Probability: " + simpleRouterControler.pRandomCompassRouter);
			log.info("Random Dijkstra Routing Probability: " + simpleRouterControler.pRandomDijkstraRouter);

//			double numPersons = simpleRouterControler.population.getPersons().size();
			double numPersons = withinDayPersonAgents.size();
			log.info(simpleRouterControler.randomRouterCounter + " persons use a Random Router (" + simpleRouterControler.randomRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.tabuRouterCounter + " persons use a Tabu Router (" + simpleRouterControler.tabuRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.compassRouterCounter + " persons use a Compass Router (" + simpleRouterControler.compassRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.randomCompassRouterCounter + " persons use a Random Compass Router (" + simpleRouterControler.randomCompassRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.randomDijkstraRouterCounter + " persons use a Random Dijkstra Router (" + simpleRouterControler.randomDijkstraRouterCounter / numPersons * 100.0 + "%)");
			
			log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
			log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
		}
				
		protected void collectAgents(QSim sim) {
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
			final SimpleRouterControler controler = new SimpleRouterControler(args);
			controler.setOverwriteFiles(true);
			controler.run();
		}
		System.exit(0);
	}

	public static class FreeSpeedTravelTime implements TravelTime, Cloneable {
		
		public double getLinkTravelTime(Link link, double time) {
			return link.getFreespeed(time);
		}
		
		public FreeSpeedTravelTime clone() {
			return new FreeSpeedTravelTime();
		}
	}
}
