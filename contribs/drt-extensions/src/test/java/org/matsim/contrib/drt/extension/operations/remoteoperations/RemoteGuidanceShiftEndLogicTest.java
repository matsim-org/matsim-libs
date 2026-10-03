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

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure churn-guard arithmetic of the idle-timeout recall
 * ({@link RemoteGuidanceShiftEndLogic#idleToKeep}): given the active fleet composition and the shared fleet-sizing
 * target, how many idle-in-service vehicles are spared from recall. This locks in the invariant that the deactivation
 * side recalls only DOWN to the same target the activation side ramps UP to (no floor-vs-buffer churn), independently of
 * the full QSim/fleet path exercised by {@code RunRemoteGuidanceDrtScenarioIT}.
 *
 * @author nkuehnel / MOIA
 */
public class RemoteGuidanceShiftEndLogicTest {

	@Test
	void keepsExactlyTheBufferWhenAllIdle_noChurn() {
		// full lull: 4 active, all idle in service, target 4 (e.g. floor 4). Keep all 4 → recall nothing. This is the
		// regression guard: with a floor above the responsiveness buffer the previous (buffer-only) logic recalled the
		// surplus and re-emitted it next step; now the shared target holds the whole floor.
		assertThat(RemoteGuidanceShiftEndLogic.idleToKeep(4, 4, 4)).isEqualTo(4);
	}

	@Test
	void recallsTheIdleSurplusAboveTheTarget() {
		// 6 active, 4 idle in service (2 busy), target 3 → keep 1 idle (3 - 2 busy), recall the other 3.
		assertThat(RemoteGuidanceShiftEndLogic.idleToKeep(6, 4, 3)).isEqualTo(1);
	}

	@Test
	void busyVehiclesAloneCoverTheTarget_recallAllIdle() {
		// target 3 but 5 vehicles are busy already (8 active, 3 idle) → the target is fully covered by busy work, keep 0
		// idle, recall all 3 idle vehicles (beyond timeout).
		assertThat(RemoteGuidanceShiftEndLogic.idleToKeep(8, 3, 3)).isZero();
	}

	@Test
	void neverKeepsMoreThanAreIdle() {
		// target 10 but only 2 idle in service (busy 3) → cannot keep more than exist; clamp to 2.
		assertThat(RemoteGuidanceShiftEndLogic.idleToKeep(5, 2, 10)).isEqualTo(2);
	}

	@Test
	void zeroTargetKeepsNothing() {
		// no floor, no buffer (target 0) → recall every idle-in-service vehicle beyond the timeout.
		assertThat(RemoteGuidanceShiftEndLogic.idleToKeep(3, 3, 0)).isZero();
	}
}
