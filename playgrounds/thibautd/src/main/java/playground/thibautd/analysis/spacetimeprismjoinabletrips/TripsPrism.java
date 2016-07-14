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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A structure which organizes trip {@link Record}s to make computation of
 * space time prisms (in the sense of car pooling potential) easy.
 * This class is just responsible from organising the {@link Record}s.
 * The classes responsible from imporiting the records should not do any computation.
 * <br>
 * BEWARE: this class does not uses the arrival times from the event, but new ones
 * estimated consistently with the detour travel time. This avoids strange artifacts
 * for trips with unexpectedly high or low travel time, but may be difficult to interpret
 * for non-car trips! The code should be reworked if non-car trips are to be included in
 * the analysis.
 * @author thibautd
 */
public class TripsPrism {
	private static final Logger log =
		Logger.getLogger(TripsPrism.class);

	// parameters
	// TODO: import
	private final boolean departureIsOnStartOfLink = false;
	private final boolean arrivalIsOnStartOfLink = false;
	private final double maxBeeFlySpeed = 120 / 3.6;

	// values
	private final QuadTree<Record> recordsByOrigin;
	private final QuadTree<Record> recordsByDestination;
	private final Network network;

	private final TravelTimeEstimator ttEstimator;

	// /////////////////////////////////////////////////////////////////////////
	// construction
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Initialises the structure with the trip records passed as an argument.
	 * @param trips the records to use. They can be extracted from events, plans or whatever.
	 * @param travelTime the {@link TravelTime} to use to get travel time estimates.
	 * @param network the network to use for the space dimensions.
	 */
	public TripsPrism(
			final Collection<Record> trips,
			final TravelTime travelTime,
			final Network network) {
		log.debug( "initializing prism with "+trips.size()+" records" );

		log.debug( "constructing origin quad tree" );
		recordsByOrigin = constructQuadTree(
				trips,
				new IdGetter() {
					@Override
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
					@Override
					public Id getId( final Record r ) {
						return r.getDestinationLink();
					}
				},
				arrivalIsOnStartOfLink,
				network);
		log.debug( "the destination quad tree has "+recordsByDestination.size()+" records" );
		this.network = network;
		this.ttEstimator = new TravelTimeEstimator(
				departureIsOnStartOfLink,
				arrivalIsOnStartOfLink,
				travelTime,
				network );
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

		Map<Id<Link>, ? extends Link> links = network.getLinks();
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
	/**
	 * Returns the trips in the space-time prism of a driver trip
	 * @param driverTrip the trip to consider as a driver
	 * @param maximumDetourTimeFraction the maximum fraction of the direct travel time to allow as a detour
	 * @param timeWindowWidth the time window width
	 * @return the potential passenger trips
	 */
	public List<PassengerRecord> getTripsInPrism(
			final Record driverTrip,
			final double maximumDetourTimeFraction,
			final double timeWindowWidth) {
		if (log.isTraceEnabled()) {
			log.trace( "~~~~~ getting passengers for record "+driverTrip.getTripId()+": " );
		}
		final double driverDepartureTime = driverTrip.getDepartureTime();
		final double initialTravelTime = getEstimatedTravelTime( driverTrip );
		final double detourTime = maximumDetourTimeFraction * initialTravelTime;
		final double maxTravelTime = initialTravelTime + detourTime;
		final double radius = maxTravelTime * maxBeeFlySpeed / 2;
		final double earliestPassengerArrival = driverDepartureTime - 2 * timeWindowWidth;
		final double lattestPassengerDeparture = driverDepartureTime + initialTravelTime + 2 * timeWindowWidth;

		Link origLink = getOriginLink( driverTrip );
		Link destLink = getDestinationLink( driverTrip );
		Coord center = getCenter( origLink.getCoord() , destLink.getCoord() );

		// restricted origins
		Collection<Record> records = getSpaceTimeBall(
				recordsByOrigin,
				center,
				radius,
				earliestPassengerArrival,
				lattestPassengerDeparture);

		if (log.isTraceEnabled()) {
			log.trace( records.size()+" trips with origin in the ball" );
		}

		// intersect with restricted destinations
		records.retainAll(
				getSpaceTimeBall(
					recordsByDestination,
					center,
					radius,
					earliestPassengerArrival,
					lattestPassengerDeparture));

		if (log.isTraceEnabled()) {
			log.trace( records.size()+" trips with origin and destination in the ball" );
		}

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
			remainingDistance -= CoordUtils.calcEuclideanDistance( passengerOrigin , driverOrigin );

			if (remainingDistance < 0) {
				iterator.remove();
				continue;
			}

			Coord passengerDestination = getDestinationLink( passengerRecord ).getCoord();
			remainingDistance -= CoordUtils.calcEuclideanDistance( passengerDestination , passengerOrigin );

			if (remainingDistance < 0) {
				iterator.remove();
				continue;
			}

			remainingDistance -= CoordUtils.calcEuclideanDistance( passengerDestination , driverDestination );

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
		final double driverDepartureTime = driverTrip.getDepartureTime();
		final double driverDirectTravelTime = getEstimatedTravelTime( driverTrip );
		final double driverArrivalTime = driverDepartureTime + driverDirectTravelTime;
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
			// XXX: uses car tt for all modes! avoids strange results for car mode,
			// but my be more difficult to interpret for non-car modes!
			final double passengerArrivalTime = passengerTrip.getDepartureTime()
					+ getEstimatedTravelTime( passengerTrip );

			double now = driverDepartureTime;
			DistanceAndDuration access = ttEstimator.getTravelTime(
					now,
					getOriginLink( driverTrip ),
					getOriginLink( passengerTrip ));
			now += access.duration;

			if (access.duration > maxTravelTime) continue;

			DistanceAndDuration jointSection = new DistanceAndDuration(
						getEstimatedDistance( passengerTrip ),
						getEstimatedTravelTime( passengerTrip ));
			now += jointSection.duration;

			if (access.duration + jointSection.duration > maxTravelTime) {
				continue;
			}

			DistanceAndDuration egress = ttEstimator.getTravelTime(
					now,
					getDestinationLink( passengerTrip ),
					getDestinationLink( driverTrip ));

			if (access.duration + jointSection.duration + egress.duration > maxTravelTime) {
				// detour is too important
				continue;
			}

			double timeWindow = 2 * halfTimeWindow;
			// time window necessary for the driver to be able to perform his trip
			double minTimeWindow = access.duration + jointSection.duration +
				egress.duration - driverDirectTravelTime;

			if (minTimeWindow > timeWindow) continue;

			// time window for later joint departure > earlier acceptable passenger departure
			minTimeWindow = Math.max(
					minTimeWindow,
					passengerTrip.getDepartureTime() - driverArrivalTime +
						egress.duration + jointSection.duration);

			if (minTimeWindow > timeWindow) continue;

			minTimeWindow = Math.max(
					minTimeWindow,
					// time window for earliest possible driver arrival at dest < lattest acceptable passenger departure
					driverDepartureTime - passengerArrivalTime
						+ access.duration + jointSection.duration);

			if (minTimeWindow > timeWindow) continue;

			minTimeWindow = Math.max(
					minTimeWindow,
					// time window for the joint travel time to be acceptable for the passenger
					passengerTrip.getDepartureTime() - passengerArrivalTime + jointSection.duration);

			if (minTimeWindow > timeWindow) continue;

			// if we arrive here, the current record is valid: retain it.
			DistanceAndDuration direct;
			
			if (driverTrip.getEstimatedNetworkDistance() < 0) {
				direct = ttEstimator.getTravelTime(
						driverTrip.getDepartureTime(),
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
						access.distance,
						jointSection.distance,
						egress.distance,
						access.duration,
						jointSection.duration,
						egress.duration,
						minTimeWindow / 2d));
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

	private static Coord getCenter(
			final Coord coord1,
			final Coord coord2) {
		return new Coord((coord1.getX() + coord2.getX()) / 2, (coord1.getY() + coord2.getY()) / 2);
	}

	private Collection<Record> getSpaceTimeBall(
			final QuadTree<Record> records,
			final Coord center,
			final double radius,
			final double earliestArrival,
			final double lattestDeparture) {
		if (log.isTraceEnabled()) {
			log.trace( "getting ball with center "+center+" and radius "+radius+", earliest arrival "+earliestArrival+" lattest departure "+lattestDeparture );
			log.trace( "the quad tree contains "+records.size()+" records" );
		}

		Collection<Record> spaceRestricted = records.getDisk(center.getX(), center.getY(), radius);

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
			final double dep = r.getDepartureTime();
			final double arr = dep + getEstimatedTravelTime( r );
			if (dep < lattestDeparture && arr > earliestArrival) {
				timeAndSpaceRestricted.add( r );
			}
		}

		if (log.isTraceEnabled()) {
			log.trace( "the space-time ball contains "+timeAndSpaceRestricted.size()+" records" );
		}

		return timeAndSpaceRestricted;
	}

	private double getEstimatedTravelTime(final Record r) {
		if (r.getEstimatedNetworkDistance() < 0) {
			DistanceAndDuration ds = ttEstimator.getTravelTime(
					r.getDepartureTime(),
					getOriginLink( r ),
					getDestinationLink( r ) );
			r.setEstimatedNetworkDistance( ds.distance );
			r.setEstimatedNetworkDuration( ds.duration );
			return ds.duration;
		}
		else {
			return r.getEstimatedNetworkDuration();
		}
	}

	private double getEstimatedDistance(final Record r) {
		if (r.getEstimatedNetworkDistance() < 0) {
			DistanceAndDuration ds = ttEstimator.getTravelTime(
					r.getDepartureTime(),
					getOriginLink( r ),
					getDestinationLink( r ) );
			r.setEstimatedNetworkDistance( ds.distance );
			r.setEstimatedNetworkDuration( ds.duration );
			return ds.distance;
		}
		else {
			return r.getEstimatedNetworkDistance();
		}
	}
 
	public void logStats() {
		ttEstimator.logStats();
	}

	// /////////////////////////////////////////////////////////////////////////
	// interfaces/classes
	// /////////////////////////////////////////////////////////////////////////
	private static interface IdGetter {
		public Id getId( Record record );
	}

	public static class PassengerRecord {
		private final Record passengerRecord;
		private final Record driverRecord;
		private final double directDriverDist;
		private final double directDriverFreeFlowDur;
		private final double driverAccessDist;
		private final double driverJointDist;
		private final double driverEgressDist;
		private final double driverAccessDur;
		private final double driverJointDur;
		private final double driverEgressDur;
		private final double minTimeWindow;

		private PassengerRecord(
				final Record passengerRecord,
				final Record driverRecord,
				final double directDriverDist,
				final double directDriverFreeFlowDur,
				final double driverAccessDist,
				final double driverJointDist,
				final double driverEgressDist,
				final double driverAccessDur,
				final double driverJointDur,
				final double driverEgressDur,
				final double minTimeWindow) {
			this.passengerRecord = passengerRecord;
			this.driverRecord = driverRecord;
			this.directDriverDist = directDriverDist;
			this.directDriverFreeFlowDur = directDriverFreeFlowDur;
			this.driverAccessDist = driverAccessDist;
			this.driverJointDist = driverJointDist;
			this.driverEgressDist = driverEgressDist;
			this.driverAccessDur = driverAccessDur;
			this.driverJointDur = driverJointDur;
			this.driverEgressDur = driverEgressDur;
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

		public double getDriverAccessDist() {
			return driverAccessDist;
		}

		public double getDriverEgressDist() {
			return driverEgressDist;
		}

		public double getDriverAccessDur() {
			return driverAccessDur;
		}

		public double getDriverEgressDur() {
			return driverEgressDur;
		}
	}
}

