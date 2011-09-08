/* *********************************************************************** *
 * project: org.matsim.*
 * JoinableTrips.java
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
package playground.thibautd.analysis.joinabletripsidentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Final data structure of the event analysis, containing information
 * on the trips and the possible "joinable" trips.
 * @author thibautd
 */
public class JoinableTrips {
	private static final Log log =
		LogFactory.getLog(JoinableTrips.class);

	private final double distanceRadius;
	private final double timeRadius;

	private final Map<Id, TripRecord> tripRecords = new HashMap<Id, TripRecord>();

	// /////////////////////////////////////////////////////////////////////////
	// constructor
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * "Computing" constructor. Given a {@link TripReconstructor} with trip
	 * information and thresholds, it identifies the joinable trips and stores them.
	 *
	 * @param distanceRadius maximum acceptable walk distance
	 * @param timeRadius maximum delay time (ie maximum earlier departure time
	 * and/or later arrival time)
	 */
	public JoinableTrips(
			final double distanceRadius,
			final double timeRadius,
			final TripReconstructor tripReconstructor) {
		this.distanceRadius = distanceRadius;
		this.timeRadius = timeRadius;

		// externalize to an "identifier"?
		identifyJoinableTrips(tripReconstructor);
	}

	// TODO: constructor "from file"
	
	// /////////////////////////////////////////////////////////////////////////
	// Joinable trip identification methods
	// /////////////////////////////////////////////////////////////////////////
	private void identifyJoinableTrips(final TripReconstructor tripReconstructor) {
		JoinableTripMap joinableTrips = getJoinableTripMap(tripReconstructor);
		makeDataBase(joinableTrips, tripReconstructor);
	}

	private JoinableTripMap getJoinableTripMap(
			final TripReconstructor tripReconstructor) {
		List<Trip> trips = tripReconstructor.getTrips();
		JoinableTripMap joinableTrips = new JoinableTripMap(); 
		QuadTree<LinkInformation> linkInformation = tripReconstructor.getLinkInformationQuadTree();
		Map<Id, ? extends Link> links = tripReconstructor.getNetwork().getLinks();

		// trip examination to identify joinable trips
		Coord currentCoord;
		Collection<LinkInformation> neighbourInformations;
		List<LinkInformation.Entry> entries;
		JoinableTrip currentJoinableTrip;
		Id driverTripId;
		JointTripCounter counter = new JointTripCounter(trips.size());

		// FIXME: a lot of copy-paste here. try to "externalise" the common procedure
		for ( Trip trip : trips ) {
			counter.logCount();
			driverTripId = trip.getId();
			for ( LinkEvent event : trip.getRouteEvents() ) {
				if ( event instanceof LinkLeaveEvent ) {
					// for departures
					double eventTime = event.getTime();

					currentCoord = links.get(event.getLinkId()).getCoord();
					neighbourInformations = linkInformation.get(
							currentCoord.getX(), 
							currentCoord.getY(),
							distanceRadius);

					for (LinkInformation info : neighbourInformations) {
						entries = info.getDepartures();

						// use the fact that entries are sorted by time!
						for (LinkInformation.Entry entry : entries) {
							if ( entry.getTripId().equals(driverTripId) ) continue;
							if ( (entry.getDepartureTime() - timeRadius < eventTime) &&
									// arrival time or latest arrival time?
									(Math.max( entry.getArrivalTime(), entry.getDepartureTime() + timeRadius ) > eventTime) ) {
								currentJoinableTrip = joinableTrips.get(
										driverTripId,
										entry.getTripId());
								currentJoinableTrip.addPassage(
										Passage.Type.pickUp,
										CoordUtils.calcDistance(currentCoord, info.getCoord()),
										eventTime - entry.getDepartureTime());
							}
						}
					}
				}
				else if ( event instanceof LinkEnterEvent ) {
					// for arrivals
					double eventTime = event.getTime();

					currentCoord = links.get(event.getLinkId()).getCoord();
					neighbourInformations = linkInformation.get(
							currentCoord.getX(), 
							currentCoord.getY(),
							distanceRadius);

					for (LinkInformation info : neighbourInformations) {
						entries = info.getArrivals();

						// use the fact that entries are sorted by time!
						for (LinkInformation.Entry entry : entries) {
							if ( entry.getTripId().equals(driverTripId) ) continue;
							if ( (entry.getArrivalTime() + timeRadius > eventTime) &&
									// departure time or sooner departure time ?
									(Math.min( entry.getDepartureTime(), entry.getArrivalTime() - timeRadius ) < eventTime) ) {
								currentJoinableTrip = joinableTrips.get(
										driverTripId,
										entry.getTripId());
								currentJoinableTrip.addPassage(
										Passage.Type.dropOff,
										CoordUtils.calcDistance(currentCoord, info.getCoord()),
										eventTime - entry.getArrivalTime());
							}
						}
					}
				}
			}
		}

		return joinableTrips;

	}

	private void makeDataBase(
			final JoinableTripMap joinableTrips,
			final TripReconstructor tripReconstructor) {
		Map<Id, List<JoinableTrip>> tripsPerPassenger =
			joinableTrips.getJoinableTripsPerPassengerTrip();

		for (Trip trip : tripReconstructor.getTrips()) {
			List<JoinableTrip> identifiedTrips = 
				tripsPerPassenger.remove(trip.getId());
			List<JoinableTrip> validTrips =
				new ArrayList<JoinableTrip>();

			if ( identifiedTrips != null ) {
				for (JoinableTrip joinableTrip : identifiedTrips) {
					if (joinableTrip.isValidTrip()) {
						validTrips.add(joinableTrip);
					}
				}
			}

			TripRecord old = tripRecords.put(trip.getId(), new TripRecord(trip, validTrips));

			if ( old != null ) {
				throw new RuntimeException("same trip added twice");
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// accessors
	// /////////////////////////////////////////////////////////////////////////
	public Map<Id, TripRecord> getTripRecords() {
		return tripRecords;
	}

	public double getDistanceRadius() {
		return this.distanceRadius;
	}

	public double getAcceptableTimeDifference() {
		return this.timeRadius;
	}

	// /////////////////////////////////////////////////////////////////////////
	// nested classes
	// /////////////////////////////////////////////////////////////////////////
	/**
	 * Contains all pertinent data about a trip.
	 */
	public static class TripRecord implements Identifiable {
		private final Id tripId;
		private final Id agentId;
		private final String mode;

		private final Id originLinkId;
		private final String originActivityType;
		private final double departureTime;

		private final Id destinationLinkId;
		private final String destinationActivityType;
		private final double arrivalTime;

		private final int legNumber;
		private final List<JoinableTrip> joinableTrips;

		private TripRecord(
				final Trip trip,
				final List<JoinableTrip> joinableTrips) {
			this.tripId = trip.getId();
			this.agentId = trip.getAgentId();
			this.mode = trip.getMode();

			this.originLinkId = trip.getDeparture().getLinkId();
			this.originActivityType = trip.getOriginActivityEnd().getActType();
			this.departureTime = trip.getDeparture().getTime();

			this.destinationLinkId = trip.getArrival().getLinkId();
			this.destinationActivityType = trip.getDestinationActivityStart().getActType();
			this.arrivalTime = trip.getArrival().getTime();

			this.legNumber = trip.getTripNumber();

			this.joinableTrips = Collections.unmodifiableList(joinableTrips);
		}

		/**
		 * Gets the trip Id for this instance.
		 *
		 * @return The trip Id.
		 */
		@Override
		public Id getId() {
			return this.tripId;
		}

		/**
		 * Gets the agent Id for this instance.
		 *
		 * @return The agent Id.
		 */
		public Id getAgentId() {
			return this.agentId;
		}

		/**
		 * Gets the mode for this instance.
		 *
		 * @return The mode.
		 */
		public String getMode() {
			return this.mode;
		}

		/**
		 * Gets the origin Link Id for this instance.
		 *
		 * @return The origin Link Id.
		 */
		public Id getOriginLinkId() {
			return this.originLinkId;
		}

		/**
		 * Gets the origin activity type for this instance.
		 *
		 * @return The origin activity type.
		 */
		public String getOriginActivityType() {
			return this.originActivityType;
		}

		/**
		 * Gets the departure time for this instance.
		 *
		 * @return The departure time.
		 */
		public double getDepartureTime() {
			return this.departureTime;
		}

		/**
		 * Gets the destination Link Id for this instance.
		 *
		 * @return The destination Link Id.
		 */
		public Id getDestinationLinkId() {
			return this.destinationLinkId;
		}

		/**
		 * Gets the destination activity type for this instance.
		 *
		 * @return The destination activity type.
		 */
		public String getDestinationActivityType() {
			return this.destinationActivityType;
		}

		/**
		 * Gets the arrival time for this instance.
		 *
		 * @return The arrival time.
		 */
		public double getArrivalTime() {
			return this.arrivalTime;
		}

		/**
		 * Gets the leg number for this instance.
		 *
		 * @return The leg number in the agent's plan.
		 */
		public int getLegNumber() {
			return this.legNumber;
		}

		/**
		 * Gets the list of joinable trips for this instance.
		 *
		 * @return The joinable trips.
		 */
		public List<JoinableTrip> getJoinableTrips() {
			return this.joinableTrips;
		}
	}

	/**
	 * Contains information about a joinable trip: the trip Id, plus additionnal
	 * "filter" information to allow easy re-definition of thresholds.
	 * <br>
	 * This filter information corresponds to a list of all events corresponding
	 * to compatible PU or DO
	 */
	public static class JoinableTrip {
		private final Id tripId;

		private boolean hasPU = false;
		private boolean hasDO = false;

		private List<Passage> passages = new ArrayList<Passage>();

		private JoinableTrip(final Id tripId) {
			this.tripId = tripId;
		}

		private void addPassage(
				final Passage.Type type,
				final double distance,
				final double timeDifference) {
			passages.add( new Passage(type, distance, timeDifference) );

			switch ( type ) {
				case pickUp:
					hasPU = true;
					break;
				case dropOff:
					hasDO = true;
					break;
			}
		}

		public Id getTripId() {
			return this.tripId;
		}

		public List<Passage> getPassages() {
			return passages;
		}

		private boolean isValidTrip() {
			return hasPU && hasDO;
		}
	}

	public static class Passage {
		public enum Type {pickUp, dropOff};

		private final Type type;
		private final double distance;
		private final double timeDifference;

		private Passage(
				final Type type,
				final double distance,
				final double timeDifference) {
			this.type = type;
			this.distance = distance;
			this.timeDifference = timeDifference;
		}

		/**
		 * Gets the type for this instance, that is, wether this passage is in
		 * the neighbourhood of the departure of of the arrival. Passages in the
		 * neighbourhood of both departure and arrival correspond to two instances
		 * (on for each type), with different distance and time difference.
		 *
		 * @return The type.
		 */
		public Type getType() {
			return this.type;
		}

		/**
		 * Gets the distance to the point of reference for this instance.
		 * The point of reference is the origin for pickUp events 
		 * and the destination for dropOff events.
		 *
		 * the distance between two links is the distance between their unique
		 * Coord returned by link.getCoord().
		 *
		 * @return The distance.
		 */
		public double getDistance() {
			return this.distance;
		}

		/**
		 * Gets the time difference to the time of reference for this instance.
		 * The time of reference is the departure time for pickUp events 
		 * and the arrival time for dropOff events.
		 *
		 * @return The timeDifference.
		 */
		public double getTimeDifference() {
			return this.timeDifference;
		}
	}

	/**
	 * Kind of a "double key map": associates a "joinable trip"
	 * instance to any tuple of passenger trip and driver trip.
	 *
	 * The lookup is more efficient if entries for a given driver trip are
	 * examined one next to the other.
	 */
	private static class JoinableTripMap {
		private final Map<Id, Map<Id, JoinableTrip>> joinableTrips = new HashMap<Id, Map<Id, JoinableTrip>>();
		private final Map<Id, List<JoinableTrip>> joinableTripsPerPassengerTrip = new HashMap<Id, List<JoinableTrip>>();

		private Map<Id, JoinableTrip> driverMap = null;
		private Id lastDriverTrip = null;

		public JoinableTrip get(final Id driver, final Id passenger) {
			if (!driver.equals(lastDriverTrip)) {
				driverMap = joinableTrips.get(driver);
				lastDriverTrip = driver;
			}
			JoinableTrip trip = null;

			if (driverMap == null) {
				driverMap = new HashMap<Id, JoinableTrip>();
				joinableTrips.put(driver, driverMap);
			}
			else {
				trip = driverMap.get(passenger);
			}

			if (trip == null) {
				trip = new JoinableTrip(driver);
				driverMap.put(passenger, trip);

				List<JoinableTrip> passengerJoinableTrips =
					joinableTripsPerPassengerTrip.get(passenger);

				if (passengerJoinableTrips == null) {
					passengerJoinableTrips = new ArrayList<JoinableTrip>();
					joinableTripsPerPassengerTrip.put(passenger, passengerJoinableTrips);
				}

				passengerJoinableTrips.add(trip);

			}

			return trip;
		}

		/**
		 * @return a copy a the map idPassengerTrip-joinableTrips. The "joinable trips"
		 * are not cleaned yet (not all have both PU and DO)
		 */
		public Map<Id, List<JoinableTrip>> getJoinableTripsPerPassengerTrip() {
			return new HashMap<Id, List<JoinableTrip>>(joinableTripsPerPassengerTrip);
		}
	}

	private static class JointTripCounter {
		private int count = 0;
		private int next =1;
		private final int total;

		public JointTripCounter(final int total) {
			this.total = total;
		}

		public void logCount() {
			count++;
			
			if ( count == next || count == total ) {
				log.info("examining trip # "+count+"/"+total);
				next *= 2;
			}
		}
	}
}

