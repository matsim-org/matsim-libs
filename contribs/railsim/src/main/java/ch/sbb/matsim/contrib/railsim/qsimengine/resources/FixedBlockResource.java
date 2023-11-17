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
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.*;


/**
 * Fixed block where each track is exclusively reserved by one agent.
 */
final class FixedBlockResource implements RailResourceInternal {

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
	 */
	private final Set<MobsimDriverAgent> reservations;

	/**
	 * Maximum number of reservations.
	 */
	private final int capacity;

	FixedBlockResource(Id<RailResource> id, List<RailLink> links) {
		this.id = id;
		this.links = links;
		this.capacity = links.stream().mapToInt(l -> l.tracks).min().orElseThrow();
		this.reservations = new HashSet<>(capacity);
		this.tracks = new HashMap<>(links.size());

		for (RailLink link : links) {
			tracks.put(link, new MobsimDriverAgent[link.tracks]);
		}
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
	public ResourceState getState(RailLink link) {
		if (reservations.isEmpty())
			return ResourceState.EMPTY;
		if (reservations.size() < capacity)
			return ResourceState.IN_USE;

		return ResourceState.EXHAUSTED;
	}

	@Override
	public boolean hasCapacity(RailLink link, TrainPosition position) {

		// there is no need to check the individual tracks here
		if (reservations.contains(position.getDriver())) {
			return true;
		}

		return reservations.size() < capacity;
	}

	@Override
	public double getReservedDist(RailLink link, TrainPosition position) {
		MobsimDriverAgent[] state = tracks.get(link);
		for (MobsimDriverAgent reserved : state) {
			if (reserved == position.getDriver()) {

				// If train is currently on the link, only the remaining distance is returned
				if (link.getLinkId().equals(position.getHeadLink()))
					return link.length - position.getHeadPosition();

				return link.length;
			}
		}

		return RailResourceInternal.NO_RESERVATION;
	}

	@Override
	public double reserve(RailLink link, TrainPosition position) {

		reservations.add(position.getDriver());

		if (reservations.size() > capacity) {
			throw new IllegalStateException("Too many reservations. Capacity needs to be checked before calling reserve.");
		}

		MobsimDriverAgent[] state = tracks.get(link);
		for (int i = 0; i < state.length; i++) {
			if (state[i] == null) {
				state[i] = position.getDriver();

				// departing train are initialized somewhere on a link
				if (link.getLinkId().equals(position.getHeadLink()))
					return link.length - position.getHeadPosition();

				return link.length;
			}
		}

		throw new IllegalStateException("No track was free.");
	}

	@Override
	public void release(RailLink link, MobsimDriverAgent driver) {

		MobsimDriverAgent[] state = tracks.get(link);
		int track = -1;
		for (int i = 0; i < state.length; i++) {
			if (state[i] == driver) {
				state[i] = null;
				track = i;
				break;
			}
		}

		if (track == -1)
			throw new AssertionError("Driver " + driver + " has not reserved the track.");

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
		}
	}

}
