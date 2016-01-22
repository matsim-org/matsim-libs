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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.ivt.utils.ConcurrentStopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Compares performance of transit routing with and without A Star.
 * <br/>
 * Improvement seems to depend on the transit schedule. For C. Dobler's full Switzerland (very heavyweight, lots of transfer links),
 * A Star performs worse. For P. Bösch full Switzerland (somehow much lighter), A Star is 25% faster.
 * <br/>
 * More details for P. Bösch full switzerland for 1000 random ODs:
 * <ul>
 *     <li>overdo 1, 16 degree landmarks: 25% faster</li>
 *     <li>overdo 1.25, 16 degree landmarks:  32% faster</li>
 *     <li>overdo 1.25, 16 degree landmarks, without "smart" per query landmark activation:  34% faster</li>
 *     <li>overdo 1, 32 degree landmarks:  ??? faster</li>
 *     <li>overdo 1.25, 32 degree landmarks:  26% faster</li>
 * </ul>
 *
 * <br/>
 * More details for P. Bösch full switzerland for 1000 random trips from the Mikrozensus:
 * <ul>
 *     <li>overdo 1, 16 degree landmarks:  0% faster</li>
 *     <li>overdo 1.25, 16 degree landmarks:  50% faster</li>
 *     <li>overdo 1, 32 degree landmarks:  20% faster</li>
 *     <li>overdo 1, 64 degree landmarks:  10% faster</li>
 *     <li>overdo 1.1, 32 degree landmarks:  20% faster</li>
 *     <li>overdo 1.25, 32 degree landmarks:  30% faster</li>
 *     <li>overdo 1.25, 16 pie slice landmarks:  8% faster</li>
 * </ul>
 *
 * @author thibautd
 */
public class RunPerformanceComparison {

	private static final int N_TRIES = 1000;
	private enum Algo { classic, AStar; }

	public static void main( final String... args ) {
		final Config config = ConfigUtils.loadConfig( args[ 0 ] );
		final Scenario sc = ScenarioUtils.loadScenario( config );

		final TransitRouterAStar testee = new TransitRouterAStar( config , sc.getTransitSchedule() );
		final TransitRouter reference = new TransitRouterImpl( new TransitRouterConfig( config ) , sc.getTransitSchedule() );

		final ConcurrentStopWatch<Algo> stopWatch = new ConcurrentStopWatch<>( Algo.class );

		if ( config.plans().getInputFile() == null ) {
			measureForRandomODs( sc, testee, reference, stopWatch );
		}
		else {
			measureForPopulation( sc, testee, reference, stopWatch );
		}

		stopWatch.printStats( TimeUnit.MILLISECONDS );
		stopWatch.printStats( TimeUnit.SECONDS );
	}

	private static void measureForPopulation(
			final Scenario sc,
			final TransitRouterAStar testee,
			final TransitRouter reference,
			final ConcurrentStopWatch<Algo> stopWatch ) {
		final StageActivityTypes stages = new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE );
		final List<TripStructureUtils.Trip> trips = new ArrayList<>();

		final Counter planCounter = new Counter( "extract trips from plan # " );
		for ( Person person : sc.getPopulation().getPersons().values() ) {
			if ( person.getPlans().isEmpty() ) {
				continue;
			}
			planCounter.incCounter();
			trips.addAll(
					 TripStructureUtils.getTrips(
							 person.getSelectedPlan(),
							 stages ) );
		}

		final Counter counter = new Counter( "Compute route for random trip # " );
		final Random random = new Random( 20151217 );
		for ( int i = 0; i < N_TRIES; i++ ) {
			final TripStructureUtils.Trip trip = trips.get( random.nextInt( trips.size() ) );

			counter.incCounter();

			stopWatch.startMeasurement( Algo.AStar );
			testee.calcRoute(
					trip.getOriginActivity().getCoord(),
					trip.getDestinationActivity().getCoord(),
					trip.getOriginActivity().getEndTime(),
					null );
			stopWatch.endMeasurement( Algo.AStar );

			stopWatch.startMeasurement( Algo.classic );
			reference.calcRoute(
					trip.getOriginActivity().getCoord(),
					trip.getDestinationActivity().getCoord(),
					trip.getOriginActivity().getEndTime(),
					null );
			stopWatch.endMeasurement( Algo.classic );
		}
		counter.printCounter();
	}

	private static void measureForRandomODs( Scenario sc,
			TransitRouterAStar testee,
			TransitRouter reference,
			ConcurrentStopWatch<Algo> stopWatch ) {
		final List<Id<TransitStopFacility>> facilityIds = new ArrayList<>( sc.getTransitSchedule().getFacilities().keySet() );
		Collections.sort( facilityIds );

		final Random random = new Random( 20151210 );
		final Counter counter = new Counter( "Compute route for random case # " );
		for ( int i = 0; i < N_TRIES; i++ ) {
			final TransitStopFacility orign =
					sc.getTransitSchedule().getFacilities().get(
							facilityIds.get(
									random.nextInt( facilityIds.size() ) ) );

			final TransitStopFacility destination =
					sc.getTransitSchedule().getFacilities().get(
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
	}
}
