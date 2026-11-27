/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.passenger.events;

import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.DrtStopCandidate;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;

import java.util.*;

/**
 * @author michalm
 */
public class DrtRequestSubmittedEvent extends PassengerRequestSubmittedEvent {
	public static final String EVENT_TYPE = "DrtRequest submitted";

	public static final String ATTRIBUTE_UNSHARED_RIDE_TIME = "unsharedRideTime";
	public static final String ATTRIBUTE_UNSHARED_RIDE_DISTANCE = "unsharedRideDistance";

	public static final String ATTRIBUTE_EARLIEST_DEPARTURE_TIME = "earliestDepartureTime";
	public static final String ATTRIBUTE_LATEST_PICKUP_TIME = "latestPickupTime";
	public static final String ATTRIBUTE_LATEST_DROPOFF_TIME = "latestDropoffTime";
	public static final String ATTRIBUTE_MAX_RIDE_DURATION = "maxRideDuration";

	public static final String ATTRIBUTE_ACCESS_WALK_DURATION = "accessWalkDuration";
	public static final String ATTRIBUTE_EGRESS_WALK_DURATION = "egressWalkDuration";
	public static final String ATTRIBUTE_ALTERNATIVE_ACCESS_LINKS = "alternativeAccessLinks";
	public static final String ATTRIBUTE_ALTERNATIVE_ACCESS_WALK_DURATIONS = "alternativeAccessWalkDurations";
	public static final String ATTRIBUTE_ALTERNATIVE_EGRESS_LINKS = "alternativeEgressLinks";
	public static final String ATTRIBUTE_ALTERNATIVE_EGRESS_WALK_DURATIONS = "alternativeEgressWalkDurations";

	/**
	 * A stop at which the passenger could alternatively be picked up or dropped off, with the walking duration
	 * between the passenger's origin/destination and this stop.
	 */
	public record StopCandidate(Id<Link> linkId, double walkDuration) {
	}

	/**
	 * The stop candidates of the request: the walking durations to/from the routed stops ({@link #getFromLinkId()},
	 * {@link #getToLinkId()}) and the alternative pickup and dropoff stops. Which stops were finally chosen is
	 * reported by the scheduled event.
	 */
	public record StopCandidates(double accessWalkDuration, double egressWalkDuration,
								 List<StopCandidate> alternativeAccess, List<StopCandidate> alternativeEgress) {
		public StopCandidates {
			alternativeAccess = List.copyOf(alternativeAccess);
			alternativeEgress = List.copyOf(alternativeEgress);
		}

		public static StopCandidates of(DrtStopCandidates candidates) {
			return new StopCandidates(candidates.accessWalkDuration(), candidates.egressWalkDuration(),
					toEventCandidates(candidates.alternativeAccess()), toEventCandidates(candidates.alternativeEgress()));
		}

		private static List<StopCandidate> toEventCandidates(List<DrtStopCandidate> candidates) {
			return candidates.stream().map(c -> new StopCandidate(c.link().getId(), c.walkDuration())).toList();
		}
	}

	private final double unsharedRideTime;
	private final double unsharedRideDistance;

	private final double earliestDepartureTime;
	private final double latestPickupTime;
	private final double latestDropoffTime;
	private final double maxRideDuration;

	@Nullable
	private final StopCandidates stopCandidates;

	public DrtRequestSubmittedEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds,
									Id<Link> fromLinkId, Id<Link> toLinkId, double unsharedRideTime, double unsharedRideDistance,
									double earliestDepartureTime, double latestPickupTime, double latestDropoffTime, double maxRideDuration,
									DvrpLoad load, String serializedDvrpLoad) {
		this(time, mode, requestId, personIds, fromLinkId, toLinkId, unsharedRideTime, unsharedRideDistance,
				earliestDepartureTime, latestPickupTime, latestDropoffTime, maxRideDuration, load, serializedDvrpLoad, null);
	}

	/**
	 * @param stopCandidates the stop candidates of the request, or {@code null} if it has none
	 */
	public DrtRequestSubmittedEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds,
									Id<Link> fromLinkId, Id<Link> toLinkId, double unsharedRideTime, double unsharedRideDistance,
									double earliestDepartureTime, double latestPickupTime, double latestDropoffTime, double maxRideDuration,
									DvrpLoad load, String serializedDvrpLoad, @Nullable StopCandidates stopCandidates) {
		super(time, mode, requestId, personIds, fromLinkId, toLinkId, load, serializedDvrpLoad);
		this.unsharedRideTime = unsharedRideTime;
		this.unsharedRideDistance = unsharedRideDistance;
		this.earliestDepartureTime = earliestDepartureTime;
		this.latestPickupTime = latestPickupTime;
		this.latestDropoffTime = latestDropoffTime;
		this.maxRideDuration = maxRideDuration;
		this.stopCandidates = stopCandidates;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	/**
	 * @return estimated travel time it would take to ride without any detours
	 */
	public final double getUnsharedRideTime() {
		return unsharedRideTime;
	}

	/**
	 * @return estimated distance it would take to ride without any detours
	 */
	public final double getUnsharedRideDistance() {
		return unsharedRideDistance;
	}

	public final double getEarliestDepartureTime() {
		return earliestDepartureTime;
	}

	public final double getLatestPickupTime() {
		return latestPickupTime;
	}

	public final double getLatestDropoffTime() {
		return latestDropoffTime;
	}

	public double getMaxRideDuration() {
		return maxRideDuration;
	}

	/**
	 * @return the stop candidates of the request, empty if it has none (no stop selection)
	 */
	public Optional<StopCandidates> getStopCandidates() {
		return Optional.ofNullable(stopCandidates);
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_UNSHARED_RIDE_TIME, unsharedRideTime + "");
		attr.put(ATTRIBUTE_UNSHARED_RIDE_DISTANCE, unsharedRideDistance + "");
		attr.put(ATTRIBUTE_EARLIEST_DEPARTURE_TIME, earliestDepartureTime + "");
		attr.put(ATTRIBUTE_LATEST_PICKUP_TIME, latestPickupTime + "");
		attr.put(ATTRIBUTE_LATEST_DROPOFF_TIME, latestDropoffTime + "");
		attr.put(ATTRIBUTE_MAX_RIDE_DURATION, maxRideDuration + "");
		if (stopCandidates != null) {
			attr.put(ATTRIBUTE_ACCESS_WALK_DURATION, stopCandidates.accessWalkDuration() + "");
			attr.put(ATTRIBUTE_EGRESS_WALK_DURATION, stopCandidates.egressWalkDuration() + "");
			attr.put(ATTRIBUTE_ALTERNATIVE_ACCESS_LINKS, joinLinks(stopCandidates.alternativeAccess()));
			attr.put(ATTRIBUTE_ALTERNATIVE_ACCESS_WALK_DURATIONS, joinWalkDurations(stopCandidates.alternativeAccess()));
			attr.put(ATTRIBUTE_ALTERNATIVE_EGRESS_LINKS, joinLinks(stopCandidates.alternativeEgress()));
			attr.put(ATTRIBUTE_ALTERNATIVE_EGRESS_WALK_DURATIONS, joinWalkDurations(stopCandidates.alternativeEgress()));
		}
		return attr;
	}

	public static DrtRequestSubmittedEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_MODE));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		String[] personIdsAttribute = attributes.get(ATTRIBUTE_PERSON).split(",");
		List<Id<Person>> personIds = new ArrayList<>();
		for (String person : personIdsAttribute) {
			personIds.add(Id.create(person, Person.class));
		}
		Id<Link> fromLinkId = Id.createLinkId(attributes.get(ATTRIBUTE_FROM_LINK));
		Id<Link> toLinkId = Id.createLinkId(attributes.get(ATTRIBUTE_TO_LINK));
		double unsharedRideTime = Double.parseDouble(attributes.get(ATTRIBUTE_UNSHARED_RIDE_TIME));
		double unsharedRideDistance = Double.parseDouble(attributes.get(ATTRIBUTE_UNSHARED_RIDE_DISTANCE));
		double earliestDepartureTime = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_EARLIEST_DEPARTURE_TIME, "NaN"));
		double latestPickupTime = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_LATEST_PICKUP_TIME, "NaN"));
		double latestDropoffTime = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_LATEST_DROPOFF_TIME, "NaN"));
		double maxRideDuration = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_MAX_RIDE_DURATION, "NaN"));
		String serializedLoad  = attributes.get(ATTRIBUTE_LOAD);
		return new DrtRequestSubmittedEvent(time, mode, requestId, personIds, fromLinkId, toLinkId, unsharedRideTime,
				unsharedRideDistance, earliestDepartureTime, latestPickupTime, latestDropoffTime, maxRideDuration,
			null, serializedLoad, parseStopCandidates(attributes));
	}

	// lists are comma-separated, like the person ids of the event
	private static String joinLinks(List<StopCandidate> candidates) {
		return String.join(",", candidates.stream().map(c -> c.linkId().toString()).toList());
	}

	private static String joinWalkDurations(List<StopCandidate> candidates) {
		return String.join(",", candidates.stream().map(c -> c.walkDuration() + "").toList());
	}

	@Nullable
	private static StopCandidates parseStopCandidates(Map<String, String> attributes) {
		if (!attributes.containsKey(ATTRIBUTE_ACCESS_WALK_DURATION)) {
			return null; // no stop candidates, or written before they were reported
		}
		return new StopCandidates(Double.parseDouble(attributes.get(ATTRIBUTE_ACCESS_WALK_DURATION)),
				Double.parseDouble(attributes.get(ATTRIBUTE_EGRESS_WALK_DURATION)),
				parseCandidates(attributes.get(ATTRIBUTE_ALTERNATIVE_ACCESS_LINKS),
						attributes.get(ATTRIBUTE_ALTERNATIVE_ACCESS_WALK_DURATIONS)),
				parseCandidates(attributes.get(ATTRIBUTE_ALTERNATIVE_EGRESS_LINKS),
						attributes.get(ATTRIBUTE_ALTERNATIVE_EGRESS_WALK_DURATIONS)));
	}

	private static List<StopCandidate> parseCandidates(String links, String walkDurations) {
		if (links == null || links.isEmpty()) {
			return List.of();
		}
		String[] linkIds = links.split(",");
		String[] durations = walkDurations.split(",");
		if (linkIds.length != durations.length) {
			throw new IllegalArgumentException(
					"Stop candidate links and walk durations differ in length: " + links + " / " + walkDurations);
		}
		List<StopCandidate> candidates = new ArrayList<>(linkIds.length);
		for (int i = 0; i < linkIds.length; i++) {
			candidates.add(new StopCandidate(Id.createLinkId(linkIds[i]), Double.parseDouble(durations[i])));
		}
		return candidates;
	}
}
