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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.TravelDistanceStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegHistogramListener;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.LinkStatsControlerListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulationFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalDepartureHandler;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngine;
import org.matsim.core.mobsim.qsim.multimodalsimengine.MultiModalSimEngineFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.BikeTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.PTTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.RideTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeCalculatorWithBufferFactory;
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.WalkTravelTime;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.EnsureActivityReachability;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.MultiModalNetworkCreator;
import org.matsim.core.mobsim.qsim.multimodalsimengine.tools.NonCarRouteDropper;
import org.matsim.core.mobsim.queuesim.QueueSimulationFactory;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.InvertedNetworkLegRouter;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.counts.CountControlerListener;
import org.matsim.counts.Counts;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.population.VspPlansCleaner;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.TransitControlerListener;
import org.matsim.pt.counts.PtCountControlerListener;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.roadpricing.PlansCalcAreaTollRoute;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.signalsystems.controler.DefaultSignalsControllerListenerFactory;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;
import org.matsim.vis.snapshotwriters.VisMobsim;

/**
 * The Controler is responsible for complete simulation runs, including the
 * initialization of all required data, running the iterations and the
 * replanning, analyses, etc.
 *
 * @author mrieser
 */
public class Controler extends AbstractController {

	public static final String DIRECTORY_ITERS = "ITERS";
	public static final String FILENAME_EVENTS_TXT = "events.txt.gz";
	public static final String FILENAME_EVENTS_XML = "events.xml.gz";
	public static final String FILENAME_LINKSTATS = "linkstats.txt.gz";
	public static final String FILENAME_SCORESTATS = "scorestats";
	public static final String FILENAME_TRAVELDISTANCESTATS = "traveldistancestats";
	public static final String FILENAME_POPULATION = "output_plans.xml.gz";
	public static final String FILENAME_NETWORK = "output_network.xml.gz";
	public static final String FILENAME_HOUSEHOLDS = "output_households.xml.gz";
	public static final String FILENAME_LANES = "output_lanes.xml.gz";
	public static final String FILENAME_CONFIG = "output_config.xml.gz";

	protected static final Logger log = Logger.getLogger(Controler.class);

	public static final Layout DEFAULTLOG4JLAYOUT = new PatternLayout(
			"%d{ISO8601} %5p %C{1}:%L %m%n");

	Integer iteration = null;

	protected final Config config;
	protected ScenarioImpl scenarioData = null;

	protected final EventsManagerImpl events;

	private final String configFileName;
	private final String dtdFileName;

	protected Network network = null;
	protected Population population = null;
	private Counts counts = null;

	protected TravelTimeCalculator travelTimeCalculator = null;
	private TravelDisutility travelCostCalculator = null;
	protected ScoringFunctionFactory scoringFunctionFactory = null;
	protected StrategyManager strategyManager = null;


	/**
	 * Defines in which iterations the events should be written. <tt>1</tt> is
	 * in every iteration, <tt>2</tt> in every second, <tt>10</tt> in every
	 * 10th, and so forth. <tt>0</tt> disables the writing of events completely.
	 */
	/* package */int writeEventsInterval = -1;
	/* package */int writePlansInterval = -1;

	/* default analyses */
	/* package */CalcLinkStats linkStats = null;
	/* package */CalcLegTimes legTimes = null;
	/* package */VolumesAnalyzer volumes = null;

	private boolean createGraphs = true;
	protected boolean scenarioLoaded = false;
	private PlansScoring plansScoring = null;
	private RoadPricing roadPricing = null;
	private ScoreStats scoreStats = null;
	private TravelDistanceStats travelDistanceStats = null;

	private TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();
	/**
	 * Attribute for the routing factory
	 */
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	private final List<MobsimListener> simulationListener = new ArrayList<MobsimListener>();

	private TravelTimeCalculatorFactory travelTimeCalculatorFactory;

	private TravelDisutilityFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
	private MobsimFactory mobsimFactory = null;

	private SignalsControllerListenerFactory signalsFactory = new DefaultSignalsControllerListenerFactory();
	private TransitRouterFactory transitRouterFactory = null;

	private MobsimFactoryRegister mobsimFactoryRegister;
	private SnapshotWriterFactoryRegister snapshotWriterRegister;

	protected boolean dumpDataAtEnd = true;
	private boolean overwriteFiles = false;
	private Map<String, TravelTime> multiModalTravelTimes;


	/**
	 * Initializes a new instance of Controler with the given arguments.
	 *
	 * @param args
	 *            The arguments to initialize the controler with.
	 *            <code>args[0]</code> is expected to contain the path to a
	 *            configuration file, <code>args[1]</code>, if set, is expected
	 *            to contain the path to a local copy of the DTD file used in
	 *            the configuration file.
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

	public Controler(final Scenario scenario) {
		this(null, null, null, scenario);
	}

	private Controler(final String configFileName, final String dtdFileName, final Config config, final Scenario scenario) {
		this.configFileName = configFileName;
		this.dtdFileName = dtdFileName;
		if (scenario != null) {
			this.scenarioLoaded = true;
			this.scenarioData = (ScenarioImpl) scenario;
			this.config = scenario.getConfig();
		} else {
			if (this.configFileName == null) {
				if (config == null) {
					throw new IllegalArgumentException("Either the config or the filename of a configfile must be set to initialize the Controler.");
				}
				this.config = config;
			} else {
				this.config = ConfigUtils.loadConfig(this.configFileName);
				this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
			}
			this.scenarioData = (ScenarioImpl) ScenarioUtils.createScenario(this.config);
		}
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		MobsimRegistrar mobsimRegistrar = new MobsimRegistrar();
		this.mobsimFactoryRegister = mobsimRegistrar.getFactoryRegister();
		SnapshotWriterRegistrar snapshotWriterRegistrar = new SnapshotWriterRegistrar();
		this.snapshotWriterRegister = snapshotWriterRegistrar.getFactoryRegister();
		this.events = (EventsManagerImpl) createEventsManager(this.config);
		this.config.parallelEventHandling().makeLocked();
	}

	protected EventsManager createEventsManager(final Config config) {
		return EventsUtils.createEventsManager(this.config);
	}

	/**
	 * Starts the simulation.
	 */
	public void run() {
		loadConfig();
		setupOutputDirectory(this.config.controler().getOutputDirectory(), this.config.controler().getRunId(), this.overwriteFiles);
		init();
		run(config);
	}

	private void init() {
		if (this.config.multiModal().isMultiModalSimulationEnabled()) {
			setupMultiModalSimulation();
		}
		if (this.config.scenario().isUseTransit()) {
			setupTransitSimulation();
		}
		loadData();
		setUp();
	}

	private final void setupMultiModalSimulation() {
		log.info("setting up multi modal simulation");

		// set Route Factories
		LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
		for (String mode : CollectionUtils.stringToArray(this.config.multiModal().getSimulatedModes())) {
			((PopulationFactoryImpl) this.getPopulation().getFactory()).setRouteFactory(mode, factory);
		}
	}

	private final void setupTransitSimulation() {
		log.info("setting up transit simulation");
		if (!this.config.scenario().isUseVehicles()) {
			log.warn("Your are using Transit but not Vehicles. This most likely won't work.");
		}

		Set<EventsFileFormat> formats = EnumSet.copyOf(this.config.controler().getEventsFileFormats());
		formats.add(EventsFileFormat.xml);
		this.config.controler().setEventsFileFormats(formats);

		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		this.config.planCalcScore().addActivityParams(transitActivityParams);

		// the QSim reads the config by itself, and configures itself as a
		// transit-enabled mobsim. kai, nov'11
	}



	@Override
	protected void prepareForSim() {
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(this.getPopulation(), this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), Controler.this.scenarioData);
			}
		});
	}

	@Override
	protected void runMobSim(int iteration) {
		this.iteration = iteration;
		runMobSim();
	}

	protected void runMobSim() {
		Mobsim sim = getNewMobsim();
		sim.run();
	}

	/**
	 * Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, and after the
	 * scenario data (network, population) is read.
	 */
	protected void setUp() {

		if (this.travelTimeCalculatorFactory != null) {
			log.info("travelTimeCalculatorFactory already set, ignoring default");
		} else {
			if (this.config.multiModal().isMultiModalSimulationEnabled()) {
				this.travelTimeCalculatorFactory = new TravelTimeCalculatorWithBufferFactory();
			} else {
				this.travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
			}
		}

		this.travelTimeCalculator = this.travelTimeCalculatorFactory.createTravelTimeCalculator(this.network, this.config.travelTimeCalculator());

		if (this.config.multiModal().isMultiModalSimulationEnabled()) {
			if (this.config.multiModal().isCreateMultiModalNetwork()) {
				log.info("Creating multi modal network.");
				new MultiModalNetworkCreator(this.config.multiModal()).run(this.scenarioData.getNetwork());
			}

			if (this.config.multiModal().isEnsureActivityReachability()) {
				log.info("Relocating activities that cannot be reached by the transport modes of their from- and/or to-legs...");
				new EnsureActivityReachability(this.scenarioData).run(this.scenarioData.getPopulation());
			}

			if (this.config.multiModal().isDropNonCarRoutes()) {
				log.info("Dropping existing routes of modes which are simulated with the multi modal mobsim.");
				new NonCarRouteDropper(this.config.multiModal()).run(this.scenarioData.getPopulation());
			}


			PlansCalcRouteConfigGroup configGroup = this.config.plansCalcRoute();
			multiModalTravelTimes = new HashMap<String, TravelTime>();
			multiModalTravelTimes.put(TransportMode.car, this.getTravelTimeCalculator());
			multiModalTravelTimes.put(TransportMode.walk, new WalkTravelTime(configGroup));
			multiModalTravelTimes.put(TransportMode.bike, new BikeTravelTime(configGroup,
					new WalkTravelTime(configGroup)));
			multiModalTravelTimes.put(TransportMode.ride, new RideTravelTime(this.getTravelTimeCalculator(), 
					new WalkTravelTime(configGroup)));
			multiModalTravelTimes.put(TransportMode.pt, new PTTravelTime(configGroup, 
					this.getTravelTimeCalculator(), new WalkTravelTime(configGroup)));
			
		}

		if (this.travelCostCalculator == null) {
			this.travelCostCalculator = this.travelCostCalculatorFactory.createTravelDisutility(this.travelTimeCalculator, this.config.planCalcScore());
		}
		this.events.addHandler(this.travelTimeCalculator);

		if (this.leastCostPathCalculatorFactory != null) {
			log.info("leastCostPathCalculatorFactory already set, ignoring RoutingAlgorithmType specified in config");
		} else {
			if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.Dijkstra)) {
				this.leastCostPathCalculatorFactory = new DijkstraFactory();
			} else if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.AStarLandmarks)) {
				this.leastCostPathCalculatorFactory = new AStarLandmarksFactory(
						this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()), this.config.global().getNumberOfThreads());
			} else if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.FastDijkstra)) {
				this.leastCostPathCalculatorFactory = new FastDijkstraFactory();
			} else if (this.config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.FastAStarLandmarks)) {
				this.leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(
						this.network, new FreespeedTravelTimeAndDisutility(this.config.planCalcScore()));
			} else {
				throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
			}
		}

		/*
		 * TODO [MR] linkStats uses ttcalc and volumes, but ttcalc has
		 * 15min-steps, while volumes uses 60min-steps! It works a.t.m., but the
		 * traveltimes in linkStats are the avg. traveltimes between xx.00 and
		 * xx.15, and not between xx.00 and xx.59
		 */
		this.linkStats = new CalcLinkStats(this.network);
		this.volumes = new VolumesAnalyzer(3600, 24 * 3600 - 1, this.network);
		this.events.addHandler(this.volumes);
		this.legTimes = new CalcLegTimes();
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
	 * Loads values from the configuration object to initialize the settings
	 */
	private void loadConfig() {
		checkConfigConsistencyAndWriteToLog("Complete config dump directly after reading the config file.  " +
				"See later for config dump after setup.");

		/*
		 * use writeEventsInterval from config file, only if not already
		 * initialized programmatically
		 */
		if (this.writeEventsInterval == -1) {
			this.writeEventsInterval = this.config.controler().getWriteEventsInterval();
		}
		if (this.writePlansInterval == -1) {
			this.writePlansInterval = this.config.controler().getWritePlansInterval();
		}
	}

	/**
	 * Design decisions:
	 * <ul>
	 * <li>I extracted this method since it is now called <i>twice</i>: once
	 * directly after reading, and once before the iterations start. The second
	 * call seems more important, but I wanted to leave the first one there in
	 * case the program fails before that config dump. Might be put into the
	 * "unexpected shutdown hook" instead. kai, dec'10
	 * </ul>
	 *
	 * @param message
	 *            the message that is written just before the config dump
	 */
	protected final void checkConfigConsistencyAndWriteToLog(final String message) {
		log.info(message);
		String newline = System.getProperty("line.separator");// use native line endings for logfile
		StringWriter writer = new StringWriter();
		new ConfigWriter(this.scenarioData.getConfig()).writeStream(new PrintWriter(writer), newline);
		log.info(newline + newline + writer.getBuffer().toString());
		log.info("Complete config dump done.");
		log.info("Checking consistency of config...");
		this.scenarioData.getConfig().checkConsistency();
		log.info("Checking consistency of config done.");
	}


	/**
	 * Load all the required data. Currently, this only loads the Scenario if it
	 * was not given in the Constructor.
	 */
	protected void loadData() {
		if (!this.scenarioLoaded) {
			ScenarioUtils.loadScenario(this.scenarioData);
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
			this.scenarioLoaded = true;
		}
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
		return new CharyparNagelScoringFunctionFactory(
				this.config.planCalcScore(), this.getNetwork());
	}

	/**
	 * Loads a default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide basic functionality. <b>Note:</b> Be very
	 * careful if you overwrite this method! The order how the listeners are
	 * added is very important. Check the comments in the source file before
	 * overwriting this method!
	 */
	@Override
	protected void loadCoreListeners() {

		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important. Example: The
		 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
		 * turn is used by the PlansScoring-Listener.
		 *
		 * IMPORTANT: The execution order is reverse to the order the listeners
		 * are added to the list.
		 */

		if (this.dumpDataAtEnd) {
			this.addCoreControlerListener(new DumpDataAtEnd(scenarioData, controlerIO));
		}

		// the default handling of plans
		this.plansScoring = new PlansScoring( this.scenarioData, this.events, controlerIO, this.scoringFunctionFactory );
		this.addCoreControlerListener(this.plansScoring);

		// load road pricing, if requested
		if (this.config.scenario().isUseRoadpricing()) {
			this.roadPricing = new RoadPricing();
			this.addCoreControlerListener(this.roadPricing);
		}

		this.addCoreControlerListener(new PlansReplanning( this.strategyManager, this.population ));
		this.addCoreControlerListener(new PlansDumping(this.scenarioData, this.getFirstIteration(), this.getWritePlansInterval(),
				this.stopwatch, this.controlerIO ));


		this.addCoreControlerListener(new LegTimesListener(legTimes, controlerIO));
		this.addCoreControlerListener(new EventsHandling(this.events, this.getWriteEventsInterval(),
				this.getConfig().controler().getEventsFileFormats(), this.getControlerIO() ));
		// must be last being added (=first being executed)


		loadControlerListeners();
	}

	/**
	 * Loads the default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide some more basic functionality. Unlike the
	 * core ControlerListeners the order in which the listeners of this method
	 * are added must not affect the correctness of the code.
	 */
	protected void loadControlerListeners() {
		// optional: LegHistogram
		this.addControlerListener(new LegHistogramListener(this.events, this.createGraphs));

		// optional: score stats
		this.scoreStats = new ScoreStats(this.population,
				this.controlerIO.getOutputFilename(FILENAME_SCORESTATS), this.createGraphs);
		this.addControlerListener(this.scoreStats);

		// optional: travel distance stats
		this.travelDistanceStats = new TravelDistanceStats(this.population, this.network,
				this.controlerIO .getOutputFilename(FILENAME_TRAVELDISTANCESTATS), this.createGraphs);
		this.addControlerListener(this.travelDistanceStats);

		// load counts, if requested
		if (this.config.counts().getCountsFileName() != null) {
			CountControlerListener ccl = new CountControlerListener(this.config.counts());
			this.addControlerListener(ccl);
			this.counts = ccl.getCounts();
		}

		if (this.config.linkStats().getWriteLinkStatsInterval() > 0) {
			this.addControlerListener(new LinkStatsControlerListener(this.config.linkStats()));
		}

		if (this.config.scenario().isUseTransit()) {
			addControlerListener(new TransitControlerListener());
			if (this.config.ptCounts().getAlightCountsFileName() != null) {
				// only works when all three files are defined! kai, oct'10
				addControlerListener(new PtCountControlerListener(this.config));
			}
		}

		if (this.config.scenario().isUseSignalSystems()) {
			addControlerListener(this.signalsFactory.createSignalsControllerListener());
		}

		if ( !this.config.vspExperimental().getActivityDurationInterpretation().equals(ActivityDurationInterpretation.minOfDurationAndEndTime)
				|| this.config.vspExperimental().isRemovingUnneccessaryPlanAttributes() ) {
			addControlerListener(new VspPlansCleaner());
		}

	}

	/* package */Mobsim getNewMobsim() {
		if (this.mobsimFactory != null) {
			Mobsim simulation = this.mobsimFactory.createMobsim(this.getScenario(), this.getEvents());
			enrichSimulation(simulation);
			return simulation;
		} else if (this.config.simulation() != null && this.config.simulation().getExternalExe() != null ) {
			ExternalMobsim simulation = new ExternalMobsim(this.scenarioData, this.events);
			simulation.setControlerIO(this.controlerIO);
			simulation.setIterationNumber(this.getIterationNumber());
			return simulation;
		} else if (this.config.controler().getMobsim() != null) {
			String mobsim = this.config.controler().getMobsim();
			MobsimFactory f = this.mobsimFactoryRegister.getInstance(mobsim);
			Mobsim simulation = f.createMobsim(this.getScenario(), this.getEvents());
			enrichSimulation(simulation);
			return simulation;
		} else {
			log.warn("Please specify which mobsim should be used in the configuration (see module 'controler', parameter 'mobsim'). Now trying to detect which mobsim to use from other parameters...");
			MobsimFactory mobsimFactory;
			if (config.getModule(QSimConfigGroup.GROUP_NAME) != null) {
				mobsimFactory = new QSimFactory();
			} else if (config.getModule("JDEQSim") != null) {
				mobsimFactory = new JDEQSimulationFactory();
			} else if (config.getModule(SimulationConfigGroup.GROUP_NAME) != null) {
				mobsimFactory = new QueueSimulationFactory();
			} else {
				log.warn("There is no configuration for a mobility simulation in the config. The Controler "
						+ "uses the default `Simulation'.  Add a (possibly empty) `Simulation' module to your config file "
						+ "to avoid this warning");
				config.addSimulationConfigGroup(new SimulationConfigGroup());
				mobsimFactory = new QueueSimulationFactory();
			}
			Mobsim simulation = mobsimFactory.createMobsim(this.getScenario(), this.getEvents());
			enrichSimulation(simulation);
			return simulation;
		}
	}

	private void enrichSimulation(final Mobsim simulation) {
		if (simulation instanceof ObservableMobsim) {
			for (MobsimListener l : this.getQueueSimulationListener()) {
				((ObservableMobsim) simulation).addQueueSimulationListeners(l);
			}
		}
		if (simulation instanceof VisMobsim) {
			int itNumber = this.getIterationNumber();
			if (config.controler().getWriteSnapshotsInterval() != 0 && itNumber % config.controler().getWriteSnapshotsInterval() == 0) {
				SnapshotWriterManager manager = new SnapshotWriterManager(config);
				for (String snapshotFormat : this.config.controler().getSnapshotFormat()) {
					SnapshotWriterFactory snapshotWriterFactory = this.snapshotWriterRegister.getInstance(snapshotFormat);
					String baseFileName = snapshotWriterFactory.getPreferredBaseFilename();
					String fileName = this.controlerIO.getIterationFilename(itNumber, baseFileName);
					SnapshotWriter snapshotWriter = snapshotWriterFactory.createSnapshotWriter(fileName, this.scenarioData);
					manager.addSnapshotWriter(snapshotWriter);
				}
				((ObservableMobsim) simulation).addQueueSimulationListeners(manager);
			}
		}
		if (simulation instanceof QSim) {
			// The QSim may need a multiModalSimEngine, which needs travel times, which are kept from iteration to iteration by the Controler.
			// One day, we may create a dynamic equivalent to the Scenario, which things like Mobsims have access to (and which would then also contain the Plan database).
			// But until then, this is too special for my taste to put into the MobsimFactory interface, so we add it here in passing.  mz 2012-03
			if (config.multiModal().isMultiModalSimulationEnabled()) {
				log.info("Using MultiModalMobsim...");
				QSim qSim = (QSim) simulation;
				MultiModalSimEngine multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(qSim, this.multiModalTravelTimes);
				qSim.addMobsimEngine(multiModalEngine);
				qSim.addDepartureHandler(new MultiModalDepartureHandler(qSim, multiModalEngine, config.multiModal()));
			}
		}
	}



	/*
	 * ===================================================================
	 * methods for core ControlerListeners
	 * ===================================================================
	 */

//	/**
//	 * Add a core ControlerListener to the Controler instance
//	 *
//	 * @param l
//	 */
//	protected final void addCoreControlerListener(final ControlerListener l) {
//		this.controlerListenerManager.addCoreControlerListener(l);
//	}

	/*
	 * ===================================================================
	 * methods for ControlerListeners
	 * ===================================================================
	 */

//	/**
//	 * Add a ControlerListener to the Controler instance
//	 *
//	 * @param l
//	 */
//	public final void addControlerListener(final ControlerListener l) {
//		this.controlerListenerManager.addControlerListener(l);
//	}

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
	 * <strong>Use this setting with caution, as it can result in data
	 * loss!</strong>
	 *
	 * @param overwrite
	 *            whether files and directories should be overwritten (true) or
	 *            not (false)
	 */
	public final void setOverwriteFiles(final boolean overwrite) {
		this.overwriteFiles = overwrite;
	}


	/**
	 * Sets in which iterations events should be written to a file. If set to
	 * <tt>1</tt>, the events will be written in every iteration. If set to
	 * <tt>2</tt>, the events are written every second iteration. If set to
	 * <tt>10</tt>, the events are written in every 10th iteration. To disable
	 * writing of events completely, set the interval to <tt>0</tt> (zero).
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

	/**
	 * @param dumpData
	 *            <code>true</code> if at the end of a run, plans, network,
	 *            config etc should be dumped to a file.
	 */
	public final void setDumpDataAtEnd(final boolean dumpData) {
		this.dumpDataAtEnd = dumpData;
	}

	/*
	 * ===================================================================
	 * Optional setters that allow to overwrite some default algorithms used
	 * ===================================================================
	 */

	public final TravelDisutility createTravelCostCalculator() {
		return this.travelCostCalculatorFactory.createTravelDisutility(
				this.travelTimeCalculator, this.config.planCalcScore());
	}

	public final TravelTime getTravelTimeCalculator() {
		return this.travelTimeCalculator;
	}

	/**
	 * Sets a new {@link org.matsim.core.scoring.ScoringFunctionFactory} to use.
	 * <strong>Note:</strong> This will reset all scores calculated so far! Only
	 * call this before any events are generated in an iteration.
	 *
	 * @param factory
	 *            The new ScoringFunctionFactory to be used.
	 */
	public final void setScoringFunctionFactory(
			final ScoringFunctionFactory factory) {
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

	public void setLeastCostPathCalculatorFactory(
			final LeastCostPathCalculatorFactory factory) {
		this.leastCostPathCalculatorFactory = factory;
	}

	/*
	 * ===================================================================
	 * Factory methods
	 * ===================================================================
	 */

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
	public PlanAlgorithm createRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes) {
		PlansCalcRoute plansCalcRoute = null;
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();

		if (this.getScenario().getConfig().scenario().isUseRoadpricing()
				&& (RoadPricingScheme.TOLL_TYPE_AREA.equals(this.scenarioData.getRoadPricingScheme().getType()))) {
			plansCalcRoute = new PlansCalcAreaTollRoute(this.config.plansCalcRoute(), this.network, travelCosts,
					travelTimes, this.getLeastCostPathCalculatorFactory(),routeFactory, this.scenarioData.getRoadPricingScheme());
			log.warn("As roadpricing with area toll is enabled a leg router for area tolls is used. Other features, " +
					"e.g. transit or multimodal simulation may not work as expected.");
		} else if (this.config.scenario().isUseTransit()) {
			plansCalcRoute = new PlansCalcTransitRoute(this.config.plansCalcRoute(), this.network, travelCosts,
					travelTimes, this.getLeastCostPathCalculatorFactory(),routeFactory, this.config.transit(),
					this.transitRouterFactory.createTransitRouter(), this.scenarioData.getTransitSchedule());
			log.warn("As simulation of public transit is enabled a leg router for transit is used. Other features, " +
					"e.g. multimodal simulation, may not work as expected.");
		} else if (this.config.multiModal().isMultiModalSimulationEnabled()) {
			plansCalcRoute = new PlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCosts, multiModalTravelTimes.get(TransportMode.car),
					this.getLeastCostPathCalculatorFactory(), routeFactory);

			
			// Define restrictions for the different modes.
			/*
			 * Car
			 */	
			Set<String> carModeRestrictions = new HashSet<String>();
			carModeRestrictions.add(TransportMode.car);
			
			/*
			 * Walk
			 */	
			Set<String> walkModeRestrictions = new HashSet<String>();
			walkModeRestrictions.add(TransportMode.bike);
			walkModeRestrictions.add(TransportMode.walk);
					
			/*
			 * Bike
			 * Besides bike mode we also allow walk mode - but then the
			 * agent only travels with walk speed (handled in MultiModalTravelTimeCost).
			 */
			Set<String> bikeModeRestrictions = new HashSet<String>();
			bikeModeRestrictions.add(TransportMode.walk);
			bikeModeRestrictions.add(TransportMode.bike);
			
			/*
			 * PT
			 * We assume PT trips are possible on every road that can be used by cars.
			 * 
			 * Additionally we also allow pt trips to use walk and / or bike only links.
			 * On those links the traveltimes are quite high and we can assume that they
			 * are only use e.g. to walk from the origin to the bus station or from the
			 * bus station to the destination.
			 */
			Set<String> ptModeRestrictions = new HashSet<String>();
			ptModeRestrictions.add(TransportMode.pt);
			ptModeRestrictions.add(TransportMode.car);
			ptModeRestrictions.add(TransportMode.bike);
			ptModeRestrictions.add(TransportMode.walk);
			
			/*
			 * Ride
			 * We assume ride trips are possible on every road that can be used by cars.
			 * Additionally we also allow ride trips to use walk and / or bike only links.
			 * For those links walk travel times are used.
			 */
			Set<String> rideModeRestrictions = new HashSet<String>();
			rideModeRestrictions.add(TransportMode.car);
			rideModeRestrictions.add(TransportMode.bike);
			rideModeRestrictions.add(TransportMode.walk);
			
			TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(this.network);
			for (String mode : CollectionUtils.stringToArray(this.config.multiModal().getSimulatedModes())) {
				
				Set<String> modeRestrictions;
				if (mode.equals(TransportMode.car)) {
					modeRestrictions = carModeRestrictions;
				} else if (mode.equals(TransportMode.walk)) {
					modeRestrictions = walkModeRestrictions;
				} else if (mode.equals(TransportMode.bike)) {
					modeRestrictions = bikeModeRestrictions;
				} else if (mode.equals(TransportMode.ride)) {
					modeRestrictions = rideModeRestrictions;
				} else if (mode.equals(TransportMode.pt)) {
					modeRestrictions = ptModeRestrictions;
				} else continue;
				
				Network subNetwork = NetworkImpl.createNetwork();
				networkFilter.filter(subNetwork, modeRestrictions);
				
				LeastCostPathCalculator routeAlgo = this.getLeastCostPathCalculatorFactory().createPathCalculator(subNetwork, travelCosts, multiModalTravelTimes.get(mode));
				plansCalcRoute.addLegHandler(mode, new NetworkLegRouter(network, routeAlgo, routeFactory));
			}
		
		} else {
			plansCalcRoute = new PlansCalcRoute(this.config.plansCalcRoute(),
					this.network, travelCosts, travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory);
		}

		if (this.getScenario().getConfig().controler().isLinkToLinkRoutingEnabled()) {
			//Note that the inverted network is created once per thread
			InvertedNetworkLegRouter invertedNetLegRouter = new InvertedNetworkLegRouter(this.getScenario(),
					this.getLeastCostPathCalculatorFactory(), this.getTravelDisutilityFactory(), travelTimes);
			plansCalcRoute.addLegHandler(TransportMode.car,	invertedNetLegRouter);
			log.warn("Link to link routing only affects car legs, which is correct if turning move costs only affect rerouting of car legs.");
		}
		return plansCalcRoute;
		
	}

	/*
	 * ===================================================================
	 * Informational methods
	 * ===================================================================
	 */

	/*
	 * ===================================================================
	 * Factory methods
	 * ===================================================================
	 */

	/**Design comments:<ul>
	 * <li> yyyy It seems to me that one would need a factory at <i>this</i> level. kai, may'12
	 * <li> An issue is that the TravelTime(Calculator) object needs to be passed into the factory.  I don't think that
	 * this is a large problem, but it needs to be dealt with. kai, may'12
	 * </ul>
	 *
	 * @return a new instance of a {@link PlanAlgorithm} to calculate the routes
	 *         of plans with the default (= the current from the last or current
	 *         iteration) travel costs and travel times. Only to be used by a
	 *         single thread, use multiple instances for multiple threads!
	 */
	public PlanAlgorithm createRoutingAlgorithm() {
		return createRoutingAlgorithm(this.createTravelCostCalculator(),
				this.getTravelTimeCalculator());
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

	public final ActivityFacilities getFacilities() {
		return this.scenarioData.getActivityFacilities();
	}

	public final Network getNetwork() {
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

	/**
	 * @deprecated Do not use this, as it may not contain values in every
	 *             iteration
	 * @return
	 */
	@Deprecated
	public final CalcLinkStats getLinkStats() {
		return this.linkStats;
	}

	public CalcLegTimes getLegTimes() {
		return this.legTimes;
	}

	public VolumesAnalyzer getVolumes() {
		return this.volumes;
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

	public TreeMap<Id, FacilityPenalty> getFacilityPenalties() {
		return this.facilityPenalties;
	}

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

	public List<MobsimListener> getQueueSimulationListener() {
		return this.simulationListener;
	}

	@Deprecated
	/**
	 * This method exposes something which is definitely private to the Controler,
	 * bypassing all the interfaces.
	 */
	public PlansScoring getPlansScoring() {
		return this.plansScoring;
	}

	public TravelTimeCalculatorFactory getTravelTimeCalculatorFactory() {
		return this.travelTimeCalculatorFactory;
	}

	public void setTravelTimeCalculatorFactory(
			final TravelTimeCalculatorFactory travelTimeCalculatorFactory) {
		this.travelTimeCalculatorFactory = travelTimeCalculatorFactory;
	}

	public TravelDisutilityFactory getTravelDisutilityFactory() {
		return this.travelCostCalculatorFactory;
	}

	public void setTravelDisutilityFactory(
			final TravelDisutilityFactory travelCostCalculatorFactory) {
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
	}

	public OutputDirectoryHierarchy getControlerIO() {
		return this.controlerIO;
	}

	/**
	 * @return the iteration number of the current iteration when the Controler
	 *         is iterating, null if the Controler is in the startup/shutdown
	 *         process
	 */
	public Integer getIterationNumber() {
		return this.iteration;
	}

	public MobsimFactory getMobsimFactory() {
		return this.mobsimFactory;
	}

	public void setMobsimFactory(final MobsimFactory mobsimFactory) {
		this.mobsimFactory = mobsimFactory;
	}

	/**
	 * Register a {@link MobsimFactory} with a given name.
	 *
	 * @param mobsimName
	 * @param mobsimFactory
	 *
	 * @see ControlerConfigGroup#getMobsim()
	 */
	public void addMobsimFactory(final String mobsimName, final MobsimFactory mobsimFactory) {
		this.mobsimFactoryRegister.register(mobsimName, mobsimFactory);
	}

	public void addSnapshotWriterFactory(final String snapshotWriterName, final SnapshotWriterFactory snapshotWriterFactory) {
		this.snapshotWriterRegister.register(snapshotWriterName, snapshotWriterFactory);
	}

	public SignalsControllerListenerFactory getSignalsControllerListenerFactory() {
		return this.signalsFactory;
	}

	public void setSignalsControllerListenerFactory(
			final SignalsControllerListenerFactory signalsFactory) {
		this.signalsFactory = signalsFactory;
	}

	public TransitRouterFactory getTransitRouterFactory() {
		return this.transitRouterFactory;
	}

	public void setTransitRouterFactory(
			final TransitRouterFactory transitRouterFactory) {
		this.transitRouterFactory = transitRouterFactory;
	}

	public int getWritePlansInterval() {
		return this.writePlansInterval;
	}
	
	public Map<String, TravelTime> getMultiModalTravelTimes() {
		return this.multiModalTravelTimes;
	}

}
