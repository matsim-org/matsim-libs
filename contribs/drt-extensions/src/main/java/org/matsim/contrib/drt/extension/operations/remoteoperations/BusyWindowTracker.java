/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Keeps a trailing-window <b>maximum</b> of the {@code busy} vehicle count (busy = active − idle-in-service) feeding the
 * {@code smoothedBusy} seam of {@link org.matsim.contrib.drt.extension.operations.remoteoperations.activation.GuidanceState}. It
 * exists to kill the <em>peak-demand</em> activation sawtooth: {@code IdleBufferActivation} targets {@code busy + buffer},
 * so when {@code busy} jitters second-to-second in the daytime peak the shared target chases every tick and the
 * idle-timeout side recalls / the scheduler reactivates one vehicle per tick (a balanced pump). Replacing the raw
 * instantaneous {@code busy} with its trailing maximum gives the target the asymmetry it needs:
 * <ul>
 *     <li>when {@code busy} rises the max jumps <em>immediately</em> → ramp-up stays responsive;</li>
 *     <li>when {@code busy} dips the max holds the recent peak → the deactivation side recalls nothing → no sawtooth;</li>
 *     <li>only once {@code busy} stays below the peak for a full {@code windowSize} (a genuine demand lull) does the max
 *         decay → an orderly, delayed ramp-down.</li>
 * </ul>
 * Binding the window to {@code idleTimeout} reads as "only shed a vehicle once demand has not needed it for a full
 * timeout".
 * <p>
 * <b>Shared, like {@link RejectionRateTracker}.</b> A single instance is read by both margins — the
 * {@link RemoteGuidanceScheduler} (activation) and {@link RemoteGuidanceShiftEndLogic} (deactivation) — so both size the
 * fleet from the same smoothed signal and the "one shared target" invariant holds. Both {@link #sample sample} it each
 * step; because the aggregate is a maximum, feeding two (possibly slightly different, cf. the "already leaving"
 * transient) observations per step is order- and duplicate-robust: a lower second observation never lowers the reported
 * peak.
 * <p>
 * Implemented as a monotonic deque (values decreasing from the front, front = current window max), so {@link #sample} is
 * O(1) amortized rather than a per-step linear scan of the window. Not an event handler, so it receives no MATSim
 * {@code reset(iteration)}; instead it self-resets when it observes time going backwards (a new iteration restarts at
 * {@code t≈0}), which would otherwise leave the previous day's late samples poisoning the max.
 *
 * @author nkuehnel / MOIA
 */
public final class BusyWindowTracker {

	private final double windowSize;

	private record Sample(double time, int busy) {}

	// monotonic deque: busy strictly non-increasing from front to back, so peekFirst() is always the window maximum.
	private final Deque<Sample> samples = new ArrayDeque<>();
	private double lastSampleTime = Double.NEGATIVE_INFINITY;

	public BusyWindowTracker(double windowSize) {
		this.windowSize = windowSize;
	}

	/**
	 * Records {@code busy} at {@code now} and returns the trailing maximum busy over the window {@code (now − windowSize,
	 * now]}. Self-resets when {@code now} is earlier than the last sample (new iteration).
	 */
	public int sample(double now, int busy) {
		if (now < lastSampleTime) {
			// time went backwards → a new iteration started; drop the previous run's samples so they cannot poison the max.
			samples.clear();
		}
		lastSampleTime = now;

		// maintain the monotonic (decreasing) invariant: drop tail samples the new one dominates. '<=' keeps the newest
		// among equal values (it expires latest), so equal peaks extend the plateau rather than shortening it.
		while (!samples.isEmpty() && samples.peekLast().busy() <= busy) {
			samples.pollLast();
		}
		samples.addLast(new Sample(now, busy));

		// evict samples that have fallen out of the trailing window from the front (the max side).
		double cutoff = now - windowSize;
		while (!samples.isEmpty() && samples.peekFirst().time() < cutoff) {
			samples.pollFirst();
		}
		return samples.peekFirst().busy();
	}
}