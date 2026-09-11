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
import org.matsim.contrib.drt.extension.operations.remoteoperations.RemoteGuidanceOperators.Operator;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-iteration runtime state of the operator pool: which operators have started, which have been released and when, and
 * which are currently processing an incident. The runtime companion to {@link RemoteGuidanceOperators}, which holds the
 * immutable specification and is therefore safe to share across iterations, while everything mutable lives here and is
 * cleared by {@link #reset()}.
 * <p>
 * An operator is <em>on duty for coverage</em> from its planned start until it is released. That window may extend past
 * the planned end, because an operator is only released once its vehicles no longer need it and it holds no incident, so
 * that the supervised fleet never exceeds the available capacity. Its <em>effective</em> end time is the deferred end
 * that results.
 *
 * @author nkuehnel / MOIA
 */
public final class RemoteGuidanceOperatorState {

	private final RemoteGuidanceOperators registry;

	private final Map<Id<DrtShift>, Runtime> runtimeById = new LinkedHashMap<>();

	/** Per-operator mutable runtime lifecycle state. */
	private static final class Runtime {
		private boolean started = false;
		private boolean released = false;
		private boolean incidentBusy = false;
		private double effectiveEndTime = Double.NaN; // set when released; the deferred (or on-time) actual end
	}

	public RemoteGuidanceOperatorState(RemoteGuidanceOperators registry) {
		this.registry = registry;
		reset();
	}

	/** Re-initialises the runtime state for a fresh iteration: every operator un-started, un-released, not busy. */
	public void reset() {
		runtimeById.clear();
		for (Id<DrtShift> id : registry.getOperators().keySet()) {
			runtimeById.put(id, new Runtime());
		}
	}

	/**
	 * @return the summed capacity of all operators on duty for coverage at {@code now}, including those retained past
	 * their planned end. The active supervised fleet must never exceed this, and it is the {@link IncidentDispatcher}'s
	 * server pool.
	 */
	public int coverageCapacityAt(double now) {
		int capacity = 0;
		for (Operator operator : registry.getOperators().values()) {
			if (onDutyForCoverage(operator, now)) {
				capacity += operator.capacity();
			}
		}
		return capacity;
	}

	/** @return the number of operators on duty for coverage at {@code now}. */
	public int onDutyForCoverageCount(double now) {
		int count = 0;
		for (Operator operator : registry.getOperators().values()) {
			if (onDutyForCoverage(operator, now)) {
				count++;
			}
		}
		return count;
	}

	/** On duty for supervision and incident processing, i.e. started and not yet released. */
	public boolean onDutyForCoverage(Operator operator, double now) {
		Runtime runtime = runtimeById.get(operator.id());
		return operator.startTime() <= now && !runtime.released;
	}

	/**
	 * Marks the operators whose planned start has been reached as started. Idempotent per operator; the caller emits the
	 * lifecycle event.
	 *
	 * @return the operators that transitioned to started on this call, in registry order.
	 */
	public List<Operator> markStarted(double now) {
		List<Operator> started = new ArrayList<>();
		for (Operator operator : registry.getOperators().values()) {
			Runtime runtime = runtimeById.get(operator.id());
			if (!runtime.started && operator.startTime() <= now) {
				runtime.started = true;
				started.add(operator);
			}
		}
		return started;
	}

	/**
	 * Marks whether the operator is currently processing an incident. Called by the {@link IncidentDispatcher} on
	 * assignment and on resolution. A busy operator is never released.
	 */
	public void setIncidentBusy(Id<DrtShift> operatorId, boolean busy) {
		Runtime runtime = runtimeById.get(operatorId);
		if (runtime != null) {
			runtime.incidentBusy = busy;
		}
	}

	/**
	 * Releases operators that have reached their planned end, one at a time and only as far as capacity allows: an
	 * operator is freed if it holds no incident and the supervised fleet still fits under the capacity that remains after
	 * removing it. The check is re-evaluated per operator, so several operators ending in the same step release only as
	 * far as the shrinking active fleet permits. Their effective end time is fixed to {@code now}.
	 *
	 * @return the operators released on this call, in registry order, so the caller can emit their ended events.
	 */
	public List<Operator> releaseElapsedOperators(double now, int activeSupervised) {
		List<Operator> released = new ArrayList<>();
		for (Operator operator : registry.getOperators().values()) {
			Runtime runtime = runtimeById.get(operator.id());
			if (isPendingRelease(operator, now) && !runtime.incidentBusy
					&& activeSupervised <= coverageCapacityAt(now) - operator.capacity()) {
				runtime.released = true;
				runtime.effectiveEndTime = now;
				released.add(operator);
			}
		}
		return released;
	}

	/** True once the operator has reached its planned end but has not been released yet. */
	public boolean isPendingRelease(Operator operator, double now) {
		Runtime runtime = runtimeById.get(operator.id());
		return now >= operator.plannedEndTime() && !runtime.released;
	}

	public boolean isReleased(Id<DrtShift> operatorId) {
		return runtimeById.get(operatorId).released;
	}

	public boolean isIncidentBusy(Id<DrtShift> operatorId) {
		return runtimeById.get(operatorId).incidentBusy;
	}

	/** @return the operator's effective, possibly deferred end time once released, else {@code NaN}. */
	public double effectiveEndTime(Id<DrtShift> operatorId) {
		return runtimeById.get(operatorId).effectiveEndTime;
	}
}
