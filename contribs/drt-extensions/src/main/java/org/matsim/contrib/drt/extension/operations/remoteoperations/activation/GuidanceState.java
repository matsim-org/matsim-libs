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
 * An immutable read-model of remote guidance activation at one simulation second. Every
 * {@link ActivationTrigger} decides purely from this snapshot — it never sees the raw {@code Fleet} — so triggers are
 * unit-testable without a running QSim (just construct a {@code GuidanceState}). The scheduler builds one snapshot per
 * step from the fleet and the operator registry.
 * <p>
 * The two idle counts are distinct: {@code idleAtHub} vehicles wait out of service on a {@code WaitForShiftTask} and are
 * the only ones that can be brought in, while {@code idleInService} vehicles are already active and merely have no
 * committed work.
 *
 * @param activeCount         number of currently active (supervised) virtual shifts.
 * @param activationCapacity  the summed capacity of operators within their planned window, i.e. the
 *                            hard upper bound on the active count.
 * @param idleAtHub           number of out-of-service vehicles waiting at a hub (activation source).
 * @param idleInService       number of active vehicles idle in service with no committed work (ready buffer).
 * @param smoothedBusy        the busy count {@code activeCount − idleInService}, as the trailing-window maximum from
 *                            {@code BusyWindowTracker}, or the instantaneous value when no window is configured. The
 *                            demand-load signal {@code IdleBufferActivation} sizes its buffer on top of.
 * @param recentRejectionRate the recent rejection rate {@code rejected / (rejected + scheduled)} over a trailing window,
 *                            from {@code RejectionRateTracker}, or 0 when no rejection activation is configured. The
 *                            demand-pressure signal read by {@code RejectionRateActivation}.
 *
 * @author nkuehnel / MOIA
 */
public record GuidanceState(int activeCount, int activationCapacity, int idleAtHub, int idleInService,
							int smoothedBusy, double recentRejectionRate) {

	/**
	 * Convenience constructor without smoothing: {@code smoothedBusy} defaults to the instantaneous
	 * {@code activeCount − idleInService}. Used when no busy window is configured, and by the unit tests.
	 */
	public GuidanceState(int activeCount, int activationCapacity, int idleAtHub, int idleInService,
						 double recentRejectionRate) {
		this(activeCount, activationCapacity, idleAtHub, idleInService, activeCount - idleInService, recentRejectionRate);
	}
}
