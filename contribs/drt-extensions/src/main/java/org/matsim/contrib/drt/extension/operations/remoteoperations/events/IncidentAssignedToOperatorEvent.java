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
 * Fired when a remote-guidance incident is taken under supervision by an operator, i.e. when the operator resource is
 * occupied. This is the incident-driven analogue of {@link VehicleActivatedForRemoteGuidanceEvent} (routine
 * supervision): it
 * marks the start of the operator's busy time for this incident and consumes capacity slots according to severity
 * (severity 2 → one slot, severity 3 → the operator's whole capacity κ).
 * <p>
 * If the incident could be taken immediately this event fires at the same time as the {@link IncidentStartedEvent};
 * otherwise the gap between the two is the queue delay incurred while all operators were saturated. The operator busy
 * interval is {@code [thisEvent, IncidentResolvedEvent]}, which is generally shorter than the vehicle hold interval
 * {@code [IncidentStartedEvent, IncidentResolvedEvent]}.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentAssignedToOperatorEvent extends AbstractRemoteGuidanceEvent {

	public static final String EVENT_TYPE = "remote guidance incident assigned";

	public static final String ATTRIBUTE_SEVERITY = "severity";

	private final int severity;

	public IncidentAssignedToOperatorEvent(double time, String mode, Id<DvrpVehicle> vehicleId,
										   Id<DrtShift> operatorId, int severity) {
		super(time, mode, operatorId, vehicleId);
		this.severity = severity;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public int getSeverity() {
		return severity;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SEVERITY, Integer.toString(severity));
		return attr;
	}
}