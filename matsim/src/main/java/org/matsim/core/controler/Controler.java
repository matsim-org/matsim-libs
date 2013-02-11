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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.TravelDistanceStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
import org.matsim.core.controler.listener.ControlerListener;
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
import org.matsim.core.mobsim.qsim.multimodalsimengine.router.util.TravelTimeCalculatorWithBuffer;
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
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.LinkToLinkTripRouterFactory;
import org.matsim.core.router.MultimodalSimulationTripRouterFactory;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.InvertedNetworkLegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
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
import org.matsim.population.VspPlansCleaner;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;
import org.matsim.pt.counts.PtCountControlerListener;
import org.matsim.pt.router.PlansCalcTransitRoute;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.signalsystems.controler.DefaultSignalsControllerListenerFactory;
import org.matsim.signalsystems.controler.SignalsControllerListenerFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterFactory;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;

/**
 * The Controler is responsible for complete simulation runs, including the
 * initialization of all required data, running the iterations and the
 * replanning, analyses, etc.
 *
 * @author mrieser
 */
public class Controler extends AbstractController {
	// yyyy Design thoughts:
	// * Seems to me that we should try to get everything here final.  Flexibility is provided by the ability to set or add factories.  If this is
	// not sufficient, people should use AbstractController.  kai, jan'13

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

	protected final Config config;
	protected ScenarioImpl scenarioData = null;

	protected final EventsManager events;

	private final String configFileName;

	protected Network network = null;
	protected Population population = null;
	private Counts counts = null;

	public static interface TerminationCriterion {
		boolean continueIterations( int iteration ) ;
	}
	
	private TerminationCriterion terminationCriterion = new TerminationCriterion() {
	
		@Override
		public boolean continueIterations(int iteration) {
			return (iteration <= config.controler().getLastIteration());
		}
		
	};
	protected TravelTimeCalculator travelTimeCalculator = null;
	protected ScoringFunctionFactory scoringFunctionFactory = null;
	protected StrategyManager strategyManager = null;


	/* default analyses */
	/* package */CalcLinkStats linkStats = null;
	/* package */CalcLegTimes legTimes = null;
	/* package */VolumesAnalyzer volumes = null;

	private boolean createGraphs = true;
	protected boolean scenarioLoaded = false;
	private PlansScoring plansScoring = null;
	private ScoreStats scoreStats = null;
	private TravelDistanceStats travelDistanceStats = null;

	/**
	 * Attribute for the routing factory
	 */
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	private final List<MobsimListener> simulationListeners = new ArrayList<MobsimListener>();

	private TravelTimeCalculatorFactory travelTimeCalculatorFactory;

	private TravelDisutilityFactory travelCostCalculatorFactory = new TravelCostCalculatorFactoryImpl();
	private MobsimFactory thisMobsimFactory = null;

	private SignalsControllerListenerFactory signalsFactory = new DefaultSignalsControllerListenerFactory();
	private TransitRouterFactory transitRouterFactory = null;
	private TripRouterFactory tripRouterFactory = null;

	private MobsimFactoryRegister mobsimFactoryRegister;
	private SnapshotWriterFactoryRegister snapshotWriterRegister;
	private PlanStrategyFactoryRegister planStrategyFactoryRegister;
	
	protected boolean dumpDataAtEnd = true;
	private boolean overwriteFiles = false;
	private Map<String, TravelTime> multiModalTravelTimes;

	private boolean useTripRouting = true;
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
		this(args.length > 0 ? args[0] : null, null, null);
	}

	public Controler(final String configFileName) {
		this(configFileName, null, null);
	}

	public Controler(final Config config) {
		this(null, config, null);
	}

	public Controler(final Scenario scenario) {
		this(null, null, scenario);
	}

	private Controler(final String configFileName, final Config config, final Scenario scenario) {
		this.configFileName = configFileName;
		if (scenario != null) {
			this.scenarioLoaded = true;
			this.scenarioData = (ScenarioImpl) scenario;
			this.config = scenario.getConfig();
			this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
			checkConfigConsistencyAndWriteToLog(this.config,"Complete config dump directly after reading/getting the config file.  " +
			"See later for config dump after setup.");
		} else {
			if (this.configFileName == null) {
				if (config == null) {
					throw new IllegalArgumentException("Either the config or the filename of a configfile must be set to initialize the Controler.");
				}
				this.config = config;
			} else {
				this.config = ConfigUtils.loadConfig(this.configFileName);
			}
			this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
			checkConfigConsistencyAndWriteToLog(this.config,"Complete config dump directly after reading/getting the config file.  " +
			"See later for config dump after setup.");
			this.scenarioData = (ScenarioImpl) ScenarioUtils.createScenario(this.config);
		}
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		MobsimRegistrar mobsimRegistrar = new MobsimRegistrar();
		this.mobsimFactoryRegister = mobsimRegistrar.getFactoryRegister();
		SnapshotWriterRegistrar snapshotWriterRegistrar = new SnapshotWriterRegistrar();
		this.snapshotWriterRegister = snapshotWriterRegistrar.getFactoryRegister();
		PlanStrategyRegistrar planStrategyFactoryRegistrar = new PlanStrategyRegistrar();
		this.planStrategyFactoryRegister = planStrategyFactoryRegistrar.getFactoryRegister();
		this.events = EventsUtils.createEventsManager(this.config);
		if (this.config.multiModal().isMultiModalSimulationEnabled()) {
			// Actually, this is not so much about multi-modal but about within-day replanning.
			// It provides last iteration's travel times to be used during the current interation.
			// It just happens to be used only for the "multi-modal" routers. michaz '13
			this.travelTimeCalculatorFactory = new TravelTimeCalculatorWithBufferFactory();
		} else {
			this.travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		}
		this.config.parallelEventHandling().makeLocked();
	}

	/**
	 * Starts the iterations.
	 */
	public final void run() {
		// yyyy cannot make this final since it is overridden about 6 times. kai, jan'13

		setupOutputDirectory(this.config.controler().getOutputDirectory(), this.config.controler().getRunId(), this.overwriteFiles);
		if (this.config.multiModal().isMultiModalSimulationEnabled()) {
			setupMultiModalSimulation();
		}
		if (this.config.scenario().isUseTransit()) {
			setupTransitSimulation();
		}
		loadData();
		run(config);
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

		// The following two lines were introduced in nov/12.  _In addition_, the conversion of ActivityParams to
		// ActivityUtilityParameters will set the scoreAtAll flag to false (also introduced in nov/12).  kai, nov'12
		transitActivityParams.setOpeningTime(0.) ;
		transitActivityParams.setClosingTime(0.) ;
		
		this.config.planCalcScore().addActivityParams(transitActivityParams);
		// yy would this overwrite user-defined definitions of "pt interaction"?
		// No, I think that the user-defined parameters are set later, in fact overwriting this setting here.
		// kai, nov'12
	
		// the QSim reads the config by itself, and configures itself as a
		// transit-enabled mobsim. kai, nov'11
	}

	/**
	 * Loads the Scenario if it was not given in the constructor.
	 */
	protected void loadData() {
		// yyyy cannot make this final since it is overridden about 16 times. kai, jan'13

		if (!this.scenarioLoaded) {
			ScenarioUtils.loadScenario(this.scenarioData);
			this.network = this.scenarioData.getNetwork();
			this.population = this.scenarioData.getPopulation();
			this.scenarioLoaded = true;
		}
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
		// yyyy cannot make this final since it is overridden about 4 times. kai, jan'13

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
			this.addCoreControlerListener(new DumpDataAtEnd(scenarioData, getControlerIO()));
		}
	
	
		if (this.scoringFunctionFactory == null) {
			this.scoringFunctionFactory = loadScoringFunctionFactory();
		}
	
		// the default handling of plans
		this.plansScoring = new PlansScoring( this.scenarioData, this.events, getControlerIO(), this.scoringFunctionFactory );
		this.addCoreControlerListener(this.plansScoring);

		this.strategyManager = loadStrategyManager();
		this.addCoreControlerListener(new PlansReplanning(this.strategyManager, population));
		this.addCoreControlerListener(new PlansDumping(this.scenarioData, this.getFirstIteration(), this.config.controler().getWritePlansInterval(),
				this.stopwatch, this.getControlerIO() ));
	
	
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
		this.addCoreControlerListener(new LegTimesListener(legTimes, getControlerIO()));
	
		this.addCoreControlerListener(new EventsHandling(this.events, this.getConfig().controler().getWriteEventsInterval(),
				this.getConfig().controler().getEventsFileFormats(), this.getControlerIO() ));
		// must be last being added (=first being executed)
	
	
		loadControlerListeners();
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
		// yyyy cannot make this final since it is overridden about 10 times. kai, jan'13
		
		return new CharyparNagelScoringFunctionFactory(
				this.config.planCalcScore(), this.getNetwork());
	}

	/**
	 * Loads the default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide some more basic functionality. Unlike the
	 * core ControlerListeners the order in which the listeners of this method
	 * are added must not affect the correctness of the code.
	 */
	protected void loadControlerListeners() {
		// yyyy cannot make this method final since is is overridden about 13 times.  kai, jan'13

		// optional: LegHistogram
		this.addControlerListener(new LegHistogramListener(this.events, this.createGraphs));
	
		// optional: score stats
		this.scoreStats = new ScoreStats(this.population,
				this.getControlerIO().getOutputFilename(FILENAME_SCORESTATS), this.createGraphs);
		this.addControlerListener(this.scoreStats);
	
		// optional: travel distance stats
		this.travelDistanceStats = new TravelDistanceStats(this.population, this.network,
				this.getControlerIO() .getOutputFilename(FILENAME_TRAVELDISTANCESTATS), this.createGraphs);
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

	@Override
	protected void prepareForSim() {
		// yyyy cannot make this final since it is overridden at 2 locations.  kai, jan'13
		
		setUp();
		
		// make sure all routes are calculated.
		ParallelPersonAlgorithmRunner.run(this.getPopulation(), this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(createRoutingAlgorithm(), Controler.this.scenarioData);
			}
		});
	}

	/**
	 * Initializes the Controler with the parameters from the configuration.
	 * This method is called after the configuration is loaded, after the
	 * scenario data (network, population) is read, and after all ControlerListeners
	 * have processed their startup event.
	 * <p/>
	 * Design comments/questions:<ul>
	 * <li> "from the configuration" sounds too narrow.  Should be something like "from everything that is there at this point,
	 * including, say, factories."  kai, dec'12
	 * </ul>
	 */
	protected void setUp() {
		// yyyy cannot make this final since it is overridden at about 25 locations.  kai, jan'13
		
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
			multiModalTravelTimes.put(TransportMode.car, this.travelTimeCalculator.getLinkTravelTimes());
			multiModalTravelTimes.put(TransportMode.walk, new WalkTravelTime(configGroup));
			multiModalTravelTimes.put(TransportMode.bike, new BikeTravelTime(configGroup,
					new WalkTravelTime(configGroup)));
			multiModalTravelTimes.put(TransportMode.ride, new RideTravelTime(((TravelTimeCalculatorWithBuffer) this.travelTimeCalculator).getTravelTimesFromPreviousIteration(), 
					new WalkTravelTime(configGroup)));
			multiModalTravelTimes.put(TransportMode.pt, new PTTravelTime(configGroup, 
					((TravelTimeCalculatorWithBuffer) this.travelTimeCalculator).getTravelTimesFromPreviousIteration(), new WalkTravelTime(configGroup)));
	
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
	
		if ( config.scenario().isUseTransit() && getTransitRouterFactory() == null ) {
			setTransitRouterFactory(
					new TransitRouterImplFactory(
							getScenario().getTransitSchedule(),
							new TransitRouterConfig(
									config.planCalcScore(),
									config.plansCalcRoute(),
									config.transitRouter(),
									config.vspExperimental() )));
		}
	
		if ( getUseTripRouting() && tripRouterFactory == null ) {
			tripRouterFactory = new TripRouterFactoryImpl(
					getScenario(),
					getTravelDisutilityFactory(),
					getLinkTravelTimes(),
					getLeastCostPathCalculatorFactory(),
					getScenario().getConfig().scenario().isUseTransit() ? getTransitRouterFactory() : null);
	
			if ( config.multiModal().isMultiModalSimulationEnabled() ) {
				tripRouterFactory = new MultimodalSimulationTripRouterFactory(
						network,
						population.getFactory(),
						getLeastCostPathCalculatorFactory(),
						createTravelCostCalculator(),
						multiModalTravelTimes,
						config.multiModal(),
						tripRouterFactory);
			}
	
			if (this.getScenario().getConfig().controler().isLinkToLinkRoutingEnabled()) {
				tripRouterFactory = new LinkToLinkTripRouterFactory(
						getScenario(),
						getLeastCostPathCalculatorFactory(),
						getTravelDisutilityFactory(),
						travelTimeCalculator.getLinkToLinkTravelTimes(),
						getPopulation().getFactory(),
						tripRouterFactory);
			}
		}
	
	}

	/**
	 * @return A fully initialized StrategyManager for the plans replanning.
	 */
	protected StrategyManager loadStrategyManager() {
		// yyyy cannot make this final: overridden at about 40 locations.  kai, jan'2013
		StrategyManager manager = new StrategyManager();
		StrategyManagerConfigLoader.load(this, manager, this.planStrategyFactoryRegister);
		return manager;
	}

	@Override
	protected final boolean continueIterations(int it) {
		return terminationCriterion.continueIterations(it);
	}
	
	@Override
	protected final void runMobSim(int iteration) {
		this.thisIteration = iteration; // yyyy this should not be necessary any more. kai, feb'13
		runMobSim();
	}

	protected void runMobSim() {
		// yyyy cannot make this final: overridden at about 15 locations.  kai, jan'13
		Mobsim sim = getNewMobsim();
		sim.run();
	}

	/* package */ Mobsim getNewMobsim() {
		// overridden once for a test case (not so bad since it is package protected). kai, jan'13
		if (this.thisMobsimFactory != null) {
			Mobsim simulation = this.thisMobsimFactory.createMobsim(this.getScenario(), this.getEvents());
			enrichSimulation(simulation);
			return simulation;
		} else if (this.config.simulation() != null && this.config.simulation().getExternalExe() != null ) {
			ExternalMobsim simulation = new ExternalMobsim(this.scenarioData, this.events);
			simulation.setControlerIO(this.getControlerIO());
			simulation.setIterationNumber(this.thisIteration);
			return simulation;
		} else if (this.config.controler().getMobsim() != null) {
			String mobsim = this.config.controler().getMobsim();
			MobsimFactory f = this.mobsimFactoryRegister.getInstance(mobsim);
			Mobsim simulation = f.createMobsim(this.getScenario(), this.getEvents());
			enrichSimulation(simulation);
			return simulation;
		} else {
			log.warn("Please specify which mobsim should be used in the configuration (see module 'controler', parameter 'mobsim'). " +
					"Now trying to detect which mobsim to use from other parameters...");
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
			for (MobsimListener l : this.getMobsimListeners()) {
				((ObservableMobsim) simulation).addQueueSimulationListeners(l);
			}

			int itNumber = this.thisIteration;
			if (config.controler().getWriteSnapshotsInterval() != 0 && itNumber % config.controler().getWriteSnapshotsInterval() == 0) {
				SnapshotWriterManager manager = new SnapshotWriterManager(config);
				for (String snapshotFormat : this.config.controler().getSnapshotFormat()) {
					SnapshotWriterFactory snapshotWriterFactory = this.snapshotWriterRegister.getInstance(snapshotFormat);
					String baseFileName = snapshotWriterFactory.getPreferredBaseFilename();
					String fileName = this.getControlerIO().getIterationFilename(itNumber, baseFileName);
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
				qSim.addDepartureHandler(new MultiModalDepartureHandler(multiModalEngine, config.multiModal()));
			}
		}
	}

	public final void removeControlerListener(final ControlerListener l) {
		this.controlerListenerManager.removeControlerListener(l);
	}

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

	public final TravelDisutility createTravelCostCalculator() {
		return this.travelCostCalculatorFactory.createTravelDisutility(
				this.travelTimeCalculator.getLinkTravelTimes(), this.config.planCalcScore());
	}

	public final TravelTime getLinkTravelTimes() {
		return this.travelTimeCalculator.getLinkTravelTimes();
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
	@Deprecated // use addPlanStrategyFactory instead. kai, jan'13
	public final StrategyManager getStrategyManager() {
		return this.strategyManager;
	}

	public final LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return this.leastCostPathCalculatorFactory;
	}

	public final void setLeastCostPathCalculatorFactory(
			final LeastCostPathCalculatorFactory factory) {
		this.leastCostPathCalculatorFactory = factory;
	}

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
		// yyyy can't make this final: overridden at about 20 locations.  kai, jan'13
		
		return useTripRouting ?
			new PlanRouter(
				getTripRouterFactory().createTripRouter(),
				((ScenarioImpl)getScenario()).getActivityFacilities()) :
			createOldRoutingAlgorithm(
				this.createTravelCostCalculator(),
				travelTimeCalculator.getLinkTravelTimes(),
				travelTimeCalculator.getLinkToLinkTravelTimes());
	}

	private PlanAlgorithm createOldRoutingAlgorithm(final TravelDisutility travelCosts, final TravelTime travelTimes, LinkToLinkTravelTime linkToLinkTravelTime) {
		PlansCalcRoute plansCalcRoute = null;
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();
		if (this.config.scenario().isUseTransit()) {
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
					this.getLeastCostPathCalculatorFactory(), this.getTravelDisutilityFactory(), linkToLinkTravelTime);
			plansCalcRoute.addLegHandler(TransportMode.car,	invertedNetLegRouter);
			log.warn("Link to link routing only affects car legs, which is correct if turning move costs only affect rerouting of car legs.");
		}
		return plansCalcRoute;
	}

	public final TripRouterFactory getTripRouterFactory() {
		if ( !useTripRouting ) {
			throw new IllegalStateException( "cannot get the trip router: useTripRouting is false" );
		}
		return tripRouterFactory;
	}

	public final void setTripRouterFactory(final TripRouterFactory factory) {
		tripRouterFactory = factory;
	}

	public final void setUseTripRouting(final boolean useTripRouting) {
		this.useTripRouting = useTripRouting;
	}

	public final boolean getUseTripRouting() {
		return useTripRouting;
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

	public final Scenario getScenario() {
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

	public final CalcLegTimes getLegTimes() {
		return this.legTimes;
	}

	public final VolumesAnalyzer getVolumes() {
		return this.volumes;
	}

	/**
	 * @return Returns the scoreStats.
	 */
	public final ScoreStats getScoreStats() {
		return this.scoreStats;
	}

	public final List<MobsimListener> getMobsimListeners() {
		return this.simulationListeners;
	}

	@Deprecated
	/**
	 * This method exposes something which is definitely private to the Controler,
	 * bypassing all the interfaces.
	 */
	public final PlansScoring getPlansScoring() {
		return this.plansScoring;
	}

	@Deprecated
	public final TravelTimeCalculatorFactory getTravelTimeCalculatorFactory() {
		return this.travelTimeCalculatorFactory;
	}

	public final void setTravelTimeCalculatorFactory(
			final TravelTimeCalculatorFactory travelTimeCalculatorFactory) {
		this.travelTimeCalculatorFactory = travelTimeCalculatorFactory;
	}

	public final TravelDisutilityFactory getTravelDisutilityFactory() {
		return this.travelCostCalculatorFactory;
	}

	public final void setTravelDisutilityFactory(
			final TravelDisutilityFactory travelCostCalculatorFactory) {
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
	}

//	/**
//	 * yyyy Christoph Dobler overrides this method at some point. --???  
//	 */
//	public OutputDirectoryHierarchy getControlerIO() {
//		return this.controlerIO;
//	}
	// now in AbstractController

	/**
	 * @return The result of this function is not reliable. It is the iteration number, but it is only set  
	 * before running the Mobsim, not at the proper start of the iteration.
	 * If you need the iteration number, just be an EventHandler or an IterationStartsListener and you will be told.
	 */
	@Deprecated
	public final Integer getIterationNumber() {
		log.warn("Controler.getIterationNumber() is deprecated and wrong. If you need the iteration number, just be an EventHandler or an IterationStartsListener and you will be told.");
		return this.thisIteration;
	}
	
	public final void setTerminationCriterion(TerminationCriterion terminationCriterion) {
		this.terminationCriterion = terminationCriterion;
	}

	public final MobsimFactory getMobsimFactory() {
		return this.thisMobsimFactory;
	}

	public final void setMobsimFactory(final MobsimFactory mobsimFactory) {
		this.thisMobsimFactory = mobsimFactory;
	}

	/**
	 * Register a {@link MobsimFactory} with a given name.
	 *
	 * @param mobsimName
	 * @param mobsimFactory
	 *
	 * @see ControlerConfigGroup#getMobsim()
	 */
	public final void addMobsimFactory(final String mobsimName, final MobsimFactory mobsimFactory) {
		this.mobsimFactoryRegister.register(mobsimName, mobsimFactory);
	}

	public final void addSnapshotWriterFactory(final String snapshotWriterName, final SnapshotWriterFactory snapshotWriterFactory) {
		this.snapshotWriterRegister.register(snapshotWriterName, snapshotWriterFactory);
	}
	
	public final void addPlanStrategyFactory(final String planStrategyFactoryName, final PlanStrategyFactory planStrategyFactory) {
		this.planStrategyFactoryRegister.register(planStrategyFactoryName, planStrategyFactory);
	}

	public final SignalsControllerListenerFactory getSignalsControllerListenerFactory() {
		return this.signalsFactory;
	}

	public final void setSignalsControllerListenerFactory(
			final SignalsControllerListenerFactory signalsFactory) {
		this.signalsFactory = signalsFactory;
	}

	public final TransitRouterFactory getTransitRouterFactory() {
		return this.transitRouterFactory;
	}

	public final void setTransitRouterFactory(
			final TransitRouterFactory transitRouterFactory) {
		this.transitRouterFactory = transitRouterFactory;
	}

	public final Map<String, TravelTime> getMultiModalTravelTimes() {
		return this.multiModalTravelTimes;
	}

}
