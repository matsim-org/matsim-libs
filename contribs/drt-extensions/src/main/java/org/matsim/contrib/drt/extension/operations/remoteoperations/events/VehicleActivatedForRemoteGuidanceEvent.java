/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * Fired when a vehicle is activated for remote guidance, i.e. brought into service under the (aggregate) supervision
 * of the remote guidance operator pool by means of a virtual driver shift. This models the activation margin
 * (activeCount goes up) and carries <em>no</em> operator id: since there is no operator&harr;vehicle binding, a
 * supervised vehicle is not bound to any specific operator — only the aggregate capacity ceiling matters.
 *
 * @author nkuehnel / MOIA
 */
public class VehicleActivatedForRemoteGuidanceEvent extends Event {

	public static final String EVENT_TYPE = "remote guidance vehicle activated";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

	private final String mode;
	private final Id<DvrpVehicle> vehicleId;

	public VehicleActivatedForRemoteGuidanceEvent(double time, String mode, Id<DvrpVehicle> vehicleId) {
		super(time);
		this.mode = mode;
		this.vehicleId = vehicleId;
	}

	public String getMode() {
		return mode;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_VEHICLE_ID, vehicleId + "");
		return attr;
	}
}
