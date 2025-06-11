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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Event that occurs when a train was re-routed.
 */
public class RailsimDetourEvent extends Event implements HasVehicleId {

	public static final String EVENT_TYPE = "railsimDetourEvent";

	private final Id<Vehicle> vehicleId;
	private final Id<Link> start;
	private final Id<Link> end;
	private final List<Id<Link>> detour;
	private final Id<TransitStopFacility> newStop;

	public RailsimDetourEvent(double time, Id<Vehicle> vehicleId, Id<Link> start, Id<Link> end, List<Id<Link>> detour,
							  Id<TransitStopFacility> newStop) {
		super(time);
		this.vehicleId = vehicleId;
		this.start = start;
		this.end = end;
		this.detour = detour;
		this.newStop = newStop;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<TransitStopFacility> getNewStop() {
		return newStop;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();

		attributes.put(HasVehicleId.ATTRIBUTE_VEHICLE, vehicleId.toString());
		attributes.put("start", start.toString());
		attributes.put("end", end.toString());
		attributes.put("detour", detour.stream().map(Object::toString).collect(Collectors.joining(",")));
		attributes.put("newStop", Objects.toString(newStop));

		return attributes;
	}
}
