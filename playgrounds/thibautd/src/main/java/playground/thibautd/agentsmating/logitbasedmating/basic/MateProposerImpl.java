/* *********************************************************************** *
 * project: org.matsim.*
 * MateProposerImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.thibautd.agentsmating.logitbasedmating.framework.MateProposer;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;

/**
 * @author thibautd
 */
public class MateProposerImpl implements MateProposer {
	private static final double EPSILON = 1E-7;
	//private static final int MAX_NUMBER_MATES = 100;
	private static final double BEE_FLY_SPEED = 20d / 3.6d; //20 km/h
	private static final double TIME_WINDOW_DRIVER = 15 * 60;
	private static final double TIME_WINDOW_PASSENGER = 30 * 60;

	private final Network network;

	// QuadTree has to be constructed for each list: keeping the
	// list corresponding to the current QuadTree allows not to
	// have to reconstruct it.
	private List<? extends TripRequest> currentMateList = null;
	private QuadTree< ? extends Wrapper<? extends TripRequest> > originQT = null;

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	public MateProposerImpl( final Network network ) {
		this.network = network;
	}

	// /////////////////////////////////////////////////////////////////////////
	// interface
	// /////////////////////////////////////////////////////////////////////////
	@Override
	public <T extends TripRequest> List<T> proposeMateList(
			final TripRequest trip,
			final List<T> allPossibleMates) throws UnhandledMatingDirectionException {
		if (trip.getTripType() != TripRequest.Type.DRIVER) {
			throw new UnhandledMatingDirectionException("can only handle affectation of passengers to drivers");
		}

		if (allPossibleMates != currentMateList) {
			constructQuadTree( allPossibleMates );
			currentMateList = allPossibleMates;
		}

		Wrapper<TripRequest> driverWrapper = new Wrapper<TripRequest>( trip );

		@SuppressWarnings("unchecked")
		Collection< Wrapper<T> > spatiallyRestrainedPossibleMates = (Collection< Wrapper<T> >)
				getSpatiallyRestrainedMates( driverWrapper );
		return getMateProposals( driverWrapper , spatiallyRestrainedPossibleMates );
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private <T extends TripRequest> void constructQuadTree( final List<T> mates ) {
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		List< Wrapper<T> > wrappers = new ArrayList< Wrapper<T> >(mates.size());
		for (T request : mates) {
			Wrapper<T> wrapper = new Wrapper<T>( request );
			wrappers.add( wrapper );

			double x = wrapper.origin.getX();
			double y = wrapper.destination.getY();

			minX = Math.min( minX , x );
			maxX = Math.max( maxX , x );
			minY = Math.min( minY , y );
			maxY = Math.max( maxY , y );
		}

		QuadTree< Wrapper<T> > quadTree =
			new QuadTree< Wrapper<T> >(
					minX + EPSILON,
					minY + EPSILON,
					maxX + EPSILON,
					maxY + EPSILON);

		for (Wrapper<T> wrapper : wrappers) {
			quadTree.put( wrapper.origin.getX() , wrapper.destination.getY() , wrapper );
		}

		originQT = quadTree;
	}

	private Collection< ? extends Wrapper<? extends TripRequest> > getSpatiallyRestrainedMates(
			final Wrapper<? extends TripRequest> driverTrip) {
		// we pick all origins belonging to the smallest circle which contains
		// the ellipse of all points reachable by agmenting trip duration by
		// at most two time windows (at departure and arrival)
		// ---------------------------------------------------------------------
		double radius = (driverTrip.distance / 2) + TIME_WINDOW_DRIVER * BEE_FLY_SPEED;
		Coord center = CoordUtils.getCenter( driverTrip.origin , driverTrip.destination );
		return originQT.get( center.getX() , center.getY() , radius );
	}

	/**
	 * Given a set of mates containing all possible mates, does the following:
	 * -removes all temporally unacceptable mates
	 * -removes all detour-unacceptable mates
	 * -returns the N bests
	 *
	 *  if several mates have the same detour value, they are sorted
	 *  by time difference.
	 */
	private <T extends TripRequest> List<T> getMateProposals(
			final Wrapper<? extends TripRequest> trip,
			final Collection< Wrapper<T> > spatiallyRestrainedMates) {
		List<T> proposals = new ArrayList<T>();

		double driverEarliestDeparture = trip.request.getDepartureTime() - TIME_WINDOW_DRIVER;
		double driverLattestArrival = trip.request.getPlanArrivalTime() + TIME_WINDOW_DRIVER;

		for (Wrapper<T> passengerWrapper : spatiallyRestrainedMates) {
			double passengerLattestArrival = passengerWrapper.request.getPlanArrivalTime() + TIME_WINDOW_PASSENGER;
			double passengerEarliestDeparture = passengerWrapper.request.getDepartureTime() - TIME_WINDOW_PASSENGER;

			if (passengerLattestArrival > driverEarliestDeparture &&
					passengerEarliestDeparture < driverLattestArrival) {
				double pickUpTravelTime = CoordUtils.calcDistance(
						trip.origin, passengerWrapper.origin) * BEE_FLY_SPEED;
				double dropOffTravelTime = CoordUtils.calcDistance(
						trip.destination, passengerWrapper.destination) * BEE_FLY_SPEED;
				double jointTravelTime = passengerWrapper.distance * BEE_FLY_SPEED;

				double earliestPossibleArrivalTimeAtPassenger =
					Math.min( passengerEarliestDeparture , driverEarliestDeparture + pickUpTravelTime )
					+ jointTravelTime;
				double lattestAcceptableArrivalTimeAtPassenger =
					Math.min( passengerLattestArrival , driverLattestArrival - dropOffTravelTime );

				if (earliestPossibleArrivalTimeAtPassenger < lattestAcceptableArrivalTimeAtPassenger) {
					proposals.add( passengerWrapper.request );
				}
			}
		}

		return proposals;
	}

	// /////////////////////////////////////////////////////////////////////////
	// helper classes
	// /////////////////////////////////////////////////////////////////////////
	private class Wrapper<T extends TripRequest> {
		public final double distance;
		public final Coord origin;
		public final Coord destination;
		public final T request;

		public Wrapper(
				final T request) {
			this.request = request;
			this.origin = network.getLinks().get(
					request.getOriginLinkId()).getCoord();
			this.destination = network.getLinks().get(
					request.getDestinationLinkId()).getCoord();
			this.distance = CoordUtils.calcDistance( origin , destination );
		}
	}
}

