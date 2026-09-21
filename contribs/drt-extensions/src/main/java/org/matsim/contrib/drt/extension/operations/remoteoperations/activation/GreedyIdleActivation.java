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
 * Greedy activation trigger: activate every idle-at-hub vehicle while
 * capacity allows. It proposes to bring the active count up by the whole pool of idle-at-hub vehicles.
 * <p>
 * Kept as a first-class trigger for comparison / backwards compatibility, but <b>not</b> part of the default trigger
 * set: on its own it drives the active fleet to the capacity ceiling regardless of demand and re-emits every recalled vehicle
 * next step, which is exactly the low-demand sawtooth the default {@link IdleBufferActivation} damps. Plug it in
 * explicitly to recover the greedy baseline.
 *
 * @author nkuehnel / MOIA
 */
public final class GreedyIdleActivation implements ActivationTrigger {

	@Override
	public int desiredActive(GuidanceState state, double now) {
		return state.activeCount() + state.idleAtHub();
	}
}
