/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftImpl;

/**
 * Unit tests for {@link RemoteGuidanceScheduler#startedVirtualShift}: the shift queue is start-time-ordered and holds
 * shifts from assignment (not from start), so its head may be a not-yet-started future shift. Idle/recall decisions must
 * key on the STARTED (and not ended) virtual shift, never blindly on {@code peek()}. These cases pin that behaviour.
 *
 * @author nkuehnel / MOIA
 */
public class StartedVirtualShiftTest {

	private static final String VIRTUAL = "remoteGuidanceVirtual";

	private static DrtShift shift(String id, double start, String type, boolean started, boolean ended) {
		DrtShift s = new DrtShiftImpl(Id.create(id, DrtShift.class), start, 100_000, null, null, null, type, false);
		if (started) {
			s.start();
		}
		if (ended) {
			s.end();
		}
		return s;
	}

	private static ShiftDvrpVehicle vehicleWith(DrtShift... shifts) {
		Queue<DrtShift> queue = new PriorityQueue<>(java.util.Comparator.comparingDouble(DrtShift::getStartTime));
		for (DrtShift s : shifts) {
			queue.add(s);
		}
		ShiftDvrpVehicle vehicle = mock(ShiftDvrpVehicle.class);
		when(vehicle.getShifts()).thenReturn(queue);
		return vehicle;
	}

	@Test
	void picksTheStartedVirtualShift_notTheEarlierUnstartedQueueHead() {
		// a future, not-yet-started virtual shift (start 0 → sorts to the head) plus the actually-running one (start 50).
		// peek() would return the unstarted head; startedVirtualShift must return the running one.
		DrtShift future = shift("future", 0, VIRTUAL, false, false);
		DrtShift running = shift("running", 50, VIRTUAL, true, false);
		ShiftDvrpVehicle vehicle = vehicleWith(future, running);

		assertThat(vehicle.getShifts().peek()).isSameAs(future); // documents the trap
		Optional<DrtShift> started = RemoteGuidanceScheduler.startedVirtualShift(vehicle);
		assertThat(started).isPresent();
		assertThat(started.get().getId()).isEqualTo(Id.create("running", DrtShift.class));
	}

	@Test
	void emptyWhenOnlyAnUnstartedShiftIsQueued() {
		ShiftDvrpVehicle vehicle = vehicleWith(shift("future", 0, VIRTUAL, false, false));
		assertThat(RemoteGuidanceScheduler.startedVirtualShift(vehicle)).isEmpty();
	}

	@Test
	void emptyWhenTheStartedShiftHasEnded() {
		ShiftDvrpVehicle vehicle = vehicleWith(shift("done", 0, VIRTUAL, true, true));
		assertThat(RemoteGuidanceScheduler.startedVirtualShift(vehicle)).isEmpty();
	}

	@Test
	void emptyWhenTheStartedShiftIsNotVirtual() {
		// a started driver shift must not be mistaken for a virtual (remote-guidance) one.
		ShiftDvrpVehicle vehicle = vehicleWith(shift("driver", 0, "regular", true, false));
		assertThat(RemoteGuidanceScheduler.startedVirtualShift(vehicle)).isEmpty();
	}

	@Test
	void emptyWhenNoShiftsAtAll() {
		assertThat(RemoteGuidanceScheduler.startedVirtualShift(vehicleWith())).isEmpty();
	}
}
