/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.activation;

/**
 * Demand-driven activation trigger: while the recent request-rejection rate
 * ({@link GuidanceState#recentRejectionRate()}) exceeds {@code rejectionRateThreshold}, it targets the full activation
 * capacity, i.e. "under demand pressure, put every vehicle the operator pool can supervise into service".
 * Below the threshold it proposes nothing (target 0), so the responsiveness buffer and the minimum-fleet floor govern the
 * fleet size again and it ramps back down.
 * <p>
 * Because triggers return an absolute target combined by {@code max} and that combined target governs both margins
 * (activation ramps up, deactivation recalls down), this trigger automatically prevents the deactivation side from
 * recalling vehicles while rejections are elevated — no extra deactivation-side wiring needed.
 *
 * @author nkuehnel / MOIA
 */
public final class RejectionRateActivation implements ActivationTrigger {

	private final double rejectionRateThreshold;

	public RejectionRateActivation(double rejectionRateThreshold) {
		this.rejectionRateThreshold = rejectionRateThreshold;
	}

	@Override
	public int desiredActive(GuidanceState state, double now) {
		// strictly ABOVE the threshold → go to the ceiling; the reconciler clamps to the actual capacity anyway, so naming the
		// capacity here is exact. At or below the threshold → 0 (buffer/floor take over). Strict '>' means threshold 0.0
		// fires on any non-zero rejection rate, and a rate exactly equal to the threshold does not fire.
		return state.recentRejectionRate() > rejectionRateThreshold ? state.activationCapacity() : 0;
	}
}
