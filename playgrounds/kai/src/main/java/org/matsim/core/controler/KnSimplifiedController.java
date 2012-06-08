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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.WorstPlanForRemovalSelector;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author nagel
 *
 */
public class KnSimplifiedController {
	
	public final IterationStopWatch stopwatch = new IterationStopWatch();

	private static final Logger log = Logger.getLogger(KnSimplifiedController.class);

	private CollectLogMessagesAppender collectLogMessagesAppender;
	private String dtdFileName;
	protected Throwable uncaughtException;
	private Config config;
	private Scenario scenarioData;
	private Network network;
	private Population population;
	
	private Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			shutdown(true);
		}
	};

	private String outputPath;

	private ControlerIO controlerIO;

	private boolean overwriteFiles;
	private boolean dumpDataAtEnd;
	private EventsManager events;
	
	private final ControlerListenerManager controlerListenerManager = new ControlerListenerManager(null); 

	private CalcLegTimes legTimes;

	
	private void run() {
		setUpOutputDir(); // output dir needs to be before logging
		initEvents(); // yy I do not understand why events need to be before logging
		initLogging(); // logging needs to be early
		setUp(); // setup needs to be after events since most things need events
		loadCoreListeners();

		this.controlerListenerManager.fireControlerStartupEvent();
		
		this.checkConfigConsistencyAndWriteToLog("Config dump before doIterations:");

//		doIterations();
		log.error("yyyyyy iterations not yet implemented.  Skipping ...") ;

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

		final EventsHandling eventsHandling = new EventsHandling((EventsManagerImpl) this.events,
				this.config.controler().getWriteEventsInterval(), this.config.controler().getEventsFileFormats(),
				this.controlerIO, this.legTimes );
		this.controlerListenerManager.addCoreControlerListener(eventsHandling); 
		// must be last being added (=first being executed)
	}

	private void doIterations() {
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(this.population, this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
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
//			runMobSim();
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
	
	// ############################################################################################################################
	// ############################################################################################################################
	//	controler infrastructure (should be hidden somehow)  	
	KnSimplifiedController() {
		// catch logs before doing something
		this.collectLogMessagesAppender = new CollectLogMessagesAppender();
		Logger.getRootLogger().addAppender(this.collectLogMessagesAppender);
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		log.info("Used Controler-Class: " + this.getClass().getCanonicalName());

		this.dtdFileName = null ;

		// make sure we know about any exceptions that lead to abortion of the program
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				log.warn("Getting uncaught Exception in Thread " + t.getName(), e);
				KnSimplifiedController.this.uncaughtException = e;
			}
		});

		// now do other stuff
		
		this.config = ConfigUtils.createConfig() ;
		this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		checkConfigConsistencyAndWriteToLog("Complete config dump after reading the config file:");

		this.scenarioData = ScenarioUtils.loadScenario( config ) ;
		
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();

		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		// (yy why here?  why not earlier when runtime system infrastructure is constructed? kai, mar'12)

	}

	private void resetRandomNumbers(int iteration) {
		MatsimRandom.reset(this.config.global().getRandomSeed()
				+ iteration);
		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
		// "not-randomness" is the first
		// draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
	}

	private AbstractMultithreadedModule wrapPlanAlgo( final PlanAlgorithm planAlgo ) {
		// wrap it into the AbstractMultithreadedModule:
		final AbstractMultithreadedModule router = new AbstractMultithreadedModule(this.config.global().getNumberOfThreads()) {
			@Override
			public PlanAlgorithm getPlanAlgoInstance() {
				return planAlgo ;
			}
		};
		return router;
	}

	/**
	 * Creates the path where all iteration-related data should be stored.
	 * 
	 * @param iteration
	 */
	private void makeIterationPath(final int iteration) {
		File dir = new File(this.controlerIO.getIterationPath(iteration));
		if (!dir.mkdir()) {
			if (this.overwriteFiles && dir.exists()) {
				log.info("Iteration directory "
						+ this.controlerIO.getIterationPath(iteration)
						+ " exists already.");
			} else {
				log.warn("Could not create iteration directory "
						+ this.controlerIO.getIterationPath(iteration) + ".");
			}
		}
	}

	/**
	 * in particular select if single cpu handler to use or parallel
	 */
	private void initEvents() {
		final String PARALLEL_EVENT_HANDLING = "parallelEventHandling";
		final String NUMBER_OF_THREADS = "numberOfThreads";
		final String ESTIMATED_NUMBER_OF_EVENTS = "estimatedNumberOfEvents";
		String numberOfThreads = this.config.findParam(PARALLEL_EVENT_HANDLING, NUMBER_OF_THREADS);
		String estimatedNumberOfEvents = this.config.findParam(PARALLEL_EVENT_HANDLING, ESTIMATED_NUMBER_OF_EVENTS);

		if (numberOfThreads != null) {
			int numOfThreads = Integer.parseInt(numberOfThreads);
			// the user wants to user parallel events handling
			if (estimatedNumberOfEvents != null) {
				int estNumberOfEvents = Integer.parseInt(estimatedNumberOfEvents);
				this.events = new ParallelEventsManagerImpl(numOfThreads, estNumberOfEvents);
			} else {
				this.events = new ParallelEventsManagerImpl(numOfThreads);
			}
		} else {
			this.events = EventsUtils.createEventsManager();
		}
	}
	/**
	 * Initializes log4j to write log output to files in output directory.
	 */
	private void initLogging() {
		Logger.getRootLogger().removeAppender(this.collectLogMessagesAppender);
		try {
			IOUtils.initOutputDirLogging(this.config.controler().getOutputDirectory(), 
					this.collectLogMessagesAppender.getLogEvents(), this.config.controler().getRunId());
			this.collectLogMessagesAppender.close();
			this.collectLogMessagesAppender = null;
		} catch (IOException e) {
			log.error("Cannot create logfiles: " + e.getMessage());
			e.printStackTrace();
		}
	}
	void shutdown(final boolean unexpected) {
		// yyyyyy this has not yet been trimmed

		if (unexpected) {
			log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
		} else {
			log.info("S H U T D O W N   ---   start regular shutdown.");
		}
		if (this.uncaughtException != null) {
			log.warn(
					"Shutdown probably caused by the following Exception.", this.uncaughtException);
		}

		this.controlerListenerManager.fireControlerShutdownEvent(unexpected);

		if (this.dumpDataAtEnd) {
			// dump plans
			new PopulationWriter(this.population, this.network).write(this.controlerIO.getOutputFilename(Controler.FILENAME_POPULATION));
			// dump network
			new NetworkWriter(this.network).write(this.controlerIO.getOutputFilename(Controler.FILENAME_NETWORK));
			// dump config
			new ConfigWriter(this.config).write(this.controlerIO.getOutputFilename(Controler.FILENAME_CONFIG));

			if (!unexpected	&& this.config.vspExperimental().isWritingOutputEvents()) {
				File toFile = new File(	this.controlerIO.getOutputFilename("output_events.xml.gz"));
				File fromFile = new File(this.controlerIO.getIterationFilename(this.config.controler().getLastIteration(), "events.xml.gz"));
				IOUtils.copyFile(fromFile, toFile);
			}
		}
		if (unexpected) {
			log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
		} else {
			log.info("S H U T D O W N   ---   regular shutdown completed.");
		}
		try {
			Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
		} catch (IllegalStateException e) {
			log.info("Cannot remove shutdown hook. " + e.getMessage());
		}
		this.shutdownHook = null; // important for test cases to free the memory
		this.collectLogMessagesAppender = null;
		IOUtils.closeOutputDirLogging();
	}

	private void checkConfigConsistencyAndWriteToLog(final String message) {
		log.info(message);
		String newline = System.getProperty("line.separator");// use native line endings for logfile
		StringWriter writer = new StringWriter();
		new ConfigWriter(this.config).writeStream(new PrintWriter(writer), newline);
		log.info(newline + newline + writer.getBuffer().toString());
		log.info("Complete config dump done.");
		log.info("Checking consistency of config...");
		this.config.checkConsistency();
		log.info("Checking consistency of config done.");
	}
	
	private final void setUpOutputDir() {
		this.outputPath = this.config.controler().getOutputDirectory();
		if (this.outputPath.endsWith("/")) {
			this.outputPath = this.outputPath.substring(0, this.outputPath.length() - 1);
		}
		if (this.config.controler().getRunId() != null) {
			this.controlerIO = new ControlerIO(this.outputPath, this.scenarioData.createId(this.config.controler().getRunId()));
		} else {
			this.controlerIO = new ControlerIO(this.outputPath);
		}

		// make the tmp directory
		File outputDir = new File(this.outputPath);
		if (outputDir.exists()) {
			if (outputDir.isFile()) {
				throw new RuntimeException("Cannot create output directory. "
						+ this.outputPath + " is a file and cannot be replaced by a directory.");
			}
			if (outputDir.list().length > 0) {
				if (this.overwriteFiles) {
					System.out.flush();
					log.warn("###########################################################");
					log.warn("### THE CONTROLER WILL OVERWRITE FILES IN:");
					log.warn("### " + this.outputPath);
					log.warn("###########################################################");
					System.err.flush();
				} else {
					// the directory is not empty, we do not overwrite any
					// files!
					throw new RuntimeException(
							"The output directory " + this.outputPath
									+ " exists already but has files in it! Please delete its content or the directory and " +
											"start again. We will not delete or overwrite any existing files.");
				}
			}
		} else {
			if (!outputDir.mkdirs()) {
				throw new RuntimeException(
						"The output directory path " + this.outputPath
								+ " could not be created. Check pathname and permissions!");
			}
		}

		File tmpDir = new File(this.controlerIO.getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			throw new RuntimeException("The tmp directory "
					+ this.controlerIO.getTempPath() + " could not be created.");
		}
		File itersDir = new File(this.outputPath + "/" + Controler.DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			throw new RuntimeException("The iterations directory "
					+ (this.outputPath + "/" + Controler.DIRECTORY_ITERS)
					+ " could not be created.");
		}
	}


}
