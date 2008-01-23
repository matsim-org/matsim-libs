/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
import java.net.URL;

import javax.swing.event.EventListenerList;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
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
import org.matsim.events.Events;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.events.handler.EventHandlerI;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.gbl.Gbl;
import org.matsim.mobsim.ExternalMobsim;
import org.matsim.mobsim.QueueNetworkLayer;
import org.matsim.mobsim.QueueSimulation;
import org.matsim.mobsim.Simulation;
import org.matsim.mobsim.SimulationTimer;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriter;
import org.matsim.planomat.PlanomatConfig;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.MyRecentEventsBasedEstimator;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.trafficmonitoring.AbstractTravelTimeCalculator;
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
	private static final String FILENAME_EVENTS = "events.txt.gz";
	public static final String FILENAME_LINKSTATS = "linkstats.txt";
	public static final String FILENAME_SCORESTATS = "scorestats.txt";

	private enum ControlerState {Init, Running, Shutdown, Finished};
	private ControlerState state = ControlerState.Init;

	private static String outputPath = null;

	private int traveltimeBinSize = 15*60; // use default of 15mins
	private boolean overwriteFiles = false;
	private static int iteration = -1;

	/** The swing event listener list to manage ControlerListeners efficiently. */
	private final EventListenerList listenerList = new EventListenerList();

	/** The Config instance the Controler uses. */
	protected final Config config;
	private final String configFileName;
	private final String dtdFileName;

	protected final Events events = new Events();
	protected NetworkLayer network = null;
	protected Plans population = null;

	protected AbstractTravelTimeCalculator travelTimeCalculator = null;
	protected TravelCostI travelCostCalculator = null;
	protected LegTravelTimeEstimator legTravelTimeEstimator = null;
	protected ScoringFunctionFactory scoringFunctionFactory = null;

	/*default*/ EventWriterTXT eventWriter = null;
	/*default*/ boolean writeEvents = true;
	private boolean eventWriterAdded = false;

	/* default analyses */
	/*default*/ CalcLinkStats linkStats = null;
	/*default*/ CalcLegTimes legTimes = null;
	/*default*/ VolumesAnalyzer volumes = null;

	private boolean createGraphs = true;

	private String externalMobsim = null;

	public final IterationStopWatch stopwatch = new IterationStopWatch();
	private ScenarioData scenarioData = null;

	private static final Logger log = Logger.getLogger(Controler.class);

	/** initializes Log4J */
	static {
		final String logProperties = "log4j.xml";
		URL url = Loader.getResource(logProperties);
		if (url != null) {
			PropertyConfigurator.configure(url);
		} else {
			Logger root = Logger.getRootLogger();
			root.setLevel(Level.INFO);
			PatternLayout layout = new PatternLayout("%d{ISO8601} %5p %C{1}:%L %m%n");
			ConsoleAppender consoleAppender = new ConsoleAppender(layout, "System.out");
			consoleAppender.setName("A1");
			root.addAppender(consoleAppender);
			consoleAppender.setLayout(layout);
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
	 * @param args The arguments to initialize the controler with. <code>args[0]</code> is exptected to
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

	public Controler(final Config config, final QueueNetworkLayer network, final Plans population) {
		this(null, null, config);
		this.network = network;
		this.population = population;
	}

	private Controler(final String configFileName, final String dtdFileName, final Config config) {
		super();
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

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown(true);
			}
		});
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
		loadData();
		setup();
		loadCoreListeners();
		fireControlerStartupEvent();
	}

	private void doIterations() {
		int firstIteration = this.config.controler().getFirstIteration();
		int lastIteration = this.config.controler().getLastIteration();
		this.state = ControlerState.Running;

		for (iteration = firstIteration; iteration <= lastIteration && this.state == ControlerState.Running; iteration++) {
			log.info("ITERATION " + iteration + " BEGINS");
			this.stopwatch.setCurrentIteration(Controler.iteration);
			this.stopwatch.beginOperation("iteration");
			makeIterationPath(iteration);
			resetRandomNumbers(iteration);

			fireControlerIterationStartsEvent(iteration);
			if (iteration > firstIteration) {
				this.stopwatch.beginOperation("replanning");
				fireControlerReplanningEvent(iteration);
				this.stopwatch.endOperation("replanning");
			}
			fireControlerBeforeMobsimEvent(iteration);
			this.stopwatch.beginOperation("mobsim");
			resetRandomNumbers(iteration);
			runMobSim();
			this.stopwatch.endOperation("mobsim");
			fireControlerAfterMobsimEvent(iteration);
			fireControlerScoringEvent(iteration);
			fireControlerIterationEndsEvent(iteration);
			this.stopwatch.endOperation("iteration");
			this.stopwatch.write(getOutputFilename("stopwatch.txt"));
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

			// closing events file
			if (this.eventWriter != null) {
				this.eventWriter.closefile();
			}
			// dump plans
			new PlansWriter(this.population, getOutputFilename("output_plans.xml.gz"),
					this.config.plans().getOutputVersion()).write();

			// dump facilities, if an output file is specified TODO [MR] use fixed path as for plans
			if (this.config.facilities().getOutputFile() != null) {
				new FacilitiesWriter((Facilities)Gbl.getWorld().getLayer(Facilities.LAYER_TYPE)).write();
			}

			// dump network, if an output file is specified
			new NetworkWriter(this.network, getOutputFilename("output_network.xml.gz")).write();

			// dump world  TODO [MR] use fixed path like for plans
			if (this.config.world().getOutputFile() != null) {
				new WorldWriter(Gbl.getWorld()).write();
			}

			// dump config  TODO [MR] use fixed path
			if (this.config.config().getOutputFile() != null) {
				new ConfigWriter(this.config).write();
			}

			if (unexpected) {
				log.info("S H U T D O W N   ---   unexpected shutdown request completed.");
			} else {
				log.info("S H U T D O W N   ---   retular shutdown completed.");
			}
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
		this.legTravelTimeEstimator = initLegTravelTimeEstimator(this.travelTimeCalculator);
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
		this.writeEvents = this.externalMobsim == null; // do not write events when using an external mobsim

		this.scoringFunctionFactory = new CharyparNagelScoringFunctionFactory();
	}

	/* ===================================================================
	 * private methods
	 * =================================================================== */

	private LegTravelTimeEstimator initLegTravelTimeEstimator(final TravelTimeI linkTravelTimeCalculator) {
		/* TODO [MR] move this method somewhere else, it should be more general instead just being here in the
		 * Controler. Think of a bigger picture: this estimator as well as travel time / cost calculators
		 * are all kind of singletons, and all have similar requirements (plans, network, events). Maybe this
		 * could somehow be generalized? -marcel/18jan2008   */
		/* This estimator is currently only used with the planomat, maybe moving it to the planomat somewhere?
		 * -marcel/18jan2008  */
		LegTravelTimeEstimator estimator = null;

		int timeBinSize = 900;
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(this.network, timeBinSize);
		this.events.addHandler(tDepDelayCalc);

		/* it would be nice to load the estimator via reflection,
		 * but if we just use make instead of Eclipse (as usual on a remote server)
		 * only classes occurring in the code are compiled, so we do it the classic way. */
		String estimatorName = PlanomatConfig.getLegTravelTimeEstimatorName();
		if (estimatorName == null) {
			return null;
		}
		if (estimatorName.equalsIgnoreCase("MyRecentEventsBasedEstimator")) {

			estimator = new MyRecentEventsBasedEstimator();
			this.events.addHandler((EventHandlerI) estimator);

		} else if (estimatorName.equalsIgnoreCase("CetinCompatibleLegTravelTimeEstimator")) {
			estimator = new CetinCompatibleLegTravelTimeEstimator(linkTravelTimeCalculator, tDepDelayCalc);
		} else if (estimatorName.equalsIgnoreCase("CharyparEtAlCompatibleLegTravelTimeEstimator")) {
			estimator = new CharyparEtAlCompatibleLegTravelTimeEstimator(linkTravelTimeCalculator, tDepDelayCalc);
		} else {
			Gbl.errorMsg("Invalid name of implementation of LegTravelTimeEstimatorI: " + estimatorName);
		}
		return estimator;
	}


	/**
	 * Loads the configuration object with the correct settings.
	 */
	private void loadConfig() {
		if (this.configFileName != null) {
			new MatsimConfigReader(this.config).readFile(this.configFileName, this.dtdFileName);
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
			if (!outputDir.mkdir()) {
				throw new RuntimeException("The output directory " + outputPath + " could not be created. Does it's parent directory exist?");
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
			this.scenarioData = new ScenarioData(this.config);
			this.network = loadNetwork();
			this.population = loadPopulation();
		}
	}

	/**
	 * Loads the network for the simulation.  In most cases, this should be an instance of {@link QueueNetworkLayer}
	 * for the standard QueueSimulation.
	 * <br>
	 * <strong>It is highly recommended NOT to overwrite this method!</strong> This method should be private, but is
	 * only protected at the moment because of backward-compatibility with the old Controler class. In general,
	 * it is recommended to pass a custom network and population using the special
	 * {@link #Controler(Config, QueueNetworkLayer, Plans) Constructor}.
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
	 * {@link #Controler(Config, QueueNetworkLayer, Plans) Constructor}.
	 *
	 * @return The population to be used for the simulation.
	 */
	protected Plans loadPopulation() {
		return this.scenarioData.getPopulation();
	}

	/** Loads a default set of {@link org.matsim.controler.listener ControlerListener} to privide basic functionality.
	 * <b>Note:</b> Be very careful if you overwrite this method! The order how the listeners are added is very important.
	 * Check the comments in the source file before overwriting this method!
	 */
	protected void loadCoreListeners() {

		/* The order how the listeners are added is very important!
		 * As dependencies between different listeners exist or listeners
		 * may read and write to common variables, the order is important.
		 * Example: The RoadPricing-Listener modifies the travelTimeCostCalculator,
		 * which in turn is used by the PlansReplanning-Listener.
		 */

		this.addControlerListener(new CoreControlerListener());

		this.addControlerListener(new PlansScoring());

		// load road pricing, if requested
		if (this.config.roadpricing().getTollLinksFile() != null) {
			this.addControlerListener(new RoadPricing());
		}

		this.addControlerListener(new PlansReplanning(this.population, this.travelCostCalculator, this.travelTimeCalculator, this.legTravelTimeEstimator));
		this.addControlerListener(new PlansDumping());
		this.addControlerListener(new LegHistogramListener(this.events, this.createGraphs));

		// load score stats
		try {
			ScoreStats scoreStats = new ScoreStats(this.population, getOutputFilename(FILENAME_SCORESTATS), this.createGraphs);
			this.addControlerListener(scoreStats);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// load counts, if requested
		if (this.config.counts().getCountsFileName() != null) {
			this.addControlerListener(new CountControlerListener(this.config));
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

	private void resetRandomNumbers(final int iteration) {
		Gbl.random.setSeed(this.config.global().getRandomSeed() + iteration);
		Gbl.random.nextDouble(); // draw one because of strange "not-randomness" is the first draw...
	}

	/*default*/ void enableEventsWriter() {
		if (this.eventWriter != null && !this.eventWriterAdded) {
			this.events.addHandler(this.eventWriter);
			this.eventWriterAdded = true;
		}
	}

	/*default*/ void disableEventsWriter() {
		if (this.eventWriter != null) {
			this.events.removeHandler(this.eventWriter);
			this.eventWriterAdded = false;
		}
	}

	/* ===================================================================
	 * protected methods for overwriting
	 * =================================================================== */

	protected void runMobSim() {
		SimulationTimer.setTime(0);

		if (this.externalMobsim == null) {
			Simulation sim = new QueueSimulation((QueueNetworkLayer)this.network, this.population, this.events);
			sim.run();
		} else {
			ExternalMobsim sim = new ExternalMobsim(this.population, this.events);
			sim.run();
		}
	}

	/* ===================================================================
	 * methods for ControlerListeners
	 * =================================================================== */

	/**
	 * Add a ControlerListener to the Controler instance
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
    StartupListener[] listener = this.listenerList.getListeners(StartupListener.class);
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
    ShutdownListener[] listener = this.listenerList.getListeners(ShutdownListener.class);
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
		IterationStartsListener[] listener = this.listenerList.getListeners(IterationStartsListener.class);
		for (int i = 0; i < listener.length; i++) {
    	listener[i].notifyIterationStarts(event);
    }
	}

	/**
	 * Notifies all ControlerIterationEndsListeners
	 * @param iteration
	 */
	private void fireControlerIterationEndsEvent(final int iteration) {
		IterationEndsEvent event = new IterationEndsEvent(this, iteration);
		IterationEndsListener[] listener = this.listenerList.getListeners(IterationEndsListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyIterationEnds(event);
		}
	}

	/**
	 * Notifies all ControlerScoringListeners
	 * @param iteration
	 */
	private void fireControlerScoringEvent(final int iteration) {
		ScoringEvent event = new ScoringEvent(this, iteration);
		ScoringListener[] listener = this.listenerList.getListeners(ScoringListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyScoring(event);
		}
	}

	/**
	 * Notifies all ControlerReplanningListeners
	 * @param iteration
	 */
	private void fireControlerReplanningEvent(final int iteration) {
		ReplanningEvent event = new ReplanningEvent(this, iteration);
		ReplanningListener[] listener = this.listenerList.getListeners(ReplanningListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyReplanning(event);
		}
	}

	/**
	 * Notifies all ControlerBeforeMobsimListeners
	 * @param iteration
	 */
	private void fireControlerBeforeMobsimEvent(final int iteration) {
		BeforeMobsimEvent event = new BeforeMobsimEvent(this, iteration);
		BeforeMobsimListener[] listener = this.listenerList.getListeners(BeforeMobsimListener.class);
		for (int i = 0; i < listener.length; i++) {
			listener[i].notifyBeforeMobsim(event);
		}
	}

	/**
	 * Notifies all ControlerAfterMobsimListeners
	 * @param iteration
	 */
	private void fireControlerAfterMobsimEvent(final int iteration) {
		AfterMobsimEvent event = new AfterMobsimEvent(this, iteration);
		AfterMobsimListener[] listener = this.listenerList.getListeners(AfterMobsimListener.class);
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
	 * Sets whether the events should be written to a file or not.
	 * External simulation usually write the events on their own,
	 * so we don't need to write them out a second time when reading
	 * them.<br />
	 * The writing of events can be paused during a simulation by calling
	 * <code>writeEvents(false)</code> and resumed later by calling
	 * <code>writeEvents(true)</code>. It is however not possible to
	 * start writing events during an iteration if the iteration started
	 * without writing events, as the event writer is only initialized
	 * at the beginning of an iteration if the writing of events is
	 * activated at that time.
	 *
	 * @param writeEvents
	 */
	public final void setWriteEvents(final boolean writeEvents) {
		if (this.writeEvents != writeEvents) {
			this.writeEvents = writeEvents;
			if (writeEvents) {
				enableEventsWriter();
			} else {
				disableEventsWriter();
			}
		}
	}

	/**
	 * @return true if events are written to a file.
	 */
	public final boolean getWriteEvents() {
		return this.writeEvents;
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

	public final void setTravelCostCalculator(final TravelCostI travelCostCalculator) {
		this.travelCostCalculator = travelCostCalculator;
	}

	public final TravelCostI getTravelCostCalculator() {
		return this.travelCostCalculator;
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

	public final NetworkLayer getNetwork() {
		return this.network;
	}

	public final Plans getPopulation() {
		return this.population;
	}

	public final Events getEvents() {
		return this.events;
	}

	public final CalcLinkStats getLinkStats() {
		return this.linkStats;
	}

	public final TravelTimeI getLinkTravelTimes() {
		return this.travelTimeCalculator;
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

	/**
	 * A ControlerListener that controls the most critical parts of the
	 * simulation process. This code could be integrated into the Controler
	 * class directly, but would make it more cumbersome to read. So it is
	 * implemented as a ControlerListener, to keep the structure of the
	 * Controler as simple as possible.
	 */
	/*default*/ class CoreControlerListener implements IterationStartsListener, BeforeMobsimListener, AfterMobsimListener {

		public void notifyIterationStarts(final IterationStartsEvent event) {
			Controler.this.events.resetHandlers(event.getIteration());
			Controler.this.events.resetCounter();
		}

		public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
			Controler.this.travelTimeCalculator.resetTravelTimes();

			if (Controler.this.writeEvents) {
				if (Controler.this.eventWriter == null) {
					Controler.this.eventWriter = new EventWriterTXT(Controler.getIterationFilename(FILENAME_EVENTS));
				} else {
					Controler.this.eventWriter.init(Controler.getIterationFilename(FILENAME_EVENTS));
				}
				enableEventsWriter(); // make sure it is added
			}

			if (event.getIteration() % 10 == 6) {
				Controler.this.events.addHandler(Controler.this.volumes);
			}
		}

		public void notifyAfterMobsim(final AfterMobsimEvent event) {
			if (Controler.this.eventWriter != null) {
				Controler.this.eventWriter.closefile();
			}

			if ((event.getIteration() % 10 == 0 && event.getIteration() > event.getControler().getFirstIteration()) || (event.getIteration() % 10 >= 6)) {
				Controler.this.linkStats.addData(Controler.this.volumes, Controler.this.travelTimeCalculator);
			}

			if (event.getIteration() % 10 == 0 && event.getIteration() > event.getControler().getFirstIteration()) {
				Controler.this.events.removeHandler(Controler.this.volumes);
				Controler.this.linkStats.writeFile(getIterationFilename(FILENAME_LINKSTATS));
			}
		}

	}

	/* ===================================================================
	 * main
	 * =================================================================== */

	public static void main(final String[] args) {
		if (args == null || args.length == 0) {
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
