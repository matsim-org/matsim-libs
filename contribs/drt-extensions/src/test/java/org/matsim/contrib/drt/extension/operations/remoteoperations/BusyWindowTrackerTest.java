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
 * Unit tests for {@link BusyWindowTracker}: the trailing-window maximum of the busy count that damps the peak-demand
 * activation sawtooth. Locks in the four properties the fix relies on: a window of 0 is a pass-through (unchanged
 * behaviour), the max jumps up immediately, it holds the recent peak across a dip (the anti-churn asymmetry), it decays
 * only after the peak leaves the window, and it self-resets across an iteration boundary (backwards time).
 *
 * @author nkuehnel / MOIA
 */
public class BusyWindowTrackerTest {

	@Test
	void zeroWindow_isPassThrough() {
		// window 0 → smoothedBusy must equal the instantaneous busy at every step (the disabled default: unchanged behaviour).
		BusyWindowTracker tracker = new BusyWindowTracker(0);
		assertThat(tracker.sample(0, 5)).isEqualTo(5);
		assertThat(tracker.sample(1, 3)).isEqualTo(3);
		assertThat(tracker.sample(2, 8)).isEqualTo(8);
		assertThat(tracker.sample(3, 0)).isZero();
	}

	@Test
	void risesImmediately() {
		// ramp-up stays responsive: a higher busy is reported the very same step, never delayed.
		BusyWindowTracker tracker = new BusyWindowTracker(600);
		assertThat(tracker.sample(0, 2)).isEqualTo(2);
		assertThat(tracker.sample(60, 9)).isEqualTo(9);
	}

	@Test
	void holdsPeakAcrossADip_noChurn() {
		// the anti-sawtooth core: busy peaks at 10 then dips; within the window the reported max stays 10, so the target
		// busy+buffer does not drop and the deactivation side recalls nothing.
		BusyWindowTracker tracker = new BusyWindowTracker(600);
		assertThat(tracker.sample(0, 10)).isEqualTo(10);
		assertThat(tracker.sample(60, 7)).isEqualTo(10);
		assertThat(tracker.sample(120, 9)).isEqualTo(10);
		assertThat(tracker.sample(300, 8)).isEqualTo(10);
	}

	@Test
	void decaysOncePeakLeavesTheWindow() {
		// only a genuine, sustained lull lowers the target: once the t=0 peak of 10 has aged past the 600 s window, the
		// max falls to the highest sample still inside it.
		BusyWindowTracker tracker = new BusyWindowTracker(600);
		assertThat(tracker.sample(0, 10)).isEqualTo(10);
		assertThat(tracker.sample(200, 6)).isEqualTo(10); // peak still in window
		assertThat(tracker.sample(400, 7)).isEqualTo(10); // still in window
		// t=700: the t=0 peak (700-0=700 > 600) has left the window; the surviving samples in (100, 700] are 6 and 7.
		assertThat(tracker.sample(700, 4)).isEqualTo(7);
	}

	@Test
	void equalPeakExtendsThePlateau() {
		// two equal peaks: the later one must win the tie so the plateau extends to its expiry, not the earlier one's.
		BusyWindowTracker tracker = new BusyWindowTracker(600);
		assertThat(tracker.sample(0, 10)).isEqualTo(10);
		assertThat(tracker.sample(300, 10)).isEqualTo(10);
		// t=700: first peak aged out (700 > 600), but the t=300 peak is still in window (700-300=400 <= 600) → still 10.
		assertThat(tracker.sample(700, 5)).isEqualTo(10);
	}

	@Test
	void selfResetsOnBackwardsTime() {
		// a new iteration restarts at t≈0; the previous run's high samples must not poison the new window's max.
		BusyWindowTracker tracker = new BusyWindowTracker(600);
		assertThat(tracker.sample(50000, 30)).isEqualTo(30);
		// time goes backwards → new iteration → cleared. The stale 30 must not survive.
		assertThat(tracker.sample(0, 4)).isEqualTo(4);
		assertThat(tracker.sample(60, 6)).isEqualTo(6);
	}
}