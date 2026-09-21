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
 * Activation trigger keeping a responsiveness buffer of {@code desiredBuffer} vehicles idle in service: it targets
 * enough active vehicles that, on top of those currently busy, {@code desiredBuffer} are free to absorb an incoming
 * request without a hub activation delay.
 * <p>
 * The target is the absolute level {@code smoothedBusy + desiredBuffer}, where {@code smoothedBusy} is the busy count
 * {@code activeCount − idleInService} as smoothed by {@code BusyWindowTracker}. It may fall below the current active
 * count: when demand drops and too many vehicles sit idle, the target tells the deactivation side to recall the surplus
 * down to the buffer, while the activation side clamps at {@code max(0, target − active)} and so reads it as refilling the
 * buffer.
 * <p>
 * Because both sides read this one target, a demand lull settles at {@code desiredBuffer} idle vehicles rather than
 * activating every idle-at-hub vehicle and then churning it out on the idle timeout. Using the smoothed rather than the
 * instantaneous busy count damps the same oscillation at peak demand, where the raw count jitters from second to second.
 *
 * @author nkuehnel / MOIA
 */
public final class IdleBufferActivation implements ActivationTrigger {

	private final int desiredBuffer;

	public IdleBufferActivation(int desiredBuffer) {
		this.desiredBuffer = desiredBuffer;
	}

	@Override
	public int desiredActive(GuidanceState state, double now) {
		// hold desiredBuffer vehicles free on top of the smoothed busy ones. May be below activeCount, which asks the
		// deactivation side to recall the idle surplus; the activation side clamps negative gaps to 0.
		return state.smoothedBusy() + desiredBuffer;
	}
}
