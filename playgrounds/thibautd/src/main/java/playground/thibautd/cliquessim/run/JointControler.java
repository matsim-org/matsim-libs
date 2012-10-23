/* *********************************************************************** *
 * project: org.matsim.*
 * JointControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.run;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.cliquessim.population.ScenarioWithCliques;
import playground.thibautd.cliquessim.qsim.JointQSimFactory;
import playground.thibautd.cliquessim.replanning.JointPlansReplanning;
import playground.thibautd.cliquessim.replanning.JointStrategyManager;
import playground.thibautd.cliquessim.router.JointPlanRouter;
import playground.thibautd.cliquessim.router.JointTripRouterFactory;

/**
 * Custom controler for handling clique replanning
 * @author thibautd
 */
public class JointControler extends Controler {
	private static final Logger log =
		Logger.getLogger(JointControler.class);


	/**
	 * replacement for the private super() fields.
	 */
	private PlansScoring plansScoring = null;
	//private JointPlansScoring plansScoring = null;
	//private RoadPricing roadPricing = null;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */

	/**
	 * Only constructor available, to enforce the initialization of a
	 * ScenarioWithCliques in the controler.
	 * The config has to be set in the scenario before.
	 */
	public JointControler(final ScenarioWithCliques scenario) {
		super(scenario);
	}

	/*
	 * =========================================================================
	 * overrided methods
	 * =========================================================================
	 */
	@Override
	protected void setUp() {
		super.setUp();

		ParallelPersonAlgorithmRunner.run(
				getPopulation(),
				getConfig().global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new ImportedJointRoutesChecker( getTripRouterFactory().createTripRouter() );
					}
		});
	}

	@Override
	protected void loadControlerListeners() {
		addControlerListener( new StartupListener() {
			@Override
			public void notifyStartup(final StartupEvent event) {
				setTripRouterFactory( new JointTripRouterFactory( event.getControler() ) );
			}
		});

		super.loadControlerListeners();
	}

	@Override
	protected void loadData() {
		setMobsimFactory( new JointQSimFactory() );

		// this is done in JointControlerUtils
		//ModeRouteFactory rFactory = ((PopulationFactoryImpl) getPopulation().getFactory()).getModeRouteFactory();
		//rFactory.setRouteFactory(
		//		JointActingTypes.DRIVER,
		//		new RouteFactory() {
		//			@Override
		//			public Route createRoute(
		//				final Id s,
		//				final Id e) {
		//				return new DriverRoute( s , e );
		//			}
		//		});
		//rFactory.setRouteFactory(
		//		JointActingTypes.PASSENGER,
		//		new RouteFactory() {
		//			@Override
		//			public Route createRoute(
		//				final Id s,
		//				final Id e) {
		//				return new PassengerRoute( s , e );
		//			}
		//		});
		super.loadData();
	}

	/**
	 * Same as the loadCoreListeners of the base class, excepts that it loads a
	 * JointPlanReplanning instance instead of a PlansReplanning one.
	 * This allows handling PopulationWithCliques populations.
	 * This has the drawback of breaking the getRoadPricing (final) method of
	 * the controler.
	 * {@inheritDoc}
	 * @see Controler#loadCoreListeners()
	 */
	@Override
	protected void loadCoreListeners() {
		/*
		 * The order how the listeners are added is very important! As
		 * dependencies between different listeners exist or listeners may read
		 * and write to common variables, the order is important. Example: The
		 * RoadPricing-Listener modifies the scoringFunctionFactory, which in
		 * turn is used by the PlansScoring-Listener. Note that the execution
		 * order is contrary to the order the listeners are added to the list.
		 */

		if (this.dumpDataAtEnd) {
			this.addCoreControlerListener(new DumpDataAtEnd(scenarioData, controlerIO));
		}

		this.plansScoring = new PlansScoring(this.scenarioData, this.events, controlerIO, this.scoringFunctionFactory ) ;

		this.addControlerListener(this.plansScoring);

		this.addCoreControlerListener(new JointPlansReplanning());
		this.addCoreControlerListener(new PlansDumping(this.scenarioData, this.getFirstIteration(), this.getWritePlansInterval(),
				this.stopwatch, this.controlerIO ));

		this.addCoreControlerListener(new LegTimesListener(getLegTimes(), controlerIO));
		this.addCoreControlerListener(new EventsHandling(this.events, this.getWriteEventsInterval(),
				this.getConfig().controler().getEventsFileFormats(), this.getControlerIO() ));

		loadControlerListeners();
	}

	@Override
	public PlansScoring getPlansScoring() {
		return plansScoring;
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new JointStrategyManager();
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(
			final TravelDisutility travelCosts,
			final TravelTime travelTimes) {
		TripRouterFactory tripRouterFactory = getTripRouterFactory();
		return new JointPlanRouter( tripRouterFactory.createTripRouter() );
	}

	@Override
	public TripRouterFactory getTripRouterFactory() {
		return new JointTripRouterFactory( this );
	}
}
