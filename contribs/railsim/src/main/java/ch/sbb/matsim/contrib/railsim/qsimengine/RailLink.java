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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Rail links which can has multiple tracks and corresponds to exactly one link.
 */
public final class RailLink implements HasLinkId {

	private final Id<Link> id;

	/**
	 * States per track.
	 */
	private final TrackState[] state;

	private final boolean isEntryLink;
	private final boolean isExitLink;

	/**
	 * Drivers of each blocked track.
	 */
	private final MobsimDriverAgent[] blocked;

	final double length;
	final double freeSpeed;
	final double minimumHeadwayTime;

	/**
	 * ID of the resource this link belongs to.
	 */
	@Nullable
	final Id<RailResource> resource;

	public RailLink(Link link) {
		id = link.getId();
		state = new TrackState[RailsimUtils.getTrainCapacity(link)];
		Arrays.fill(state, TrackState.FREE);
		blocked = new MobsimDriverAgent[state.length];
		length = link.getLength();
		freeSpeed = link.getFreespeed();
		minimumHeadwayTime = RailsimUtils.getMinimumHeadwayTime(link);
		String resourceId = RailsimUtils.getResourceId(link);
		resource = resourceId != null ? Id.create(resourceId, RailResource.class) : null;
		isEntryLink = RailsimUtils.isEntryLink(link);
		isExitLink = RailsimUtils.isExitLink(link);
	}

	@Override
	public Id<Link> getLinkId() {
		return id;
	}

	@Nullable
	public Id<RailResource> getResourceId() {
		return resource;
	}

	/**
	 * Number of tracks on this link.
	 */
	public int getNumberOfTracks() {
		return state.length;
	}

	/**
	 * Returns the allowed freespeed, depending on the context, which is given via driver.
	 */
	public double getAllowedFreespeed(MobsimDriverAgent driver) {
		return Math.min(freeSpeed, driver.getVehicle().getVehicle().getType().getMaximumVelocity());
	}

	/**
	 * Check if driver has already reserved this link.
	 */
	public boolean isBlockedBy(MobsimDriverAgent driver) {
		for (MobsimDriverAgent reservation : blocked) {
			if (reservation == driver)
				return true;
		}
		return false;
	}

	/**
	 * Whether at least one track is free.
	 */
	boolean hasFreeTrack() {
		for (TrackState trackState : state) {
			if (trackState == TrackState.FREE)
				return true;
		}
		return false;
	}

	/**
	 * Block a track that was previously reserved.
	 */
	int blockTrack(MobsimDriverAgent driver) {
		for (int i = 0; i < state.length; i++) {
			if (state[i] == TrackState.FREE) {
				blocked[i] = driver;
				state[i] = TrackState.BLOCKED;
				return i;
			}
		}
		throw new IllegalStateException("No track was free.");
	}

	/**
	 * Release a non-free track to be free again.
	 */
	int releaseTrack(MobsimDriverAgent driver) {
		for (int i = 0; i < state.length; i++) {
			if (blocked[i] == driver) {
				state[i] = TrackState.FREE;
				blocked[i] = null;
				return i;
			}
		}
		throw new AssertionError("Driver " + driver + " has not reserved the track.");
	}


	/**
	 * Entry link of a station relevant for re-routing.
	 */
	public boolean isEntryLink() {
		return isEntryLink;
	}

	/**
	 * Exit link of a station for re-routing.
	 */
	public boolean isExitLink() {
		return isExitLink;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RailLink link = (RailLink) o;
		return Objects.equals(id, link.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "RailLink{" +
			"id=" + id +
			", resource=" + resource +
			'}';
	}
}
