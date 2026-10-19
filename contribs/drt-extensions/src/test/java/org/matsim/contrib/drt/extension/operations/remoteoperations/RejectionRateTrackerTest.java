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
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;

/**
 * Unit tests for the trailing-window rejection-rate measurement ({@link RejectionRateTracker}): windowing, pruning of
 * expired events, mode filtering, and the empty-window fallback.
 *
 * @author nkuehnel / MOIA
 */
public class RejectionRateTrackerTest {

	private static final String MODE = "drt";
	private static final double WINDOW = 100.0;

	private static PassengerRequestScheduledEvent scheduled(double time, String mode) {
		return new PassengerRequestScheduledEvent(time, mode, Id.create("r" + time + mode, Request.class),
				List.of(), Id.create("v", org.matsim.contrib.dvrp.fleet.DvrpVehicle.class), time + 60, time + 120);
	}

	private static PassengerRequestRejectedEvent rejected(double time, String mode) {
		return new PassengerRequestRejectedEvent(time, mode, Id.create("r" + time + mode, Request.class), List.of(),
				"cause");
	}

	@Test
	void emptyWindow_rateIsZero() {
		RejectionRateTracker tracker = new RejectionRateTracker(MODE, WINDOW);
		assertThat(tracker.rejectionRate(500.0)).isZero();
	}

	@Test
	void computesFractionWithinWindow() {
		RejectionRateTracker tracker = new RejectionRateTracker(MODE, WINDOW);
		// at t≈1000: 1 rejected + 3 scheduled → 0.25
		tracker.handleEvent(rejected(980, MODE));
		tracker.handleEvent(scheduled(985, MODE));
		tracker.handleEvent(scheduled(990, MODE));
		tracker.handleEvent(scheduled(995, MODE));
		assertThat(tracker.rejectionRate(1000.0)).isCloseTo(0.25, within(1e-9));
	}

	@Test
	void prunesEventsOlderThanWindow() {
		RejectionRateTracker tracker = new RejectionRateTracker(MODE, WINDOW);
		tracker.handleEvent(rejected(800, MODE));   // will fall out of [900,1000]
		tracker.handleEvent(scheduled(950, MODE));  // stays
		tracker.handleEvent(rejected(960, MODE));   // stays
		// window [900,1000]: 1 rejected + 1 scheduled → 0.5 (the t=800 rejection is pruned)
		assertThat(tracker.rejectionRate(1000.0)).isCloseTo(0.5, within(1e-9));
	}

	@Test
	void ignoresOtherModes() {
		RejectionRateTracker tracker = new RejectionRateTracker(MODE, WINDOW);
		tracker.handleEvent(rejected(990, "other"));
		tracker.handleEvent(scheduled(990, MODE));
		// only the drt scheduled request counts → 0 rejected of 1 → 0.0
		assertThat(tracker.rejectionRate(1000.0)).isZero();
	}

	@Test
	void resetClearsState() {
		RejectionRateTracker tracker = new RejectionRateTracker(MODE, WINDOW);
		tracker.handleEvent(rejected(990, MODE));
		tracker.reset(0);
		assertThat(tracker.rejectionRate(1000.0)).isZero();
	}
}
