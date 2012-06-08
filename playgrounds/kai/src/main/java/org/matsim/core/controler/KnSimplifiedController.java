/* *********************************************************************** *
 * project: kai
 * GautengOwnController.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.controler;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.vis.otfvis.OTFFileWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;

/**
 * @author nagel
 *
 */
public class KnSimplifiedController extends AbstractController {
	
	public final IterationStopWatch stopwatch = new IterationStopWatch();

	public Config config  ;
	private Network network  ;
	private Population population  ;
	
	private CalcLegTimes legTimes;

	
	// ############################################################################################################################
	// ############################################################################################################################
	//	stuff that is related to the control flow	
	
	KnSimplifiedController() {
		
		Config cfg = ConfigUtils.createConfig() ;
		cfg.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		checkConfigConsistencyAndWriteToLog("Complete config dump after reading the config file:");

		this.scenarioData = (ScenarioImpl) ScenarioUtils.loadScenario( cfg ) ;
		
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		this.config = this.scenarioData.getConfig();

	}
	private void run() {
		setUpOutputDir(); // output dir needs to be before logging
		initEvents(); // yy I do not understand why events need to be before logging
		initLogging(); // logging needs to be early
		setUp(); // setup needs to be after events since most things need events
		loadCoreListeners();

		this.controlerListenerManager.fireControlerStartupEvent();
		
		this.checkConfigConsistencyAndWriteToLog("Config dump before doIterations:");

		doIterations();

		shutdown(false);
	}
	/**
	 * Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, and after the
	 * scenario data (network, population) is read.
	 */
	private void setUp() {
		
		// add a couple of useful event handlers:
		this.events.addHandler(new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network));

		this.legTimes = new CalcLegTimes();
		this.events.addHandler(legTimes);

	}
	
	private void doIterations() {
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(this.population, this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@SuppressWarnings("synthetic-access")
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), KnSimplifiedController.this.scenarioData);
			}
		});

		int firstIteration = this.config.controler().getFirstIteration();
		int lastIteration = this.config.controler().getLastIteration();
		String divider = "###################################################";
		String marker = "### ";

		for (int iteration = firstIteration; iteration <= lastIteration; iteration++ ) {
			this.stopwatch.setCurrentIteration(iteration) ;
			
			log.info(divider);
			log.info(marker + "ITERATION " + iteration + " BEGINS");
			this.stopwatch.setCurrentIteration(iteration);
			this.stopwatch.beginOperation("iteration");
			makeIterationPath(iteration);
			resetRandomNumbers(iteration);

			this.controlerListenerManager.fireControlerIterationStartsEvent(iteration);
			if (iteration > firstIteration) {
				this.stopwatch.beginOperation("replanning");
				this.controlerListenerManager.fireControlerReplanningEvent(iteration);
				this.stopwatch.endOperation("replanning");
			}
			this.controlerListenerManager.fireControlerBeforeMobsimEvent(iteration);
			this.stopwatch.beginOperation("mobsim");
			resetRandomNumbers(iteration);
			runMobSim(iteration);
			log.error("will not work without mobsim; aborting ...") ; System.exit(-1) ;
			this.stopwatch.endOperation("mobsim");
			log.info(marker + "ITERATION " + iteration + " fires after mobsim event");
			this.controlerListenerManager.fireControlerAfterMobsimEvent(iteration);
			log.info(marker + "ITERATION " + iteration + " fires scoring event");
			this.controlerListenerManager.fireControlerScoringEvent(iteration);
			log.info(marker + "ITERATION " + iteration + " fires iteration end event");
			this.controlerListenerManager.fireControlerIterationEndsEvent(iteration);
			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(this.controlerIO.getOutputFilename("stopwatch.txt"));
			log.info(marker + "ITERATION " + iteration + " ENDS");
			log.info(divider);
		}
	}
	
	// ############################################################################################################################
	// ############################################################################################################################
	//	stuff that is related to the configuration of matsim  	
	
	/**
	 * The order how the listeners are added is very important! As
	 * dependencies between different listeners exist or listeners may read
	 * and write to common variables, the order is important. Example: The
	 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
	 * turn is used by the PlansScoring-Listener.
	 * <br/>
	 * IMPORTANT: The execution order is reverse to the order the listeners
	 * are added to the list.
	 */
	private void loadCoreListeners() {

		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory( this.config.planCalcScore(), this.network );
		final PlansScoring plansScoring = new PlansScoring( this.scenarioData, this.events, scoringFunctionFactory );
		this.controlerListenerManager.addControlerListener(plansScoring);
		
		StrategyManager strategyManager = buildStrategyManager() ;
		this.controlerListenerManager.addCoreControlerListener(new PlansReplanning( strategyManager, this.population ));

		final PlansDumping plansDumping = new PlansDumping( this.scenarioData, this.config.controler().getFirstIteration(), 
				this.config.controler().getWritePlansInterval(), this.stopwatch, this.controlerIO );
		this.controlerListenerManager.addCoreControlerListener(plansDumping);

		final EventsHandling eventsHandling = new EventsHandling(this.events,
				this.config.controler().getWriteEventsInterval(), this.config.controler().getEventsFileFormats(),
				this.controlerIO, this.legTimes );
		this.controlerListenerManager.addCoreControlerListener(eventsHandling); 
		// must be last being added (=first being executed)
	}

	private StrategyManager buildStrategyManager() {
		StrategyManager strategyManager = new StrategyManager() ;
		{
			strategyManager.setPlanSelectorForRemoval( new WorstPlanForRemovalSelector() ) ;
		}
		{
			PlanStrategy strategy = new PlanStrategyImpl( new ExpBetaPlanChanger(this.config.planCalcScore().getBrainExpBeta()) ) ;
			strategyManager.addStrategy(strategy, 0.9) ;
		}
		{
			PlanStrategy strategy = new PlanStrategyImpl( new ExpBetaPlanSelector(this.config.planCalcScore())) ;
			strategy.addStrategyModule(wrapPlanAlgo( this.createRoutingAlgorithm() )) ;
			strategyManager.addStrategy(strategy, 0.1) ;
		}
		return strategyManager ;
	}
	private PlansCalcRoute createRoutingAlgorithm() {
		// factory to generate routes:
		final ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();

		// travel time:
		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		final TravelTimeCalculator travelTime = travelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		this.events.addHandler(travelTime);

		// travel disutility (generalized cost)
		final TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutility(travelTime, config.planCalcScore());
		
		// define the factory for the "computer science" router.  Needs to be a factory because it might be used multiple
		// times (e.g. for car router, pt router, ...)
		final LeastCostPathCalculatorFactory leastCostPathFactory = new DijkstraFactory();
		
		// plug it together
		final PlansCalcRoute plansCalcRoute = new PlansCalcRoute(config.plansCalcRoute(), network, travelDisutility, 
				travelTime, leastCostPathFactory, routeFactory);
		return plansCalcRoute;
	}
	protected void runMobSim(int iteration) {
		QSim simulation = new QSim( this.scenarioData, this.events ) ;
		if (config.controler().getWriteSnapshotsInterval() != 0 && iteration % config.controler().getWriteSnapshotsInterval() == 0) {
			// yyyy would be nice to have the following encapsulated in some way:
			// === begin ===
			SnapshotWriterManager manager = new SnapshotWriterManager(config);
			SnapshotWriterFactory snapshotWriterFactory = new OTFFileWriterFactory() ;
			String baseFileName = snapshotWriterFactory.getPreferredBaseFilename();
			String fileName = this.controlerIO.getIterationFilename(iteration, baseFileName);
			SnapshotWriter snapshotWriter = snapshotWriterFactory.createSnapshotWriter(fileName, this.scenarioData);
			manager.addSnapshotWriter(snapshotWriter);
			// === end ===
			simulation.addQueueSimulationListeners(manager);
		}
		Mobsim sim = simulation;
		sim.run();
	}
	
	// ############################################################################################################################
	// ############################################################################################################################
	public static void main( String[] args ) {
		KnSimplifiedController controller = new KnSimplifiedController() ;
		controller.run() ;
	}


}
