/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.config;

/**
 * Selects which remote guidance activation policy the {@code ActivationReconciler} is built from. Both
 * extensive-margin sides (the scheduler ramping up, the shift-end logic recalling down) build an identical reconciler
 * from this choice, so they always share one fleet-sizing target.
 *
 * @author nkuehnel / MOIA
 */
public enum ActivationPolicy {
	/**
	 * The default demand-responsive policy: the {@code minActiveFleet} floor plus one idle-in-service responsiveness
	 * buffer, and a demand-driven rejection trigger if {@code rejectionActivation} is configured. Keeps only a small ready
	 * buffer active during lulls, which damps the low-demand activate/recall sawtooth.
	 */
	buffered,
	/**
	 * Activates every idle-at-hub vehicle while capacity allows, driving the active fleet to the capacity ceiling
	 * regardless of demand. The floor still applies but is dominated; the responsiveness buffer and the rejection trigger
	 * have no effect here and are not wired.
	 */
	greedy
}
