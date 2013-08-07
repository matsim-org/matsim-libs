/* *********************************************************************** *
 * project: org.matsim.*
 * BenchmarkPerformanceOfPassingEvents.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.EventsToScoreNoFilter;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

/**
 * To test the performance of passing arbitrary events to scoring functions.
 * Executes a mobsim iteration with two versions of EventsToScore
 * @author thibautd
 */
public class BenchmarkPerformanceOfPassingEvents {
	private static final Logger log =
		Logger.getLogger(BenchmarkPerformanceOfPassingEvents.class);

	private final static int N_TRIES = 5;

	public static void main(final String[] args) {
		final Config config = ConfigUtils.loadConfig( args[ 0 ] );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final long startWithFilter = System.nanoTime();
		final MobsimFactory mobsimFactory = new QSimFactory();
		for ( int i=0; i < N_TRIES; i++ ) {
			final EventsToScore e2s =
				new EventsToScore(
						sc,
						new CharyparNagelScoringFunctionFactory(
							config.planCalcScore(),
							sc.getNetwork() ) );
			final EventsManager events = EventsUtils.createEventsManager();
			events.addHandler( e2s );
			mobsimFactory.createMobsim( sc , events ).run();
		}
		final long endWithFilter = System.nanoTime();

		final long startNoFilter = System.nanoTime();
		for ( int i=0; i < N_TRIES; i++ ) {
			final EventsToScoreNoFilter e2s =
				new EventsToScoreNoFilter(
						sc,
						new CharyparNagelScoringFunctionFactory(
							config.planCalcScore(),
							sc.getNetwork() ) );
			final EventsManager events = EventsUtils.createEventsManager();
			events.addHandler( e2s );
			mobsimFactory.createMobsim( sc , events ).run();
		}
		final long endNoFilter = System.nanoTime();

		final double timeFilter = endWithFilter - startWithFilter;
		final double timeNoFilter = endNoFilter - startNoFilter;

		log.info( "filtering took "+(timeFilter / timeNoFilter)+" the time without filtering" );
	}
}


