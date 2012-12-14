/* *********************************************************************** *
 * project: org.matsim.*
 * GroupLevelImmutableControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.controller;

import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;

import playground.thibautd.cliquessim.run.ImportedJointRoutesChecker;
import playground.thibautd.socnetsim.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;

/**
 * A simple controler for the process with joint plans.
 * It does not extends the full Controler, so some features defined in it may
 * not be available, and, sadly, modules which require a Controler are not compatible.
 * @author thibautd
 */
public class ImmutableJointController extends AbstractController {
	private final Scenario scenario;
	private final EventsManager events;

	private final TravelTimeCalculator travelTime;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final CalcLegTimes legTimes;

	private final ReplanningListener replanner;
	private final MobsimFactory mobsimFactory = new JointQSimFactory();
	private final TripRouterFactory tripRouterFactory;

	ImmutableJointController(
			final Scenario scenario,
			final ReplanningListener replanner,
			final ScoringFunctionFactory scoringFunctionFactory) {
		this.scenario = scenario;
		this.replanner = replanner;
		this.scoringFunctionFactory = scoringFunctionFactory;
		checkConfigConsistencyAndWriteToLog(
				scenario.getConfig(),
				"Complete config dump after reading the config file:");

		this.setupOutputDirectory(
				scenario.getConfig().controler().getOutputDirectory(),
				scenario.getConfig().controler().getRunId(),
				true);

		this.events = EventsUtils.createEventsManager( scenario.getConfig() );

		// some analysis utils
		this.events.addHandler(
				new VolumesAnalyzer(
					3600, 24 * 3600 - 1,
					scenario.getNetwork()));
		this.legTimes = new CalcLegTimes();
		this.events.addHandler( legTimes );
		this.travelTime =
			new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator());
		this.events.addHandler(travelTime);	

		final TravelDisutilityFactory costFactory =
			new TravelCostCalculatorFactoryImpl();
		this.tripRouterFactory = new JointTripRouterFactory(
				scenario,
				costFactory,
				travelTime,
				new AStarLandmarksFactory(
						scenario.getNetwork(), 
						costFactory.createTravelDisutility(
							travelTime,
							scenario.getConfig().planCalcScore())),
				null); // last arg: transit router factory.
	}

	public void run() {
		super.run( scenario.getConfig() );
	}

	@Override
	protected void loadCoreListeners() {
		final DumpDataAtEnd dumpDataAtEnd = new DumpDataAtEnd(scenario, controlerIO);
		this.addControlerListener(dumpDataAtEnd);
		
		this.addControlerListener( new PlansScoring(
					scenario,
					events,
					controlerIO,
					scoringFunctionFactory) );

		if (replanner == null) throw new NullPointerException();
		this.addCoreControlerListener( replanner );

		this.addCoreControlerListener(
				 new PlansDumping(
					scenario,
					scenario.getConfig().controler().getFirstIteration(), 
					scenario.getConfig().controler().getWritePlansInterval(),
					stopwatch,
					controlerIO ));

		this.addCoreControlerListener(
				new LegTimesListener(
					legTimes,
					controlerIO));
		
		this.addCoreControlerListener(
				new EventsHandling(
						(EventsManagerImpl) events,
						scenario.getConfig().controler().getWriteEventsInterval(),
						scenario.getConfig().controler().getEventsFileFormats(),
						controlerIO ));
	}

	@Override
	protected void runMobSim(int iteration) {
		mobsimFactory.createMobsim( scenario , events ).run();
	}

	@Override
	protected void prepareForSim() {
		checkConfigConsistencyAndWriteToLog(
				scenario.getConfig(),
				"Config dump before doIterations:");

		final AbstractPersonAlgorithm prepareForSim =
				new PersonPrepareForSim(
						new PlanRouter( tripRouterFactory.createTripRouter() ),
						scenario);
		final AbstractPersonAlgorithm checkJointRoutes =
				new ImportedJointRoutesChecker( tripRouterFactory.createTripRouter() );

		ParallelPersonAlgorithmRunner.run(
				scenario.getPopulation(),
				scenario.getConfig().global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public AbstractPersonAlgorithm getPersonAlgorithm() {
						return new AbstractPersonAlgorithm() {
							@Override
							public void run(final Person person) {
								checkJointRoutes.run( person );
								prepareForSim.run( person );
							}
						};
					}
		});
	}

	@Override
	protected boolean continueIterations(int iteration) {
		return iteration <= scenario.getConfig().controler().getLastIteration();
	}
}

