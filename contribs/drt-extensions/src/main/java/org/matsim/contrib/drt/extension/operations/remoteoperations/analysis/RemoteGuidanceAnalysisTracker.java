/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentAssignedToOperatorEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentResolvedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentStartedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.RemoteGuidanceOperatorEndedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.RemoteGuidanceOperatorStartedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleActivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent.DeactivationReason;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.events.handler.BasicEventHandler;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller-scoped accumulator for the remote-guidance operator-utilisation KPIs. It subscribes
 * to the five RG events via {@link BasicEventHandler} (there are no dedicated {@code *EventHandler} interfaces) and turns
 * them into three raw, per-iteration data structures that {@link RemoteGuidanceAnalysisControlerListener} aggregates and
 * writes out:
 * <ul>
 *     <li><b>completed incidents</b> — one {@link IncidentRecord} per closed incident, reconstructed by matching
 *         {@link IncidentStartedEvent} → {@link IncidentAssignedToOperatorEvent} → {@link IncidentResolvedEvent} on the
 *         vehicle id (a held vehicle carries at most one open incident at a time). This is the atomic table behind the
 *         incident log, the incident summary, and the operator-busy time series.</li>
 *     <li><b>activation changes</b> — a chronological {@code +1 / -1} step sequence from
 *         {@link VehicleActivatedForRemoteGuidanceEvent} / {@link VehicleDeactivatedForRemoteGuidanceEvent}, behind the
 *         active-vehicle time series.</li>
 *     <li><b>deactivation-reason counts</b> — tallied per {@link DeactivationReason}, behind the deactivation-reason
 *         breakdown.</li>
 *     <li><b>operator lifecycle</b> — a chronological {@code +1 / -1} step sequence from
 *         {@link RemoteGuidanceOperatorStartedEvent} / {@link RemoteGuidanceOperatorEndedEvent}, giving the REAL on-duty
 *         operator count over time (the utilisation denominator, event-driven rather than a retroactive registry
 *         query), plus one {@link OperatorRecord} per ended operator carrying planned vs. actual end for the
 *         operator-hours real-vs-planned stream.</li>
 * </ul>
 * Everything here is derivable from RG events alone (no join with core-DRT output, no registry access), so it carries no
 * dependency risk; the registry-derived capacities used for the utilisation ratios are queried in the listener, not here.
 *
 * @author nkuehnel / MOIA
 */
public final class RemoteGuidanceAnalysisTracker implements BasicEventHandler {

	private final String mode;

	// incidents still holding (not yet resolved), keyed by vehicle: a held vehicle has at most one open incident.
	private Map<Id<DvrpVehicle>, OpenIncident> openIncidents = new HashMap<>();
	private List<IncidentRecord> completedIncidents = new ArrayList<>();

	// chronological extensive-margin step sequence (+1 activation, -1 deactivation).
	private List<ActivationChange> activationChanges = new ArrayList<>();
	private Map<DeactivationReason, Integer> deactivationReasonCounts = new EnumMap<>(DeactivationReason.class);
	private int activationCount = 0;

	// chronological operator-lifecycle step sequences: operatorChanges = head-count (+1 started, -1 ended);
	// coverageChanges = κ-weighted coverage capacity (+κ started, -κ ended). Both reconstruct real (effective) windows
	// from the events, NOT from a retroactive query of mutable registry state. Plus one record per ended operator.
	private List<OperatorChange> operatorChanges = new ArrayList<>();
	private List<CoverageChange> coverageChanges = new ArrayList<>();
	private List<OperatorRecord> operatorRecords = new ArrayList<>();
	private Map<Id<DrtShift>, Double> operatorStartTimes = new HashMap<>();
	private Map<Id<DrtShift>, Integer> operatorCapacities = new HashMap<>();

	public RemoteGuidanceAnalysisTracker(String mode) {
		this.mode = mode;
	}

	/** A single completed incident lifecycle, all times absolute seconds. */
	public record IncidentRecord(Id<DvrpVehicle> vehicleId, Id<DrtShift> operatorId, int severity, Id<Link> linkId,
								 double startTime, double assignTime, double resolveTime, double expectedDuration,
								 double actualDuration, boolean queued, double queueDelay) {
	}

	/** One extensive-margin change: {@code delta} is +1 for an activation, -1 for a deactivation. */
	public record ActivationChange(double time, int delta) {
	}

	/** One operator-lifecycle head-count change: {@code delta} is +1 for a start, -1 for an end. */
	public record OperatorChange(double time, int delta) {
	}

	/** One coverage-capacity change: {@code delta} is +κ at an operator start, -κ at its (effective) end. */
	public record CoverageChange(double time, int delta) {
	}

	/** A completed operator duty period: the actual end may exceed {@code plannedEndTime} by the retention overhead. */
	public record OperatorRecord(Id<DrtShift> operatorId, double startTime, double plannedEndTime, double actualEndTime) {
	}

	// mutable, in-flight incident state between started and resolved.
	private static final class OpenIncident {
		private final int severity;
		private final Id<Link> linkId;
		private final double startTime;
		private final double expectedDuration;
		private double assignTime = Double.NaN;
		private Id<DrtShift> operatorId = null;

		private OpenIncident(int severity, Id<Link> linkId, double startTime, double expectedDuration) {
			this.severity = severity;
			this.linkId = linkId;
			this.startTime = startTime;
			this.expectedDuration = expectedDuration;
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (event instanceof IncidentStartedEvent started) {
			if (started.getMode().equals(mode)) {
				openIncidents.put(started.getVehicleId(), new OpenIncident(started.getSeverity(), started.getLinkId(),
						started.getTime(), started.getExpectedDuration()));
			}
		} else if (event instanceof IncidentAssignedToOperatorEvent assigned) {
			if (assigned.getMode().equals(mode)) {
				OpenIncident open = openIncidents.get(assigned.getVehicleId());
				if (open != null) {
					open.assignTime = assigned.getTime();
					open.operatorId = assigned.getOperatorId();
				}
			}
		} else if (event instanceof IncidentResolvedEvent resolved) {
			if (resolved.getMode().equals(mode)) {
				OpenIncident open = openIncidents.remove(resolved.getVehicleId());
				if (open != null) {
					completedIncidents.add(new IncidentRecord(resolved.getVehicleId(), resolved.getOperatorId(),
							resolved.getSeverity(), open.linkId, open.startTime, open.assignTime, resolved.getTime(),
							open.expectedDuration, resolved.getActualDuration(), resolved.isQueued(),
							resolved.getQueueDelay()));
				}
			}
		} else if (event instanceof VehicleActivatedForRemoteGuidanceEvent activated) {
			if (activated.getMode().equals(mode)) {
				activationChanges.add(new ActivationChange(activated.getTime(), +1));
				activationCount++;
			}
		} else if (event instanceof VehicleDeactivatedForRemoteGuidanceEvent deactivated) {
			if (deactivated.getMode().equals(mode)) {
				activationChanges.add(new ActivationChange(deactivated.getTime(), -1));
				deactivationReasonCounts.merge(deactivated.getReason(), 1, Integer::sum);
			}
		} else if (event instanceof RemoteGuidanceOperatorStartedEvent started) {
			if (started.getMode().equals(mode)) {
				operatorChanges.add(new OperatorChange(started.getTime(), +1));
				coverageChanges.add(new CoverageChange(started.getTime(), started.getCapacity()));
				operatorStartTimes.put(started.getOperatorId(), started.getTime());
				operatorCapacities.put(started.getOperatorId(), started.getCapacity());
			}
		} else if (event instanceof RemoteGuidanceOperatorEndedEvent ended) {
			if (ended.getMode().equals(mode)) {
				operatorChanges.add(new OperatorChange(ended.getTime(), -1));
				int capacity = operatorCapacities.getOrDefault(ended.getOperatorId(), 0);
				coverageChanges.add(new CoverageChange(ended.getTime(), -capacity));
				double startTime = operatorStartTimes.getOrDefault(ended.getOperatorId(), Double.NaN);
				operatorRecords.add(new OperatorRecord(ended.getOperatorId(), startTime, ended.getPlannedEndTime(),
						ended.getTime()));
			}
		}
	}

	public List<IncidentRecord> getCompletedIncidents() {
		return completedIncidents;
	}

	public List<ActivationChange> getActivationChanges() {
		return activationChanges;
	}

	public Map<DeactivationReason, Integer> getDeactivationReasonCounts() {
		return deactivationReasonCounts;
	}

	public int getActivationCount() {
		return activationCount;
	}

	public List<OperatorChange> getOperatorChanges() {
		return operatorChanges;
	}

	public List<CoverageChange> getCoverageChanges() {
		return coverageChanges;
	}

	public List<OperatorRecord> getOperatorRecords() {
		return operatorRecords;
	}

	@Override
	public void reset(int iteration) {
		this.openIncidents = new HashMap<>();
		this.completedIncidents = new ArrayList<>();
		this.activationChanges = new ArrayList<>();
		this.deactivationReasonCounts = new EnumMap<>(DeactivationReason.class);
		this.activationCount = 0;
		this.operatorChanges = new ArrayList<>();
		this.coverageChanges = new ArrayList<>();
		this.operatorRecords = new ArrayList<>();
		this.operatorStartTimes = new HashMap<>();
		this.operatorCapacities = new HashMap<>();
	}
}
