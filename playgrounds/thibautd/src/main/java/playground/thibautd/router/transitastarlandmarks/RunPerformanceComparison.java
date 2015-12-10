/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.router.transitastarlandmarks;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import playground.ivt.utils.ConcurrentStopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author thibautd
 */
public class RunPerformanceComparison {

	private static final int N_TRIES = 1000;
	private enum Algo { classic, AStar; }

	public static void main( final String... args ) {
		final Config config = ConfigUtils.loadConfig( args[ 0 ] );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransitRouterConfig transitConfig = new TransitRouterConfig( config );

		final TransitRouterAStar testee = new TransitRouterAStar( transitConfig , sc.getTransitSchedule() );
		final TransitRouter reference = new TransitRouterImpl( transitConfig , sc.getTransitSchedule() );

		final List<Id<ActivityFacility>> facilityIds = new ArrayList<>( sc.getActivityFacilities().getFacilities().keySet() );
		Collections.sort( facilityIds );

		final ConcurrentStopWatch<Algo> stopWatch = new ConcurrentStopWatch<>( Algo.class );
		final Random random = new Random( 20151210 );
		final Counter counter = new Counter( "Compute route for random case # " );
		for ( int i = 0; i < N_TRIES; i++ ) {
			final ActivityFacility orign =
					sc.getActivityFacilities().getFacilities().get(
							facilityIds.get(
									random.nextInt( facilityIds.size() ) ) );

			final ActivityFacility destination =
					sc.getActivityFacilities().getFacilities().get(
							facilityIds.get(
									random.nextInt( facilityIds.size() ) ) );

			final double time = random.nextDouble() * 24 * 3600;

			counter.incCounter();

			stopWatch.startMeasurement( Algo.AStar );
			testee.calcRoute( orign.getCoord(), destination.getCoord(), time, null );
			stopWatch.endMeasurement( Algo.AStar );

			stopWatch.startMeasurement( Algo.classic );
			reference.calcRoute( orign.getCoord(), destination.getCoord(), time, null );
			stopWatch.endMeasurement( Algo.classic );
		}
		counter.printCounter();

		stopWatch.printStats( TimeUnit.MILLISECONDS );
		stopWatch.printStats( TimeUnit.SECONDS );
	}
}
