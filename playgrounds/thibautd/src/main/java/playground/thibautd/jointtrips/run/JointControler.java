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
package playground.thibautd.jointtrips.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.ScenarioWithCliques;
import playground.thibautd.jointtrips.qsim.JointQSimFactory;
import playground.thibautd.jointtrips.replanning.JointPlansReplanning;
import playground.thibautd.jointtrips.replanning.JointStrategyManager;
import playground.thibautd.jointtrips.router.DriverRoutingModule;
import playground.thibautd.jointtrips.router.DriverRoutingModuleFactory;
import playground.thibautd.jointtrips.router.JointPlanRouter;
import playground.thibautd.jointtrips.router.PassengerRoutingModule;
import playground.thibautd.jointtrips.router.PassengerRoutingModuleFactory;
import playground.thibautd.router.RoutingModule;
import playground.thibautd.router.RoutingModuleFactory;
import playground.thibautd.router.TripRouterFactory;
import playground.thibautd.router.controler.MultiLegRoutingControler;

/**
 * Custom controler for handling clique replanning
 * @author thibautd
 */
public class JointControler extends MultiLegRoutingControler {
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


		this.plansScoring = new PlansScoring(this.scenarioData, this.events, controlerIO, this.scoringFunctionFactory ) ;

		this.addControlerListener(this.plansScoring);

		// load road pricing, if requested
		if (this.config.scenario().isUseRoadpricing()) {
			//this.roadPricing = new RoadPricing(); //XXX roadProcing is private!
			//this.addCoreControlerListener(this.roadPricing);
			this.addCoreControlerListener(new RoadPricing());
			log.warn("RoadPricing set in JointControler: getRoadPricing will be"
					+" broken.");
		}

		this.addCoreControlerListener(new JointPlansReplanning());
		this.addCoreControlerListener(new PlansDumping());

		this.addCoreControlerListener(new EventsHandling(this.events));
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
	public TripRouterFactory getTripRouterFactory() {
		TripRouterFactory routerFactory = super.getTripRouterFactory();

		routerFactory.setRoutingModuleFactory(
				JointActingTypes.PASSENGER,
				new PassengerRoutingModuleFactory( getPopulation().getFactory() ));
		routerFactory.setRoutingModuleFactory(
				JointActingTypes.DRIVER,
				new DriverRoutingModuleFactory( getPopulation().getFactory() ));

		return routerFactory;
	}
	
	//@Override
	//public PlanAlgorithm createRoutingAlgorithm() {
	//	 return createRoutingAlgorithm(
	//			 this.createTravelCostCalculator(),
	//			 this.getTravelTimeCalculator());
	//}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(
			final TravelDisutility travelCosts,
			final TravelTime travelTimes) {
		PlansCalcRoute plansCalcRoute = null;

		TripRouterFactory tripRouterFactory = getTripRouterFactory();
		//plansCalcRoute = new PlanRouterWrapper(
		//		getConfig().plansCalcRoute(),
		//		getNetwork(),
		//		travelCosts,
		//		travelTimes,
		//		getLeastCostPathCalculatorFactory(),
		//		tripRouterFactory.getModeRouteFactory(),
		//		tripRouterFactory,
		//		new JointPlanRouter( tripRouterFactory.createTripRouter() ));

		//return plansCalcRoute;
		return new JointPlanRouter( tripRouterFactory.createTripRouter() );
	}

	///**
	// * Exports plans in an importable format
	// */
	//@Override
	//protected void shutdown(final boolean unexpected) {
	//	super.shutdown(unexpected);

	//	ControlerIO io = getControlerIO();
	//	if (io != null) {
	//		PopulationWriter popWriter = new PopulationWriter(this.population, this.network, (this.getScenario()).getKnowledges());
	//		popWriter.setWriterHandler(new PopulationWithJointTripsWriterHandler(this.network,(this.getScenario()).getKnowledges()));

	//		popWriter.write(io.getOutputFilename(FILENAME_POPULATION));
	//	}
	//}
}
