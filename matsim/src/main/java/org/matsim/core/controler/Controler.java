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

package org.matsim.core.controler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

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
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.TravelDistanceStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.corelisteners.LegHistogramListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.algorithms.EventWriter;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.parallelEventsHandler.ParallelEventsManagerImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.framework.IOSimulation;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.ObservableSimulation;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.jdeqsim.parallel.PJDEQSimulation;
import org.matsim.core.mobsim.queuesim.QueueNetwork;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.network.NetworkChangeEventsWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorInvertedNetProxyFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.io.CollectLogMessagesAppender;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.ParallelQSimFactory;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.roadpricing.PlansCalcAreaTollRoute;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;
import org.matsim.world.algorithms.WorldCheck;

/**
 * The Controler is responsible for complete simulation runs, including the
 * initialization of all required data, running the iterations and the
 * replanning, analyses, etc.
 *
 * @author mrieser
 */
public class Controler {

	private static final String DIRECTORY_ITERS = "ITERS";
	/* package */static final String FILENAME_EVENTS_TXT = "events.txt.gz";
	/* package */static final String FILENAME_EVENTS_XML = "events.xml.gz";
	public static final String FILENAME_LINKSTATS = "linkstats.txt";
	public static final String FILENAME_SCORESTATS = "scorestats.txt";
	public static final String FILENAME_TRAVELDISTANCESTATS = "traveldistancestats.txt";

	private enum ControlerState {
		Init, Running, Shutdown, Finished
	}

	private ControlerState state = ControlerState.Init;

	private static String outputPath = null;

	public static final Layout DEFAULTLOG4JLAYOUT = new PatternLayout("%d{ISO8601} %5p %C{1}:%L %m%n");

	private boolean overwriteFiles = false;
	private static int iteration = -1;

	/** The Config instance the Controler uses. */
	protected final Config config;
	private final String configFileName;
	private final String dtdFileName;

	protected EventsManagerImpl events = null;
	protected NetworkImpl network = null;
	protected Population population = null;
	private Counts counts = null;

	protected TravelTimeCalculator travelTimeCalculator = null;
	protected TravelCost travelCostCalculator = null;
	protected ScoringFunctionFactory scoringFunctionFactory = null;
	protected StrategyManager strategyManager = null;

	/**
	 * Defines in which iterations the events should be written. <tt>1</tt> is
	 * in every iteration, <tt>2</tt> in every second, <tt>10</tt> in every
	 * 10th, and so forth. <tt>0</tt> disables the writing of events
	 * completely.
	 */
	/* package */int writeEventsInterval = -1;

	/* default analyses */
	/* package */CalcLinkStats linkStats = null;
	/* package */CalcLegTimes legTimes = null;
	/* package */VolumesAnalyzer volumes = null;

	private boolean createGraphs = true;

	public final IterationStopWatch stopwatch = new IterationStopWatch();
	final protected ScenarioImpl scenarioData;
	protected boolean scenarioLoaded = false;
	private PlansScoring plansScoring = null;
	private RoadPricing roadPricing = null;
	private ScoreStats scoreStats = null;
	private TravelDistanceStats travelDistanceStats = null;
	/**
	 * This variable is used to store the log4j output before it can be written
	 * to a file. This is needed to set the output directory before logging.
	 */
	private CollectLogMessagesAppender collectLogMessagesAppender = null;

	private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();
	/**
	 * Attribute for the routing factory
	 */
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	/**
	 * This instance encapsulates all behavior concerning the
	 * ControlerEvents/Listeners
	 */
	private final ControlerListenerManager controlerListenerManager = new ControlerListenerManager(this);

	private static final Logger log = Logger.getLogger(Controler.class);

	private final List<SimulationListener> simulationListener = new ArrayList<SimulationListener>();

	private final Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			shutdown(true);
		}
	};
	protected ScenarioLoaderImpl loader;

	private TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();

	private TravelCostCalculatorFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
	private ControlerIO controlerIO;

  private MobsimFactory mobsimFactory = new QueueSimulationFactory();


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
			consoleAppender.setLayout(DEFAULTLOG4JLAYOUT);
			log.error("");
			log.error("Could not find configuration file " + logProperties + " for Log4j in the classpath.");
			log.error("A default configuration is used, setting log level to INFO with a ConsoleAppender.");
			log.error("");
			log.error("");
		}
	}

	/**
	 * Initializes a new instance of Controler with the given arguments.
	 *
	 * @param args
	 *            The arguments to initialize the controler with.
	 *            <code>args[0]</code> is expected to contain the path to a
	 *            configuration file, <code>args[1]</code>, if set, is
	 *            expected to contain the path to a local copy of the DTD file
	 *            used in the configuration file.
	 */
	public Controler(final String[] args) {
		this(args.length > 0 ? args[0] : null, args.length > 1 ? args[1] : null, null, null);
	}

	public Controler(final String configFileName) {
		this(configFileName, null, null, null);
	}

	public Controler(final Config config) {
		this(null, null, config, null);
	}

	public Controler(final ScenarioImpl scenario) {
		this(null, null, null, scenario);
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
	}

	private Controler(final String configFileName, final String dtdFileName, final Config config, final ScenarioImpl scenario) {
		// catch logs before doing something
		this.collectLogMessagesAppender = new CollectLogMessagesAppender();
		Logger.getRootLogger().addAppender(this.collectLogMessagesAppender);
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		this.configFileName = configFileName;
		this.dtdFileName = dtdFileName;

		// now do other stuff
		if (scenario != null) {
			this.scenarioLoaded = true;
			this.scenarioData = scenario;
			this.config = scenario.getConfig();
		} else {
			if (configFileName == null) {
				if (config == null) {
					throw new IllegalArgumentException(
					"Either the config or the filename of a configfile must be set to initialize the Controler.");
				}
				this.config = config;
			} else {
				this.config = new Config();
				this.config.addCoreModules();
				this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
			}
			this.scenarioData = new ScenarioImpl(this.config);
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
		}

		Gbl.setConfig(this.config);
		Runtime.getRuntime().addShutdownHook(this.shutdownHook);
	}

	/**
	 * Starts the simulation.
	 */
	public void run() {
		if (this.state == ControlerState.Init) {
			init();
			this.controlerListenerManager.fireControlerStartupEvent();
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
		loadCoreListeners();
		loadControlerListeners();
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
		ParallelPersonAlgorithmRunner.run(this.getPopulation(), this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(getRoutingAlgorithm(), Controler.this.network);
			}
		});

		int firstIteration = this.config.controler().getFirstIteration();
		int lastIteration = this.config.controler().getLastIteration();
		this.state = ControlerState.Running;
		String divider = "###################################################";
		String marker = "### ";

		for (iteration = firstIteration; (iteration <= lastIteration) && (this.state == ControlerState.Running); iteration++) {
			log.info(divider);
			log.info(marker + "ITERATION " + iteration + " BEGINS");
			this.stopwatch.setCurrentIteration(Controler.iteration);
			this.stopwatch.beginOperation("iteration");
			makeIterationPath(iteration);
			resetRandomNumbers();

			this.controlerListenerManager.fireControlerIterationStartsEvent(iteration);
			if (iteration > firstIteration) {
				this.stopwatch.beginOperation("replanning");
				this.controlerListenerManager.fireControlerReplanningEvent(iteration);
				this.stopwatch.endOperation("replanning");
			}
			this.controlerListenerManager.fireControlerBeforeMobsimEvent(iteration);
			this.stopwatch.beginOperation("mobsim");
			resetRandomNumbers();
			runMobSim();
			this.stopwatch.endOperation("mobsim");
			this.controlerListenerManager.fireControlerAfterMobsimEvent(iteration);
			this.controlerListenerManager.fireControlerScoringEvent(iteration);
			this.controlerListenerManager.fireControlerIterationEndsEvent(iteration);
			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(this.getNameForOutputFilename("stopwatch.txt"));
			log.info(marker + "ITERATION " + iteration + " ENDS");
			log.info(divider);
		}

	}

	protected void shutdown(final boolean unexpected) {
		ControlerState oldState = this.state;
		this.state = ControlerState.Shutdown;
		if (oldState == ControlerState.Running) {
			if (unexpected) {
				log.warn("S H U T D O W N   ---   received unexpected shutdown request.");
			} else {
				log.info("S H U T D O W N   ---   start regular shutdown.");
			}
			this.controlerListenerManager.fireControlerShutdownEvent(unexpected);
			// dump plans
			new PopulationWriter(this.population, this.network, (this.getScenario()).getKnowledges()).writeFile(this
							.getNameForOutputFilename("output_plans.xml.gz"));
			// dump network
			new NetworkWriter(this.network).writeFile(this.getNameForOutputFilename("output_network.xml.gz"));
			// dump world
			new WorldWriter(this.getWorld()).writeFile(this.getNameForOutputFilename("output_world.xml.gz"));
			// dump config
			new ConfigWriter(this.config).writeFile(this.getNameForOutputFilename("output_config.xml.gz"));
			// dump facilities
			ActivityFacilities facilities = this.getFacilities();
			if (facilities != null) {
				new FacilitiesWriter((ActivityFacilitiesImpl) facilities).writeFile(this.getNameForOutputFilename("output_facilities.xml.gz"));
			}
			if (this.network.getFactory().isTimeVariant()) {
				new NetworkChangeEventsWriter().write(this.getNameForOutputFilename("output_change_events.xml.gz"), this.network.getNetworkChangeEvents());
			}

			if (unexpected) {
				log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
			} else {
				// only remove hook when regular shutdown, otherwise we can't as
				// the shutdown is already in progress...
				Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				log.info("S H U T D O W N   ---   regular shutdown completed.");
			}
			IOUtils.closeOutputDirLogging();
		}
	}

	/**
	 * Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, and after the
	 * scenario data (network, population) is read.
	 */
	protected void setUp() {
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

		if (this.scoringFunctionFactory == null) {
			this.scoringFunctionFactory = loadScoringFunctionFactory();
		}

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

		File tmpDir = new File(getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			throw new RuntimeException("The tmp directory " + getTempPath() + " could not be created.");
		}
		File itersDir = new File(outputPath + "/" + DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			throw new RuntimeException("The iterations directory " + (outputPath + "/" + DIRECTORY_ITERS)
					+ " could not be created.");
		}
	}

	/**
	 * Load all the required data. Currently, this only calls
	 * {@link #loadNetwork()} and {@link #loadPopulation()}, if this data was
	 * not given in the Constructor.
	 */
	protected void loadData() {
		if (!this.scenarioLoaded) {
			this.loader = new ScenarioLoaderImpl(this.scenarioData);
			this.loader.loadScenario();
			this.network = loadNetwork();
			this.population = loadPopulation();
			this.scenarioLoaded = true;

			if (this.getWorld() != null) {
				new WorldCheck().run(this.getWorld());
			}
		}
	}

	/**
	 * Loads the network for the simulation. In most cases, this should be an
	 * instance of {@link QueueNetwork} for the standard QueueSimulation. <br>
	 * <strong>It is highly recommended NOT to overwrite this method!</strong>
	 * This method should be private, but is only protected at the moment
	 * because of backward-compatibility with the old Controler class. In
	 * general, it is recommended to pass a custom network and population using
	 * the special {@link #Controler(ScenarioImpl) Constructor}.
	 *
	 * @deprecated Use the constructor
	 *             {@link #Controler(ScenarioImpl)}
	 *             instead.
	 * @return The network to be used for the simulation.
	 */
	@Deprecated
	protected NetworkImpl loadNetwork() {
		return this.scenarioData.getNetwork();
	}

	/**
	 * Loads the population for the simulation. <br>
	 * <strong>It is highly recommended NOT to overwrite this method!</strong>
	 * This method should be private, but is only protected at the moment
	 * because of backward-compatibility with the old Controler class. In
	 * general, it is recommended to pass a custom network and population using
	 * the special {@link #Controler(ScenarioImpl) Constructor}.
	 *
	 * @deprecated Use the constructor
	 *             {@link #Controler(ScenarioImpl)}
	 *             instead.
	 * @return The population to be used for the simulation.
	 */
	@Deprecated
	protected Population loadPopulation() {
		return this.scenarioData.getPopulation();
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		StrategyManagerConfigLoader.load(this, manager);
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
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelScoringFunctionFactory(this.config.charyparNagelScoring());
	}

	/**
	 * Loads a default set of
	 * {@link org.matsim.core.controler.listener ControlerListener} to provide
	 * basic functionality. <b>Note:</b> Be very careful if you overwrite this
	 * method! The order how the listeners are added is very important. Check
	 * the comments in the source file before overwriting this method!
	 */
	protected void loadCoreListeners() {

		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important. Example: The
		 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
		 * turn is used by the PlansScoring-Listener. Note that the execution
		 * order is contrary to the order the listeners are added to the list.
		 */

		this.addCoreControlerListener(new CoreControlerListener());

		// the default handling of plans
		this.plansScoring = new PlansScoring();
		this.addCoreControlerListener(this.plansScoring);

		// load road pricing, if requested
		if (this.config.roadpricing().getTollLinksFile() != null) {
			this.roadPricing = new RoadPricing();
			this.addCoreControlerListener(this.roadPricing);
		}

		this.addCoreControlerListener(new PlansReplanning());
		this.addCoreControlerListener(new PlansDumping());
	}

	/**
	 * Loads the default set of
	 * {@link org.matsim.core.controler.listener ControlerListener} to provide
	 * some more basic functionality. Unlike the core ControlerListeners the
	 * order in which the listeners of this method are added must not affect the
	 * correctness of the code.
	 */
	protected void loadControlerListeners() {
		// optional: LegHistogram
		this.addControlerListener(new LegHistogramListener(this.events, this.createGraphs));

		// optional: score stats
		try {
			this.scoreStats = new ScoreStats(this.population, this.getNameForOutputFilename(FILENAME_SCORESTATS), this.createGraphs);
			this.addControlerListener(this.scoreStats);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// optional: travel distance stats
		try {
			this.travelDistanceStats = new TravelDistanceStats(this.population, this
					.getNameForOutputFilename(FILENAME_TRAVELDISTANCESTATS), this.createGraphs);
			this.addControlerListener(this.travelDistanceStats);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// load counts, if requested
		if (this.config.counts().getCountsFileName() != null) {
			CountControlerListener ccl = new CountControlerListener(this.config);
			this.addControlerListener(ccl);
			this.counts = ccl.getCounts();
		}
	}

	/**
	 * Creates the path where all iteration-related data should be stored.
	 *
	 * @param iteration
	 */
	private void makeIterationPath(final int iteration) {
		File dir = new File(getIterationPath(iteration));
		if (!dir.mkdir()) {
			if (this.overwriteFiles && dir.exists()) {
				log.info("Iteration directory " + getIterationPath(iteration) + " exists already.");
			} else {
				log.warn("Could not create iteration directory " + getIterationPath(iteration) + ".");
			}
		}
	}

	private void resetRandomNumbers() {
		MatsimRandom.reset(this.config.global().getRandomSeed() + iteration);
		MatsimRandom.getRandom().nextDouble(); // draw one because of strange
		// "not-randomness" is the first
		// draw...
		// Fixme [kn] this should really be ten thousand draws instead of just
		// one
	}

	/*
	 * ===================================================================
	 * protected methods for overwriting
	 * ===================================================================
	 */

	protected void runMobSim() {
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
			final String NUMBER_OF_THREADS = "numberOfThreads";
			String numberOfThreads = this.config.findParam(JDEQ_SIM, NUMBER_OF_THREADS);
			int numOfThreads = 0;

			if (numberOfThreads != null) {
				numOfThreads = Integer.parseInt(numberOfThreads);
			}

			if ((this.config.getModule(JDEQ_SIM) != null) && (numOfThreads > 1)) {
				PJDEQSimulation sim = new PJDEQSimulation(this.network, this.population, this.events,numOfThreads);
				sim.run();
			} else if (this.config.getModule(JDEQ_SIM) != null) {
				JDEQSimulation sim = new JDEQSimulation(this.network, this.population, this.events);
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
			ExternalMobsim sim = new ExternalMobsim(this.population, this.network, this.events);
			sim.setControlerIO(controlerIO);
			sim.setIterationNumber(this.getIteration());
			sim.run();
		}
	}

	/*
	 * ===================================================================
	 * methods for core ControlerListeners
	 * ===================================================================
	 */

	/**
	 * Add a core ControlerListener to the Controler instance
	 *
	 * @param l
	 */
	protected final void addCoreControlerListener(final ControlerListener l) {
		this.controlerListenerManager.addCoreControlerListener(l);
	}

	/*
	 * ===================================================================
	 * methods for ControlerListeners
	 * ===================================================================
	 */

	/**
	 * Add a ControlerListener to the Controler instance
	 *
	 * @param l
	 */
	public final void addControlerListener(final ControlerListener l) {
		this.controlerListenerManager.addControlerListener(l);
	}

	/**
	 * Removes a ControlerListener from the Controler instance
	 *
	 * @param l
	 */
	public final void removeControlerListener(final ControlerListener l) {
		this.controlerListenerManager.removeControlerListener(l);
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
	 * Sets whether graphs showing some analyses should automatically be
	 * generated during the simulation. The generation of graphs usually takes a
	 * small amount of time that does not have any weight in big simulations,
	 * but add a significant overhead in smaller runs or in test cases where the
	 * graphical output is not even requested.
	 *
	 * @param createGraphs
	 *            true if graphs showing analyses' output should be generated.
	 */
	public final void setCreateGraphs(final boolean createGraphs) {
		this.createGraphs = createGraphs;
	}

	/**
	 * @return true if analyses should create graphs showing there results.
	 */
	public final boolean getCreateGraphs() {
		return this.createGraphs;
	}

	/*
	 * ===================================================================
	 * Optional setters that allow to overwrite some default algorithms used
	 * ===================================================================
	 */

	public final void setTravelCostCalculator(final TravelCost travelCostCalculator) {
		this.travelCostCalculator = travelCostCalculator;
	}

	public final TravelCost getTravelCostCalculator() {
		return this.travelCostCalculator;
	}

	public final TravelTime getTravelTimeCalculator() {
		return this.travelTimeCalculator;
	}

	/**
	 * Sets a new {@link org.matsim.core.scoring.ScoringFunctionFactory} to use.
	 * <strong>Note:</strong> This will reset all scores calculated so far!
	 * Only call this before any events are generated in an iteration.
	 *
	 * @param factory
	 *            The new ScoringFunctionFactory to be used.
	 */
	public final void setScoringFunctionFactory(final ScoringFunctionFactory factory) {
		this.scoringFunctionFactory = factory;
	}

	/**
	 * @return the currently used
	 *         {@link org.matsim.core.scoring.ScoringFunctionFactory} for
	 *         scoring plans.
	 */
	public final ScoringFunctionFactory getScoringFunctionFactory() {
		return this.scoringFunctionFactory;
	}

	/**
	 * @return Returns the {@link org.matsim.core.replanning.StrategyManager}
	 *         used for the replanning of plans.
	 */
	public final StrategyManager getStrategyManager() {
		return this.strategyManager;
	}

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return this.leastCostPathCalculatorFactory;
	}

	public void setLeastCostPathCalculatorFactory(final LeastCostPathCalculatorFactory factory) {
		this.leastCostPathCalculatorFactory = factory;
	}

	/*
	 * ===================================================================
	 * Factory methods
	 * ===================================================================
	 */

	/**
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes
	 *         of plans with the default (= the current from the last or current
	 *         iteration) travel costs and travel times. Only to be used by a
	 *         single thread, use multiple instances for multiple threads!
	 */
	public PlanAlgorithm getRoutingAlgorithm() {
		return getRoutingAlgorithm(this.getTravelCostCalculator(), this.getTravelTimeCalculator());
	}

	/**
	 * @param travelCosts
	 *            the travel costs to be used for the routing
	 * @param travelTimes
	 *            the travel times to be used for the routing
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes
	 *         of plans with the specified travelCosts and travelTimes. Only to
	 *         be used by a single thread, use multiple instances for multiple
	 *         threads!
	 */
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {
		if ((this.roadPricing != null)
				&& (RoadPricingScheme.TOLL_TYPE_AREA.equals(this.roadPricing.getRoadPricingScheme().getType()))) {
			return new PlansCalcAreaTollRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, this
					.getLeastCostPathCalculatorFactory(), this.roadPricing.getRoadPricingScheme());
		}
		return new PlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCosts, travelTimes, this
				.getLeastCostPathCalculatorFactory());
	}

	/*
	 * ===================================================================
	 * Informational methods
	 * ===================================================================
	 */
	/**
	 * @deprecated use non static method getIterationNumber
	 */
	@Deprecated
	public static final int getIteration() {
		// I don't really like this to be static..., marcel, 17jan2008
		return iteration;
	}

	public final int getFirstIteration() {
		return this.config.controler().getFirstIteration();
	}

	public final int getLastIteration() {
		return this.config.controler().getLastIteration();
	}

	public final Config getConfig() {
		return this.config;
	}

	@Deprecated
	public final World getWorld() {
		return this.scenarioData.getWorld();
	}

	public final ActivityFacilities getFacilities() {
		return this.scenarioData.getActivityFacilities();
	}

	public final NetworkImpl getNetwork() {
		return this.network;
	}

	public final Population getPopulation() {
		return this.population;
	}

	public final EventsManager getEvents() {
		return this.events;
	}

	public final ScenarioImpl getScenario() {
		return this.scenarioData;
	}

	/**
	 * @return real-world traffic counts if available, <code>null</code> if no
	 *         data is available.
	 */
	public final Counts getCounts() {
		return this.counts;
	}

	public final CalcLinkStats getLinkStats() {
		return this.linkStats;
	}

	public VolumesAnalyzer getVolumes() {
		return this.volumes;
	}

	/**
	 * @return Returns the RoadPricing-ControlerListener, or null if no road
	 *         pricing is simulated.
	 */
	public final RoadPricing getRoadPricing() {
		// TODO integrate roadPricing (Scheme) better in scenario
		return this.roadPricing;
	}

	/**
	 * @return Returns the scoreStats.
	 */
	public ScoreStats getScoreStats() {
		return this.scoreStats;
	}

	/**
	 * Returns the path to a directory where temporary files can be stored.
	 *
	 * @return path to a temp-directory.
	 * @deprecated use non static member ControlerIO to generate the desired String
	 */
	@Deprecated
	public static final String getTempPath() {
		return outputPath + "/tmp";
	}

	/**
	 * Returns the path to the specified iteration directory. The directory path
	 * does not include the trailing '/'.
	 *
	 * @param iter
	 *            the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 * @deprecated use non static member ControlerIO to generate the desired String
	 */
	@Deprecated
	public static final String getIterationPath(final int iter) {
		return outputPath + "/" + Controler.DIRECTORY_ITERS + "/it." + iter;
	}
	/**
	 * Returns the complete filename to access an iteration-file with the given
	 * basename.
	 *
	 * @param filename
	 *            the basename of the file to access
	 * @param iteration
	 *            the iteration to which the path of the file should point
	 * @return complete path and filename to a file in a iteration directory
	 * @deprecated use non static member ControlerIO to generate the desired String
	 */
	@Deprecated
	public static final String getIterationFilename(final String filename, final int iteration) {
		return getIterationPath(iteration) + "/" + iteration + "." + filename;
	}

	/**
	 * @param filename
	 *            the basename of the file to access
	 * @return complete path to filename prefixed with the runId in the
	 *         controler config module (if set) to a file in the
	 *         output-directory
	 * @deprecated use non static member ControlerIO to generate the desired String
	 */
	@Deprecated
	public final String getNameForOutputFilename(final String filename) {
		StringBuilder s = new StringBuilder(outputPath);
		s.append('/');
		if (this.config.controler().getRunId() != null) {
			s.append(this.config.controler().getRunId());
			s.append('.');
		}
		s.append(filename);
		return s.toString();
	}
	/**
	 * @param filename
	 *            the basename of the file to access
	 * @return complete path to filename prefixed with the runId in the
	 *         controler config module (if set) to a file in the
	 *         output-directory  of the current iteration
	 *  @deprecated use non static member ControlerIO to generate the desired String
	 */
	@Deprecated
	public final String getNameForIterationFilename(final String filename){
		StringBuilder s = new StringBuilder(getIterationPath(iteration));
		s.append('/');
		if (this.config.controler().getRunId() != null) {
			s.append(this.config.controler().getRunId());
			s.append('.');
		}
		s.append(iteration);
		s.append(".");
		s.append(filename);
		return s.toString();
	}

	/**
	 * Returns the complete filename to access a file in the output-directory.
	 *
	 * @param filename
	 *            the basename of the file to access
	 * @return complete path and filename to a file in the output-directory
	 * @deprecated use non static member ControlerIO to generate the desired String
	 */
	@Deprecated
	public static final String getOutputFilename(final String filename) {
		return outputPath + "/" + filename;
	}

	public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
		return this.facilityPenalties;
	}

	public void setFacilityPenalties(final TreeMap<Id, FacilityPenalty> facilityPenalties) {
		this.facilityPenalties = facilityPenalties;
	}

	/**
	 * A ControlerListener that controls the most critical parts of the
	 * simulation process. This code could be integrated into the Controler
	 * class directly, but would make it more cumbersome to read. So it is
	 * implemented as a ControlerListener, to keep the structure of the
	 * Controler as simple as possible.
	 */
	protected static class CoreControlerListener implements StartupListener, BeforeMobsimListener, AfterMobsimListener,
			ShutdownListener {

		private final List<EventWriter> eventWriters = new LinkedList<EventWriter>();

		public CoreControlerListener() {
			// empty public constructor for protected class
		}

		@Override
    public void notifyStartup(StartupEvent event) {
		  Config c = event.getControler().getScenario().getConfig();
		  QSimConfigGroup conf = (QSimConfigGroup) c.getModule(QSimConfigGroup.GROUP_NAME);
		  if (conf != null){
		    if (conf.getNumberOfThreads() > 1){
		      event.getControler().setMobsimFactory(new ParallelQSimFactory());
		    }
		    else {
		      event.getControler().setMobsimFactory(new QSimFactory());
		    }
		  }
		  else if (c.getModule("JDEQSim") == null) {
		    log.warn("There might be no configuration for a mobility simulation in the config. The Controler " +
		        " uses the default QueueSimulation that might not have all features implemented.");
		  }
    }

		public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
			Controler controler = event.getControler();
			controler.events.resetHandlers(event.getIteration());
			controler.events.resetCounter();

			if ((controler.writeEventsInterval > 0) && (event.getIteration() % controler.writeEventsInterval == 0)) {
				for (EventsFileFormat format : controler.config.controler().getEventsFileFormats()) {
					switch (format) {
					case txt:
						this.eventWriters.add(new EventWriterTXT(event.getControler().getNameForIterationFilename(FILENAME_EVENTS_TXT)));
						break;
					case xml:
						this.eventWriters.add(new EventWriterXML(event.getControler().getNameForIterationFilename(FILENAME_EVENTS_XML)));
						break;
					default:
						log.warn("Unknown events file format specified: " + format.toString() + ".");
					}
				}
				for (EventWriter writer : this.eventWriters) {
					controler.getEvents().addHandler(writer);
				}
			}

			if (event.getIteration() % 10 == 6) {
				controler.volumes.reset(event.getIteration());
				controler.events.addHandler(controler.volumes);
			}

			// init for event processing of new iteration
			controler.events.initProcessing();
		}

		public void notifyAfterMobsim(final AfterMobsimEvent event) {
			Controler controler = event.getControler();
			int iteration = event.getIteration();

			// prepare for finishing iteration
			controler.events.finishProcessing();

			for (EventWriter writer : this.eventWriters) {
				writer.closeFile();
				event.getControler().getEvents().removeHandler(writer);
			}
			this.eventWriters.clear();

			if (((iteration % 10 == 0) && (iteration > event.getControler().getFirstIteration())) || (iteration % 10 >= 6)) {
				controler.linkStats.addData(controler.volumes, controler.travelTimeCalculator);
			}

			if ((iteration % 10 == 0) && (iteration > event.getControler().getFirstIteration())) {
				controler.events.removeHandler(controler.volumes);
				controler.linkStats.writeFile(event.getControler().getNameForIterationFilename(FILENAME_LINKSTATS));
			}

			if (controler.legTimes != null) {
				controler.legTimes.writeStats(event.getControler().getControlerIO().getIterationFilename(event.getControler().getIteration(), "tripdurations.txt"));
				// - print averages in log
				log.info("[" + iteration + "] average trip duration is: " + (int) controler.legTimes.getAverageTripDuration()
						+ " seconds = " + Time.writeTime(controler.legTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
			}
		}

		public void notifyShutdown(final ShutdownEvent event) {
			for (EventWriter writer : this.eventWriters) {
				writer.closeFile();
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
			final Controler controler = new Controler(args);
			controler.run();
		}
		System.exit(0);
	}

	public List<SimulationListener> getQueueSimulationListener() {
		return this.simulationListener;
	}

	public PlansScoring getPlansScoring() {
		return this.plansScoring;
	}


	public TravelTimeCalculatorFactory getTravelTimeCalculatorFactory() {
		return this.travelTimeCalculatorFactory;
	}


	public void setTravelTimeCalculatorFactory(TravelTimeCalculatorFactory travelTimeCalculatorFactory) {
		this.travelTimeCalculatorFactory = travelTimeCalculatorFactory;
	}


	public TravelCostCalculatorFactory getTravelCostCalculatorFactory() {
		return this.travelCostCalculatorFactory;
	}


	public void setTravelCostCalculatorFactory(TravelCostCalculatorFactory travelCostCalculatorFactory) {
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
	}


	public ControlerIO getControlerIO() {
		return controlerIO;
	}

	public Integer getIterationNumber() {
		return this.getIteration();
	}

  public MobsimFactory getMobsimFactory() {
    return mobsimFactory;
  }

  public void setMobsimFactory(MobsimFactory mobsimFactory) {
    this.mobsimFactory = mobsimFactory;
  }

}
