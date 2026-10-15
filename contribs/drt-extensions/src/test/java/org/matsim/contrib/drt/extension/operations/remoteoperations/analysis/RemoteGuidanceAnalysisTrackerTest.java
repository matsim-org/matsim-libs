/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.IncidentRecord;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentAssignedToOperatorEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentResolvedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentStartedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.analysis.RemoteGuidanceAnalysisTracker.OperatorRecord;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.RemoteGuidanceOperatorEndedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.RemoteGuidanceOperatorStartedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleActivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent.DeactivationReason;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * Unit tests for {@link RemoteGuidanceAnalysisTracker}: incident-lifecycle reconstruction (started → assigned →
 * resolved matched on the vehicle id), the extensive-margin +1/-1 step sequence, deactivation-reason tallying, mode
 * filtering, and reset.
 *
 * @author nkuehnel / MOIA
 */
public class RemoteGuidanceAnalysisTrackerTest {

	private static final String MODE = "drt";

	private static Id<DvrpVehicle> veh(String id) {
		return Id.create(id, DvrpVehicle.class);
	}

	private static Id<DrtShift> op(String id) {
		return Id.create(id, DrtShift.class);
	}

	private static Id<Link> link(String id) {
		return Id.createLinkId(id);
	}

	@Test
	void reconstructsIncidentLifecycle_immediateAssignment() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);

		tracker.handleEvent(new IncidentStartedEvent(100, MODE, veh("v1"), 2, 300, link("l1")));
		tracker.handleEvent(new IncidentAssignedToOperatorEvent(100, MODE, veh("v1"), op("o1"), 2));
		tracker.handleEvent(new IncidentResolvedEvent(410, MODE, veh("v1"), op("o1"), 2, 310, false, 0));

		assertThat(tracker.getCompletedIncidents()).hasSize(1);
		IncidentRecord r = tracker.getCompletedIncidents().get(0);
		assertThat(r.vehicleId()).isEqualTo(veh("v1"));
		assertThat(r.operatorId()).isEqualTo(op("o1"));
		assertThat(r.severity()).isEqualTo(2);
		assertThat(r.linkId()).isEqualTo(link("l1"));
		assertThat(r.startTime()).isEqualTo(100);
		assertThat(r.assignTime()).isEqualTo(100);
		assertThat(r.resolveTime()).isEqualTo(410);
		assertThat(r.expectedDuration()).isEqualTo(300);
		assertThat(r.actualDuration()).isEqualTo(310);
		assertThat(r.queued()).isFalse();
		assertThat(r.queueDelay()).isEqualTo(0);
	}

	@Test
	void reconstructsIncidentLifecycle_queuedAssignment() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);

		tracker.handleEvent(new IncidentStartedEvent(100, MODE, veh("v1"), 3, 300, link("l1")));
		// no free operator for 50s → assigned later; the started link/severity are preserved from the start event.
		tracker.handleEvent(new IncidentAssignedToOperatorEvent(150, MODE, veh("v1"), op("o2"), 3));
		tracker.handleEvent(new IncidentResolvedEvent(460, MODE, veh("v1"), op("o2"), 3, 360, true, 50));

		IncidentRecord r = tracker.getCompletedIncidents().get(0);
		assertThat(r.assignTime()).isEqualTo(150);
		assertThat(r.operatorId()).isEqualTo(op("o2"));
		assertThat(r.linkId()).isEqualTo(link("l1"));
		assertThat(r.queued()).isTrue();
		assertThat(r.queueDelay()).isEqualTo(50);
	}

	@Test
	void openIncidentIsNotReported() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);
		tracker.handleEvent(new IncidentStartedEvent(100, MODE, veh("v1"), 1, 200, link("l1")));
		tracker.handleEvent(new IncidentAssignedToOperatorEvent(100, MODE, veh("v1"), op("o1"), 1));
		// never resolved
		assertThat(tracker.getCompletedIncidents()).isEmpty();
	}

	@Test
	void sequentialIncidentsOnSameVehicleAreDistinct() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);

		tracker.handleEvent(new IncidentStartedEvent(100, MODE, veh("v1"), 1, 100, link("l1")));
		tracker.handleEvent(new IncidentAssignedToOperatorEvent(100, MODE, veh("v1"), op("o1"), 1));
		tracker.handleEvent(new IncidentResolvedEvent(200, MODE, veh("v1"), op("o1"), 1, 100, false, 0));

		tracker.handleEvent(new IncidentStartedEvent(500, MODE, veh("v1"), 2, 100, link("l2")));
		tracker.handleEvent(new IncidentAssignedToOperatorEvent(500, MODE, veh("v1"), op("o2"), 2));
		tracker.handleEvent(new IncidentResolvedEvent(600, MODE, veh("v1"), op("o2"), 2, 100, false, 0));

		assertThat(tracker.getCompletedIncidents()).hasSize(2);
		assertThat(tracker.getCompletedIncidents()).extracting(IncidentRecord::linkId)
				.containsExactly(link("l1"), link("l2"));
	}

	@Test
	void activationChangesAndReasonCounts() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);

		tracker.handleEvent(new VehicleActivatedForRemoteGuidanceEvent(100, MODE, veh("v1")));
		tracker.handleEvent(new VehicleActivatedForRemoteGuidanceEvent(150, MODE, veh("v2")));
		tracker.handleEvent(new VehicleDeactivatedForRemoteGuidanceEvent(300, MODE, veh("v1"),
				DeactivationReason.idleTimeout));
		tracker.handleEvent(new VehicleDeactivatedForRemoteGuidanceEvent(400, MODE, veh("v2"),
				DeactivationReason.capacityExceeded));

		assertThat(tracker.getActivationCount()).isEqualTo(2);
		assertThat(tracker.getActivationChanges()).extracting(RemoteGuidanceAnalysisTracker.ActivationChange::delta)
				.containsExactly(+1, +1, -1, -1);
		assertThat(tracker.getDeactivationReasonCounts())
				.containsEntry(DeactivationReason.idleTimeout, 1)
				.containsEntry(DeactivationReason.capacityExceeded, 1);
	}

	@Test
	void operatorLifecycleReconstruction() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);

		tracker.handleEvent(new RemoteGuidanceOperatorStartedEvent(0, MODE, op("o1"), 5));
		tracker.handleEvent(new RemoteGuidanceOperatorStartedEvent(0, MODE, op("o2"), 3));
		// o1 ends on time; o2 is retained 120s past its planned end.
		tracker.handleEvent(new RemoteGuidanceOperatorEndedEvent(3600, MODE, op("o1"), 3600));
		tracker.handleEvent(new RemoteGuidanceOperatorEndedEvent(3720, MODE, op("o2"), 3600));

		assertThat(tracker.getOperatorChanges()).extracting(RemoteGuidanceAnalysisTracker.OperatorChange::delta)
				.containsExactly(+1, +1, -1, -1);
		// coverage capacity is κ-weighted: +5, +3, then -5 (o1), -3 (o2) → the ended deltas match each operator's κ.
		assertThat(tracker.getCoverageChanges()).extracting(RemoteGuidanceAnalysisTracker.CoverageChange::delta)
				.containsExactly(5, 3, -5, -3);

		assertThat(tracker.getOperatorRecords()).hasSize(2);
		OperatorRecord o1 = tracker.getOperatorRecords().get(0);
		assertThat(o1.operatorId()).isEqualTo(op("o1"));
		assertThat(o1.startTime()).isEqualTo(0);
		assertThat(o1.plannedEndTime()).isEqualTo(3600);
		assertThat(o1.actualEndTime()).isEqualTo(3600); // no retention
		OperatorRecord o2 = tracker.getOperatorRecords().get(1);
		assertThat(o2.actualEndTime() - o2.plannedEndTime()).isEqualTo(120); // 120s retention
	}

	@Test
	void otherModeIsIgnored() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);

		tracker.handleEvent(new IncidentStartedEvent(100, "otherMode", veh("v1"), 1, 100, link("l1")));
		tracker.handleEvent(new IncidentAssignedToOperatorEvent(100, "otherMode", veh("v1"), op("o1"), 1));
		tracker.handleEvent(new IncidentResolvedEvent(200, "otherMode", veh("v1"), op("o1"), 1, 100, false, 0));
		tracker.handleEvent(new VehicleActivatedForRemoteGuidanceEvent(100, "otherMode", veh("v1")));
		tracker.handleEvent(new RemoteGuidanceOperatorStartedEvent(0, "otherMode", op("o1"), 5));
		tracker.handleEvent(new RemoteGuidanceOperatorEndedEvent(100, "otherMode", op("o1"), 100));

		assertThat(tracker.getCompletedIncidents()).isEmpty();
		assertThat(tracker.getActivationCount()).isZero();
		assertThat(tracker.getOperatorChanges()).isEmpty();
		assertThat(tracker.getOperatorRecords()).isEmpty();
	}

	@Test
	void resetClearsAllState() {
		RemoteGuidanceAnalysisTracker tracker = new RemoteGuidanceAnalysisTracker(MODE);
		tracker.handleEvent(new IncidentStartedEvent(100, MODE, veh("v1"), 1, 100, link("l1")));
		tracker.handleEvent(new IncidentAssignedToOperatorEvent(100, MODE, veh("v1"), op("o1"), 1));
		tracker.handleEvent(new IncidentResolvedEvent(200, MODE, veh("v1"), op("o1"), 1, 100, false, 0));
		tracker.handleEvent(new VehicleActivatedForRemoteGuidanceEvent(100, MODE, veh("v1")));
		tracker.handleEvent(new RemoteGuidanceOperatorStartedEvent(0, MODE, op("o1"), 5));
		tracker.handleEvent(new RemoteGuidanceOperatorEndedEvent(100, MODE, op("o1"), 100));

		tracker.reset(1);

		assertThat(tracker.getCompletedIncidents()).isEmpty();
		assertThat(tracker.getActivationChanges()).isEmpty();
		assertThat(tracker.getDeactivationReasonCounts()).isEmpty();
		assertThat(tracker.getActivationCount()).isZero();
		assertThat(tracker.getOperatorChanges()).isEmpty();
		assertThat(tracker.getCoverageChanges()).isEmpty();
		assertThat(tracker.getOperatorRecords()).isEmpty();
	}
}
