/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleRouterControler.java
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

package playground.christoph.controler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scoring.functions.OnlyTravelDependentScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.withinday.controller.WithinDayController;
import org.matsim.withinday.replanning.identifiers.filter.CollectionAgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;

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

public class SimpleRouterControler extends WithinDayController {

	private static final Logger log = Logger.getLogger(SimpleRouterControler.class);

	/*
	 * Select which size the known Areas should have in the Simulation.
	 * The needed Parameter is the Name of the Table in the MYSQL Database.
	 */
	protected String tableName = "BatchTable1_1";

	protected ArrayList<PlanAlgorithm> replanners;

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

	protected InitialIdentifier randomIdentifier;
	protected InitialIdentifier tabuIdentifier;
	protected InitialIdentifier compassIdentifier;
	protected InitialIdentifier randomCompassIdentifier;
	protected InitialIdentifier dijkstraIdentifier;
	protected WithinDayInitialReplanner randomReplanner;
	protected WithinDayInitialReplanner tabuReplanner;
	protected WithinDayInitialReplanner compassReplanner;
	protected WithinDayInitialReplanner randomCompassReplanner;
	protected WithinDayInitialReplanner randomDijkstraReplanner;

	protected FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
	protected TravelDisutilityFactory travelCostFactory = new OnlyTimeDependentTravelCostCalculatorFactory();
	protected TravelDisutility travelCost = travelCostFactory.createTravelDisutility(travelTime, null);

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
		this.setScoringFunctionFactory(new OnlyTravelDependentScoringFunctionFactory());
	}

	/*
	 * New Routers for the Replanning are used instead of using the controler's.
	 * By doing this every person can use a personalized Router.
	 */
	@Override
	protected void initReplanners(QSim sim) {
		
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sim.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		AbstractMultithreadedModule router;

		// TODO: fix this...
		
//		// BasicReplanners (Random, Tabu, Compass, ...)
//		// each replanner can handle an arbitrary number of persons
//		RandomRoute randomRoute = new RandomRoute(this.network);
//		router = new ReplanningModule(config, network, randomRoute, null, new SimpleRouterFactory(), routeFactory);
//		this.randomIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
//		this.randomReplanner = new InitialReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
//		this.randomReplanner.addAgentsToReplanIdentifier(this.randomIdentifier);
//		super.getReplanningManager().addIntialReplanner(this.randomReplanner);
//
//		TabuRoute tabuRoute = new TabuRoute(this.network);
//		router = new ReplanningModule(config, network, tabuRoute, null, new SimpleRouterFactory(), routeFactory);
//		this.tabuIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
//		this.tabuReplanner = new InitialReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
//		this.tabuReplanner.addAgentsToReplanIdentifier(this.tabuIdentifier);
//		super.getReplanningManager().addIntialReplanner(this.tabuReplanner);
//
//		CompassRoute compassRoute = new CompassRoute(this.network);
//		router = new ReplanningModule(config, network, compassRoute, null, new SimpleRouterFactory(), routeFactory);
//		this.compassIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
//		this.compassReplanner = new InitialReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
//		this.compassReplanner.addAgentsToReplanIdentifier(this.compassIdentifier);
//		super.getReplanningManager().addIntialReplanner(this.compassReplanner);
//
//		RandomCompassRoute randomCompassRoute = new RandomCompassRoute(this.network);
//		router = new ReplanningModule(config, network, randomCompassRoute, null, new SimpleRouterFactory(), routeFactory);
//		this.randomCompassIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
//		this.randomCompassReplanner = new InitialReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
//		this.randomCompassReplanner.addAgentsToReplanIdentifier(this.randomCompassIdentifier);
//		super.getReplanningManager().addIntialReplanner(this.randomCompassReplanner);
//
//		RandomDijkstraRoute randomDijkstraRoute = new RandomDijkstraRoute(this.network, this.travelCostFactory, this.travelTime);
//		randomDijkstraRoute.setDijsktraWeightFactor(randomDijsktraWeightFactor);
//		router = new ReplanningModule(config, network, randomDijkstraRoute, null, new SimpleRouterFactory(), routeFactory);
//		this.dijkstraIdentifier = new InitialIdentifierImplFactory(sim).createIdentifier();
//		this.randomDijkstraReplanner = new InitialReplannerFactory(this.scenarioData, router, 1.0).createReplanner();
//		this.randomDijkstraReplanner.addAgentsToReplanIdentifier(this.dijkstraIdentifier);
//		super.getReplanningManager().addIntialReplanner(this.randomDijkstraReplanner);	
	}
	
	@Override
	protected void runMobSim() {
		
		super.getWithinDayEngine().doDuringActivityReplanning(false);
		super.getWithinDayEngine().doDuringLegReplanning(false);

		/*
		 * Use a FixedOrderQueueSimulationListener to bundle the Listeners and
		 * ensure that they are started in the needed order.
		 */
		ReplanningFlagInitializer rfi = new ReplanningFlagInitializer(this);
		super.getFixedOrderSimulationListener().addSimulationInitializedListener(rfi);

		super.runMobSim();
	}

	public static class ReplanningFlagInitializer implements MobsimInitializedListener {

		protected SimpleRouterControler simpleRouterControler;
		protected Collection<Id> withinDayAgents;

		protected int noReplanningCounter = 0;
		protected int initialReplanningCounter = 0;
		protected int actEndReplanningCounter = 0;
		protected int leaveLinkReplanningCounter = 0;

		public ReplanningFlagInitializer(SimpleRouterControler controler) {
			this.simpleRouterControler = controler;
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
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
			Set<Id> randomAgents = new TreeSet<Id>();
			Set<Id> tabuAgents = new TreeSet<Id>();
			Set<Id> compassAgents = new TreeSet<Id>();
			Set<Id> randomCompassAgents = new TreeSet<Id>();
			Set<Id> dijkstraAgents = new TreeSet<Id>();
			
			for (Id agentId : this.withinDayAgents) {
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
					randomAgents.add(agentId);
					initialReplanningCounter++;
					simpleRouterControler.randomRouterCounter++;
				}
				// Tabu Router
				else if (pTabuRouterLow <= probability && probability < pTabuRouterHigh) {
					tabuAgents.add(agentId);
					initialReplanningCounter++;
					simpleRouterControler.tabuRouterCounter++;
				}
				// Compass Router
				else if (pCompassRouterLow <= probability && probability < pCompassRouterHigh) {
					compassAgents.add(agentId);
					initialReplanningCounter++;
					simpleRouterControler.compassRouterCounter++;
				}
				// Random Compass Router
				else if (pRandomCompassRouterLow <= probability && probability < pRandomCompassRouterHigh) {
					randomCompassAgents.add(agentId);
					initialReplanningCounter++;
					simpleRouterControler.randomCompassRouterCounter++;
				}
				// Random Dijkstra Router
				else if (pRandomDijkstraRouterLow <= probability && probability <= pRandomDijkstraRouterHigh) {
					dijkstraAgents.add(agentId);
					initialReplanningCounter++;
					simpleRouterControler.randomDijkstraRouterCounter++;
				}
				// No Router
				else {
					noReplanningCounter++;
				}

				// (de)activate replanning if they are not needed
				if (initialReplanningCounter == 0) simpleRouterControler.getWithinDayEngine().doInitialReplanning(false);
				else simpleRouterControler.getWithinDayEngine().doInitialReplanning(true);
			}
			
			Set<Id> includedAgents;
			CollectionAgentFilterFactory agentFilterFactory;

			includedAgents = new LinkedHashSet<Id>(randomAgents);
			agentFilterFactory = new CollectionAgentFilterFactory(includedAgents);
			simpleRouterControler.randomIdentifier.addAgentFilter(agentFilterFactory.createAgentFilter());

			includedAgents = new LinkedHashSet<Id>(tabuAgents);
			agentFilterFactory = new CollectionAgentFilterFactory(includedAgents);
			simpleRouterControler.tabuIdentifier.addAgentFilter(agentFilterFactory.createAgentFilter());
			
			includedAgents = new LinkedHashSet<Id>(compassAgents);
			agentFilterFactory = new CollectionAgentFilterFactory(includedAgents);
			simpleRouterControler.compassIdentifier.addAgentFilter(agentFilterFactory.createAgentFilter());
			
			includedAgents = new LinkedHashSet<Id>(randomCompassAgents);
			agentFilterFactory = new CollectionAgentFilterFactory(includedAgents);
			simpleRouterControler.randomCompassIdentifier.addAgentFilter(agentFilterFactory.createAgentFilter());
			
			includedAgents = new LinkedHashSet<Id>(dijkstraAgents);
			agentFilterFactory = new CollectionAgentFilterFactory(includedAgents);
			simpleRouterControler.dijkstraIdentifier.addAgentFilter(agentFilterFactory.createAgentFilter());
			
			log.info("Random Routing Probability: " + simpleRouterControler.pRandomRouter);
			log.info("Tabu Routing Probability: " + simpleRouterControler.pTabuRouter);
			log.info("Compass Routing Probability: " + simpleRouterControler.pCompassRouter);
			log.info("Random Compass Routing Probability: " + simpleRouterControler.pRandomCompassRouter);
			log.info("Random Dijkstra Routing Probability: " + simpleRouterControler.pRandomDijkstraRouter);

			double numPersons = withinDayAgents.size();
			log.info(simpleRouterControler.randomRouterCounter + " persons use a Random Router (" + simpleRouterControler.randomRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.tabuRouterCounter + " persons use a Tabu Router (" + simpleRouterControler.tabuRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.compassRouterCounter + " persons use a Compass Router (" + simpleRouterControler.compassRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.randomCompassRouterCounter + " persons use a Random Compass Router (" + simpleRouterControler.randomCompassRouterCounter / numPersons * 100.0 + "%)");
			log.info(simpleRouterControler.randomDijkstraRouterCounter + " persons use a Random Dijkstra Router (" + simpleRouterControler.randomDijkstraRouterCounter / numPersons * 100.0 + "%)");

			log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
			log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
		}

		protected void collectAgents(QSim sim) {
			this.withinDayAgents = new ArrayList<Id>();

			for (MobsimAgent mobsimAgent : sim.getAgents()) {
					withinDayAgents.add(mobsimAgent.getId());
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
}
