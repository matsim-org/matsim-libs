/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.*;


/**
 * Fixed block where each track is exclusively reserved by one agent.
 */
final class FixedBlockResource implements RailResourceInternal {

	private static final Logger log = LogManager.getLogger(FixedBlockResource.class);

	private final Id<RailResource> id;

	/**
	 * Links belonging to this resource.
	 */
	private final List<RailLink> links;

	/**
	 * Reservations per link and per track.
	 */
	private final Map<RailLink, MobsimDriverAgent[]> tracks;

	/**
	 * Tracks drivers that have at least one reservation on any link os this resource.
	 * The link a driver used to enter the resource is stored for each.
	 */
	private final Map<MobsimDriverAgent, RailLink> reservations;

	/**
	 * Maximum number of reservations.
	 */
	private final int capacity;

	FixedBlockResource(Id<RailResource> id, List<RailLink> links) {
		this.id = id;
		this.links = links;
		this.capacity = links.stream().mapToInt(l -> l.tracks).min().orElseThrow();
		this.reservations = new HashMap<>(capacity);
		this.tracks = new HashMap<>(links.size());

		for (RailLink link : links) {
			tracks.put(link, new MobsimDriverAgent[link.tracks]);
		}
	}

	@Override
	public Id<RailResource> getId() {
		return id;
	}

	@Override
	public ResourceType getType() {
		return ResourceType.fixedBlock;
	}

	@Override
	public List<RailLink> getLinks() {
		return links;
	}

	@Override
	public int getTotalCapacity() {
		return capacity;
	}

	@Override
	public ResourceState getState(RailLink link) {
		if (reservations.isEmpty())
			return ResourceState.EMPTY;
		if (reservations.size() < capacity)
			return ResourceState.IN_USE;

		return ResourceState.EXHAUSTED;
	}

	@Override
	public boolean hasCapacity(double time, RailLink link, int track, TrainPosition position) {

		if (track >= 0)
			throw new IllegalArgumentException("Fixed block does not support choosing individual tracks.");

		// there is no need to check the individual tracks here
		if (reservations.containsKey(position.getDriver())) {
			return true;
		}

		// Can return any free track
		if (track == RailResourceManager.ANY_TRACK)
			return reservations.size() < capacity;

		assert track == RailResourceManager.ANY_TRACK_NON_BLOCKING;

		boolean samePresent = false;
		for (RailLink enterLink : reservations.values()) {
			if (enterLink == link) {
				samePresent = true;
				break;
			}
		}

		// if same direction is already used, one extra track needs to be available
		return samePresent ? reservations.size() < capacity - 1 : reservations.size() < capacity;
	}


	@Override
	public double getReservedDist(RailLink link, TrainPosition position) {
		MobsimDriverAgent[] state = tracks.get(link);
		for (MobsimDriverAgent reserved : state) {
			if (reserved == position.getDriver()) {
				return link.length;
			}
		}

		return RailResourceInternal.NO_RESERVATION;
	}

	@Override
	public double reserve(double time, RailLink link, int track, TrainPosition position) {

		if (track >= 0)
			throw new IllegalArgumentException("Fixed block does not support choosing individual tracks.");

		assert position.getDriver() != null: "Driver must not be null.";

		// store the link the driver used to enter the resource
		if (!reservations.containsKey(position.getDriver()))
			reservations.put(position.getDriver(), link);

		if (reservations.size() > capacity) {
			throw new IllegalStateException("Too many reservations. Capacity needs to be checked before calling reserve.");
		}

		MobsimDriverAgent[] state = tracks.get(link);
		for (int i = 0; i < state.length; i++) {
			if (state[i] == null) {
				state[i] = position.getDriver();
				return link.length;
			}
		}

		throw new IllegalStateException("No track was free.");
	}

	@Override
	public boolean release(RailLink link, MobsimDriverAgent driver) {

		MobsimDriverAgent[] state = tracks.get(link);
		int track = -1;
		for (int i = 0; i < state.length; i++) {
			if (state[i] == driver) {
				state[i] = null;
				track = i;
				break;
			}
		}

//		This may happen in rare cases, but is not a problem
//		if (track == -1)
//			log.warn("Driver {} released {} multiple times.", driver, link.getLinkId());

		boolean allFree = true;
		for (MobsimDriverAgent[] others : tracks.values()) {
			for (MobsimDriverAgent other : others) {
				if (other == driver) {
					allFree = false;
					break;
				}
			}
		}

		// if the driver has no more reservations, remove it from the set
		if (allFree) {
			reservations.remove(driver);
			return true;
		}

		return false;
	}

}
