/* *********************************************************************** *
 * project: org.matsim.*
 * LaneExitEvent
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.Lane;
import org.matsim.vehicles.Vehicle;


/**
 * 
 * @author dgrether
 *
 */
public final class LaneLeaveEvent extends Event {
	
	public static final String EVENT_TYPE = "left lane";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "link";
	public static final String ATTRIBUTE_LANE = "lane";

	private final Id<Vehicle> vehicleId;
	private final Id<Link> linkId;
	private final Id<Lane> laneId;

	public LaneLeaveEvent(double time, Id<Vehicle> vehicleId, Id<Link> linkId, Id<Lane> laneId) {
		super(time);
		this.laneId = laneId;
		this.vehicleId = vehicleId;
		this.linkId = linkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_LANE, this.laneId.toString());
		return attr;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<Link> getLinkId() {
		return this.linkId;
	}

	public Id<Lane> getLaneId() {
		return this.laneId;
	}
}
