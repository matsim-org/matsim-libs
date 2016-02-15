/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeEstimator.java
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
package playground.thibautd.analysis.spacetimeprismjoinabletrips;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author thibautd
 */
public class TravelTimeEstimator {
	private static final Logger log =
		Logger.getLogger(TravelTimeEstimator.class);

	private final Map<Od, DistanceAndPath> freeFlowsDistanceAndDurationsBetweenNodes = new TreeMap<Od, DistanceAndPath>();
	private final LeastCostPathCalculator shortPathAlgo;
	private final TravelTime travelTime;
	private final boolean departureIsOnStartOfLink;
	private final boolean arrivalIsOnStartOfLink;
	private long nTravelTimeEstimations = 0;
	private long nShortestPathComputations = 0;

	public TravelTimeEstimator(
			final boolean departureIsOnStartOfLink,
			final boolean arrivalIsOnStartOfLink,
			final TravelTime travelTime,
			final Network network) {
		FreespeedTravelTimeAndDisutility dis = new FreespeedTravelTimeAndDisutility( -1 , 0 , -1 );
		shortPathAlgo = new FastAStarLandmarksFactory( network, dis).createPathCalculator( network , dis , dis );
		this.departureIsOnStartOfLink = departureIsOnStartOfLink;
		this.arrivalIsOnStartOfLink = arrivalIsOnStartOfLink;
		this.travelTime = travelTime;
	}

	public final DistanceAndDuration getTravelTime(
			final double departureTime,
			final Link o,
			final Link d) {
		DistanceAndPath estimate = getTravelTimeBetweenNodes(
				o.getToNode(),
				d.getFromNode());
		double dist = estimate.distance;
		double tt = 0;

		if (departureIsOnStartOfLink) {
			dist += o.getLength();
			tt += travelTime.getLinkTravelTime( o , departureTime, null, null);
		}

		tt += estimate.getTravelTime( departureTime + tt , travelTime );

		if (!arrivalIsOnStartOfLink) {
			dist += d.getLength();
			tt += travelTime.getLinkTravelTime( d , departureTime + tt, null, null);
		}

		return new DistanceAndDuration( dist , tt );
	}

	private DistanceAndPath getTravelTimeBetweenNodes(
			final Node originNode,
			final Node destinationNode) {
		Od od = new Od( originNode.getId() , destinationNode.getId() );
		DistanceAndPath estimate = freeFlowsDistanceAndDurationsBetweenNodes.get( od ) ;

		nTravelTimeEstimations++;
		if (estimate == null) {
			nShortestPathComputations++;
			LeastCostPathCalculator.Path p = shortPathAlgo.calcLeastCostPath(
					originNode,
					destinationNode,
					0,
					null,
					null);
			estimate = new DistanceAndPath( p );
			freeFlowsDistanceAndDurationsBetweenNodes.put( od , estimate );
		}

		return estimate;
	}

	private static class DistanceAndPath {
		private final double distance;
		private final List<Link> links;
		// TODO: cache travel time per time bin if too slow

		private DistanceAndPath(final LeastCostPathCalculator.Path p) {
			double dist = 0;
			for (Link l : p.links) dist += l.getLength();
			distance = dist;
			links = p.links;
		}

		private double getTravelTime(
				final double departureTime,
				final TravelTime estimator) {
			double now = departureTime;

			for (Link l : links) {
				now += estimator.getLinkTravelTime( l , now, null, null);
			}

			return now - departureTime;
		}
	}

	public void logStats() {
		log.info(getClass().getSimpleName()+" statistics:" );
		log.info( nTravelTimeEstimations+" travel time estimations realised" );
		log.info( nShortestPathComputations+" shortest path computed" );
		log.info( "final cache size: "+freeFlowsDistanceAndDurationsBetweenNodes.size() );
	}
}

