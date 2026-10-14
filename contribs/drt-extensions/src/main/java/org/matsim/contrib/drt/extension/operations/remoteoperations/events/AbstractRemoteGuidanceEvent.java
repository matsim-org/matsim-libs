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
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * Base class for events relating a remote guidance operator (identified by its operator shift id) to a supervised
 * vehicle. Together, the assigned/released events allow reconstructing operator utilization (how many of an operator's
 * capacity slots were occupied over time) and vehicle handovers between operators.
 *
 * @author nkuehnel / MOIA
 */
public abstract class AbstractRemoteGuidanceEvent extends Event {

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_OPERATOR_ID = "operator_id";
	public static final String ATTRIBUTE_VEHICLE_ID = "vehicle";

	private final String mode;
	private final Id<DrtShift> operatorId;
	private final Id<DvrpVehicle> vehicleId;

	protected AbstractRemoteGuidanceEvent(double time, String mode, Id<DrtShift> operatorId, Id<DvrpVehicle> vehicleId) {
		super(time);
		this.mode = mode;
		this.operatorId = operatorId;
		this.vehicleId = vehicleId;
	}

	public String getMode() {
		return mode;
	}

	public Id<DrtShift> getOperatorId() {
		return operatorId;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_OPERATOR_ID, operatorId + "");
		attr.put(ATTRIBUTE_VEHICLE_ID, vehicleId + "");
		return attr;
	}
}