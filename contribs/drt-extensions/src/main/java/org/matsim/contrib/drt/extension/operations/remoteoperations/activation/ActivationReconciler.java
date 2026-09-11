/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.activation;

import org.matsim.contrib.drt.extension.operations.remoteoperations.config.ActivationPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Combines a set of {@link ActivationTrigger}s into the single fleet-sizing target that governs remote guidance
 * activation: the number of vehicles that <em>should</em> be active right now ({@link #desired}). This is pure
 * policy over a {@link GuidanceState} snapshot — no fleet, no side effects — so it is unit-testable with a hand-built
 * state.
 * <p>
 * <b>One shared target for both margins.</b> {@code desired} is read by <em>both</em> sides of activation and deactivation:
 * the {@link org.matsim.contrib.drt.extension.operations.remoteoperations.RemoteGuidanceScheduler} turns it into how many new
 * virtual shifts to {@link #toEmit emit} (ramp up), and the deactivation side
 * ({@code RemoteGuidanceShiftEndLogic}) uses the same value to decide how far it may recall idle vehicles (ramp down).
 * Because every upward force — minimum-fleet floor ({@link MinFleetActivation}), responsiveness buffer
 * ({@link IdleBufferActivation}), and any future demand-driven trigger — is an ordinary trigger combined here by
 * {@code max}, both sides always agree on the target and neither side re-derives it separately.
 * <p>
 * Reconciliation (absolute target + max, OR semantics):
 * <pre>
 *     desired = max over triggers of trigger.desiredActive(state, now)   // any trigger may pull the target up
 *     desired = clamp(desired, 0, state.activationCapacity())            // hard ceiling (a physical limit)
 *     toEmit  = max(0, desired - state.activeCount())                    // activation only ramps up …
 *     toEmit  = min(toEmit, state.idleAtHub())                           // … and only as far as idle-at-hub allows
 * </pre>
 * The reconciler itself never recalls; a target below the active count simply yields {@code toEmit == 0}. Ramping down
 * to {@code desired} is the deactivation side's job (subject to its own timeout / passenger-in-service constraints).
 *
 * @author nkuehnel / MOIA
 */
public final class ActivationReconciler {

	private final List<ActivationTrigger> triggers;

	public ActivationReconciler(List<ActivationTrigger> triggers) {
		this.triggers = List.copyOf(triggers);
	}

	/**
	 * Builds the reconciler for the configured {@link ActivationPolicy}. This is the one place the trigger
	 * set is defined per policy, so the scheduler (activation) and the shift-end logic (deactivation) build an identical
	 * reconciler and thus share the same {@link #desired} target.
	 * <ul>
	 *     <li>{@link ActivationPolicy#buffered} — minimum-fleet floor {@link MinFleetActivation} + a single
	 *         {@link IdleBufferActivation} responsiveness buffer + (iff a threshold is configured) a demand-driven
	 *         {@link RejectionRateActivation}. Damps the low-demand sawtooth.</li>
	 *     <li>{@link ActivationPolicy#greedy} — minimum-fleet floor + {@link GreedyIdleActivation} (activate every
	 *         idle-at-hub vehicle up to the capacity ceiling). The buffer and rejection trigger are irrelevant under greedy (dominated) and
	 *         are not wired.</li>
	 * </ul>
	 *
	 * @param rejectionRateThreshold if present (and policy is buffered), adds a {@link RejectionRateActivation} at this
	 *                               threshold; if empty, no demand-driven trigger is wired.
	 */
	public static ActivationReconciler create(ActivationPolicy policy, int minActiveFleet, int readyBufferSize,
			OptionalDouble rejectionRateThreshold) {
		List<ActivationTrigger> triggers = new ArrayList<>();
		triggers.add(new MinFleetActivation(minActiveFleet));
		switch (policy) {
			case buffered -> {
				triggers.add(new IdleBufferActivation(readyBufferSize));
				rejectionRateThreshold.ifPresent(threshold -> triggers.add(new RejectionRateActivation(threshold)));
			}
			case greedy -> triggers.add(new GreedyIdleActivation());
		}
		return new ActivationReconciler(triggers);
	}

	/**
	 * Convenience factory for the default {@link ActivationPolicy#buffered} policy, for callers that do not vary it.
	 */
	public static ActivationReconciler createDefault(int minActiveFleet, int readyBufferSize,
			OptionalDouble rejectionRateThreshold) {
		return create(ActivationPolicy.buffered, minActiveFleet, readyBufferSize, rejectionRateThreshold);
	}

	/**
	 * The shared fleet-sizing target: how many vehicles should be active given {@code state}, i.e. the {@code max} over
	 * all triggers clamped to the activation capacity. Always in {@code [0, activationCapacity]}. Both the
	 * activation and the deactivation side read this same value.
	 */
	public int desired(GuidanceState state, double now) {
		int desired = 0;
		for (ActivationTrigger trigger : triggers) {
			desired = Math.max(desired, trigger.desiredActive(state, now));
		}
		// cap at the hard ceiling last: the activation capacity is a physical limit and must win over any trigger.
		return Math.min(Math.max(0, desired), state.activationCapacity());
	}

	/**
	 * @return how many new virtual shifts to emit at {@code now}, given the current {@code state}. Always {@code >= 0}
	 * and never more than the free activation capacity nor the number of idle-at-hub vehicles.
	 */
	public int toEmit(GuidanceState state, double now) {
		int toEmit = Math.max(0, desired(state, now) - state.activeCount());
		return Math.min(toEmit, state.idleAtHub());
	}
}
