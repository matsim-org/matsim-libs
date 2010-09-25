/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package playground.mzilske.city2000w;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.ControlerIO;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.modules.ReRouteLandmarks;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorInvertedNetProxyFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;



/**
 * The Controler is responsible for complete simulation runs, including the
 * initialization of all required data, running the iterations and the
 * replanning, analyses, etc.
 *
 * @author mrieser
 */
public final class MZControler {

	public static final String DIRECTORY_ITERS = "ITERS";
	public static final String FILENAME_EVENTS_TXT = "events.txt.gz";
	public static final String FILENAME_EVENTS_XML = "events.xml.gz";
	public static final String FILENAME_LINKSTATS = "linkstats.txt";
	public static final String FILENAME_SCORESTATS = "scorestats.txt";
	public static final String FILENAME_TRAVELDISTANCESTATS = "traveldistancestats.txt";
	public static final String FILENAME_POPULATION = "output_plans.xml.gz";
	public static final String FILENAME_NETWORK = "output_network.xml.gz";
	public static final String FILENAME_HOUSEHOLDS = "output_households.xml.gz";
	public static final String FILENAME_LANES = "output_lanes.xml.gz";
	public static final String FILENAME_SIGNALSYSTEMS = "output_signalsystems.xml.gz";
	public static final String FILENAME_SIGNALSYSTEMS_CONFIG = "output_signalsystem_configuration.xml.gz";
	public static final String FILENAME_CONFIG = "output_config.xml.gz";

	private enum ControlerState {
		Init, Running, Shutdown, Finished
	}

	private ControlerState state = ControlerState.Init;

	private String outputPath = null;
	
	private boolean overwriteFiles = false;
	
	private Integer iteration = null;

	/** The Config instance the Controler uses. */
	private final Config config;

	private EventsManagerImpl events = null;
	private Network network = null;
	private Population population = null;
	private TravelTimeCalculator travelTimeCalculator = null;
	private PersonalizableTravelCost travelCostCalculator = null;
	private ScoringFunctionFactory scoringFunctionFactory = null;
	private StrategyManager strategyManager = null;

	/**
	 * Defines in which iterations the events should be written. <tt>1</tt> is
	 * in every iteration, <tt>2</tt> in every second, <tt>10</tt> in every
	 * 10th, and so forth. <tt>0</tt> disables the writing of events
	 * completely.
	 */
	private int writeEventsInterval = -1;

	/* default analyses */
	private CalcLinkStats linkStats = null;
	private CalcLegTimes legTimes = null;
	private VolumesAnalyzer volumes = null;

	private final IterationStopWatch stopwatch = new IterationStopWatch();
	final private Scenario scenarioData;

	/**
	 * Attribute for the routing factory
	 */
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	private Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			shutdown(true);
		}
	};

	private TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();

	private TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
	private ControlerIO controlerIO;

	private MobsimFactory mobsimFactory = new QueueSimulationFactory();

	private EventsToScore planScorer;

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);
	private static int externalCounter = 0;

	public MZControler(final Scenario scenario) {
		this.scenarioData = scenario;
		this.config = scenario.getConfig();
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
	}

	/**
	 * Starts the simulation.
	 */
	public void run() {
		if (this.state == ControlerState.Init) {
			doRun();
		} else {
			throw new RuntimeException("Controler in wrong state to call 'run()'. Expected state: <Init> but was <" + this.state + ">");
		}
	}

	private void doRun() {
		setUpOutputDir();
		initEvents();
		setUp();
		scoringStartup();
		preparePersonsForSim();
		doIterations();
		shutdown(false);
	}



	/**
	 * select if single cpu handler to use or parallel
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
			this.events = new EventsManagerImpl();
		}
	}

	private void doIterations() {
		int firstIteration = this.config.controler().getFirstIteration();
		int lastIteration = this.config.controler().getLastIteration();
		this.state = ControlerState.Running;
		String divider = "###################################################";
		String marker = "### ";

		for (this.iteration = firstIteration; (this.iteration <= lastIteration) && (this.state == ControlerState.Running); this.iteration++) {
			log.info(divider);
			log.info(marker + "ITERATION " + this.iteration + " BEGINS");
			this.stopwatch.setCurrentIteration(this.iteration);
			this.stopwatch.beginOperation("iteration");
			makeIterationPath(this.iteration);
			resetRandomNumbers();

			this.planScorer.reset(iteration);
			if (this.iteration > firstIteration) {
				this.stopwatch.beginOperation("replanning");
				replanningReplanning(iteration);
				this.stopwatch.endOperation("replanning");
			}
			coreBeforeMobsim(iteration);
			dumpPlansBeforeMobsim(this.iteration);
			this.stopwatch.beginOperation("mobsim");
			resetRandomNumbers();
			runMobSim();
			this.stopwatch.endOperation("mobsim");
			log.info(marker + "ITERATION " + this.iteration + " fires after mobsim event");
			coreAfterMobsim(iteration);
			log.info(marker + "ITERATION " + this.iteration + " fires scoring event");
			this.planScorer.finish();
			log.info(marker + "ITERATION " + this.iteration + " fires iteration end event");
			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(this.controlerIO.getOutputFilename("stopwatch.txt"));
			log.info(marker + "ITERATION " + this.iteration + " ENDS");
			log.info(divider);
		}
		this.iteration = null;
	}

	private void preparePersonsForSim() {
		ParallelPersonAlgorithmRunner.run(this.population, this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), (NetworkImpl) network);
			}
		});
	}

	private void shutdown(final boolean unexpected) {
		ControlerState oldState = this.state;
		this.state = ControlerState.Shutdown;
		if (oldState == ControlerState.Running) {
			if (unexpected) {
				log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
			} else {
				log.info("S H U T D O W N   ---   start regular shutdown.");
			}
			coreShutdown();
			// dump plans
			new PopulationWriter(this.population, this.network).write(this.controlerIO.getOutputFilename(FILENAME_POPULATION));
			// dump network
			new NetworkWriter(this.network).write(this.controlerIO.getOutputFilename(FILENAME_NETWORK));
			// dump config
			new ConfigWriter(this.config).write(this.controlerIO.getOutputFilename(FILENAME_CONFIG));


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
			IOUtils.closeOutputDirLogging();
		}
	}

	/**
	 * Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, and after the
	 * scenario data (network, population) is read.
	 */
	private void setUp() {
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = this.travelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config
					.travelTimeCalculator());
		}
		if (this.travelCostCalculator == null) {
			this.travelCostCalculator = this.travelCostCalculatorFactory.createTravelCostCalculator(this.travelTimeCalculator, this.config
					.charyparNagelScoring());
		}
		this.events.addHandler(this.travelTimeCalculator);

		if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.Dijkstra)) {
			this.leastCostPathCalculatorFactory = new DijkstraFactory();
		} else if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.AStarLandmarks)) {
			this.leastCostPathCalculatorFactory = new AStarLandmarksFactory(this.network, new FreespeedTravelTimeCost(this.config
					.charyparNagelScoring()));
		} else {
			throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
		}

		if (this.config.controler().isLinkToLinkRoutingEnabled()) {
			this.leastCostPathCalculatorFactory = new LeastCostPathCalculatorInvertedNetProxyFactory(
					this.leastCostPathCalculatorFactory);
		}

		/*
		 * TODO [MR] linkStats uses ttcalc and volumes, but ttcalc has
		 * 15min-steps, while volumes uses 60min-steps! It works a.t.m., but the
		 * traveltimes in linkStats are the avg. traveltimes between xx.00 and
		 * xx.15, and not between xx.00 and xx.59
		 */
		this.linkStats = new CalcLinkStats(this.network);
		this.volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network);
		this.legTimes = new CalcLegTimes(this.population);
		this.events.addHandler(this.legTimes);

		this.scoringFunctionFactory = loadScoringFunctionFactory();


		this.strategyManager = loadStrategyManager();
	}

	private final List<EventWriter> eventWriters = new LinkedList<EventWriter>();

	private final void setUpOutputDir() {
		outputPath = this.config.controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		if (this.config.controler().getRunId() != null) {
			this.controlerIO =  new ControlerIO(outputPath, this.scenarioData.createId(this.config.controler().getRunId()));
		}
		else {
			this.controlerIO =  new ControlerIO(outputPath);
		}

		// make the tmp directory
		File outputDir = new File(outputPath);
		if (outputDir.exists()) {
			if (outputDir.isFile()) {
				throw new RuntimeException("Cannot create output directory. " + outputPath
						+ " is a file and cannot be replaced by a directory.");
			}
			if (outputDir.list().length > 0) {
				if (this.overwriteFiles) {
					log.warn("###########################################################");
					log.warn("### THE CONTROLER WILL OVERWRITE FILES IN:");
					log.warn("### " + outputPath);
					log.warn("###########################################################");
				} else {
					// the directory is not empty, we do not overwrite any
					// files!
					throw new RuntimeException(
							"The output directory "
							+ outputPath
							+ " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
				}
			}
		} else {
			if (!outputDir.mkdirs()) {
				throw new RuntimeException("The output directory path " + outputPath
						+ " could not be created. Check pathname and permissions!");
			}
		}

		File tmpDir = new File(this.controlerIO.getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			throw new RuntimeException("The tmp directory " + this.controlerIO.getTempPath() + " could not be created.");
		}
		File itersDir = new File(outputPath + "/" + DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			throw new RuntimeException("The iterations directory " + (outputPath + "/" + DIRECTORY_ITERS)
					+ " could not be created.");
		}
	}


	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	private StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		load(manager);
		return manager;
	}

	/**
	 * Loads the {@link ScoringFunctionFactory} to be used for plans-scoring.
	 * This method will only be called if the user has not yet manually set a
	 * custom scoring function with
	 * {@link #setScoringFunctionFactory(ScoringFunctionFactory)}.
	 *
	 * @return The ScoringFunctionFactory to be used for plans-scoring.
	 */
	private ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelScoringFunctionFactory(this.config.charyparNagelScoring());
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
				log.info("Iteration directory " + this.controlerIO.getIterationPath(iteration) + " exists already.");
			} else {
				log.warn("Could not create iteration directory " + this.controlerIO.getIterationPath(iteration) + ".");
			}
		}
	}

	private void resetRandomNumbers() {
		MatsimRandom.reset(this.config.global().getRandomSeed() + this.iteration);
		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
		// "not-randomness" is the first
		// draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
	}

	private void runMobSim() {
		Simulation simulation = mobsimFactory.createMobsim(this.scenarioData, this.events);
		simulation.run();
	}


	/**
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes
	 *         of plans with the default (= the current from the last or current
	 *         iteration) travel costs and travel times. Only to be used by a
	 *         single thread, use multiple instances for multiple threads!
	 */
	private PlanAlgorithm createRoutingAlgorithm() {
		return new PlansCalcRoute(this.config.plansCalcRoute(), this.network, this.travelCostCalculatorFactory.createTravelCostCalculator(this.travelTimeCalculator, this.config.charyparNagelScoring()), this.travelTimeCalculator, this.leastCostPathCalculatorFactory);
	}


	private void coreBeforeMobsim(int iteration) {
		events.resetHandlers(iteration);
		events.resetCounter();

		if ((writeEventsInterval > 0) && (iteration % writeEventsInterval == 0)) {
			for (EventsFileFormat format : config.controler().getEventsFileFormats()) {
				switch (format) {
				case txt:
					this.eventWriters.add(new EventWriterTXT(this.controlerIO.getIterationFilename(iteration,FILENAME_EVENTS_TXT)));
					break;
				case xml:
					this.eventWriters.add(new EventWriterXML(this.controlerIO.getIterationFilename(iteration, FILENAME_EVENTS_XML)));
					break;
				default:
					log.warn("Unknown events file format specified: " + format.toString() + ".");
				}
			}
			for (EventWriter writer : this.eventWriters) {
				events.addHandler(writer);
			}
		}

		if (iteration % 10 == 6) {
			volumes.reset(iteration);
			events.addHandler(volumes);
		}

		// init for event processing of new iteration
		events.initProcessing();
	}

	private void coreAfterMobsim(int iteration) {

		// prepare for finishing iteration
		events.finishProcessing();

		for (EventWriter writer : this.eventWriters) {
			writer.closeFile();
			events.removeHandler(writer);
		}
		this.eventWriters.clear();

		if (((iteration % 10 == 0) && (iteration > this.config.controler().getFirstIteration())) || (iteration % 10 >= 6)) {
			linkStats.addData(volumes, travelTimeCalculator);
		}

		if ((iteration % 10 == 0) && (iteration > this.config.controler().getFirstIteration())) {
			events.removeHandler(volumes);
			linkStats.writeFile(this.controlerIO.getIterationFilename(iteration, FILENAME_LINKSTATS));
		}

		if (legTimes != null) {
			legTimes.writeStats(this.controlerIO.getIterationFilename(iteration, "tripdurations.txt"));
			// - print averages in log
			log.info("[" + iteration + "] average trip duration is: " + (int) legTimes.getAverageTripDuration()
					+ " seconds = " + Time.writeTime(legTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
		}
	}

	private void coreShutdown() {
		for (EventWriter writer : eventWriters) {
			writer.closeFile();
		}
	}

	public void setMobsimFactory(MobsimFactory mobsimFactory) {
		this.mobsimFactory = mobsimFactory;
	}


	/**
	 * Reads and instantiates the strategy modules specified in the config-object.
	 *
	 * @param controler the {@link MZControler} that provides miscellaneous data for the replanning modules
	 * @param manager the {@link StrategyManager} to be configured according to the configuration
	 */
	private void load(final StrategyManager manager) {
		Config config = this.config;
		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());

		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			if (classname.startsWith("org.matsim.demandmodeling.plans.strategies.")) {
				classname = classname.replace("org.matsim.demandmodeling.plans.strategies.", "");
			}

			PlanStrategyImpl strategy = loadStrategy(classname, settings);

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			manager.addStrategy(strategy, rate);

			// now check if this modules should be disabled after some iterations
			if (settings.getDisableAfter() >= 0) {
				int maxIter = settings.getDisableAfter();
				if (maxIter >= this.config.controler().getFirstIteration()) {
					manager.addChangeRequest(maxIter + 1, strategy, 0.0);
				} else {
					/* The controler starts at a later iteration than this change request is scheduled for.
					 * make the change right now.					 */
					manager.changeWeightOfStrategy(strategy, 0.0);
				}
			}
		}
	}

	private void dumpPlansBeforeMobsim(int iteration) {
		if ((iteration % 10 == 0) || (iteration == (this.config.controler().getFirstIteration() + 1))) {
			stopwatch.beginOperation("dump all plans");
			log.info("dumping plans...");
			new PopulationWriter(this.population, this.network)
			.write(this.controlerIO.getIterationFilename(iteration, "plans.xml.gz"));
			log.info("finished plans dump.");
			stopwatch.endOperation("dump all plans");
		}
	}

	private PlanStrategyImpl loadStrategy(final String name, final StrategyConfigGroup.StrategySettings settings) {
		Network network = this.network;
		PersonalizableTravelCost travelCostCalc = this.travelCostCalculatorFactory.createTravelCostCalculator(this.travelTimeCalculator, this.config.charyparNagelScoring());
		PersonalizableTravelTime travelTimeCalc = this.travelTimeCalculator;
		Config config = this.config;

		PlanStrategyImpl strategy = null;
		if (name.equals("KeepLastSelected")) {
			strategy = new PlanStrategyImpl(new KeepSelected());
		} else if (name.equals("ReRoute") || name.equals("threaded.ReRoute")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRoute(this));
		} else if (name.equals("ReRoute_Dijkstra")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRouteDijkstra(config, network, travelCostCalc, travelTimeCalc));
		} else if (name.equals("ReRoute_Landmarks")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
		} else if (name.equals("TimeAllocationMutator") || name.equals("threaded.TimeAllocationMutator")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			TimeAllocationMutator tam = new TimeAllocationMutator(config);
//			tam.setUseActivityDurations(config.vspExperimental().isUseActivityDurations());
			// functionality moved into TimeAllocationMutator.  kai, aug'10
			strategy.addStrategyModule(tam);
		} else if (name.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new TimeAllocationMutator(config, 7200));
			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
		} else if (name.equals("ExternalModule")) {
			externalCounter++;
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			String exePath = settings.getExePath();
			ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controlerIO, scenarioData, 1);
			em.setIterationNumber(iteration);
			strategy.addStrategyModule(em);
		} else if (name.equals("BestScore")) {
			strategy = new PlanStrategyImpl(new BestPlanSelector());
		} else if (name.equals("SelectExpBeta")) {
			strategy = new PlanStrategyImpl(new ExpBetaPlanSelector(config.charyparNagelScoring()));
		} else if (name.equals("ChangeExpBeta")) {
			strategy = new PlanStrategyImpl(new ExpBetaPlanChanger(config.charyparNagelScoring().getBrainExpBeta()));
		} else if (name.equals("SelectRandom")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
		} else if (name.equals("ChangeLegMode")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new ChangeLegMode(config));
			strategy.addStrategyModule(new ReRoute(this));
		} else {
			//classes loaded by name must not be part of the matsim core
			if (name.startsWith("org.matsim")) {
				log.error("Strategies in the org.matsim package must not be loaded by name!");
			}
			else {
				try {
					Class<? extends PlanStrategyImpl> klas = (Class<? extends PlanStrategyImpl>) Class.forName(name);
					Class[] args = new Class[1];
					args[0] = Scenario.class;
					Constructor<? extends PlanStrategyImpl> c = null;
					try{
						c = klas.getConstructor(args);
						strategy = c.newInstance(scenarioData);
					} catch(NoSuchMethodException e){
						log.warn("Cannot find Constructor in PlanStrategy " + name + " with single argument of type Scenario. " +
								"This is not fatal, trying to find other constructor, however a constructor expecting Scenario as " +
						"single argument is recommented!" );
					}
					if (c == null){
						args[0] = MZControler.class;
						c = klas.getConstructor(args);
						strategy = c.newInstance(this);
					}
					log.info("Loaded PlanStrategy from class " + name);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		return strategy;
	}

	private static class ReRoute extends AbstractMultithreadedModule {

		private final MZControler controler;

		public ReRoute(final MZControler controler) {
			super(controler.config.global());
			this.controler = controler;
		}

		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
			return this.controler.createRoutingAlgorithm();
		}

	}


	private void replanningReplanning(int iteration) {
		this.strategyManager.run(this.population, iteration);
	}

	private void scoringStartup() {
		this.planScorer = new EventsToScore(this.population, this.scoringFunctionFactory, this.config.charyparNagelScoring().getLearningRate());
		Logger.getLogger(PlansScoring.class).debug("PlanScoring loaded ScoringFunctionFactory");
		events.addHandler(this.planScorer);
	}

	void setScoringFunctionFactory(ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

}


