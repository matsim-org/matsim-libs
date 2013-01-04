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

import java.io.File;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.LegTimesListener;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonPrepareForSim;

import playground.thibautd.cliquessim.run.ImportedJointRoutesChecker;

/**
 * A simple controler for the process with joint plans.
 * It does not extends the full Controler, so some features defined in it may
 * not be available, and, sadly, modules which require a Controler are not compatible.
 * @author thibautd
 */
public final class ImmutableJointController extends AbstractController {
	private final ControllerRegistry registry;
	private final ReplanningListener replanner;

	public ImmutableJointController(
			final ControllerRegistry registry,
			final ReplanningListener replanner) {
		if ( new File( registry.getScenario().getConfig().controler().getOutputDirectory() ).exists() ) {
			throw new RuntimeException( "output directory "+
						registry.getScenario().getConfig().controler().getOutputDirectory()
						+" already exists!" );
		}

		this.replanner = replanner;
		checkConfigConsistencyAndWriteToLog(
				registry.getScenario().getConfig(),
				"Complete config dump after reading the config file:");
		this.registry = registry;

		this.setupOutputDirectory(
				registry.getScenario().getConfig().controler().getOutputDirectory(),
				registry.getScenario().getConfig().controler().getRunId(),
				true);
	}

	public void run() {
		super.run( registry.getScenario().getConfig() );
	}

	@Override
	protected void loadCoreListeners() {
		final DumpDataAtEnd dumpDataAtEnd = new DumpDataAtEnd(registry.getScenario(), controlerIO);
		this.addControlerListener(dumpDataAtEnd);
		
		this.addControlerListener( new PlansScoring(
					registry.getScenario(),
					registry.getEvents(),
					controlerIO,
					registry.getScoringFunctionFactory()) );

		if (replanner == null) throw new NullPointerException();
		this.addCoreControlerListener( replanner );

		this.addCoreControlerListener(
				 new PlansDumping(
					registry.getScenario(),
					registry.getScenario().getConfig().controler().getFirstIteration(), 
					registry.getScenario().getConfig().controler().getWritePlansInterval(),
					stopwatch,
					controlerIO ));

		this.addCoreControlerListener(
				new LegTimesListener(
					registry.getLegTimes(),
					controlerIO));
		
		this.addCoreControlerListener(
				new EventsHandling(
						(EventsManagerImpl) registry.getEvents(),
						registry.getScenario().getConfig().controler().getWriteEventsInterval(),
						registry.getScenario().getConfig().controler().getEventsFileFormats(),
						controlerIO ));
	}

	@Override
	protected void runMobSim(int iteration) {
		registry.getMobsimFactory().createMobsim(
				registry.getScenario(),
				registry.getEvents() ).run();
	}

	@Override
	protected void prepareForSim() {
		checkConfigConsistencyAndWriteToLog(
				registry.getScenario().getConfig(),
				"Config dump before doIterations:");

		ParallelPersonAlgorithmRunner.run(
				registry.getScenario().getPopulation(),
				registry.getScenario().getConfig().global().getNumberOfThreads(),
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {
					@Override
					public PersonAlgorithm getPersonAlgorithm() {
						return new PreparePersonAlgorithm( registry );
					}
				});
	}

	@Override
	protected boolean continueIterations(int iteration) {
		return iteration <= registry.getScenario().getConfig().controler().getLastIteration();
	}

	public final ControllerRegistry getRegistry() {
		return registry;
	}

	public OutputDirectoryHierarchy getControlerIO() {
		return super.controlerIO;
	}

	private static class PreparePersonAlgorithm extends AbstractPersonAlgorithm {
		private final AbstractPersonAlgorithm prepareForSim;
		private final AbstractPersonAlgorithm checkJointRoutes;

		public PreparePersonAlgorithm(final ControllerRegistry registry) {
			prepareForSim =
				new PersonPrepareForSim(
						new PlanRouter( registry.getTripRouterFactory().createTripRouter() ),
						registry.getScenario());
			checkJointRoutes =
				new ImportedJointRoutesChecker( registry.getTripRouterFactory().createTripRouter() );
		}

		@Override
		public void run(final Person person) {
			checkJointRoutes.run( person );
			prepareForSim.run( person );
		}
	}

}

