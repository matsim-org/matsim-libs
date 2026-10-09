/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations;

import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;

import java.util.ArrayDeque;

/**
 * Measures the recent request-rejection rate of one DRT mode over a trailing time window, feeding the demand-driven
 * {@code RejectionRateActivation} trigger (the {@link org.matsim.contrib.drt.extension.operations.remoteoperations.activation.GuidanceState#recentRejectionRate()}
 * seam). The rate is {@code rejected / (rejected + scheduled)} of the mode's requests whose event time falls within the
 * last {@code windowSize} seconds.
 * <p>
 * A single tracker instance is shared across the QSim scope: both the {@link RemoteGuidanceScheduler} (activation) and
 * {@link RemoteGuidanceShiftEndLogic} (deactivation) read {@link #rejectionRate(double)} when building their
 * {@code GuidanceState}, so both margins see the same demand-pressure signal and the shared activation target stays
 * consistent. Timestamps are stored per event and pruned lazily on read, so memory is bounded by the number of requests
 * in one window.
 *
 * @author nkuehnel / MOIA
 */
public final class RejectionRateTracker
		implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler {

	private final String mode;
	private final double windowSize;

	private final ArrayDeque<Double> rejectedTimes = new ArrayDeque<>();
	private final ArrayDeque<Double> scheduledTimes = new ArrayDeque<>();

	public RejectionRateTracker(String mode, double windowSize) {
		this.mode = mode;
		this.windowSize = windowSize;
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			rejectedTimes.addLast(event.getTime());
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(mode)) {
			scheduledTimes.addLast(event.getTime());
		}
	}

	/**
	 * @return the rejection rate {@code rejected / (rejected + scheduled)} over the window {@code [now - windowSize,
	 * now]}, or {@code 0.0} if no requests fell in the window (no demand pressure signal). Prunes expired timestamps.
	 */
	public double rejectionRate(double now) {
		double cutoff = now - windowSize;
		int rejected = prune(rejectedTimes, cutoff);
		int scheduled = prune(scheduledTimes, cutoff);
		int total = rejected + scheduled;
		return total == 0 ? 0.0 : (double) rejected / total;
	}

	/** Drops timestamps at or before {@code cutoff} from the front of the queue; returns the remaining count. */
	private static int prune(ArrayDeque<Double> times, double cutoff) {
		while (!times.isEmpty() && times.peekFirst() <= cutoff) {
			times.pollFirst();
		}
		return times.size();
	}

	@Override
	public void reset(int iteration) {
		rejectedTimes.clear();
		scheduledTimes.clear();
	}
}
