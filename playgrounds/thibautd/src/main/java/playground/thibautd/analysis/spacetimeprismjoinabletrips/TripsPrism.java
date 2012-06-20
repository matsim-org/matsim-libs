/* *********************************************************************** *
 * project: org.matsim.*
 * TripsPrism.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * A structure which organizes trip {@link Record}s to make computation of
 * space time prisms (in the sense of car pooling potential) easy.
 * This class is just responsible from organising the {@link Record}s.
 * The classes responsible from imporiting the records should not do any computation.
 * @author thibautd
 */
public class TripsPrism {
	private static final Logger log =
		Logger.getLogger(TripsPrism.class);

	// parameters
	// TODO: import
	private final boolean departureIsOnStartOfLink = false;
	private final boolean arrivalIsOnStartOfLink = true;
	private final double maxBeeFlySpeed = 100 / 3.6;

	// values
	private final QuadTree<Record> recordsByOrigin;
	private final QuadTree<Record> recordsByDestination;
	private final Network network;

	// caches
	private final Map<Od, DistanceAndDuration> freeFlowsDistanceAndDurationsBetweenNodes = new TreeMap<Od, DistanceAndDuration>();

	private final LeastCostPathCalculator shortPathAlgo;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Initialises the structure with the trip records passed as an argument.
	 * @param trips the records to use. They can be extracted from events, plans or whatever.
	 * @param network the network to use for the space dimensions.
	 */
	public TripsPrism(
			final Collection<Record> trips,
			final Network network) {
		log.debug( "initializing prism with "+trips.size()+" records" );

		log.debug( "constructing origin quad tree" );
		recordsByOrigin = constructQuadTree(
				trips,
				new IdGetter() {
					public Id getId( final Record r ) {
						return r.getOriginLink();
					}
				},
				departureIsOnStartOfLink,
				network);
		log.debug( "the origin quad tree has "+recordsByOrigin.size()+" records" );
		log.debug( "constructing destination quad tree" );
		recordsByDestination = constructQuadTree(
				trips,
				new IdGetter() {
					public Id getId( final Record r ) {
						return r.getDestinationLink();
					}
				},
				arrivalIsOnStartOfLink,
				network);
		log.debug( "the destination quad tree has "+recordsByDestination.size()+" records" );
		this.network = network;
		FreespeedTravelTimeAndDisutility dis = new FreespeedTravelTimeAndDisutility( -1 , 0 , -1 );
		shortPathAlgo = new FastAStarLandmarksFactory( network, dis).createPathCalculator( network , dis , dis );
	}

	private static QuadTree<Record> constructQuadTree(
			final Collection<Record> trips,
			final IdGetter getter,
			final boolean useStartNode,
			final Network network) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		List<Tuple<Coord, Record>> records = new ArrayList<Tuple<Coord, Record>>();

		Map<Id, ? extends Link> links = network.getLinks();
		for (Record r : trips) {
			Coord c = useStartNode ?
				links.get( getter.getId( r ) ).getFromNode().getCoord() :
				links.get( getter.getId( r ) ).getToNode().getCoord();
			records.add( new Tuple<Coord, Record>( c , r ) );

			minX = Math.min( minX , c.getX() );
			maxX = Math.max( maxX , c.getX() );
			minY = Math.min( minY , c.getY() );
			maxY = Math.max( maxY , c.getY() );
		}

		log.debug( "minX="+minX+", minY="+minY+", maxX="+maxX+", maxY="+maxY );

		QuadTree<Record> qt = new QuadTree<Record>( minX-0.1, minY-0.1, maxX+0.1, maxY+0.1 );

		for (Tuple<Coord, Record> t : records) {
			qt.put( t.getFirst().getX(), t.getFirst().getY(), t.getSecond() );
		}

		return qt;
	}

	// /////////////////////////////////////////////////////////////////////////
	// public "prism" methods
	// /////////////////////////////////////////////////////////////////////////
	public List<PassengerRecord> getTripsInPrism(
			final Record driverTrip,
			final double maximumDetourTimeFraction,
			final double timeWindowWidth) {
		if (log.isTraceEnabled()) {
			log.trace( "~~~~~ getting passengers for record "+driverTrip.getTripId()+": " );
		}
		final double initialTravelTime = driverTrip.getArrivalTime() - driverTrip.getDepartureTime();
		double detourTime = maximumDetourTimeFraction * initialTravelTime;
		double radius = (initialTravelTime + Math.min( detourTime, timeWindowWidth )) * maxBeeFlySpeed / 2;

		Link origLink = getOriginLink( driverTrip );
		Link destLink = getDestinationLink( driverTrip );
		Coord center = getCenter( origLink.getCoord() , destLink.getCoord() );

		// restricted origins
		Collection<Record> records = getSpaceTimeBall(
				recordsByOrigin,
				center,
				radius,
				new DoubleGetter() {
					@Override
					public double getValue(final Record record) {
						return record.getDepartureTime();
					}
				},
				driverTrip.getDepartureTime() - detourTime / 2,
				driverTrip.getArrivalTime() + detourTime / 2);

		if (log.isTraceEnabled()) {
			log.trace( records.size()+" trips with origin in the ball" );
		}

		// intersect with restricted destinations
		records.retainAll(
				getSpaceTimeBall(
					recordsByDestination,
					center,
					radius,
					new DoubleGetter() {
						@Override
						public double getValue(final Record record) {
							return record.getArrivalTime();
						}
					},
					driverTrip.getDepartureTime() - detourTime / 2,
					driverTrip.getArrivalTime() + detourTime / 2));

		if (log.isTraceEnabled()) {
			log.trace( records.size()+" trips with origin and destination in the ball" );
		}

		double maxTravelTime = initialTravelTime + detourTime;
		pruneNonEllipsisElements(
				origLink.getCoord(),
				destLink.getCoord(),
				records,
				maxTravelTime);

		return pruneNonPrismElements(
				driverTrip,
				records,
				maxTravelTime,
				timeWindowWidth);
	}

	/**
	 * bee-fly based pruning.
	 * This is valid only if the max bee-fly speed is high enough,
	 * but allows to avoid routing too much.
	 */
	private void pruneNonEllipsisElements(
			final Coord driverOrigin,
			final Coord driverDestination,
			final Collection<Record> records,
			final double maxTravelTime) {
		Iterator<Record> iterator = records.iterator();

		if (log.isTraceEnabled()) {
			log.trace( "pruning ellipsis: start with "+records.size()+" records" );
		}

		double maxDistance = maxTravelTime * maxBeeFlySpeed;
		while (iterator.hasNext()) {
			double remainingDistance = maxDistance;
			Record passengerRecord = iterator.next();

			Coord passengerOrigin = getOriginLink( passengerRecord ).getCoord();
			remainingDistance -= CoordUtils.calcDistance( passengerOrigin , driverOrigin );

			if (remainingDistance < 0) {
				iterator.remove();
				continue;
			}

			Coord passengerDestination = getDestinationLink( passengerRecord ).getCoord();
			remainingDistance -= CoordUtils.calcDistance( passengerDestination , passengerOrigin );

			if (remainingDistance < 0) {
				iterator.remove();
				continue;
			}

			remainingDistance -= CoordUtils.calcDistance( passengerDestination , driverDestination );

			if (remainingDistance < 0) {
				iterator.remove();
				continue;
			}		
		}

		if (log.isTraceEnabled()) {
			log.trace( "pruning ellipsis: start with "+records.size()+" records" );
		}
	}

	private List<PassengerRecord> pruneNonPrismElements(
			final Record driverTrip,
			final Collection<Record> records,
			final double maxTravelTime,
			final double halfTimeWindow) {
		List<PassengerRecord> prism = new ArrayList<PassengerRecord>();
		Iterator<Record> iterator = records.iterator();

		if (log.isTraceEnabled()) {
			log.trace( "pruning prism: start with "+records.size()+" records" );
		}

		while (iterator.hasNext()) {
			Record passengerTrip = iterator.next();
			if (passengerTrip.getAgentId().equals( driverTrip.getAgentId() )) {
				// do not count trips of the same agent
				continue;
			}

			DistanceAndDuration access = getTravelTime(
					getOriginLink( driverTrip ),
					getOriginLink( passengerTrip ));
			DistanceAndDuration egress = getTravelTime(
					getDestinationLink( passengerTrip ),
					getDestinationLink( driverTrip ));

			// use free flow even if better estimates can be obtained from the record
			// for car trips, for consistency reasons
			DistanceAndDuration jointSection;
			
			if (passengerTrip.getEstimatedNetworkDistance() < 0) {
				jointSection = getTravelTime(
					getOriginLink( passengerTrip ),
					getDestinationLink( passengerTrip ) );
				passengerTrip.setEstimatedNetworkDistance( jointSection.distance );
				passengerTrip.setEstimatedNetworkDuration( jointSection.duration );
			}
			else {
				jointSection = new DistanceAndDuration(
						passengerTrip.getEstimatedNetworkDistance(),
						passengerTrip.getEstimatedNetworkDuration());
			}

			if (access.duration + jointSection.duration + egress.duration > maxTravelTime) {
				// detour is too important
				continue;
			}

			double timeWindow = 2 * halfTimeWindow;
			// time window for later joint departure > earlier acceptable passenger departure
			double minTimeWindow = passengerTrip.getDepartureTime() - driverTrip.getArrivalTime() +
				egress.duration + jointSection.duration;

			if (minTimeWindow > timeWindow) continue;

			minTimeWindow = Math.max(
					minTimeWindow,
					// time window for earliest possible driver arrival at dest < lattest acceptable passenger departure
					driverTrip.getDepartureTime() - passengerTrip.getArrivalTime()
						+ access.duration + jointSection.duration);

			if (minTimeWindow > timeWindow) continue;

			minTimeWindow = Math.max(
					minTimeWindow,
					// time window for the joint travel time to be acceptable for the passenger
					passengerTrip.getDepartureTime() - passengerTrip.getArrivalTime() + jointSection.duration);

			if (minTimeWindow > timeWindow) continue;

			// if we arrive here, the current record is valid: retain it.
			DistanceAndDuration direct;
			
			if (driverTrip.getEstimatedNetworkDistance() < 0) {
				direct = getTravelTime(
					getOriginLink( driverTrip ),
					getDestinationLink( driverTrip ) );
				driverTrip.setEstimatedNetworkDistance( direct.distance );
				driverTrip.setEstimatedNetworkDuration( direct.duration );
			}
			else {
				direct = new DistanceAndDuration(
						driverTrip.getEstimatedNetworkDistance(),
						driverTrip.getEstimatedNetworkDuration());
			}

			prism.add(
					new PassengerRecord(
						passengerTrip,
						driverTrip,
						direct.distance,
						direct.duration,
						access.distance + jointSection.distance + egress.distance,
						access.duration + jointSection.duration + egress.duration,
						Math.max( 0 , minTimeWindow ) / 2d));
		}

		if (log.isTraceEnabled()) {
			log.trace( "pruning prism: end with "+prism.size()+" records" );
		}

		return prism;
	}

	private Link getOriginLink( final Record r ) {
		Link l = r.getOriginLinkRef();

		if (l == null) {
			l = network.getLinks().get( r.getOriginLink() );
			r.setOriginLinkRef( l );
		} 

		return l;
	}

	private Link getDestinationLink( final Record r ) {
		Link l = r.getDestinationLinkRef();

		if (l == null) {
			l = network.getLinks().get( r.getDestinationLink() );
			r.setDestinationLinkRef( l );
		} 

		return l;
	}

	private DistanceAndDuration getTravelTime(
			final Link o,
			final Link d) {
		DistanceAndDuration estimate = getTravelTimeBetweenNodes(
				o.getToNode(),
				d.getFromNode());
		double tt = estimate.duration;
		double dist = estimate.distance;

		if (departureIsOnStartOfLink) {
			tt += o.getLength() / o.getFreespeed();
			dist += o.getLength();
		}
		if (!arrivalIsOnStartOfLink) {
			tt += d.getLength() / d.getFreespeed();
			dist += d.getLength();
		}

		return new DistanceAndDuration( dist , tt );
	}

	private DistanceAndDuration getTravelTimeBetweenNodes(
			final Node originNode,
			final Node destinationNode) {
		Od od = new Od( originNode.getId() , destinationNode.getId() );
		DistanceAndDuration estimate = freeFlowsDistanceAndDurationsBetweenNodes.get( od ) ;

		if (estimate == null) {
			LeastCostPathCalculator.Path p = shortPathAlgo.calcLeastCostPath(
					originNode,
					destinationNode,
					0,
					null,
					null);
			double tt = p.travelTime;
			double dist = 0;

			for (Link l : p.links) dist += l.getLength();

			estimate = new DistanceAndDuration( dist , tt );
			freeFlowsDistanceAndDurationsBetweenNodes.put( od , estimate );
		}

		return estimate;
	}

	private static Coord getCenter(
			final Coord coord1,
			final Coord coord2) {
		return new CoordImpl(
				(coord1.getX() + coord2.getX()) / 2,
				(coord1.getY() + coord2.getY()) / 2);
	}

	private static Collection<Record> getSpaceTimeBall(
			final QuadTree<Record> records,
			final Coord center,
			final double radius,
			final DoubleGetter orderingValueGetter,
			final double minOrderingValue,
			final double maxOrderingValue) {
		if (log.isTraceEnabled()) {
			log.trace( "getting ball with center "+center+" and radius "+radius+", min time limit "+minOrderingValue+" max time limit "+maxOrderingValue );
			log.trace( "the quad tree contains "+records.size()+" records" );
		}

		Collection<Record> spaceRestricted = records.get( center.getX() , center.getY(), radius );

		if (log.isTraceEnabled()) {
			log.trace( "the space ball contains "+spaceRestricted.size()+" records" );
		}

		// use a tree set to allow efficient computing of the intersection
		// this is valid only of no two records have the same id
		Set<Record> timeAndSpaceRestricted =
			new TreeSet<Record>(
					new Comparator<Record>() {
							@Override
							public int compare(final Record arg0, final Record arg1) {
								return arg0.getTripId().compareTo( arg1.getTripId() );
							}} );

		for (Record r : spaceRestricted) {
			double val = orderingValueGetter.getValue( r );

			if (val >= minOrderingValue && val <= maxOrderingValue) {
				timeAndSpaceRestricted.add( r );
			}
		}

		if (log.isTraceEnabled()) {
			log.trace( "the space-time ball contains "+timeAndSpaceRestricted.size()+" records" );
		}

		return timeAndSpaceRestricted;
	}

	// /////////////////////////////////////////////////////////////////////////
	// interfaces
	// /////////////////////////////////////////////////////////////////////////
	private static interface DoubleGetter {
		public double getValue( Record record );
	}

	private static interface IdGetter {
		public Id getId( Record record );
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class Od implements Comparable<Od> {
		// this is use for caching in a tree map: comparison should
		// be as efficient as possible. Storing as strings removes
		// the cost of repetidly unboxing ids without loss of information.
		private final String origin, destination;

		public Od(
				final Id origin,
				final Id destination) {
			this.origin = origin.toString();
			this.destination = destination.toString();
		}

		@Override
		public boolean equals(final Object other) {
			return other instanceof Od &&
				((Od) other).origin.equals( origin ) &&
				((Od) other).destination.equals( destination );
		}

		@Override
		public int hashCode() {
			return origin.hashCode() + 1000 * destination.hashCode();
		}

		@Override
		public final int compareTo(final Od other) {
			final int comp = origin.compareTo( other.origin );
			return comp == 0 ? destination.compareTo( other.destination ) : comp;
		}
	}

	private static class DistanceAndDuration {
		public final double distance;
		public final double duration;

		public DistanceAndDuration(
				final double distance,
				final double duration) {
			this.distance = distance;
			this.duration = duration;
		}
	}

	public static class PassengerRecord {
		private final Record passengerRecord;
		private final Record driverRecord;
		private final double directDriverDist;
		private final double directDriverFreeFlowDur;
		private final double driverJointDist;
		private final double driverJointDur;
		private final double minTimeWindow;

		private PassengerRecord(
				final Record passengerRecord,
				final Record driverRecord,
				final double directDriverDist,
				final double directDriverFreeFlowDur,
				final double driverJointDist,
				final double driverJointDur,
				final double minTimeWindow) {
			this.passengerRecord = passengerRecord;
			this.driverRecord = driverRecord;
			this.directDriverDist = directDriverDist;
			this.directDriverFreeFlowDur = directDriverFreeFlowDur;
			this.driverJointDist = driverJointDist;
			this.driverJointDur = driverJointDur;
			this.minTimeWindow = minTimeWindow;
		}

		public Record getPassengerRecord() {
			return passengerRecord;
		}

		public Record getDriverRecord() {
			return driverRecord;
		}

		public double getDirectDriverDist() {
			return directDriverDist;
		}

		public double getDirectDriverFreeFlowDur() {
			return directDriverFreeFlowDur;
		}

		public double getDriverJointDist() {
			return driverJointDist;
		}

		public double getDriverJointDur() {
			return driverJointDur;
		}

		public double getMinTimeWindow() {
			return minTimeWindow;
		}
	}
}

