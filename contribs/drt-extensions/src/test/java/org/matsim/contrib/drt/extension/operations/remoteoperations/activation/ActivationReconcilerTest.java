/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.activation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.ActivationPolicy;

/**
 * Unit tests for the activation reconciliation: pure over a hand-built {@link GuidanceState}, no QSim. Documents
 * both the {@link ActivationReconciler#toEmit} ramp-up decision and the shared {@link ActivationReconciler#desired}
 * target that the deactivation side reads too, which is the single value that damps the low-demand sawtooth.
 *
 * @author nkuehnel / MOIA
 */
public class ActivationReconcilerTest {

	private static final double NOW = 3600.0;

	/** The default RG activation policy: the minimum-fleet floor + one ready buffer, exactly as the runtime builds it. */
	private static ActivationReconciler defaultReconciler(int minActiveFleet) {
		return ActivationReconciler.createDefault(minActiveFleet, 1, OptionalDouble.empty());
	}

	@Test
	void idleBuffer_activatesOneWhenNoReadySpare() {
		// nothing active, no idle-in-service buffer, plenty idle at hub, capacity available → activate exactly one
		GuidanceState state = new GuidanceState(0, 10, 5, 0, 0.0);
		assertThat(defaultReconciler(0).toEmit(state, NOW)).isEqualTo(1);
	}

	@Test
	void idleBuffer_activatesNothingWhenBufferAlreadyPresent() {
		// one active vehicle idle in service = the ready buffer is satisfied → do not ramp up further
		GuidanceState state = new GuidanceState(1, 10, 5, 1, 0.0);
		assertThat(defaultReconciler(0).toEmit(state, NOW)).isZero();
	}

	@Test
	void idleBuffer_doesNotGreedilyFillCapacity() {
		// contrast with the greedy baseline: with 8 idle-at-hub vehicles and capacity 10, the default set keeps just one
		// buffer, whereas greedy would pull all 8 in.
		GuidanceState state = new GuidanceState(2, 10, 8, 0, 0.0);
		assertThat(defaultReconciler(0).toEmit(state, NOW)).isEqualTo(1);

		ActivationReconciler greedy = new ActivationReconciler(List.of(new GreedyIdleActivation()));
		assertThat(greedy.toEmit(state, NOW)).isEqualTo(8);
	}

	@Test
	void idleBuffer_ofSizeThree_activatesTheGapToTarget() {
		// buffer target 3, two vehicles already idle in service → activate one more to reach the target
		ActivationReconciler r = new ActivationReconciler(List.of(new IdleBufferActivation(3)));
		GuidanceState state = new GuidanceState(5, 10, 4, 2, 0.0);
		assertThat(r.toEmit(state, NOW)).isEqualTo(1);
	}

	@Test
	void idleBuffer_ofSizeThree_activatesNothingWhenBufferMet() {
		// buffer target 3, already three idle in service → satisfied, emit nothing
		ActivationReconciler r = new ActivationReconciler(List.of(new IdleBufferActivation(3)));
		GuidanceState state = new GuidanceState(6, 10, 4, 3, 0.0);
		assertThat(r.toEmit(state, NOW)).isZero();
	}

	@Test
	void idleBuffer_ofSizeThree_activatesFullTargetFromCold() {
		// nothing active, none idle in service → pull in the full buffer target of three at once
		ActivationReconciler r = new ActivationReconciler(List.of(new IdleBufferActivation(3)));
		GuidanceState state = new GuidanceState(0, 10, 8, 0, 0.0);
		assertThat(r.toEmit(state, NOW)).isEqualTo(3);
	}

	@Test
	void floor_pullsFleetUpToMinActiveFleet() {
		// hard floor of 4, nothing active, buffer already present (so IdleBuffer proposes nothing) → floor still emits 4
		GuidanceState state = new GuidanceState(0, 10, 6, 1, 0.0);
		assertThat(defaultReconciler(4).toEmit(state, NOW)).isEqualTo(4);
	}

	@Test
	void ceiling_clampsDesiredToActivationCapacity() {
		// floor 8 but capacity only 5 → never emit past the ceiling
		GuidanceState state = new GuidanceState(0, 5, 10, 0, 0.0);
		assertThat(defaultReconciler(8).toEmit(state, NOW)).isEqualTo(5);
	}

	@Test
	void noFreeCapacity_emitsNothing() {
		// already at the ceiling → nothing to emit even without a ready buffer
		GuidanceState state = new GuidanceState(10, 10, 3, 0, 0.0);
		assertThat(defaultReconciler(0).toEmit(state, NOW)).isZero();
	}

	@Test
	void limitedByIdleAtHub_cannotEmitMoreThanAvailableSource() {
		// floor wants 6 but only 2 vehicles idle at a hub can be activated this step
		GuidanceState state = new GuidanceState(0, 10, 2, 0, 0.0);
		assertThat(defaultReconciler(6).toEmit(state, NOW)).isEqualTo(2);
	}

	@Test
	void reconcilerNeverRecalls_returnsZeroNotNegativeWhenOverDesired() {
		// active count exceeds every trigger's desire (e.g. after a demand spike subsided) → emit 0, never negative;
		// bringing the fleet down is the deactivation side's job.
		GuidanceState state = new GuidanceState(7, 10, 3, 2, 0.0);
		assertThat(defaultReconciler(0).toEmit(state, NOW)).isZero();
	}

	@Test
	void desired_isTheSharedTargetTheDeactivationSideRecallsDownTo() {
		// The deactivation side reads desired() (not toEmit) to know how far it may recall. With a buffer of 1 and 2
		// vehicles busy, the target is busy + buffer = 3 even though 6 are active and 4 idle in service — i.e. the target
		// sits BELOW the active count, which is exactly the signal to recall the idle surplus down to 3.
		ActivationReconciler r = ActivationReconciler.createDefault(0, 1, OptionalDouble.empty());
		GuidanceState state = new GuidanceState(6, 10, 0, 4, 0.0);
		assertThat(r.desired(state, NOW)).isEqualTo(3);
		// and it never proposes emitting on the way down
		assertThat(r.toEmit(state, NOW)).isZero();
	}

	@Test
	void desired_floorHoldsTheTargetUpWhenBufferWouldRecallEverything() {
		// buffer alone would target busy(0) + 1 = 1, but the minimum-fleet floor of 4 holds the shared target at 4, so the
		// deactivation side keeps 4 active in a full lull — the buffer-vs-floor churn cannot arise because both sides read
		// this one value.
		ActivationReconciler r = ActivationReconciler.createDefault(4, 1, OptionalDouble.empty());
		GuidanceState state = new GuidanceState(4, 10, 0, 4, 0.0);
		assertThat(r.desired(state, NOW)).isEqualTo(4);
	}

	@Test
	void rejectionTrigger_overThreshold_targetsFullCapacity() {
		// with a rejection trigger at threshold 0.1, a recent rejection rate of 0.3 pushes the target to the full
		// activation capacity of 10, regardless of the modest buffer/floor — demand pressure wins.
		ActivationReconciler r = ActivationReconciler.createDefault(0, 1, OptionalDouble.of(0.1));
		GuidanceState state = new GuidanceState(2, 10, 8, 0, 0.3);
		assertThat(r.desired(state, NOW)).isEqualTo(10);
		assertThat(r.toEmit(state, NOW)).isEqualTo(8); // ramp 2 → 10, limited by 8 idle-at-hub
	}

	@Test
	void rejectionTrigger_belowThreshold_defersToBufferAndFloor() {
		// same policy, but the rejection rate 0.05 is below the 0.1 threshold → the trigger proposes 0 and the shared
		// target falls back to the buffer/floor (here: buffer 1, nothing idle in service → target busy(2)+1 = 3).
		ActivationReconciler r = ActivationReconciler.createDefault(0, 1, OptionalDouble.of(0.1));
		GuidanceState state = new GuidanceState(2, 10, 8, 0, 0.05);
		assertThat(r.desired(state, NOW)).isEqualTo(3);
	}

	@Test
	void createFactory_bufferedPolicy_keepsOneBuffer() {
		// the config-selected 'buffered' policy behaves like the default: with 8 idle at hub it keeps just one buffer.
		ActivationReconciler r = ActivationReconciler.create(ActivationPolicy.buffered, 0, 1, OptionalDouble.empty());
		GuidanceState state = new GuidanceState(2, 10, 8, 0, 0.0);
		assertThat(r.toEmit(state, NOW)).isEqualTo(1);
	}

	@Test
	void createFactory_greedyPolicy_fillsCapacity() {
		// the config-selected 'greedy' policy pulls every idle-at-hub vehicle in, up to the capacity ceiling.
		ActivationReconciler r = ActivationReconciler.create(ActivationPolicy.greedy, 0, 1, OptionalDouble.empty());
		GuidanceState state = new GuidanceState(2, 10, 8, 0, 0.0);
		assertThat(r.desired(state, NOW)).isEqualTo(10); // busy target 2+8=10, at ceiling
		assertThat(r.toEmit(state, NOW)).isEqualTo(8);
	}

	@Test
	void createFactory_greedyPolicy_stillHonoursFloorButFloorIsDominated() {
		// greedy already targets the ceiling, so the minimum-fleet floor is dominated but does no harm when set.
		ActivationReconciler r = ActivationReconciler.create(ActivationPolicy.greedy, 3, 1, OptionalDouble.empty());
		GuidanceState state = new GuidanceState(0, 10, 2, 0, 0.0);
		// greedy target = 0+2 = 2; floor = 3 → max = 3; clamped to ceiling 10 → 3
		assertThat(r.desired(state, NOW)).isEqualTo(3);
	}

	@Test
	void createFactory_greedyPolicy_ignoresRejectionThreshold() {
		// under greedy the rejection trigger is not wired; a configured threshold has no additional effect (greedy
		// already targets the ceiling anyway, so this just documents that no RejectionRateActivation is added).
		ActivationReconciler r = ActivationReconciler.create(ActivationPolicy.greedy, 0, 1, OptionalDouble.of(0.1));
		GuidanceState state = new GuidanceState(1, 10, 0, 0, 0.3); // rate over threshold, but nothing idle at hub
		// greedy target = 1+0 = 1 (no idle-at-hub to pull); a wired rejection trigger would have targeted the capacity ceiling of 10.
		assertThat(r.desired(state, NOW)).isEqualTo(1);
	}
}
