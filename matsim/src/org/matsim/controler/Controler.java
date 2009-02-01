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

package org.matsim.controler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.TreeMap;

import javax.swing.event.EventListenerList;

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
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.config.MatsimConfigReader;
import org.matsim.controler.corelisteners.LegHistogramListener;
import org.matsim.controler.corelisteners.PlansDumping;
import org.matsim.controler.corelisteners.PlansReplanning;
import org.matsim.controler.corelisteners.PlansScoring;
import org.matsim.controler.corelisteners.RoadPricing;
import org.matsim.controler.events.AfterMobsimEvent;
import org.matsim.controler.events.BeforeMobsimEvent;
import org.matsim.controler.events.IterationEndsEvent;
import org.matsim.controler.events.IterationStartsEvent;
import org.matsim.controler.events.ReplanningEvent;
import org.matsim.controler.events.ScoringEvent;
import org.matsim.controler.events.ShutdownEvent;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.AfterMobsimListener;
import org.matsim.controler.listener.BeforeMobsimListener;
import org.matsim.controler.listener.ControlerListener;
import org.matsim.controler.listener.IterationEndsListener;
import org.matsim.controler.listener.IterationStartsListener;
import org.matsim.controler.listener.ReplanningListener;
import org.matsim.controler.listener.ScoringListener;
import org.matsim.controler.listener.ShutdownListener;
import org.matsim.controler.listener.StartupListener;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.events.parallelEventsHandler.ParallelEvents;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.mobsim.queuesim.ExternalMobsim;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.StrategyManagerConfigLoader;
import org.matsim.roadpricing.PlansCalcAreaTollRoute;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.io.IOUtils;
import org.matsim.utils.logging.CollectLogMessagesAppender;
import org.matsim.utils.misc.Time;
import org.matsim.world.World;
import org.matsim.world.WorldWriter;

/**
 * The Controler is responsible for complete simulation runs, including
 * the initialization of all required data, running the iterations and
 * the replanning, analyses, etc.
 *
 * @author mrieser
 */
public class Controler {

	private static final String DIRECTORY_ITERS = "ITERS";
	/*package*/ static final String FILENAME_EVENTS = "events.txt.gz";
	public static final String FILENAME_LINKSTATS = "linkstats.txt";
	public static final String FILENAME_SCORESTATS = "scorestats.txt";
	public static final String FILENAME_TRAVELDISTANCESTATS="traveldistancestats.txt";

	private enum ControlerState {Init, Running, Shutdown, Finished}
	private ControlerState state = ControlerState.Init;

	private static String outputPath = null;

	public static final Layout DEFAULTLOG4JLAYOUT = new PatternLayout("%d{ISO8601} %5p %C{1}:%L %m%n");

	private int traveltimeBinSize = 15*60; // use default of 15mins
	private boolean overwriteFiles = false;
	private static int iteration = -1;

	/** The swing event listener list to manage ControlerListeners efficiently. First list manages core listeners
	 * which are called first when a ControlerEvent is thrown. I.e. this list contains the listeners that are
	 * always running in a predefined order to ensure correctness.
	 * The second list manages the other listeners, which can be added by calling addControlerListener(...).
	 * A normal ControlerListener is not allowed to depend on the execution of other ControlerListeners.
	 */
	private final EventListenerList coreListenerList = new EventListenerList();
	private final EventListenerList listenerList = new EventListenerList();

	/** The Config instance the Controler uses. */
	protected final Config config;
	private final String configFileName;
	private final String dtdFileName;

	protected Events events = null;
	protected NetworkLayer network = null;
	protected Population population = null;
	private Counts counts = null;
	private final NetworkFactory networkFactory = new NetworkFactory();

	protected TravelTimeCalculator travelTimeCalculator = null;
	protected TravelCost travelCostCalculator = null;
	protected ScoringFunctionFactory scoringFunctionFactory = null;
	protected StrategyManager strategyManager = null;

	/** Stores data commonly used by all router instances. */
	private PreProcessLandmarks commonRoutingData = null;

	/**
	 * Defines in which iterations the events should be written. <tt>1</tt> is in every iteration,
	 * <tt>2</tt> in every second, <tt>10</tt> in every 10th, and so forth. <tt>0</tt> disables the writing
	 * of events completely.
	 */
	/*package*/ int writeEventsInterval = 1;

	/* default analyses */
	/*package*/ CalcLinkStats linkStats = null;
	/*package*/ CalcLegTimes legTimes = null;
	/*package*/ VolumesAnalyzer volumes = null;

	private boolean createGraphs = true;

	private String externalMobsim = null;

	public final IterationStopWatch stopwatch = new IterationStopWatch();
	private ScenarioData scenarioData = null;
	private RoadPricing roadPricing = null;
	private ScoreStats scoreStats = null;
	private TravelDistanceStats travelDistanceStats = null;
	/**
	 * This variable is used to store the log4j output before it can be written to
	 * a file. This is needed to set the output directory before logging.
	 */
	private CollectLogMessagesAppender collectLogMessagesAppender = null;

	private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

	private static final Logger log = Logger.getLogger(Controler.class);

	private final Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			shutdown(true);
		}
	};

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
	 * @param args The arguments to initialize the controler with. <code>args[0]</code> is expected to
	 * 		contain the path to a configuration file, <code>args[1]</code>, if set, is expected to contain
	 * 		the path to a local copy of the DTD file used in the configuration file.
	 */
	public Controler(final String[] args) {
		this(args.length > 0 ? args[0] : null,
				args.length > 1 ? args[1] : null,
				null);
	}

	public Controler(final String configFileName) {
		this(configFileName, null, null);
	}

	public Controler(final String configFileName, final String dtdFileName) {
		this(configFileName, dtdFileName, null);
	}

	public Controler(final Config config) {
		this(null, null, config);
	}

	public Controler(final Config config, final NetworkLayer network, final Population population) {
		this(null, null, config);
		this.network = network;
		this.population = population;
	}

	private Controler(final String configFileName, final String dtdFileName, final Config config) {
		super();
		//catch logs before doing something
		this.collectLogMessagesAppender = new CollectLogMessagesAppender();
		Logger.getRootLogger().addAppender(this.collectLogMessagesAppender);
		Gbl.printSystemInfo();
		Gbl.printBuildInfo();
		this.configFileName = configFileName;
		this.dtdFileName = dtdFileName;
		if (configFileName == null) {
			if (config == null) {
				throw new IllegalArgumentException("Either the config or the filename of a configfile must be set to initialize the Controler.");
			}
			this.config = config;
		} else {
			this.config = new Config();
			this.config.addCoreModules();
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
			doIterations();
			shutdown(false);
		} else {
			log.error("Controler in wrong state to call 'run()'. Expected state: <Init> but was <" + this.state + ">");
		}
	}

	private void init() {
		loadConfig();
		setupOutputDir();
		initEvents();
		initLogging();
		loadData();
		setup();
		loadCoreListeners();
		loadControlerListeners();
		fireControlerStartupEvent();
	}

	/**
	 * select if single cpu handler to use or parallel
	 */
	private void initEvents() {
		final String PARALLEL_EVENT_HANDLING="parallelEventHandling";
		final String NUMBER_OF_THREADS="numberOfThreads";
		final String ESTIMATED_NUMBER_OF_EVENTS="estimatedNumberOfEvents";
		String numberOfThreads = Gbl.getConfig().findParam(PARALLEL_EVENT_HANDLING, NUMBER_OF_THREADS);
		String estimatedNumberOfEvents = Gbl.getConfig().findParam(PARALLEL_EVENT_HANDLING, ESTIMATED_NUMBER_OF_EVENTS);

		if (numberOfThreads!=null){
			int numOfThreads=Integer.parseInt(numberOfThreads);
			// the user wants to user parallel events handling
			if (estimatedNumberOfEvents!=null){
				int estNumberOfEvents=Integer.parseInt(estimatedNumberOfEvents);
				this.events=new ParallelEvents(numOfThreads,estNumberOfEvents);
			} else {
				this.events=new ParallelEvents(numOfThreads);
			}
		} else {
			this.events=new Events();
		}
	}

	private void doIterations() {
		int firstIteration = this.config.controler().getFirstIteration();
		int lastIteration = this.config.controler().getLastIteration();
		this.state = ControlerState.Running;
		String divider = "###################################################" ;
		String marker = "### " ;

		for (iteration = firstIteration; (iteration <= lastIteration) && (this.state == ControlerState.Running); iteration++) {
			log.info(divider);
			log.info(marker + "ITERATION " + iteration + " BEGINS");
			this.stopwatch.setCurrentIteration(Controler.iteration);
			this.stopwatch.beginOperation("iteration");
			makeIterationPath(iteration);
			resetRandomNumbers();

			fireControlerIterationStartsEvent(iteration);
			if (iteration > firstIteration) {
				this.stopwatch.beginOperation("replanning");
				fireControlerReplanningEvent(iteration);
				this.stopwatch.endOperation("replanning");
			}
			fireControlerBeforeMobsimEvent(iteration);
			this.stopwatch.beginOperation("mobsim");
			resetRandomNumbers();
			runMobSim();
			this.stopwatch.endOperation("mobsim");
			fireControlerAfterMobsimEvent(iteration);
			fireControlerScoringEvent(iteration);
			fireControlerIterationEndsEvent(iteration);
			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(getOutputFilename("stopwatch.txt"));
			log.info(marker + "ITERATION " + iteration + " ENDS") ;
			log.info(divider) ;
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
			fireControlerShutdownEvent(unexpected);

			// dump plans
			new PopulationWriter(this.population, getOutputFilename("output_plans.xml.gz"),
					this.config.plans().getOutputVersion()).write();
			//dump network
			new NetworkWriter(this.network, getOutputFilename("output_network.xml.gz")).write();
			// dump world
			new WorldWriter(Gbl.getWorld(), getOutputFilename("output_world.xml.gz")).write();
			// dump config
			new ConfigWriter(this.config, getOutputFilename("output_config.xml.gz")).write();
			// dump facilities
			Facilities facilities = (Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE);
			if (facilities != null) {
				new FacilitiesWriter(facilities, getOutputFilename("output_facilities.xml.gz")).write();
			}

			Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
			if (unexpected) {
				log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
			} else {
				log.info("S H U T D O W N   ---   regular shutdown completed.");
			}
			IOUtils.closeOutputDirLogging();
		}
	}

	/** Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, and after the
	 * scenario data (network, population) is read.
	 */
	protected void setup() {
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;
		if (this.travelTimeCalculator == null) {
			this.travelTimeCalculator = this.config.controler().getTravelTimeCalculator(this.network, (int)endTime);
		}
		if (this.travelCostCalculator == null) {
			this.travelCostCalculator = new TravelTimeDistanceCostCalculator(this.travelTimeCalculator);
		}
		this.events.addHandler(this.travelTimeCalculator);

		/* TODO [MR] linkStats uses ttcalc and volumes, but ttcalc has 15min-steps,
		 * while volumes uses 60min-steps! It works a.t.m., but the traveltimes
		 * in linkStats are the avg. traveltimes between xx.00 and xx.15, and not
		 * between xx.00 and xx.59
		 */
		this.linkStats = new CalcLinkStats(this.network);
		this.volumes = new VolumesAnalyzer(3600, 24*3600-1, this.network);
		this.legTimes = new CalcLegTimes(this.population);
		this.events.addHandler(this.legTimes);

		this.externalMobsim = this.config.simulation().getExternalExe();

		if (this.scoringFunctionFactory == null) {
			this.scoringFunctionFactory = loadScoringFunctionFactory();
		}

		this.strategyManager = loadStrategyManager();
	}

	/* ===================================================================
	 * private methods
	 * =================================================================== */

	/**
	 * Initializes log4j to write log output to files in output directory.
	 */
	private void initLogging() {
		Logger.getRootLogger().removeAppender(this.collectLogMessagesAppender);
		try {
			IOUtils.initOutputDirLogging(this.config.controler().getOutputDirectory(), this.collectLogMessagesAppender.getLogEvents());
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
		log.info("Complete config dump:");
		StringWriter writer = new StringWriter();
		ConfigWriter configwriter = new ConfigWriter(this.config, new PrintWriter(writer));
		configwriter.write();
		log.info("\n\n" + writer.getBuffer().toString());
		log.info("Complete config dump done.");

		if (this.config.network().isTimeVariantNetwork()) {
			log.info("setting TimeVariantLinkImpl as link prototype in NetworkFactory.");
			this.networkFactory.setLinkPrototype(TimeVariantLinkImpl.class);
		}
	}

	private final void setupOutputDir() {
		outputPath = this.config.controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length()-1);
		}

		// make the tmp directory
		File outputDir = new File(outputPath);
		if (outputDir.exists()) {
			if (outputDir.isFile()) {
				throw new RuntimeException("Cannot create output directory. " + outputPath + " is a file and cannot be replaced by a directory.");
			}
			if (outputDir.list().length > 0) {
				if (this.overwriteFiles) {
					log.warn("###########################################################");
					log.warn("### THE CONTROLER WILL OVERWRITE FILES IN:");
					log.warn("### " + outputPath);
					log.warn("###########################################################");
				} else {
					// the directory is not empty, we do not overwrite any files!
					throw new RuntimeException("The output directory " + outputPath + " exists already but has files in it! Please delete its content or the directory and start again. We will not delete or overwrite any existing files.");
				}
			}
		} else {
			if (!outputDir.mkdirs()) {
				throw new RuntimeException("The output directory path " + outputPath + " could not be created. Check pathname and permissions!");
			}
		}

		File tmpDir = new File(getTempPath());
		if (!tmpDir.mkdir() && !tmpDir.exists()) {
			throw new RuntimeException("The tmp directory " + getTempPath() + " could not be created.");
		}
		File itersDir = new File(outputPath + "/" + DIRECTORY_ITERS);
		if (!itersDir.mkdir() && !itersDir.exists()) {
			throw new RuntimeException("The iterations directory " + (outputPath + "/" + DIRECTORY_ITERS) + " could not be created.");
		}
	}

	/** Load all the required data. Currently, this only calls {@link #loadNetwork()} and {@link #loadPopulation()},
	 * if this data was not given in the Constructor.
	 * <br>
	 * <strong>It is highly recommended NOT to overwrite this method!</strong> This method should be private, but is
	 * only protected at the moment because of backward-compatibility with the old Controler class. In the future,
	 * additional data should be loaded by implementing a {@link org.matsim.controler.listener.StartupListener}.
	 */
	protected void loadData() {
		if (this.network == null) {
			this.scenarioData = new ScenarioData(this.config, this.networkFactory);
			this.network = loadNetwork();
			this.population = loadPopulation();
		}
	}

	/**
	 * Loads the network for the simulation.  In most cases, this should be an instance of {@link QueueNetwork}
	 * for the standard QueueSimulation.
	 * <br>
	 * <strong>It is highly recommended NOT to overwrite this method!</strong> This method should be private, but is
	 * only protected at the moment because of backward-compatibility with the old Controler class. In general,
	 * it is recommended to pass a custom network and population using the special
	 * {@link #Controler(Config, QueueNetwork, Population) Constructor}.
	 *
	 * @return The network to be used for the simulation.
	 */
	protected NetworkLayer loadNetwork() {
		return this.scenarioData.getNetwork();
	}

	/**
	 * Loads the population for the simulation.
	 * <br>
	 * <strong>It is highly recommended NOT to overwrite this method!</strong> This method should be private, but is
	 * only protected at the moment because of backward-compatibility with the old Controler class. In general,
	 * it is recommended to pass a custom network and population using the special
	 * {@link #Controler(Config, QueueNetwork, Population) Constructor}.
	 *
	 * @return The population to be used for the simulation.
	 */
	protected Population loadPopulation() {
		return this.scenarioData.getPopulation();
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		StrategyManagerConfigLoader.load(this, this.config, manager);
		return manager;
	}

	/**
	 * Loads the {@link ScoringFunctionFactory} to be used for plans-scoring. This
	 * method will only be called if the user has not yet manually set a custom scoring
	 * function with {@link #setScoringFunctionFactory(ScoringFunctionFactory)}.
	 *
	 * @return The ScoringFunctionFactory to be used for plans-scoring.
	 */
	protected ScoringFunctionFactory loadScoringFunctionFactory() {
		return new CharyparNagelScoringFunctionFactory();
//	return new CharyparNagelOpenTimesScoringFunctionFactory();
	}

	/** Loads a default set of {@link org.matsim.controler.listener ControlerListener} to provide basic functionality.
	 * <b>Note:</b> Be very careful if you overwrite this method! The order how the listeners are added is very important.
	 * Check the comments in the source file before overwriting this method!
	 */
	protected void loadCoreListeners() {

		/* The order how the listeners are added is very important!
		 * As dependencies between different listeners exist or listeners
		 * may read and write to common variables, the order is important.
		 * Example: The RoadPricing-Listener modifies the scoringFunctionFactory,
		 * which in turn is used by the PlansScoring-Listener.
		 * Note that the execution order is contrary to the order the listeners are added to the list.
		 */

		this.addCoreControlerListener(new CoreControlerListener());

		// the default handling of plans
		this.addCoreControlerListener(new PlansScoring());


		// load road pricing, if requested
		if (this.config.roadpricing().getTollLinksFile() != null) {
			this.roadPricing = new RoadPricing();
			this.addCoreControlerListener(this.roadPricing);
		}

		this.addCoreControlerListener(new PlansReplanning());
		this.addCoreControlerListener(new PlansDumping());
	}

	/**
	 * Loads the default set of {@link org.matsim.controler.listener ControlerListener} to provide some more basic functionality.
	 * Unlike the core ControlerListeners the order in which the listeners of this method are added must not affect
	 * the correctness of the code.
	 */
	protected void loadControlerListeners() {
		// optional: LegHistogram
		this.addControlerListener(new LegHistogramListener(this.events, this.createGraphs));

		// optional: score stats
		try {
			this.scoreStats = new ScoreStats(this.population, getOutputFilename(FILENAME_SCORESTATS), this.createGraphs);
			this.addControlerListener(this.scoreStats);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// optional: travel distance stats
		try {
			this.travelDistanceStats = new TravelDistanceStats(this.population, getOutputFilename(FILENAME_TRAVELDISTANCESTATS), this.createGraphs);
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
		MatsimRandom.random.nextDouble(); // draw one because of strange "not-randomness" is the first draw...
		// Fixme [kn] this should really be ten thousand draws instead of just one
	}

	/* ===================================================================
	 * protected methods for overwriting
	 * =================================================================== */

	protected void runMobSim() {
		if (this.externalMobsim == null) {
			QueueSimulation sim = new QueueSimulation(this.network, this.population, this.events);
			sim.run();
		} else {
			ExternalMobsim sim = new ExternalMobsim(this.population, this.events);
			sim.run();
		}
	}

	/* ===================================================================
	 * methods for core ControlerListeners
	 * =================================================================== */

	/**
	 * Add a core ControlerListener to the Controler instance
	 *
	 * @param l
	 */
	@SuppressWarnings("unchecked")
	protected final void addCoreControlerListener(final ControlerListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (ControlerListener.class.isAssignableFrom(interfaces[i])) {
				this.coreListenerList.add(interfaces[i], l);
			}
		}
	}

	/* ===================================================================
	 * methods for ControlerListeners
	 * =================================================================== */

	/**
	 * Add a ControlerListener to the Controler instance
	 *
	 * @param l
	 */
	@SuppressWarnings("unchecked")
	public final void addControlerListener(final ControlerListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (ControlerListener.class.isAssignableFrom(interfaces[i])) {
				this.listenerList.add(interfaces[i], l);
			}
		}
	}

	/**
	 * Removes a ControlerListener from the Controler instance
	 *
	 * @param l
	 */
	@SuppressWarnings("unchecked")
	public final void removeControlerListener(final ControlerListener l) {
		Class[] interfaces = l.getClass().getInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			if (ControlerListener.class.isAssignableFrom(interfaces[i])) {
				this.listenerList.remove(interfaces[i], l);
			}
		}
	}

	/**
	 * Notifies all ControlerListeners
	 */
	private void fireControlerStartupEvent() {
		StartupEvent event = new StartupEvent(this);
		StartupListener[] listener = this.coreListenerList.getListeners(StartupListener.class);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyStartup(event);
    }
    listener = this.listenerList.getListeners(StartupListener.class);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyStartup(event);
    }
	}

	/**
	 * Notifies all ControlerListeners
	 * @param unexpected Whether the shutdown is unexpected or not.
	 */
	private void fireControlerShutdownEvent(final boolean unexpected) {
		ShutdownEvent event = new ShutdownEvent(this, unexpected);
    ShutdownListener[] listener = this.coreListenerList.getListeners(ShutdownListener.class);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyShutdown(event);
    }
    listener = this.listenerList.getListeners(ShutdownListener.class);
    for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyShutdown(event);
    }
	}

	/**
	 * Notifies all ControlerSetupIterationStartsListeners
	 * @param iteration
	 */
	private void fireControlerIterationStartsEvent(final int iteration) {
		IterationStartsEvent event = new IterationStartsEvent(this, iteration);
		IterationStartsListener[] listener = this.coreListenerList.getListeners(IterationStartsListener.class);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyIterationStarts(event);
    }
		listener = this.listenerList.getListeners(IterationStartsListener.class);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyIterationStarts(event);
    }
	}

	/**
	 * Notifies all ControlerIterationEndsListeners
	 *
	 * @param iteration
	 */
	private void fireControlerIterationEndsEvent(final int iteration) {
		IterationEndsEvent event = new IterationEndsEvent(this, iteration);
		IterationEndsListener[] listener = this.coreListenerList.getListeners(IterationEndsListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyIterationEnds(event);
		}
		listener = this.listenerList.getListeners(IterationEndsListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyIterationEnds(event);
		}
	}

	/**
	 * Notifies all ControlerScoringListeners
	 *
	 * @param iteration
	 */
	private void fireControlerScoringEvent(final int iteration) {
		ScoringEvent event = new ScoringEvent(this, iteration);
		ScoringListener[] listener = this.coreListenerList.getListeners(ScoringListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyScoring(event);
		}
		listener = this.listenerList.getListeners(ScoringListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyScoring(event);
		}
	}

	/**
	 * Notifies all ControlerReplanningListeners
	 *
	 * @param iteration
	 */
	private void fireControlerReplanningEvent(final int iteration) {
		ReplanningEvent event = new ReplanningEvent(this, iteration);
		ReplanningListener[] listener = this.coreListenerList.getListeners(ReplanningListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyReplanning(event);
		}
		listener = this.listenerList.getListeners(ReplanningListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyReplanning(event);
		}
	}

	/**
	 * Notifies all ControlerBeforeMobsimListeners
	 *
	 * @param iteration
	 */
	private void fireControlerBeforeMobsimEvent(final int iteration) {
		BeforeMobsimEvent event = new BeforeMobsimEvent(this, iteration);
		BeforeMobsimListener[] listener = this.coreListenerList.getListeners(BeforeMobsimListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyBeforeMobsim(event);
		}
		listener = this.listenerList.getListeners(BeforeMobsimListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyBeforeMobsim(event);
		}
	}

	/**
	 * Notifies all ControlerAfterMobsimListeners
	 *
	 * @param iteration
	 */
	private void fireControlerAfterMobsimEvent(final int iteration) {
		AfterMobsimEvent event = new AfterMobsimEvent(this, iteration);
		AfterMobsimListener[] listener = this.coreListenerList.getListeners(AfterMobsimListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyAfterMobsim(event);
		}
		listener = this.listenerList.getListeners(AfterMobsimListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyAfterMobsim(event);
		}
	}

	/* ===================================================================
	 * Options
	 * =================================================================== */

	/**
	 * Sets whether the Controler is allowed to overwrite files in the output directory or not. <br>
	 * When starting, the Controler can check that the output directory is empty or does not yet exist,
	 * so no files will be overwritten (default setting). While useful in a productive environment,
	 * this security feature may be interfering in test cases or while debugging. <br>
	 * <strong>Use this setting with caution, as it can result in data loss!</strong>
	 *
	 * @param overwrite
	 *          whether files and directories should be overwritten (true) or not (false)
	 */
	public final void setOverwriteFiles(final boolean overwrite) {
		this.overwriteFiles = overwrite;
	}

	/**
	 * Returns whether the Controler is currently allowed to overwrite files in the output directory.
	 *
	 * @return true if the Controler is currently allowed to overwrite files in the output directory,
	 * 				 false if not.
	 */
	public final boolean getOverwriteFiles() {
		return this.overwriteFiles;
	}

	/**
	 * Sets the size of the time-window over which the travel times are accumulated and averaged.
	 * Changes to this parameter have no effect after {@link #run()} is called. <br>
	 * Note that smaller values for the binSize increase memory consumption to store the travel times.
	 *
	 * @param binSize The size of the time-window in seconds.
	 */
	public final void setTraveltimeBinSize(final int binSize) {
		this.traveltimeBinSize = binSize;
	}

	/**
	 * Returns the size of the time-window used to accumulate and average travel times.
	 *
	 * @return The size of the time-window in seconds.
	 */
	public final int getTraveltimeBinSize() {
		return this.traveltimeBinSize;
	}

	/**
	 * Sets in which iterations events should be written to a file.
	 * If set to <tt>1</tt>, the events will be written in every iteration.
	 * If set to <tt>2</tt>, the events are written every second iteration.
	 * If set to <tt>10</tt>, the events are written in every 10th iteration.
	 * To disable writing of events completely, set the interval to <tt>0</tt> (zero).
	 *
	 * @param interval in which iterations events should be written
	 */
	public final void setWriteEventsInterval(final int interval) {
		this.writeEventsInterval = interval;
	}

	public final int getWriteEventsInterval() {
		return this.writeEventsInterval;
	}

	/**
	 * Sets whether graphs showing some analyses should automatically be
	 * generated during the simulation. The generation of graphs usually
	 * takes a small amount of time that does not have any weight in big
	 * simulations, but add a significant overhead in smaller runs or in
	 * test cases where the graphical output is not even requested.
	 *
	 * @param createGraphs true if graphs showing analyses' output should be generated.
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

	/* ===================================================================
	 * Optional setters that allow to overwrite some default algorithms used
	 * =================================================================== */

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
	 * Sets a new {@link org.matsim.scoring.ScoringFunctionFactory} to use. <strong>Note:</strong> This will
	 * reset all scores calculated so far! Only call this before any events are generated in an iteration.
	 *
	 * @param factory The new ScoringFunctionFactory to be used.
	 */
	public final void setScoringFunctionFactory(final ScoringFunctionFactory factory) {
		this.scoringFunctionFactory = factory;
	}

	/**
	 * @return the currently used {@link org.matsim.scoring.ScoringFunctionFactory}
	 * for scoring plans.
	 */
	public final ScoringFunctionFactory getScoringFunctionFactory() {
		return this.scoringFunctionFactory;
	}

	/**
	 * @return Returns the {@link org.matsim.replanning.StrategyManager} used for the replanning of plans.
	 */
	public final StrategyManager getStrategyManager() {
		return this.strategyManager;
	}

	/* ===================================================================
	 * Factory methods
	 * =================================================================== */

	/**
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes of plans with the default
	 * (= the current from the last or current iteration) travel costs and travel times. Only to be used by
	 * a single thread, use multiple instances for multiple threads!
	 */
	public PlanAlgorithm getRoutingAlgorithm() {
		return getRoutingAlgorithm(this.getTravelCostCalculator(), this.getTravelTimeCalculator());
	}

	/**
	 * @param travelCosts the travel costs to be used for the routing
	 * @param travelTimes the travel times to be used for the routing
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes of plans with the specified
	 * travelCosts and travelTimes. Only to be used by a single thread, use multiple instances for multiple threads!
	 */
	public PlanAlgorithm getRoutingAlgorithm(final TravelCost travelCosts, final TravelTime travelTimes) {
		synchronized (this) {
			if (this.commonRoutingData == null) {
				this.commonRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
				this.commonRoutingData.run(this.network);
			}
		}

		if ((this.roadPricing != null) && (RoadPricingScheme.TOLL_TYPE_AREA.equals(this.roadPricing.getRoadPricingScheme().getType()))) {
			return new PlansCalcAreaTollRoute(this.network, this.commonRoutingData, travelCosts, travelTimes, this.roadPricing.getRoadPricingScheme());
		}
		return new PlansCalcRouteLandmarks(this.network, this.commonRoutingData, travelCosts, travelTimes);
	}

	/* ===================================================================
	 * Informational methods
	 * =================================================================== */

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

	public final World getWorld() {
		if (this.scenarioData == null) {
			return null;
		}
		return this.scenarioData.getWorld();
	}

	public final Facilities getFacilities() {
		if (this.scenarioData == null) {
			return null;
		}
		return this.scenarioData.getFacilities();
	}

	public final NetworkLayer getNetwork() {
		return this.network;
	}

	public final Population getPopulation() {
		return this.population;
	}

	public final Events getEvents() {
		return this.events;
	}

	/**
	 * @return real-world traffic counts if available, <code>null</code> if no data is available.
	 */
	public final Counts getCounts() {
		return this.counts;
	}

	protected NetworkFactory getNetworkFactory() {
		return this.networkFactory;
	}

	public final CalcLinkStats getLinkStats() {
		return this.linkStats;
	}

	public final TravelTime getLinkTravelTimes() {
		return this.travelTimeCalculator;
	}

	/**
	 * @return Returns the RoadPricing-ControlerListener, or null if no road
	 *         pricing is simulated.
	 */
	public final RoadPricing getRoadPricing() {
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
	 */
	public static final String getTempPath() {
		return outputPath + "/tmp";
	}

	/**
	 * Returns the path to the specified iteration directory. The directory path does not include the trailing '/'.
	 *
	 * @param iteration the iteration the path to should be returned
	 * @return path to the specified iteration directory
	 */
	public static final String getIterationPath(final int iteration) {
		return outputPath + "/" + Controler.DIRECTORY_ITERS + "/it." + iteration;
	}

	/**
	 * Returns the path of the current iteration directory. The directory path does not include the trailing '/'.
	 *
	 * @return path to the current iteration directory
	 */
	public static final String getIterationPath() {
		return getIterationPath(iteration);
	}

	/**
	 * Returns the complete filename to access an iteration-file with the given basename.
	 *
	 * @param filename the basename of the file to access
	 * @return complete path and filename to a file in a iteration directory
	 */
	public static final String getIterationFilename(final String filename) {
		if (iteration == -1) {
			return filename;
		}
		return getIterationPath(iteration) + "/" + iteration + "." + filename;
	}

	/**
	 * Returns the complete filename to access an iteration-file with the given basename.
	 *
	 * @param filename the basename of the file to access
	 * @param iteration the iteration to which the path of the file should point
	 * @return complete path and filename to a file in a iteration directory
	 */
	public static final String getIterationFilename(final String filename, final int iteration) {
		return getIterationPath(iteration) + "/" + iteration + "." + filename;
	}

	/**
	 * Returns the complete filename to access a file in the output-directory.
	 *
	 * @param filename the basename of the file to access
	 * @return complete path and filename to a file in the output-directory
	 */
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
	protected static class CoreControlerListener implements StartupListener, IterationStartsListener, BeforeMobsimListener, AfterMobsimListener, ShutdownListener {

		private EventWriterTXT eventWriter = null;

		public CoreControlerListener() {
			// empty public constructor for protected class
		}

		public void notifyStartup(final StartupEvent event) {
			final Controler c = event.getControler();
			// make sure all routes are calculated.
			ParallelPersonAlgorithmRunner.run(c.getPopulation(), c.config.global().getNumberOfThreads(), new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
				public AbstractPersonAlgorithm getPersonAlgorithm() {
					return new PersonPrepareForSim(c.getRoutingAlgorithm(), c.getNetwork());
				}
			});
		}

		public void notifyIterationStarts(final IterationStartsEvent event) {
			Controler controler = event.getControler();
			controler.events.resetHandlers(event.getIteration());
			controler.events.resetCounter();
		}

		public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
			Controler controler = event.getControler();
			controler.travelTimeCalculator.resetTravelTimes();

			if ((controler.writeEventsInterval > 0) && (event.getIteration() % controler.writeEventsInterval == 0)) {
				this.eventWriter = new EventWriterTXT(Controler.getIterationFilename(FILENAME_EVENTS));
				controler.getEvents().addHandler(this.eventWriter);
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

			if (this.eventWriter != null) {
				this.eventWriter.closeFile();
				event.getControler().getEvents().removeHandler(this.eventWriter);
				this.eventWriter = null;
			}

			if (((iteration % 10 == 0) && (iteration > event.getControler().getFirstIteration())) || (iteration % 10 >= 6)) {
				controler.linkStats.addData(controler.volumes, controler.travelTimeCalculator);
			}

			if ((iteration % 10 == 0) && (iteration > event.getControler().getFirstIteration())) {
				controler.events.removeHandler(controler.volumes);
				controler.linkStats.writeFile(getIterationFilename(FILENAME_LINKSTATS));
			}

			if (controler.legTimes != null) {
				controler.legTimes.writeStats(getIterationFilename("tripdurations.txt"));
				// - print average in log
				log.info("[" + iteration + "] average trip duration is: "
						+ (int)controler.legTimes.getAverageTripDuration() + " seconds = "
						+ Time.writeTime(controler.legTimes.getAverageTripDuration(), Time.TIMEFORMAT_HHMMSS));
			}
		}

		public void notifyShutdown(final ShutdownEvent event) {
			if (this.eventWriter != null) {
				this.eventWriter.closeFile();
			}
		}

	}

	/* ===================================================================
	 * main
	 * =================================================================== */

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

}
