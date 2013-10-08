/* *********************************************************************** *
 * project: org.matsim.*
 * JointDepartureEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.utils.collections.CollectionUtils;

public class JointDepartureEvent extends Event {

	public static final String EVENT_TYPE = "jointdeparture";

	public static final String ATTRIBUTE_DEPARTURE = "departure";
	public static final String ATTRIBUTE_DRIVER = "driver";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_LINK = "linkId";
	public static final String ATTRIBUTE_PASSENGERS = "passengers";

	private final Id jointDepartureId;
	private final Id linkId;
	private final Id vehicleId;
	private final Id driverId;
	private final Set<Id> passengerIds;

	public JointDepartureEvent(final double time, final Id jointDepartureId, Id linkId, Id driverId, Id vehicleId,  
			Set<Id> passengerIds) {
		super(time);
		this.jointDepartureId = jointDepartureId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.driverId = driverId;
		this.passengerIds = passengerIds;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_DEPARTURE, this.jointDepartureId.toString());
		attr.put(ATTRIBUTE_DRIVER, this.driverId.toString());
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_PASSENGERS, CollectionUtils.idSetToString(this.passengerIds));
		return attr;
	}

	public Id getJointDepartureId() {
		return this.jointDepartureId;
	}

	public String getEventType() {
		return EVENT_TYPE;
	}

}
