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
 * One concern in the remote guidance activation policy. A trigger reads the current {@link GuidanceState} and proposes
 * how many vehicles should be active, as an absolute count rather than a delta. {@link ActivationReconciler} combines
 * several triggers by taking their maximum, so any one of them may pull the fleet up, and clamps the result to
 * {@link GuidanceState#activationCapacity()}. The combined target governs deactivation as well, so adding a trigger also
 * constrains how far the deactivation side may recall.
 * <p>
 * Absolute counts rather than deltas mean two triggers reacting to the same signal name the same target and the maximum
 * de-duplicates them, where summing deltas would double-count. A target may fall below the current active count, for
 * instance a responsiveness buffer once demand drops: that is how a trigger asks the deactivation side to ramp down,
 * while the activation side clamps the negative gap to zero.
 *
 * @author nkuehnel / MOIA
 */
@FunctionalInterface
public interface ActivationTrigger {

	/**
	 * @return the number of vehicles this trigger wants active at {@code now}. Values outside {@code [0, capacity]} are
	 * fine, since the reconciler clamps, so a trigger should express its raw intent.
	 */
	int desiredActive(GuidanceState state, double now);
}
