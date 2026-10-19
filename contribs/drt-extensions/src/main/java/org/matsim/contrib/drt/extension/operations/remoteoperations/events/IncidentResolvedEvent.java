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
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * Fired when a remote-guidance incident has been fully processed by the operator and the vehicle resumes normal
 * operation. Closes the incident lifecycle
 * {@link IncidentStartedEvent} → {@link IncidentAssignedToOperatorEvent} → {@code IncidentResolvedEvent}, releasing both
 * the vehicle (it may drive again) and the operator's capacity slots.
 * <p>
 * {@code actualDuration} is the total time the vehicle was held (which may exceed the initially expected duration if the
 * incident was queued while operators were saturated). {@code queued} indicates whether the incident had to wait for a
 * free operator slot, and {@code queueDelay} is the portion of the hold spent waiting before an operator became
 * available. Both are convenience fields derivable from the gap between {@link IncidentStartedEvent} and
 * {@link IncidentAssignedToOperatorEvent}, provided here so stream-2 (operator utilisation) analysis needs no event join.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentResolvedEvent extends AbstractRemoteGuidanceEvent {

	public static final String EVENT_TYPE = "remote guidance incident resolved";

	public static final String ATTRIBUTE_SEVERITY = "severity";
	public static final String ATTRIBUTE_ACTUAL_DURATION = "actualDuration";
	public static final String ATTRIBUTE_QUEUED = "queued";
	public static final String ATTRIBUTE_QUEUE_DELAY = "queueDelay";

	private final int severity;
	private final double actualDuration;
	private final boolean queued;
	private final double queueDelay;

	public IncidentResolvedEvent(double time, String mode, Id<DvrpVehicle> vehicleId, Id<DrtShift> operatorId,
								 int severity, double actualDuration, boolean queued, double queueDelay) {
		super(time, mode, operatorId, vehicleId);
		this.severity = severity;
		this.actualDuration = actualDuration;
		this.queued = queued;
		this.queueDelay = queueDelay;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public int getSeverity() {
		return severity;
	}

	public double getActualDuration() {
		return actualDuration;
	}

	public boolean isQueued() {
		return queued;
	}

	public double getQueueDelay() {
		return queueDelay;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SEVERITY, Integer.toString(severity));
		attr.put(ATTRIBUTE_ACTUAL_DURATION, Double.toString(actualDuration));
		attr.put(ATTRIBUTE_QUEUED, Boolean.toString(queued));
		attr.put(ATTRIBUTE_QUEUE_DELAY, Double.toString(queueDelay));
		return attr;
	}
}