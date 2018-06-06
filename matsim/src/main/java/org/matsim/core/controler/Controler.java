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

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.matsim.analysis.CalcLinkStats;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.analysis.ScoreStats;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.ConfigConsistencyCheckerImpl;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioByConfigModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scoring.ScoringFunctionFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Controler is responsible for complete simulation runs, including the
 * initialization of all required data, running the iterations and the
 * replanning, analyses, etc.
 *
 * @author mrieser
 */
public final class Controler implements ControlerI, MatsimServices {
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
	public static final String FILENAME_CONFIG = "output_config.xml";
	public static final String FILENAME_CONFIG_REDUCED = "output_config_reduced.xml";
	public static final String FILENAME_PERSON_ATTRIBUTES = "output_personAttributes.xml.gz" ;
	public static final String FILENAME_COUNTS = "output_counts.xml.gz" ;
	public static final String DIVIDER = "###################################################";
	
	private static final Logger log = Logger.getLogger(Controler.class);

	public static final Layout DEFAULTLOG4JLAYOUT = new PatternLayout(
			"%d{ISO8601} %5p %C{1}:%L %m%n");

	private final Config config;
	private Scenario scenario;

	private com.google.inject.Injector injector;
	private boolean injectorCreated = false;

	@Override
	public IterationStopWatch getStopwatch() {
		return injector.getInstance(IterationStopWatch.class);
	}

	// DefaultControlerModule includes submodules. If you want less than what the Controler does
    // by default, you can leave ControlerDefaultsModule out, look at what it does,
    // and only include what you want.
    private List<AbstractModule> modules = Collections.singletonList(new ControlerDefaultsModule());

	// The module which is currently defined by the sum of the setXX methods called on this Controler.
    private AbstractModule overrides = AbstractModule.emptyModule();

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
	 *            The arguments to initialize the services with.
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

	private Controler(final String configFileName, final Config config, Scenario scenario) {
		if (scenario != null) {
			// scenario already loaded (recommended):
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
			//scenario  = ScenarioUtils.createScenario(this.config);
			//ScenarioUtils.loadScenario(scenario) ;
		}
		this.config.parallelEventHandling().makeLocked();
		this.scenario = scenario;
		this.overrides = scenario == null ?
				new ScenarioByConfigModule() :
				new ScenarioByInstanceModule(this.scenario);
	}

	/**
	 * Starts the iterations.
	 */
	@Override
	public final void run() {
		// It is better to keep this line before actually creating the injector, because:
		// - it actually means "fail if adding new Guice modules"
		// - adding Guice modules to the Controler from other Guice modules is too late.
		// This might sound silly, but might, in some cases, happen, through code that
		// - transformed a StartupListener to a Guice module
		// - that called methods such as setScoringFunctionFactory(), that redirects to addOverridingModule()
		// And this happens silently, leading to lots of time and hair lost.
		// td, nov 16
		this.injectorCreated = true;
		this.injector = Injector.createInjector(config, AbstractModule.override(Collections.singleton(new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				for (AbstractModule module : modules) {
					install(module);
				}
				// should not be necessary: created in the controler
				//install(new ScenarioByInstanceModule(scenario));
			}
		}), overrides));
		ControlerI controler = injector.getInstance(ControlerI.class);
		controler.run();
	}


	// ******** --------- *******
	// The following is the internal interface of the Controler, which
	// is meant to be called while the Controler is running (not before)..
	// ******** --------- *******

	@Override
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
    @Override
	public final Provider<TripRouter> getTripRouterProvider() {
		return this.injector.getProvider(TripRouter.class);
	}
	
	@Override
	public final TravelDisutility createTravelDisutilityCalculator() {
        return getTravelDisutilityFactory().createTravelDisutility(this.injector.getInstance(TravelTime.class));
	}

	@Override
	public final LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return this.injector.getInstance(LeastCostPathCalculatorFactory.class);
	}

	@Override
	public final ScoringFunctionFactory getScoringFunctionFactory() {
		return this.injector.getInstance(ScoringFunctionFactory.class);
	}

    @Override
	public final Config getConfig() {
		return config;
	}

    @Override
	public final Scenario getScenario() {
		if (this.injectorCreated) {
			Gbl.assertNotNull(this.injector);
			return this.injector.getInstance(Scenario.class);
		} else {
			if ( scenario == null ) {
				log.error( "Trying to get Scenario before it was instanciated.");
				log.error( "When passing a config file or a config file path to the Controler constructor," );
				log.error( "Scenario will be loaded first when the run() method is invoked." );
				throw new IllegalStateException( "Trying to get Scenario before is was instanciated." );
			}
			return this.scenario;
		}
    }

	
	@Override
	public final EventsManager getEvents() {
		if (this.injector != null) {
			return this.injector.getInstance(EventsManager.class);
		} else {
			return new EventsManager() {
				@Override
				public void processEvent(Event event) {
					Controler.this.injector.getInstance(EventsManager.class).processEvent(event);
				}

				@Override
				public void addHandler(final EventHandler handler) {
					addOverridingModule(new AbstractModule() {
						@Override
						public void install() {
							addEventHandlerBinding().toInstance(handler);
						}
					});
				}

				@Override
				public void removeHandler(EventHandler handler) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void resetHandlers(int iteration) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void initProcessing() {
					throw new UnsupportedOperationException();
				}

				@Override
				public void afterSimStep(double time) {
					throw new UnsupportedOperationException();
				}

				@Override
				public void finishProcessing() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	@Override
	public final com.google.inject.Injector getInjector() {
		return this.injector;
	}

	/**
	 * @deprecated Do not use this, as it may not contain values in every
	 *             iteration
	 */
	@Override
	@Deprecated
	public final CalcLinkStats getLinkStats() {
		return this.injector.getInstance(CalcLinkStats.class);
	}

	@Override
	public final VolumesAnalyzer getVolumes() {
		return this.injector.getInstance(VolumesAnalyzer.class);
	}

	@Override
	public final ScoreStats getScoreStats() {
		return this.injector.getInstance(ScoreStats.class);
	}

    @Override
	public final TravelDisutilityFactory getTravelDisutilityFactory() {
		return this.injector.getInstance(com.google.inject.Injector.class).getInstance(Key.get(new TypeLiteral<Map<String, TravelDisutilityFactory>>(){}))
				.get(TransportMode.car);
	}

	/**
	 * @return Returns the {@link org.matsim.core.replanning.StrategyManager}
	 *         used for the replanning of plans.
	 * @deprecated -- try to use services.addPlanStrategyFactory or services.addPlanSelectoryFactory.
	 * There are cases when this does not work, which is in particular necessary if you need to re-configure the StrategyManager
	 * during the iterations, <i>and</i> you cannot do this before the iterations start.  In such cases, using this
	 * method may be ok. kai/mzilske, aug'14
	 */
	@Override
	@Deprecated // see javadoc above
	public final StrategyManager getStrategyManager() {
		return this.injector.getInstance(StrategyManager.class);
	}

	@Override
	public OutputDirectoryHierarchy getControlerIO() {
		return injector.getInstance(OutputDirectoryHierarchy.class);
	}

	@Override
	public Integer getIterationNumber() {
		return injector.getInstance(ReplanningContext.class).getIteration();
	}
	// ******** --------- *******
	// The following methods are the outer interface of the Controler. They are used
	// to set up infrastructure from the outside, before calling run().
	// ******** --------- *******

	@Override
	public void addControlerListener(final ControlerListener controlerListener) {
		addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toInstance(controlerListener);
			}
		});
	}

	public final void setScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory) {
        this.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
            		this.bindScoringFunctionFactory().toInstance(scoringFunctionFactory);
			}
        });
	}

	public final void setTerminationCriterion(final TerminationCriterion terminationCriterion) {
		this.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TerminationCriterion.class).toInstance(terminationCriterion);
			}
		});
	}

	/**
     * Allows you to set a factory for {@link org.matsim.core.router.TripRouter} instances.
     * Do this if your use-case requires custom routing logic, for instance if you
     * implement your own complex travel mode.
     * See {@link org.matsim.core.router.TripRouter} for more information and pointers to examples.
     */
	public final void setTripRouterFactory(final javax.inject.Provider<TripRouter> factory) {
        this.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
				bind(TripRouter.class).toProvider(factory);
			}
        });
	}

    public final void addOverridingModule(AbstractModule abstractModule) {
        if (this.injectorCreated) {
            throw new RuntimeException("Too late for configuring the Controler. This can only be done before calling run.");
        }
        this.overrides = AbstractModule.override(Collections.singletonList(this.overrides), abstractModule);
    }

    public final void setModules(AbstractModule... modules) {
        if (this.injectorCreated) {
            throw new RuntimeException("Too late for configuring the Controler. This can only be done before calling run.");
        }
        this.modules = Arrays.asList(modules);
    }

}
