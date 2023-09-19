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

package ch.sbb.matsim.contrib.railsim.events;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrackState;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * Event thrown when the {@link ch.sbb.matsim.contrib.railsim.qsimengine.TrackState} of a {@link Link} changes.
 */
public final class RailsimLinkStateChangeEvent extends Event implements HasLinkId, HasVehicleId {

	public static final String EVENT_TYPE = "railsimLinkStateChangeEvent";

	public static final String ATTRIBUTE_STATE = "state";
	public static final String ATTRIBUTE_TRACK = "track";

	private final Id<Link> linkId;
	private final Id<Vehicle> vehicleId;
	private final TrackState state;
	private final int track;

	public RailsimLinkStateChangeEvent(double time, Id<Link> linkId, Id<Vehicle> vehicleId, TrackState state, int track) {
		super(time);
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.state = state;
		this.track = track;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	public TrackState getState() {
		return state;
	}

	public int getTrack() {
		return track;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_STATE, this.state.toString());
		attr.put(ATTRIBUTE_TRACK, String.valueOf(track));
		return attr;
	}
}
