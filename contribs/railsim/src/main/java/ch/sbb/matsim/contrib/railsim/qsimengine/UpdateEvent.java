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

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;

import java.util.Objects;

/**
 * Internal event that describes, when a {@link TrainState} is supposed to be updated.
 */
final class UpdateEvent implements Comparable<UpdateEvent> {

	final TrainState state;
	double plannedTime;
	Type type;

	/**
	 * Timestamp when next reservation will be checked.
	 */
	double checkReservation = -1;

	/**
	 * Whether train is waiting on the very link end for the next to be unblocked.
	 */
	boolean waitingForLink;

	/**
	 * Stores a link that is to be released.
	 */
	final RailLink unblockLink;

	UpdateEvent(TrainState state, Type type) {
		this.state = state;
		this.plannedTime = state.timestamp;
		this.type = type;
		this.waitingForLink = false;
		this.unblockLink = null;
	}

	/**
	 * Update event to unblock a link after minimum headway time.
	 */
	UpdateEvent(TrainState state, RailLink unblockLink, double time) {
		this.state = state;
		this.unblockLink = unblockLink;
		this.plannedTime = time + unblockLink.minimumHeadwayTime;
		this.type = Type.UNBLOCK_LINK;
	}

	@Override
	public int compareTo(UpdateEvent o) {
		int compare = Double.compare(plannedTime, o.plannedTime);

		if (compare == 0)
			return state.driver.getId().compareTo(o.state.driver.getId());

		return compare;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UpdateEvent that = (UpdateEvent) o;
		return Objects.equals(state, that.state);
	}

	@Override
	public int hashCode() {
		return Objects.hash(state);
	}

	/**
	 * This train currently waits for a reservation for blocked tracks.
	 */
	boolean isAwaitingReservation() {
		return checkReservation >= 0;
	}

	/**
	 * The type of the requested update.
	 */
	enum Type {

		IDLE,
		DEPARTURE,
		POSITION,
		ENTER_LINK,
		LEAVE_LINK,
		BLOCK_TRACK,
		WAIT_FOR_RESERVATION,
		SPEED_CHANGE,
		UNBLOCK_LINK

	}

}
