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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
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
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.PlanStrategy;
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
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.ptproject.qsim.ParallelQSimFactory;
import org.matsim.ptproject.qsim.QSimFactory;



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

	private static final Layout DEFAULTLOG4JLAYOUT = new PatternLayout("%d{ISO8601} %5p %C{1}:%L %m%n");

	private boolean overwriteFiles = false;
	private Integer iteration = null;

	/** The Config instance the Controler uses. */
	private final Config config;
	private final String configFileName;
	private final String dtdFileName;

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
	private boolean scenarioLoaded = false;
	/**
	 * This variable is used to store the log4j output before it can be written
	 * to a file. This is needed to set the output directory before logging.
	 */
	private CollectLogMessagesAppender collectLogMessagesAppender = null;

	/**
	 * Attribute for the routing factory
	 */
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	/**
	 * This instance encapsulates all behavior concerning the
	 * ControlerEvents/Listeners
	 */

	private final List<SimulationListener> simulationListener = new ArrayList<SimulationListener>();

	private Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			shutdown(true);
		}
	};
	private ScenarioLoaderImpl loader;

	private TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();

	private TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
	private ControlerIO controlerIO;

	private MobsimFactory mobsimFactory = new QueueSimulationFactory();

	private EventsToScore planScorer;
	private TransitConfigGroup transitConfig;

	private static final Logger log = Logger.getLogger(StrategyManagerConfigLoader.class);
	private static int externalCounter = 0;

	/** initializes Log4J */
	static {
		final String logProperties = "log4j.xml";
		URL url = Loader.getResource(logProperties);
		if (url != null) {
			PropertyConfigurator.configure(url);
		} else {
			Logger root = Logger.getRootLogger();
			root.setLevel(Level.INFO);
			ConsoleAppender consoleAppender = new ConsoleAppender(DEFAULTLOG4JLAYOUT, "System.out");
			consoleAppender.setName("A1");
			root.addAppender(consoleAppender);
			//			consoleAppender.setLayout(DEFAULTLOG4JLAYOUT);
			//			log.error("");
			//			log.error("Could not find configuration file " + logProperties + " for Log4j in the classpath.");
			//			log.error("A default configuration is used, setting log level to INFO with a ConsoleAppender.");
			//			log.error("");
			//			log.error("");
		}
	}

	public MZControler(final Scenario scenario) {
		// catch logs before doing something
		this.collectLogMessagesAppender = new CollectLogMessagesAppender();
		Logger.getRootLogger().addAppender(this.collectLogMessagesAppender);
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		log.info("Used Controler-Class: " + this.getClass().getCanonicalName());
		this.configFileName = null;
		this.dtdFileName = null;

		this.scenarioLoaded = true;
		this.scenarioData = scenario;
		this.config = scenario.getConfig();

		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);

		if (this.config.scenario().isUseTransit()) {
			setupTransitConfig();
		}
	}

	/**
	 * Starts the simulation.
	 */
	public void run() {
		if (this.state == ControlerState.Init) {
			init();
			coreStartup();
			scoringStartup();
			doIterations();
			shutdown(false);
		} else {
			log.error("Controler in wrong state to call 'run()'. Expected state: <Init> but was <" + this.state + ">");
		}
	}

	private void init() {
		loadConfig();
		setUpOutputDir();
		initEvents();
		initLogging();
		loadData();
		setUp();
	}

	private final void setupTransitConfig() {
		this.transitConfig = new TransitConfigGroup();
		if (this.config.getModule(TransitConfigGroup.GROUP_NAME) == null) {
			this.config.addModule(TransitConfigGroup.GROUP_NAME, this.transitConfig);
		} else {
			// this would not be necessary if TransitConfigGroup is part of core config
			Module oldModule = this.config.getModule(TransitConfigGroup.GROUP_NAME);
			this.config.removeModule(TransitConfigGroup.GROUP_NAME);
			this.transitConfig.addParam("transitScheduleFile", oldModule.getValue("transitScheduleFile"));
			this.transitConfig.addParam("vehiclesFile", oldModule.getValue("vehiclesFile"));
			this.transitConfig.addParam("transitModes", oldModule.getValue("transitModes"));
		}
		if (!this.config.scenario().isUseVehicles()) {
			log.warn("Your are using Transit but not Vehicles. This most likely won't work.");
		}
		Set<EventsFileFormat> formats = EnumSet.copyOf(this.config.controler().getEventsFileFormats());
		formats.add(EventsFileFormat.xml);
		this.config.controler().setEventsFileFormats(formats);
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		this.config.charyparNagelScoring().addActivityParams(transitActivityParams);
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
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(this.population, this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), (NetworkImpl) network);
			}
		});

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

			scoringIterationStarts(iteration);
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
			scoringScoring(this.iteration);
			log.info(marker + "ITERATION " + this.iteration + " fires iteration end event");
			iterationEndsEvent(this.iteration);
			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(this.controlerIO.getOutputFilename("stopwatch.txt"));
			log.info(marker + "ITERATION " + this.iteration + " ENDS");
			log.info(divider);
		}
		this.iteration = null;
	}

	private void iterationEndsEvent(Integer iteration2) {
		// TODO Auto-generated method stub

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
			this.collectLogMessagesAppender = null;
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

	/*
	 * ===================================================================
	 * private methods
	 * ===================================================================
	 */

	/**
	 * Initializes log4j to write log output to files in output directory.
	 */
	private void initLogging() {
		Logger.getRootLogger().removeAppender(this.collectLogMessagesAppender);
		try {
			IOUtils.initOutputDirLogging(this.config.controler().getOutputDirectory(), this.collectLogMessagesAppender
					.getLogEvents(), this.config.controler().getRunId());
			this.collectLogMessagesAppender.close();
			this.collectLogMessagesAppender = null;
		} catch (IOException e) {
			log.error("Cannot create logfiles: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Loads the configuration object with the correct settings.
	 */
	private void loadConfig() {
		if (this.configFileName != null) {
			try {
				new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
			} catch (IOException e) {
				log.error("Problem loading the configuration file from " + this.configFileName);
				throw new RuntimeException(e);
			}
		}
		log.info("Checking consistency of config...");
		this.config.checkConsistency();
		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		new ConfigWriter(this.config).writeStream(new PrintWriter(writer));
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");

		/* use writeEventsInterval from config file, only if not already
		 * initialized programmatically */
		if (this.writeEventsInterval == -1) {
			this.writeEventsInterval = this.config.controler().getWriteEventsInterval();
		}
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
	 * Load all the required data. Currently, this only loads the Scenario if it was
	 * not given in the Constructor.
	 */
	private void loadData() {
		if (!this.scenarioLoaded) {
			this.loader = new ScenarioLoaderImpl(this.scenarioData);
			this.loader.loadScenario();
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
			this.scenarioLoaded = true;
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
		// dg feb 09: this null checks are not really safe
		// consider the case that a user copies a config with some
		// externalMobsim or JDEQSim information in it and isn't aware
		// that those configs will be used instead of the "normal"
		// queuesimulation
		// configuration
		/*
		 * well, that's the user's problem if he doesn't know what settings he
		 * starts his simulation with. mr/mar09
		 */
		if (this.config.simulation().getExternalExe() == null) {
			final String JDEQ_SIM = "JDEQSim";
			if (this.config.getModule(JDEQ_SIM) != null) {
				JDEQSimulation sim = new JDEQSimulation(this.scenarioData, this.events);
				sim.run();
			} else {
				Simulation simulation = this.getMobsimFactory().createMobsim(this.getScenario(), this.getEvents());
				if (simulation instanceof IOSimulation){
					((IOSimulation)simulation).setControlerIO(this.getControlerIO());
					((IOSimulation)simulation).setIterationNumber(this.getIterationNumber());
				}
				if (simulation instanceof ObservableSimulation){
					for (SimulationListener l : this.getQueueSimulationListener()) {
						((ObservableSimulation)simulation).addQueueSimulationListeners(l);
					}
				}
				simulation.run();
			}
		} else {
			ExternalMobsim sim = new ExternalMobsim(this.scenarioData, this.events);
			sim.setControlerIO(this.controlerIO);
			sim.setIterationNumber(this.getIterationNumber());
			sim.run();
		}
	}



	/*
	 * ===================================================================
	 * Options
	 * ===================================================================
	 */

	/**
	 * Sets whether the Controler is allowed to overwrite files in the output
	 * directory or not. <br>
	 * When starting, the Controler can check that the output directory is empty
	 * or does not yet exist, so no files will be overwritten (default setting).
	 * While useful in a productive environment, this security feature may be
	 * interfering in test cases or while debugging. <br>
	 * <strong>Use this setting with caution, as it can result in data loss!</strong>
	 *
	 * @param overwrite
	 *            whether files and directories should be overwritten (true) or
	 *            not (false)
	 */
	public final void setOverwriteFiles(final boolean overwrite) {
		this.overwriteFiles = overwrite;
	}

	/**
	 * Returns whether the Controler is currently allowed to overwrite files in
	 * the output directory.
	 *
	 * @return true if the Controler is currently allowed to overwrite files in
	 *         the output directory, false if not.
	 */
	public final boolean getOverwriteFiles() {
		return this.overwriteFiles;
	}

	/**
	 * Sets in which iterations events should be written to a file. If set to
	 * <tt>1</tt>, the events will be written in every iteration. If set to
	 * <tt>2</tt>, the events are written every second iteration. If set to
	 * <tt>10</tt>, the events are written in every 10th iteration. To
	 * disable writing of events completely, set the interval to <tt>0</tt>
	 * (zero).
	 *
	 * @param interval
	 *            in which iterations events should be written
	 */
	public final void setWriteEventsInterval(final int interval) {
		this.writeEventsInterval = interval;
	}

	public final int getWriteEventsInterval() {
		return this.writeEventsInterval;
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

	public final EventsManager getEvents() {
		return this.events;
	}

	public final Scenario getScenario() {
		return this.scenarioData;
	}

	private void coreStartup() {
		Config c = getScenario().getConfig();
		QSimConfigGroup conf = (QSimConfigGroup) c.getModule(QSimConfigGroup.GROUP_NAME);
		if (conf != null){
			if (conf.getNumberOfThreads() > 1) {
				setMobsimFactory(new ParallelQSimFactory());
			}
			else {
				setMobsimFactory(new QSimFactory());
			}
		}
		else if (c.getModule("JDEQSim") == null) {
			log.warn("There might be no configuration for a mobility simulation in the config. The Controler " +
			" uses the default QueueSimulation that might not have all features implemented.");
		}
	}

	private void coreBeforeMobsim(int iteration) {
		events.resetHandlers(iteration);
		events.resetCounter();

		if ((writeEventsInterval > 0) && (iteration % writeEventsInterval == 0)) {
			for (EventsFileFormat format : config.controler().getEventsFileFormats()) {
				switch (format) {
				case txt:
					this.eventWriters.add(new EventWriterTXT(getControlerIO().getIterationFilename(iteration,FILENAME_EVENTS_TXT)));
					break;
				case xml:
					this.eventWriters.add(new EventWriterXML(getControlerIO().getIterationFilename(iteration, FILENAME_EVENTS_XML)));
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
			getEvents().removeHandler(writer);
		}
		this.eventWriters.clear();

		if (((iteration % 10 == 0) && (iteration > this.config.controler().getFirstIteration())) || (iteration % 10 >= 6)) {
			linkStats.addData(volumes, travelTimeCalculator);
		}

		if ((iteration % 10 == 0) && (iteration > this.config.controler().getFirstIteration())) {
			events.removeHandler(volumes);
			linkStats.writeFile(getControlerIO().getIterationFilename(iteration, FILENAME_LINKSTATS));
		}

		if (legTimes != null) {
			legTimes.writeStats(getControlerIO().getIterationFilename(iteration, "tripdurations.txt"));
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

	private List<SimulationListener> getQueueSimulationListener() {
		return this.simulationListener;
	}


	ControlerIO getControlerIO() {
		return this.controlerIO;
	}

	/**
	 * @return the iteration number of the current iteration when the Controler is iterating,
	 * null if the Controler is in the startup/shutdown process
	 */
	public Integer getIterationNumber() {
		return this.iteration;
	}

	public MobsimFactory getMobsimFactory() {
		return this.mobsimFactory;
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
	public void load(final StrategyManager manager) {
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

			PlanStrategy strategy = loadStrategy(classname, settings);

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
					manager.changeStrategy(strategy, 0.0);
				}
			}
		}
	}

	private void dumpPlansBeforeMobsim(int iteration) {
		if ((iteration % 10 == 0) || (iteration == (this.config.controler().getFirstIteration() + 1))) {
			stopwatch.beginOperation("dump all plans");
			log.info("dumping plans...");
			new PopulationWriter(this.population, this.network)
			.write(getControlerIO().getIterationFilename(iteration, "plans.xml.gz"));
			log.info("finished plans dump.");
			stopwatch.endOperation("dump all plans");
		}
	}

	private PlanStrategy loadStrategy(final String name, final StrategyConfigGroup.StrategySettings settings) {
		Network network = this.network;
		PersonalizableTravelCost travelCostCalc = this.travelCostCalculatorFactory.createTravelCostCalculator(this.travelTimeCalculator, this.config.charyparNagelScoring());
		TravelTime travelTimeCalc = this.travelTimeCalculator;
		Config config = this.config;

		PlanStrategy strategy = null;
		if (name.equals("KeepLastSelected")) {
			strategy = new PlanStrategy(new KeepSelected());
		} else if (name.equals("ReRoute") || name.equals("threaded.ReRoute")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRoute(this));
		} else if (name.equals("ReRoute_Dijkstra")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRouteDijkstra(config, network, travelCostCalc, travelTimeCalc));
		} else if (name.equals("ReRoute_Landmarks")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
		} else if (name.equals("TimeAllocationMutator") || name.equals("threaded.TimeAllocationMutator")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			TimeAllocationMutator tam = new TimeAllocationMutator(config);
//			tam.setUseActivityDurations(config.vspExperimental().isUseActivityDurations());
			// functionality moved into TimeAllocationMutator.  kai, aug'10
			strategy.addStrategyModule(tam);
		} else if (name.equals("TimeAllocationMutator7200_ReRouteLandmarks")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new TimeAllocationMutator(config, 7200));
			strategy.addStrategyModule(new ReRouteLandmarks(config, network, travelCostCalc, travelTimeCalc, new FreespeedTravelTimeCost(config.charyparNagelScoring())));
		} else if (name.equals("ExternalModule")) {
			externalCounter++;
			strategy = new PlanStrategy(new RandomPlanSelector());
			String exePath = settings.getExePath();
			ExternalModule em = new ExternalModule(exePath, "ext" + externalCounter, controlerIO, getScenario(), 1);
			em.setIterationNumber(getIterationNumber());
			strategy.addStrategyModule(em);
		} else if (name.equals("BestScore")) {
			strategy = new PlanStrategy(new BestPlanSelector());
		} else if (name.equals("SelectExpBeta")) {
			strategy = new PlanStrategy(new ExpBetaPlanSelector(config.charyparNagelScoring()));
		} else if (name.equals("ChangeExpBeta")) {
			strategy = new PlanStrategy(new ExpBetaPlanChanger(config.charyparNagelScoring().getBrainExpBeta()));
		} else if (name.equals("SelectRandom")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
		} else if (name.equals("ChangeLegMode")) {
			strategy = new PlanStrategy(new RandomPlanSelector());
			strategy.addStrategyModule(new ChangeLegMode(config));
			strategy.addStrategyModule(new ReRoute(this));
		} else {
			//classes loaded by name must not be part of the matsim core
			if (name.startsWith("org.matsim")) {
				log.error("Strategies in the org.matsim package must not be loaded by name!");
			}
			else {
				try {
					Class<? extends PlanStrategy> klas = (Class<? extends PlanStrategy>) Class.forName(name);
					Class[] args = new Class[1];
					args[0] = Scenario.class;
					Constructor<? extends PlanStrategy> c = null;
					try{
						c = klas.getConstructor(args);
						strategy = c.newInstance(getScenario());
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

	public static class ReRoute extends AbstractMultithreadedModule {

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
		getEvents().addHandler(this.planScorer);
	}

	private void scoringIterationStarts(int iteration) {
		this.planScorer.reset(iteration);
	}

	private void scoringScoring(int iteration) {
		this.planScorer.finish();
	}

	TravelTimeCalculator getTravelTimeCalculator() {
		return travelTimeCalculator;
	}

	void setScoringFunctionFactory(ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
	}

	ScoringFunctionFactory getScoringFunctionFactory() {
		return scoringFunctionFactory;
	}

}


