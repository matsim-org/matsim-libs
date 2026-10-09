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
 * Trigger expressing a hard floor on the active fleet: a constant target of {@code minActiveFleet}
 * vehicles regardless of demand, e.g. a mandated minimum level of supervised service.
 * <p>
 * <b>Why the floor is a trigger and not a special-cased seed.</b> The floor is one of several forces that set the shared
 * fleet-sizing target {@link ActivationReconciler#desired}, and that target governs <em>both</em> activation and
 * deactivation. Modelling the floor as an ordinary trigger — combined by {@code max} like every other — means both sides
 * read it through the same single value: the deactivation side never recalls below the floor, and the activation side
 * pulls up to it, with no separately re-derived floor that the two sides could disagree on. (In the earlier
 * activation-only design this was a redundant {@code nMin} seed on the reconciler and this trigger was dropped; once the
 * target became shared with deactivation, a uniform trigger is the single source again.)
 *
 * @author nkuehnel / MOIA
 */
public final class MinFleetActivation implements ActivationTrigger {

	private final int minActiveFleet;

	public MinFleetActivation(int minActiveFleet) {
		this.minActiveFleet = minActiveFleet;
	}

	@Override
	public int desiredActive(GuidanceState state, double now) {
		return minActiveFleet;
	}
}
