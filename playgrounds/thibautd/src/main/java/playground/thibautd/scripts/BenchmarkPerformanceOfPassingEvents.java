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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.EventsToScoreNoFilter;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

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
		log.info( "######################################################################" );
		log.info( "## filter " );
		log.info( "######################################################################" );
		for ( int i=0; i < N_TRIES; i++ ) {
			log.info( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			log.info( "~~ filtered try "+i );
			log.info( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final EventsToScore e2s =
				new EventsToScore(
						sc,
						new DummySfFactory() );
			final EventsManager events = EventsUtils.createEventsManager();
			events.addHandler( e2s );
			mobsimFactory.createMobsim( sc , events ).run();
		}
		final long endWithFilter = System.nanoTime();

		log.info( "######################################################################" );
		log.info( "## no filter " );
		log.info( "######################################################################" );
		final long startNoFilter = System.nanoTime();
		for ( int i=0; i < N_TRIES; i++ ) {
			log.info( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			log.info( "~~ non filtered try "+i );
			log.info( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			final EventsToScoreNoFilter e2s =
				new EventsToScoreNoFilter(
						sc,
						new DummySfFactory() );
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

class DummySfFactory implements ScoringFunctionFactory {
	@Override
	public ScoringFunction createNewScoringFunction(
			final Plan plan) {
		return new ScoringFunction() {
			// dummy sf
			double s = 0;

			@Override
			public void handleActivity(Activity activity) {
				s++;
			}

			@Override
			public void handleLeg(Leg leg) {
				s++;
			}

			@Override
			public void agentStuck(double time) {
				s++;
			}

			@Override
			public void addMoney(double amount) {
				s++;
			}

			@Override
			public void finish() {
			}

			@Override
			public double getScore() {
				return s;
			}

			@Override
			public void handleEvent(Event event) {
				// do stuff with event
				s += event.getTime();
			}
		};
	}
}
