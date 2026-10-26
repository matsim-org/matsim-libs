/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations.optimizer;

import org.matsim.contrib.drt.extension.operations.remoteoperations.schedule.IncidentHoldTask;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * Excludes a vehicle from request insertion for as long as it is dealing with a remote-guidance incident hold.
 * <p>
 * An {@link IncidentHoldTask} is spliced <em>into a running drive</em> at runtime (see
 * {@code IncidentDispatcher#beginHold}) — the diverted drive is curtailed, the hold is inserted right after it and a
 * fresh continuation drive follows. Incidents are never planned ahead: a vehicle therefore has at most one hold in its
 * schedule, and that hold is always the current or the very next task.
 * <p>
 * The base {@link org.matsim.contrib.drt.extension.operations.shifts.optimizer.ShiftVehicleDataEntryFactory} only skips
 * vehicles whose <em>current</em> task is an operational stop. That misses the narrow window right after the splice, in
 * which the (zero-length) diverted drive is still the current task and the hold is the next one. The hold then becomes a
 * regular {@code STOP} waypoint in {@link org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl}, an insertion is
 * generated across it, and {@code DefaultRequestInsertionScheduler.removeBetween} throws because it does not expect a
 * stop task between the two drive tasks it is splicing.
 * <p>
 * Since a held vehicle cannot usefully take new requests anyway (it is standing still until an operator frees it), the
 * clean and minimal fix — touching no core DRT code — is to take it out of the insertion pool entirely while a hold is
 * pending. This mirrors {@code DrtServiceEntryFactory}, which removes a vehicle while a service task is pending.
 * <p>
 * Detecting the hold is O(1): a pending hold is always either the current task or the immediately following one (see
 * {@code IncidentDispatcher#beginHold}, which splices at {@code divertedDrive.taskIdx + 1}), so only those two
 * positions need to be checked.
 *
 * @author nkuehnel / MOIA
 */
public class IncidentAwareVehicleDataEntryFactory implements VehicleEntry.EntryFactory {

	private final VehicleEntry.EntryFactory delegate;

	public IncidentAwareVehicleDataEntryFactory(VehicleEntry.EntryFactory delegate) {
		this.delegate = delegate;
	}

	@Override
	public VehicleEntry create(DvrpVehicle vehicle, double currentTime) {
		if (hasPendingIncidentHold(vehicle)) {
			return null;
		}
		return delegate.create(vehicle, currentTime);
	}

	private static boolean hasPendingIncidentHold(DvrpVehicle vehicle) {
		var schedule = vehicle.getSchedule();
		if (schedule.getStatus() != org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus.STARTED) {
			return false;
		}
		// Incidents are never planned ahead and beginHold() always splices the hold at (divertedDrive.taskIdx + 1).
		// A still-pending hold is therefore either the current task (vehicle already held) or the very next one (the
		// curtailed drive is still finishing). Once the continuation drive becomes current, the hold is PERFORMED and
		// sits behind the current index. So an O(1) check of the current and next task is both sufficient and exact.
		var tasks = schedule.getTasks();
		int currentIdx = schedule.getCurrentTask().getTaskIdx();
		if (tasks.get(currentIdx) instanceof IncidentHoldTask) {
			return true;
		}
		int nextIdx = currentIdx + 1;
		return nextIdx < tasks.size() && tasks.get(nextIdx) instanceof IncidentHoldTask;
	}
}
