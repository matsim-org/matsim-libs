/* *********************************************************************** *
 * project: org.matsim.*
 * CachingLeastCostPathAlgorithmWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.router.multimodal;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import playground.ivt.utils.SoftCache;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps a least cost path algo, caching result for further use.
 * Travel times are updated before being returned.
 * It requires the shortest path to be independent of departure time and person
 * (travel times can be).
 * This is the case with the person-dependent speeds form Christoph Dobler.
 * @author thibautd
 */
public class CachingLeastCostPathAlgorithmWrapper implements LeastCostPathCalculator {
	private static final Logger log =
		Logger.getLogger(CachingLeastCostPathAlgorithmWrapper.class);

	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final LeastCostPathCalculator delegate;

	private final SoftCache<Tuple<Node, Node>, Path> cache;
	
	private static final AtomicLong computationCount = new AtomicLong( 0 );
	private static final AtomicLong cachedCount = new AtomicLong( 0 );
	
	public <T extends TravelTime & TravelDisutility> CachingLeastCostPathAlgorithmWrapper(
			final T cost,
			final LeastCostPathCalculator delegate) {
		this.cache = new SoftCache<Tuple<Node, Node>, Path>();
		this.travelTime = cost;
		this.travelDisutility = cost;
		this.delegate = delegate;
	}

	public CachingLeastCostPathAlgorithmWrapper(
			final TravelTime travelTime,
			final TravelDisutility travelDisutility,
			final LeastCostPathCalculator delegate) {
		this.cache = new SoftCache<Tuple<Node, Node>, Path>();
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.delegate = delegate;
	}

	public CachingLeastCostPathAlgorithmWrapper(
			final SoftCache<Tuple<Node, Node>, Path> cache,
			final TravelTime travelTime,
			final TravelDisutility travelDisutility,
			final LeastCostPathCalculator delegate) {
		this.cache = cache;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.delegate = delegate;
	}

	@Override
	public Path calcLeastCostPath(
			final Node fromNode,
			final Node toNode,
			final double starttime,
			final Person person,
			final Vehicle vehicle) {
		computationCount.incrementAndGet();
		final Tuple<Node, Node> od = new Tuple<Node, Node>( fromNode , toNode );
		final Path cached = cache.get( od );
		if ( cached != null ) {
			cachedCount.incrementAndGet();
			return adapt( cached , starttime , person , vehicle );
		}

		final Path routed =
			delegate.calcLeastCostPath(
					fromNode,
					toNode,
					starttime,
					person,
					vehicle );

		cache.put( od , routed );

		// we need to adapt, because it is not required that the routing algorithm
		// uses the same travel disutility as the one used for "personnalization".
		// In particular, AccessEgressMultimodalTripRouterFactory initializes the
		// router with a non-personnalizable version of the disutility, which allows
		// to use preprocessing based speedup techniques (A*).
		return adapt( routed , starttime , person , vehicle );
	}

	private Path adapt(
			final Path cached,
			final double starttime,
			final Person p,
			final Vehicle v) {
		double tt = 0;
		double tc = 0;

		for ( Link l : cached.links ) {
			tc += travelDisutility.getLinkTravelDisutility( l , starttime + tt , p , v );
			tt += travelTime.getLinkTravelTime( l , starttime + tt , p , v );
		}

		return new Path(
				cached.nodes,
				cached.links,
				tt,
				tc );
	}

	public static void logStats() {
		log.info( "CachingLeastCostPathAlgorithmWrapper stats: " );
		log.info( computationCount.get()+" route computations" );
		final double percentage = ((double) cachedCount.get()) / computationCount.get();
		log.info( cachedCount.get()+" ("+(100 * percentage)+"%) obtained from cache" );
	}
}

