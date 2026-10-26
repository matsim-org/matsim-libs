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
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Map;

/**
 * Fired when a remote-guidance incident begins for a supervised vehicle: the (autonomous) vehicle has run into a
 * situation it cannot resolve on its own and starts holding in place. This event marks the start of the vehicle's hold
 * time (the vehicle resource is now blocked, VKT = 0).
 * <p>
 * At this point no operator has necessarily taken the incident yet — if all operators are saturated the incident is
 * queued. The operator resource (a κ capacity slot) is only occupied at the subsequent
 * {@link IncidentAssignedToOperatorEvent}; the incident is closed by {@link IncidentResolvedEvent}. Accordingly this
 * event carries no operator id (it is always {@code null} on the wire).
 *
 * @author nkuehnel / MOIA
 */
public class IncidentStartedEvent extends AbstractRemoteGuidanceEvent {

	public static final String EVENT_TYPE = "remote guidance incident started";

	public static final String ATTRIBUTE_SEVERITY = "severity";
	public static final String ATTRIBUTE_EXPECTED_DURATION = "expectedDuration";
	public static final String ATTRIBUTE_LINK = "link";

	private final int severity;
	private final double expectedDuration;
	private final Id<Link> linkId;

	public IncidentStartedEvent(double time, String mode, Id<DvrpVehicle> vehicleId,
								int severity, double expectedDuration, Id<Link> linkId) {
		super(time, mode, null, vehicleId);
		this.severity = severity;
		this.expectedDuration = expectedDuration;
		this.linkId = linkId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public int getSeverity() {
		return severity;
	}

	public double getExpectedDuration() {
		return expectedDuration;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_SEVERITY, Integer.toString(severity));
		attr.put(ATTRIBUTE_EXPECTED_DURATION, Double.toString(expectedDuration));
		attr.put(ATTRIBUTE_LINK, linkId + "");
		return attr;
	}
}