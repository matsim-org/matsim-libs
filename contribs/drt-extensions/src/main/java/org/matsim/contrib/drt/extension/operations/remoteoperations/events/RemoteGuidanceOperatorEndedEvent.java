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

import java.util.Map;

/**
 * Fired when a remote guidance operator is actually released, i.e. when it stops contributing to the supervision
 * coverage capacity and the incident server pool. The event {@link #getTime() time} is the operator's <em>effective</em>
 * end, which may be later than its {@link #getPlannedEndTime() planned end}: an operator is retained past its planned
 * end whenever releasing it earlier would exceed the remaining supervision capacity or interrupt an incident it is handling.
 * The gap {@code time - plannedEndTime} is therefore the retention (deferral) overhead, directly measurable without an
 * event join. Paired with {@link RemoteGuidanceOperatorStartedEvent} it makes the operators' real on-duty window
 * observable for utilisation analysis.
 *
 * @author nkuehnel / MOIA
 */
public class RemoteGuidanceOperatorEndedEvent extends Event {

	public static final String EVENT_TYPE = "remote guidance operator ended";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_OPERATOR_ID = "operator_id";
	public static final String ATTRIBUTE_PLANNED_END_TIME = "plannedEndTime";

	private final String mode;
	private final Id<DrtShift> operatorId;
	private final double plannedEndTime;

	public RemoteGuidanceOperatorEndedEvent(double time, String mode, Id<DrtShift> operatorId, double plannedEndTime) {
		super(time);
		this.mode = mode;
		this.operatorId = operatorId;
		this.plannedEndTime = plannedEndTime;
	}

	public String getMode() {
		return mode;
	}

	public Id<DrtShift> getOperatorId() {
		return operatorId;
	}

	/** The operator's planned end time; {@link #getTime()} is the actual (possibly deferred) end. */
	public double getPlannedEndTime() {
		return plannedEndTime;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_OPERATOR_ID, operatorId + "");
		attr.put(ATTRIBUTE_PLANNED_END_TIME, Double.toString(plannedEndTime));
		return attr;
	}
}
