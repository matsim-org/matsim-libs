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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.SimulationConfigGroup;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.external.ExternalMobsim;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.ObservableMobsim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.vis.snapshotwriters.SnapshotWriter;
import org.matsim.vis.snapshotwriters.SnapshotWriterManager;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

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
	public static final String FILENAME_EVENTS_XML = "events.xml.gz";
	public static final String FILENAME_LINKSTATS = "linkstats.txt.gz";
	public static final String FILENAME_TRAVELDISTANCESTATS = "traveldistancestats";
	public static final String FILENAME_POPULATION = "output_plans.xml.gz";
	public static final String FILENAME_NETWORK = "output_network.xml.gz";
	public static final String FILENAME_HOUSEHOLDS = "output_households.xml.gz";
	public static final String FILENAME_LANES = "output_lanes.xml.gz";
	public static final String FILENAME_CONFIG = "output_config.xml.gz";
	public static final String FILENAME_PERSON_ATTRIBUTES = "output_personAttributes.xml.gz" ; 

	private static final Logger log = Logger.getLogger(Controler.class);

	public static final Layout DEFAULTLOG4JLAYOUT = new PatternLayout(
			"%d{ISO8601} %5p %C{1}:%L %m%n");

	private final Config config; 
	private final Scenario scenario ;

	private final EventsManager events;

	private Injector injector;
	private boolean injectorCreated = false;

	public interface TerminationCriterion {
		boolean continueIterations( int iteration ) ;
	}

	private TerminationCriterion terminationCriterion = new TerminationCriterion() {

		@Override
		public boolean continueIterations(int iteration) {
			return (iteration <= config.controler().getLastIteration());
		}

	};

    // DefaultControlerModule includes submodules. If you want less than what the Controler does
    // by default, you can leave ControlerDefaultsModule out, look at what it does,
    // and only include what you want.
    private List<AbstractModule> modules = Arrays.<AbstractModule>asList(new ControlerDefaultsModule());
    // this defines the core of the process, and is mandatory: thus it is not in the "ControlerDefaultsModule",
    // which is more for default facultative stuff (analysis etc.)
    // One can override selected sub-modules by adding an overriding module.
    private final AbstractModule coreListenersModule = new ControlerDefaultCoreListenersModule();

    // The module which is currently defined by the sum of the setXX methods called on this Controler.
    private AbstractModule overrides = AbstractModule.emptyModule();

	private final List<MobsimListener> simulationListeners = new ArrayList<>();

	private boolean dumpDataAtEnd = true; 

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
		if (scenario != null) {
			// scenario already loaded (recommended):
			this.scenario  = scenario;
			this.config = scenario.getConfig();
			this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());
		} else {
			if (configFileName == null) {
				// config should already be loaded:
				if (config == null) {
					throw new IllegalArgumentException("Either the config or the filename of a configfile must be set to initialize the Controler.");
				}
				this.config = config;
			} else {
				// else load config:
				this.config = ConfigUtils.loadConfig(configFileName);
			}
			this.config.addConfigConsistencyChecker(new ConfigConsistencyCheckerImpl());

			// load scenario:
			this.scenario  = ScenarioUtils.createScenario(this.config);
			ScenarioUtils.loadScenario(this.scenario );
		}
		this.events = EventsUtils.createEventsManager(this.config);
		this.controlerListenerManager.setControler(this);
		this.config.parallelEventHandling().makeLocked();
	}

	/**
	 * Starts the iterations.
	 */
	public final void run() {
		setupOutputDirectory(
				this.config.controler().getOutputDirectory(),
				this.config.controler().getRunId(),
				this.config.controler().getOverwriteFileSetting() );
		if (this.config.transit().isUseTransit()) {
			setupTransitSimulation();
		}

		run(config);
		// "run(config)" is:
		//		loadCoreListeners();
		//		this.controlerListenerManager.fireControlerStartupEvent();
		//		checkConfigConsistencyAndWriteToLog(config, "config dump before iterations start" ) ;
		//		prepareForSim();
		//		doIterations(config.controler().getFirstIteration(), config.global().getRandomSeed());
		//		shutdown(false);

	}

	private void setupTransitSimulation() {
		// yyyy this should go away somehow. :-)
		
		log.info("setting up transit simulation");
//		if (!this.config.scenario().isUseVehicles()) {
		if ( this.config.transit().getVehiclesFile()==null ) {
			log.warn("Your are using Transit but have not provided a transit vehicles file. This most likely won't work.");
		}

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
	 * Loads a default set of {@link org.matsim.core.controler.listener
	 * ControlerListener} to provide basic functionality. 
	 * <p/>
	 * Method is final now.  If you think that you need to over-write this method, start from AbstractController instead.
	 */
	@Override
	protected final void loadCoreListeners() {
		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important.
		 *
		 * IMPORTANT: The execution order is reverse to the order the listeners
		 * are added to the list.
		 */

		this.injectorCreated = true;
        this.injector = Injector.createInjector(
                config,
                new AbstractModule() {
                    @Override
                    public void install() {
                    	final List<AbstractModule> baseModules = new ArrayList<>();
                    	baseModules.add( coreListenersModule );
                    	baseModules.addAll( modules );
                        // Use all the modules set with setModules, but overriding them with things set with
                        // other setters on this Controler.
                        install(AbstractModule.override(baseModules, overrides));

                        // Bootstrap it with the Scenario and some controler context.
						bind(OutputDirectoryHierarchy.class).toInstance(getControlerIO());
						bind(IterationStopWatch.class).toInstance(stopwatch);
						bind(Scenario.class).toInstance(scenario);
						bind(EventsManager.class).toInstance(events);
						binder().bind(Integer.class).annotatedWith(Names.named("iteration")).toProvider(new com.google.inject.Provider<Integer>() {
                            @Override
                            public Integer get() {
                                return getIterationNumber();
                            }
                        });
                    }
                });

		if (this.dumpDataAtEnd) {
			this.addCoreControlerListener( injector.getInstance( DumpDataAtEnd.class ) );
		}

        this.addCoreControlerListener( injector.getInstance( PlansScoring.class ) );
        this.addCoreControlerListener( injector.getInstance( PlansReplanning.class ) );
		this.addCoreControlerListener( injector.getInstance( PlansDumping.class ) );
		this.addCoreControlerListener( injector.getInstance( EventsHandling.class ) );
		// must be last being added (=first being executed)

        Set<EventHandler> eventHandlersDeclaredByModules = this.injector.getEventHandlersDeclaredByModules();
        for (EventHandler eventHandler : eventHandlersDeclaredByModules) {
            this.getEvents().addHandler(eventHandler);
        }
        Set<ControlerListener> controlerListenersDeclaredByModules = this.injector.getControlerListenersDeclaredByModules();
        for (ControlerListener controlerListener : controlerListenersDeclaredByModules) {
            this.addControlerListener(controlerListener);
        }

	}

	@Override
	protected final void prepareForSim() {

		if ( scenario  instanceof ScenarioImpl ) {
			((ScenarioImpl)scenario ).setLocked();
			// see comment in ScenarioImpl. kai, sep'14
		}

		/*
		 * Create single-mode network here and hand it over to PersonPrepareForSim. Otherwise, each instance would create its
		 * own single-mode network. However, this assumes that the main mode is car - which PersonPrepareForSim also does. Should
		 * be probably adapted in a way that other main modes are possible as well. cdobler, oct'15.
		 */
		final Network net;
		if (NetworkUtils.isMultimodal(this.scenario.getNetwork())) {
			log.info("Network seems to be multimodal. Create car-only network which is handed over to PersonPrepareForSim.");
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(this.scenario.getNetwork());
			net = NetworkUtils.createNetwork();
			HashSet<String> modes = new HashSet<String>();
			modes.add(TransportMode.car);
			filter.filter(net, modes);
		} else net = this.scenario.getNetwork();
		
		// make sure all routes are calculated.
        ParallelPersonAlgorithmRunner.run(getScenario().getPopulation(), this.config.global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
			@Override
			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(new PlanRouter(getTripRouterProvider().get(), getScenario().getActivityFacilities()), 
						Controler.this.scenario, net);
			}
		});
	}

	@Override
	protected final boolean continueIterations(int it) {
		return terminationCriterion.continueIterations(it);
	}

	@Override
	protected final void runMobSim() {
		Mobsim sim = getNewMobsim();
		sim.run();
	}

	private Mobsim getNewMobsim() {
		if (this.config.getModule(SimulationConfigGroup.GROUP_NAME) != null &&
				((SimulationConfigGroup) this.config.getModule(SimulationConfigGroup.GROUP_NAME)).getExternalExe() != null ) {
			ExternalMobsim simulation = new ExternalMobsim(this.scenario , this.events);
			simulation.setControlerIO(this.getControlerIO());
			simulation.setIterationNumber(this.getIterationNumber());
			return simulation;
		} else {
			Mobsim simulation = injector.getInstance(Mobsim.class);
			enrichSimulation(simulation);
			return simulation;
		}
	}

	private void enrichSimulation(final Mobsim simulation) {
		if (simulation instanceof ObservableMobsim) {
			for (MobsimListener l : this.injector.getMobsimListeners()) {
				((ObservableMobsim) simulation).addQueueSimulationListeners(l);
			}
			for (MobsimListener l : this.getMobsimListeners()) {
				((ObservableMobsim) simulation).addQueueSimulationListeners(l);
			}

			if (config.controler().getWriteSnapshotsInterval() != 0 && this.getIterationNumber() % config.controler().getWriteSnapshotsInterval() == 0) {
				SnapshotWriterManager manager = new SnapshotWriterManager(config);
				for (SnapshotWriter snapshotWriter : this.injector.getSnapshotWriters()) {
					manager.addSnapshotWriter(snapshotWriter);
				}
				((ObservableMobsim) simulation).addQueueSimulationListeners(manager);
			}
		}

	}

	// ******** --------- *******
	// The following is the internal interface of the Controler, which
	// is meant to be called while the Controler is running (not before),
	// meaning mostly from ControlerListeners, except possibly the StartupListener,
	// as the TravelTimeCalculator and the TripRouterFactory aren't available there yet.
	//
	// These, or some of these, would probably go on the ControlerEvents,
	// when they could stop passing the whole Controler.
	// 
	// Please try and do not use them for cascaded setting from the outside,
	// i.e. get something, look if it is instance of something,
	// and then change something on it. Or wrap it in something else and set it again.
	// These things are basically made to be
	// used, not changed. Send me (michaz) a mail if you need to do it.
	// I really want to sort this out.
	// ******** --------- *******

	public final TravelTime getLinkTravelTimes() {
		return this.injector.getInstance(com.google.inject.Injector.class).getInstance(Key.get(new TypeLiteral<Map<String, TravelTime>>() {}))
				.get(TransportMode.car);
	}

    /**
     * Gives access to a {@link org.matsim.core.router.TripRouter} instance.
     * This is a routing service which you can use
     * to calculate routes, e.g. from your own replanning code or your own within-day replanning
     * agent code.
     * You get a Provider (and not an instance directly) because your code may want to later
     * create more than one instance. A TripRouter is not guaranteed to be thread-safe, so
     * you must get() an instance for each thread if you plan to write multi-threaded code.
     *
     * See {@link org.matsim.core.router.TripRouter} for more information and pointers to examples.
     */
    public final Provider<TripRouter> getTripRouterProvider() {
		return this.injector.getProvider(TripRouter.class);
	}
	
	public final TravelDisutility createTravelDisutilityCalculator() {
        return getTravelDisutilityFactory().createTravelDisutility(this.injector.getInstance(TravelTime.class), getConfig().planCalcScore());
	}

	public final LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return this.injector.getInstance(LeastCostPathCalculatorFactory.class);
	}

	public final ScoringFunctionFactory getScoringFunctionFactory() {
		return this.injector.getInstance(ScoringFunctionFactory.class);
	}

    public final Config getConfig() {
		return config;
	}

    public final Scenario getScenario() {
	    return scenario;
    }

    public final EventsManager getEvents() {
	    return events;
    }

    public final Injector getInjector() {
	    return this.injector;
    }

	/**
	 * @deprecated Do not use this, as it may not contain values in every
	 *             iteration
	 */
	@Deprecated
	public final CalcLinkStats getLinkStats() {
		return this.injector.getInstance(CalcLinkStats.class);
	}

    public final VolumesAnalyzer getVolumes() {
		return this.injector.getInstance(VolumesAnalyzer.class);
	}

	public final ScoreStats getScoreStats() {
		return this.injector.getInstance(ScoreStats.class);
	}

    public final TravelDisutilityFactory getTravelDisutilityFactory() {
		return this.injector.getInstance(com.google.inject.Injector.class).getInstance(Key.get(new TypeLiteral<Map<String, TravelDisutilityFactory>>(){}))
				.get(TransportMode.car);
	}

	public final javax.inject.Provider<TransitRouter> getTransitRouterFactory() {
        return this.injector.getProvider(TransitRouter.class);
	}


	// ******** --------- *******
	// The following methods are the outer interface of the Controler. They are used
	// to set up infrastructure from the outside, before calling run().
	// Some of them may also work from the StartupListeners, I haven't sorted that out yet.
	// Contrast to the outermost interface, see below.
	// ******** --------- *******

	/**
	 * It should be possible to add or remove MobsimListeners between iterations, no problem.
	 */
	public final List<MobsimListener> getMobsimListeners() {
		return this.simulationListeners;
	}

	public final void removeControlerListener(final ControlerListener l) {
		// Not sure if necessary or when this is allowed to be called.
		this.controlerListenerManager.removeControlerListener(l);
	}

	public final void setScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory) {
        this.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
				bind(ScoringFunctionFactory.class).toInstance(scoringFunctionFactory);
			}
        });
	}

	public final void setTerminationCriterion(TerminationCriterion terminationCriterion) {
		this.terminationCriterion = terminationCriterion;
	}

	/**
     * Allows you to set a factory for {@link org.matsim.core.router.TripRouter} instances.
     * Do this if your use-case requires custom routing logic, for instance if you
     * implement your own complex travel mode.
     * See {@link org.matsim.core.router.TripRouter} for more information and pointers to examples.
     */
	public final void setTripRouterFactory(final TripRouterFactory factory) {
        this.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
				bind(TripRouterFactory.class).toInstance(factory);
			}
        });
	}

    public final void addOverridingModule(AbstractModule abstractModule) {
        if (this.injectorCreated) {
            throw new RuntimeException("Too late for configuring the Controler. This can only be done before calling run.");
        }
        this.overrides = AbstractModule.override(Arrays.asList(this.overrides), abstractModule);
    }


	/**
	 * @param dumpData
	 *            <code>true</code> if at the end of a run, plans, network,
	 *            config etc should be dumped to a file.
	 */
	public final void setDumpDataAtEnd(final boolean dumpData) {
		this.dumpDataAtEnd = dumpData;
	}
	
	
	// ******** --------- *******
	// The following methods are the outermost interface of the Controler. They are used
	// to register infrastructure provided by components, which may or may not be used
	// then, depending on what is in the Config file.
	// This is the point at which a component loader would operate - it would 
	// create this thing, go through the components to see what they provide, and put them
	// here.
	// These methods in principle be factored out to a Controller or OuterController or
	// something, which then creates and configures a Controler based on the config,
	// using the methods above.
	// ******** --------- *******

    public final void setModules(AbstractModule... modules) {
        if (this.injectorCreated) {
            throw new RuntimeException("Too late for configuring the Controler. This can only be done before calling run.");
        }
        this.modules = Arrays.asList(modules);
    }


	// ******** --------- *******
	// The following are methods which should not be used at all,
	// or where I am not sure when it is allowed to call them.
	// ******** --------- *******
	/**
	 * @return Returns the {@link org.matsim.core.replanning.StrategyManager}
	 *         used for the replanning of plans.
	 * @deprecated -- try to use controler.addPlanStrategyFactory or controler.addPlanSelectoryFactory.
	 * There are cases when this does not work, which is in particular necessary if you need to re-configure the StrategyManager
	 * during the iterations, <i>and</i> you cannot do this before the iterations start.  In such cases, using this
	 * method may be ok. kai/mzilske, aug'14
	 */
	@Deprecated // see javadoc above
	public final StrategyManager getStrategyManager() {
		return this.injector.getInstance(StrategyManager.class);
	}

}
