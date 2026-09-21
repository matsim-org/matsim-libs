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

import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.ActivationReconciler;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.GuidanceState;

/**
 * Unit tests for {@link RemoteGuidanceScheduler#busy(int, int, int)}, the busy count fed into the shared
 * {@link BusyWindowTracker}, and for what it has to exclude.
 * <p>
 * A recalled vehicle routing home is not idle in service, since its last task is no longer a stay task, so counting it as
 * busy would inflate the target by {@code busy + buffer} and activate a replacement for every vehicle going home. The
 * same holds for a shift that is in a vehicle's queue but has not started, which happens at end of day when a replacement
 * shift is assigned to a vehicle still mid-recall. Both exclusions keep busy counting only vehicles doing passenger work,
 * which is also how the deactivation side ({@link RemoteGuidanceShiftEndLogic}) derives it. The counterfactual tests
 * below assert that both are load-bearing rather than cosmetic.
 *
 * @author nkuehnel / MOIA
 */
public class RemoteGuidanceBusyTest {

	@Test
	void busy_excludesLeaving() {
		// end of day: 40 live, 2 idle in service, 38 already recalled, so no vehicle is doing passenger work.
		// Without the exclusion busy would be 40 − 2 = 38.
		assertThat(RemoteGuidanceScheduler.busy(40, 2, 38)).isZero();
	}

	@Test
	void busy_noLeaving_isUnchanged() {
		// with no vehicle winding down, busy is plain started − idleInService: the exclusion must be a no-op whenever
		// leaving is 0, i.e. everywhere except during recalls.
		assertThat(RemoteGuidanceScheduler.busy(40, 2, 0)).isEqualTo(38);
		assertThat(RemoteGuidanceScheduler.busy(92, 15, 0)).isEqualTo(77);
	}

	@Test
	void busy_countsStartedNotQueueMembership() {
		// 93 virtual shifts sit in the fleet's queues, but only 20 have started: the other 73 are assigned to vehicles
		// still mid-recall and cannot start yet. Of the 20 started, 5 are idle in service and 15 are leaving, so no
		// vehicle is doing passenger work. busy() therefore has to be fed the started count, not queue membership.
		assertThat(RemoteGuidanceScheduler.busy(20, 5, 15)).isZero();
	}

	@Test
	void runawayReproducedWhenBusyUsesQueueMembership() {
		// counterfactual: over queue membership, busy would be 93 − 5 − 15 = 73, giving a target of 73 + buffer(20) = 93,
		// so the fleet would be held at 93 while doing no passenger work at all.
		assertThat(RemoteGuidanceScheduler.busy(93, 5, 15)).isEqualTo(73);
	}

	@Test
	void busy_allIdleOrLeaving_isZero() {
		assertThat(RemoteGuidanceScheduler.busy(10, 4, 6)).isZero();
	}

	@Test
	void noReactivationWhenAllActiveVehiclesAreLeaving() {
		// end to end: feed the busy count through the default buffered reconciler at a floor of 40 and a buffer of 20.
		// With busy 0 the target collapses to the floor, and since 40 vehicles are already active, nothing is emitted.
		ActivationReconciler reconciler = ActivationReconciler.createDefault(40, 20, OptionalDouble.empty());
		int busy = RemoteGuidanceScheduler.busy(40, 2, 38);
		GuidanceState state = new GuidanceState(40, 100_000, 375, 2, busy, 0.0);

		assertThat(reconciler.desired(state, 90900)).isEqualTo(40); // the floor, not busy(0) + buffer(20) = 20
		assertThat(reconciler.toEmit(state, 90900)).isZero();
	}

	@Test
	void runawayReproducedWithoutTheFix() {
		// counterfactual: counting the leaving vehicles as busy (38) drives the target to 38 + buffer(20) = 58, which with
		// 40 active emits 18 replacements for vehicles that are going home, and does so again every step.
		ActivationReconciler reconciler = ActivationReconciler.createDefault(40, 20, OptionalDouble.empty());
		int busyIncludingLeaving = 40 - 2; // live − idleInService, without excluding the leaving vehicles
		GuidanceState state = new GuidanceState(40, 100_000, 375, 2, busyIncludingLeaving, 0.0);

		assertThat(reconciler.desired(state, 90900)).isEqualTo(58);
		assertThat(reconciler.toEmit(state, 90900)).isEqualTo(18);
	}
}