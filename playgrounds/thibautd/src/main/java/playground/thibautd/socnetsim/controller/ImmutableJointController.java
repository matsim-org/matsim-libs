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

import org.apache.log4j.Logger;
import org.matsim.analysis.LegTimesControlerListener;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.controler.AbstractController;
import org.matsim.core.controler.ControlerUtils;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ShutdownListener;

import playground.ivt.utils.SubpopulationFilter;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.pseudoqsim.PsimAwareEventsWriter;
import playground.thibautd.socnetsim.analysis.LocatedTripsWriter;
import playground.thibautd.socnetsim.controller.listeners.DumpJointDataAtEnd;
import playground.thibautd.socnetsim.controller.listeners.JointPlansDumping;
import playground.thibautd.socnetsim.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

import java.io.File;
import java.util.Collection;

/**
 * A simple controler for the process with joint plans.
 * It does not extends the full Controler, so some features defined in it may
 * not be available, and, sadly, modules which require a Controler are not compatible.
 * @author thibautd
 */
public final class ImmutableJointController extends AbstractController {
	private final Logger logger = Logger.getLogger( ImmutableJointController.class );
	private final ControllerRegistry registry;
	private final ReplanningListener replanner;

	public ImmutableJointController(
			final ControllerRegistry registry,
			final ReplanningListener replanner) {
		checkOutputdir( registry );

		this.replanner = replanner;
		ControlerUtils.checkConfigConsistencyAndWriteToLog(
				registry.getScenario().getConfig(),
				"Complete config dump after reading the config file:");
		this.registry = registry;

		this.setupOutputDirectory(
				registry.getScenario().getConfig().controler().getOutputDirectory(),
				registry.getScenario().getConfig().controler().getRunId(),
				true);
	}

	private static void checkOutputdir(ControllerRegistry registry) {
		final String path = registry.getScenario().getConfig().controler().getOutputDirectory();
		final File file = new File( path );

		if ( !file.exists() ) return;

		if ( !file.isDirectory() ) throw new RuntimeException(
				"output directory "+path+" exists and is a regular file" );

		if ( file.list().length > 0 ) throw new RuntimeException(
				"output directory "+path+" exists and is not empty" );
	}

	public void run() {
		super.run( registry.getScenario().getConfig() );
	}

	@Override
	protected void loadCoreListeners() {
		final DumpJointDataAtEnd dumpDataAtEnd =
			new DumpJointDataAtEnd(
					registry.getScenario(),
					registry.getJointPlans(),
					getControlerIO());
		this.addControlerListener(dumpDataAtEnd);
		
		this.addControlerListener(
					registry.getScoringListener() );

		if (replanner == null) throw new NullPointerException();
		this.addCoreControlerListener( replanner );

		final PseudoSimConfigGroup psimConfig = (PseudoSimConfigGroup)
				registry.getScenario().getConfig().getModule(
						PseudoSimConfigGroup.GROUP_NAME );

		this.addCoreControlerListener(
				 new BeforeMobsimListener() {
					@Override
					public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
						if ( event.getIteration() != registry.getScenario().getConfig().controler().getLastIteration() &&
							!psimConfig.isDumpingIter( event.getIteration() ) ) return;
						stopwatch.beginOperation("dump all plans");
						logger.info("dumping plans...");
						new PopulationWriter(
							registry.getScenario().getPopulation(),
							registry.getScenario().getNetwork() ).write(
							getControlerIO().getIterationFilename(
								event.getIteration(),
								"plans.xml.gz"));
						logger.info("finished plans dump.");
						stopwatch.endOperation("dump all plans");
					}
				 } );

		this.addCoreControlerListener(
				 new JointPlansDumping(
						registry.getScenario(),
						registry.getJointPlans(),
						registry.getScenario().getConfig().controler().getFirstIteration(), 
						registry.getScenario().getConfig().controler().getWritePlansInterval(),
						getControlerIO() ) {
					@Override
					protected boolean dump(final int i) {
						return i == registry.getScenario().getConfig().controler().getLastIteration() ||
							psimConfig.isDumpingIter( i );
					}
				 });

		this.addCoreControlerListener(
				new LegTimesControlerListener(
					registry.getLegTimes(),
					getControlerIO()));
		
		this.addCoreControlerListener(
				new EventsHandling(
						registry.getEvents(),
						// registry.getScenario().getConfig().controler().getWriteEventsInterval(),
						0, // handled by the PSim config group
						registry.getScenario().getConfig().controler().getEventsFileFormats(),
						getControlerIO() ));
		final PsimAwareEventsWriter eventsWriter =
			new PsimAwareEventsWriter(
					getControlerIO(),
					registry.getScenario().getConfig().controler().getLastIteration(),
					psimConfig );
		this.addCoreControlerListener( eventsWriter );
		registry.getEvents().addHandler( eventsWriter );

		// dump data file immediately, rather than regenerate it by re-reading pop file,
		// which is rather slow.
		this.addCoreControlerListener(
				new ShutdownListener() {
					@Override
					public void notifyShutdown(final ShutdownEvent event) {
						LocatedTripsWriter.write(
								new SubpopulationFilter(
										registry.getScenario().getPopulation().getPersonAttributes(),
										null ).getPersonVersion(),
								registry.getScenario().getPopulation(),
								getControlerIO().getOutputFilename( "located_trips.csv.gz" ) );
					}
				});

	}

	@Override
	protected void runMobSim() {
		registry.getMobsimFactory().createMobsim(
				registry.getScenario(),
				registry.getEvents() ).run();
	}

	@Override
	protected void prepareForSim() {
		ControlerUtils.checkConfigConsistencyAndWriteToLog(
				registry.getScenario().getConfig(),
				"Config dump before doIterations:");

		final Iterable<GenericStrategyModule<ReplanningGroup>> modules = registry.getPrepareForSimModules();
		final Collection<ReplanningGroup> groups =
				registry.getGroupIdentifier().identifyGroups(
					registry.getScenario().getPopulation() ); 
		for ( GenericStrategyModule<ReplanningGroup> module : modules ) {
			module.handlePlans(
					registry.createReplanningContext( 0 ),
					groups );
		}
	}

	@Override
	protected boolean continueIterations(int iteration) {
		return iteration <= registry.getScenario().getConfig().controler().getLastIteration();
	}

	public final ControllerRegistry getRegistry() {
		return registry;
	}
}

