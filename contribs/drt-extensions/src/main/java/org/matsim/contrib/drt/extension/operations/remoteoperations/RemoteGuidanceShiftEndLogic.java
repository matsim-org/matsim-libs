/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.ActivationReconciler;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.GuidanceState;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.ShiftEndLogic;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftSchedules;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The deactivation side of remote guidance. Virtual shifts emitted by {@link RemoteGuidanceScheduler} carry no scheduled
 * end of their own, so they run until this {@link ShiftEndLogic} recalls the vehicle, on either of two triggers:
 * <ol>
 *     <li><b>capacityExceeded</b>: an operator goes off duty, the activation capacity drops, and any vehicles supervised
 *         beyond the new ceiling are recalled. Idle vehicles are chosen first, as the least disruptive.</li>
 *     <li><b>idleTimeout</b>: a supervised vehicle that has been idle in service (on a {@link DrtStayTask} that is the
 *         last task in its schedule) for longer than the configured timeout is recalled, but only down to the target
 *         {@link ActivationReconciler} returns. Since the activation side ramps up to the same target and this side only
 *         recalls down to it, a demand lull settles at the target instead of oscillating, without needing a cooldown.</li>
 * </ol>
 * The recall itself is attempt-and-defer: the dispatcher keeps a vehicle that cannot be routed to a hub running and
 * reconsiders it next step.
 * <p>
 * The decision is memoised per simulation second. The dispatcher calls {@link #shiftEndsEarly} once per active shift, but
 * the recall set is a function of the whole active virtual fleet, so it is computed once per {@code now} and then
 * answered per entry.
 *
 * @author nkuehnel / MOIA
 */
public final class RemoteGuidanceShiftEndLogic implements ShiftEndLogic {

	private final Fleet fleet;
	private final RemoteGuidanceOperators operators;
	private final double idleTimeout;
	private final double recallLeadTime;
	private final ActivationReconciler reconciler;
	// the same instance the scheduler reads, so both margins see one rejection rate; null when no rejection activation
	// is configured
	private final RejectionRateTracker rejectionRateTracker;
	// the same instance the scheduler reads, so both margins size the fleet from one smoothed busy signal. A window of 0
	// makes it a pass-through.
	private final BusyWindowTracker busyWindowTracker;

	private double lastComputedTime = Double.NaN;
	private Set<Id<DrtShift>> recallSet = new HashSet<>();

	public RemoteGuidanceShiftEndLogic(Fleet fleet, RemoteGuidanceOperators operators, double idleTimeout,
									   double recallLeadTime, ActivationReconciler reconciler,
									   RejectionRateTracker rejectionRateTracker, BusyWindowTracker busyWindowTracker) {
		this.fleet = fleet;
		this.operators = operators;
		this.idleTimeout = idleTimeout;
		this.recallLeadTime = recallLeadTime;
		this.reconciler = reconciler;
		this.rejectionRateTracker = rejectionRateTracker;
		this.busyWindowTracker = busyWindowTracker;
	}

	@Override
	public boolean shiftEndsEarly(DrtShiftDispatcher.ShiftEntry activeShift, double now) {
		if (!isVirtualShift(activeShift.shift())) {
			// regular driver shifts run to their scheduled end and are never recalled here
			return false;
		}
		if (now != lastComputedTime) {
			recallSet = selectRecalls(now);
			lastComputedTime = now;
		}
		return recallSet.contains(activeShift.shift().getId());
	}

	/**
	 * Determines which shifts to recall this step, across the whole supervised virtual fleet: idle vehicles beyond the
	 * timeout down to the target from {@link ActivationReconciler#desired}, plus, if the active count still exceeds the
	 * look-ahead ceiling, enough further vehicles (idle first) to bring it back under capacity.
	 */
	private Set<Id<DrtShift>> selectRecalls(double now) {
		List<ShiftDvrpVehicle> active = new ArrayList<>();
		int idleAtHub = 0;
		for (DvrpVehicle vehicle : fleet.getVehicles().values()) {
			if (!(vehicle instanceof ShiftDvrpVehicle shiftVehicle)) {
				continue;
			}
			// key on the started virtual shift, not the queue head: the shift queue is ordered by start time and holds
			// assigned but unstarted future shifts, so its head may be a shift that has not started yet.
			boolean runningVirtual = RemoteGuidanceScheduler.startedVirtualShift(shiftVehicle).isPresent();
			if (runningVirtual
					&& vehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED
					// exclude vehicles already recalled and routing home. A deferred recall has not materialised a
					// changeover yet, so such a vehicle stays counted and is retried next step.
					&& !isAlreadyLeaving(shiftVehicle)) {
				active.add(shiftVehicle);
			} else if (shiftVehicle.getShifts().isEmpty()
					&& vehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED
					&& vehicle.getSchedule().getCurrentTask() instanceof WaitForShiftTask) {
				// out of service and waiting at a hub, i.e. an activation source for the GuidanceState below
				idleAtHub++;
			}
		}

		Set<Id<DrtShift>> recalled = new HashSet<>();

		// idleTimeout: recall idle vehicles beyond the timeout, but only the surplus above the shared target. keepIdle is
		// the number of idle vehicles the target wants held, i.e. the target minus the busy vehicles it is already
		// covered by, clamped to what is actually idle. The longest-idle vehicles are recalled first, as the strongest
		// demand-slack signal, and the freshest are kept.
		List<ShiftDvrpVehicle> idleInService = active.stream()
				.filter(this::isIdleInService)
				.sorted(Comparator.comparingDouble((ShiftDvrpVehicle v) -> idleInServiceElapsed(v, now))
						.thenComparing(v -> v.getId().toString()))
				.toList();
		int desired = desiredActiveCount(active.size(), idleInService.size(), idleAtHub, now);
		int keepIdle = idleToKeep(active.size(), idleInService.size(), desired);
		for (int i = keepIdle; i < idleInService.size(); i++) {
			ShiftDvrpVehicle vehicle = idleInService.get(i);
			if (idleInServiceElapsed(vehicle, now) > idleTimeout) {
				recalled.add(startedVirtualShiftId(vehicle));
			}
		}

		// capacityExceeded: if still over the ceiling, recall the excess, idle vehicles first. The ceiling is the minimum
		// activation capacity over the look-ahead window [now, now + recallLeadTime] rather than the capacity at now, so
		// vehicles start heading home before an operator's planned end and reach a hub in time.
		int ceiling = operators.minActivationCapacity(now, now + recallLeadTime);
		int excess = active.size() - ceiling;
		if (excess > recalled.size()) {
			active.stream()
					.filter(v -> !recalled.contains(startedVirtualShiftId(v)))
					.sorted(Comparator.comparingDouble((ShiftDvrpVehicle v) -> isIdleInService(v) ? 0 : 1)
							.thenComparing(v -> v.getId().toString()))
					.limit(excess - recalled.size())
					.forEach(v -> recalled.add(startedVirtualShiftId(v)));
		}

		return recalled;
	}

	/**
	 * How many idle vehicles to spare from the idle-timeout recall so that the active count settles at {@code desired}:
	 * the target minus the {@code busy = activeCount − idleInService} vehicles it is already covered by, clamped to
	 * {@code [0, idleInService]}. This is what keeps a demand lull from oscillating, since the activation side ramps up to
	 * the same target.
	 */
	static int idleToKeep(int activeCount, int idleInService, int desired) {
		int busy = activeCount - idleInService;
		return Math.min(Math.max(0, desired - busy), idleInService);
	}

	/**
	 * The target active fleet size this step, from the same {@link ActivationReconciler#desired} policy the activation side
	 * uses, so the two cannot disagree on where the fleet should settle.
	 * <p>
	 * The two snapshots differ in {@code activeCount}: this side excludes vehicles already routing home, whereas the
	 * scheduler counts every live virtual shift. Both derive {@code busy} over the non-leaving set, however, so the signal
	 * they feed the shared {@link BusyWindowTracker} agrees. That matters, because counting leaving vehicles as busy would
	 * activate a replacement for every recalled vehicle. A trigger reading {@code activeCount} directly rather than
	 * {@code busy} would have to account for the leaving transient itself. The ceiling used here is the planned-window
	 * capacity at {@code now}, as on the activation side; the look-ahead reduction belongs to the capacity pass above.
	 */
	private int desiredActiveCount(int activeCount, int idleInService, int idleAtHub, double now) {
		double rejectionRate = rejectionRateTracker == null ? 0.0 : rejectionRateTracker.rejectionRate(now);
		// feed this side's busy observation into the shared smoother as well and read back the trailing maximum. Since the
		// aggregate is a maximum, both margins sampling per step is harmless: this side's busy is never above the
		// scheduler's, so it cannot lower the reported peak.
		int smoothedBusy = busyWindowTracker.sample(now, activeCount - idleInService);
		GuidanceState state = new GuidanceState(activeCount, operators.activationCapacityAt(now), idleAtHub,
				idleInService, smoothedBusy, rejectionRate);
		return reconciler.desired(state, now);
	}

	/**
	 * @return how long the vehicle has been idle in service: {@code now - beginTime} if the current task is a
	 * {@link DrtStayTask} that is the last task in the schedule, i.e. no committed future work, else {@code 0}.
	 */
	private double idleInServiceElapsed(ShiftDvrpVehicle vehicle, double now) {
		return isIdleInService(vehicle) ? now - vehicle.getSchedule().getCurrentTask().getBeginTime() : 0.0;
	}

	/**
	 * @return {@code true} if this vehicle has already been recalled. A virtual shift materialises no changeover tail of
	 * its own, so any changeover in the schedule comes from a recall.
	 */
	private boolean isAlreadyLeaving(ShiftDvrpVehicle vehicle) {
		return ShiftSchedules.getNextShiftChangeover(vehicle.getSchedule()).isPresent();
	}

	private boolean isIdleInService(ShiftDvrpVehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
			return false;
		}
		Task current = schedule.getCurrentTask();
		return current instanceof DrtStayTask && current.equals(Schedules.getLastTask(schedule));
	}

	/**
	 * The id of the vehicle's running virtual shift. Only called for members of {@code active}, which are already known to
	 * have one, and keyed on the started shift rather than the queue head, which may be a future shift.
	 */
	private static Id<DrtShift> startedVirtualShiftId(ShiftDvrpVehicle vehicle) {
		return RemoteGuidanceScheduler.startedVirtualShift(vehicle).orElseThrow().getId();
	}

	private static boolean isVirtualShift(DrtShift shift) {
		return shift.getShiftType().map(RemoteGuidanceScheduler.VIRTUAL_SHIFT_TYPE::equals).orElse(false);
	}
}
