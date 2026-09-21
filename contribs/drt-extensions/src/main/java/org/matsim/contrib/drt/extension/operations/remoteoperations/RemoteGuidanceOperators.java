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
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftSpecification;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The immutable, spec-derived registry of remote guidance operators. An operator is nothing but a {@link DrtShift}
 * whose type equals the configured operator shift type. There is no wrapper object and no per-vehicle binding.
 * This registry is the single source of truth for the operator roster and its <em>planned</em> quantities.
 * <p>
 * <b>Spec vs. runtime (deliberate split).</b> This class holds ONLY what the shift specification determines and never
 * changes during a run: each operator's start time, planned end time and passive-supervision capacity κ, and the
 * schedule-derived aggregates over them. It carries <em>no mutable runtime state</em> — the operators' runtime
 * lifecycle (deferred release, incident-busy marking, effective end times) lives in the separate QSim-lifecycle
 * {@link RemoteGuidanceOperatorState}, which reads this registry for the immutable facts. Because there is no mutable
 * state here, there is nothing to reset between iterations, and this can safely be a single cross-iteration instance.
 * <p>
 * <b>Two capacities.</b> An operator has a <em>planned</em> end and, at runtime, an <em>effective</em> end that
 * may be deferred past it (see {@link RemoteGuidanceOperatorState}) so the coverage invariant "never more vehicles
 * supervised than can be supervised simultaneously" is never broken. The two capacities are therefore:
 * <ul>
 *     <li>{@link #activationCapacityAt(double)} — the summed capacity of operators still within their <em>planned</em> window.
 *         The ceiling for <em>activating new</em> vehicles: an operator winding down must not pull new vehicles in. This
 *         is a pure spec quantity, hence it lives here.</li>
 *     <li>the coverage capacity — the summed capacity of operators on duty in the <em>runtime</em> sense (planned window OR
 *         retained past it). This depends on runtime release state and therefore lives in
 *         {@link RemoteGuidanceOperatorState#coverageCapacityAt(double)}.</li>
 * </ul>
 * <p>
 * Capacity κ is uniform (from config) for now; per-operator heterogeneous capacity would be an additive later step
 * (attach κ as a persistent {@link DrtShift} attribute), needing no structural change here.
 *
 * @author nkuehnel / MOIA
 */
public final class RemoteGuidanceOperators {

	/** One operator = one operator-type shift: an immutable planned window and a passive-supervision capacity κ. */
	public static final class Operator {
		private final Id<DrtShift> id;
		private final double startTime;
		private final double plannedEndTime;
		private final int capacity;

		private Operator(Id<DrtShift> id, double startTime, double plannedEndTime, int capacity) {
			this.id = id;
			this.startTime = startTime;
			this.plannedEndTime = plannedEndTime;
			this.capacity = capacity;
		}

		public Id<DrtShift> id() {
			return id;
		}

		public double startTime() {
			return startTime;
		}

		public double plannedEndTime() {
			return plannedEndTime;
		}

		public int capacity() {
			return capacity;
		}

		/** On duty for activating new vehicles: within the planned window (a winding-down operator does not qualify). */
		public boolean onDutyForActivation(double now) {
			return startTime <= now && now < plannedEndTime;
		}
	}

	private final Map<Id<DrtShift>, Operator> operators = new LinkedHashMap<>();

	private RemoteGuidanceOperators(Map<Id<DrtShift>, Operator> operators) {
		this.operators.putAll(operators);
	}

	/**
	 * Builds the registry from the shift specification, taking every shift of {@code operatorShiftType} as an operator
	 * with the given uniform capacity.
	 */
	public static RemoteGuidanceOperators fromSpecification(DrtShiftsSpecification specification, String operatorShiftType,
															int capacity) {
		Map<Id<DrtShift>, Operator> operators = new LinkedHashMap<>();
		for (DrtShiftSpecification spec : specification.getShiftSpecifications().values()) {
			if (spec.getShiftType().map(operatorShiftType::equals).orElse(false)) {
				operators.put(spec.getId(), new Operator(spec.getId(), spec.getStartTime(), spec.getEndTime(), capacity));
			}
		}
		return new RemoteGuidanceOperators(operators);
	}

	/**
	 * @return the activation ceiling at {@code t}: the summed capacity of operators still within their planned window
	 * at {@code now}. Caps how many vehicles may be <em>newly activated</em>; excludes winding-down (pending-release)
	 * operators. Pure spec quantity (no runtime state).
	 */
	public int activationCapacityAt(double now) {
		int capacity = 0;
		for (Operator operator : operators.values()) {
			if (operator.onDutyForActivation(now)) {
				capacity += operator.capacity;
			}
		}
		return capacity;
	}

	/**
	 * @return the number of operators whose <em>planned</em> window covers {@code now} (i.e.
	 * {@code startTime <= now < plannedEndTime}). Depends only on the immutable shift schedule, so it is safe to query
	 * retroactively for a historical time (e.g. an end-of-iteration utilisation series). This is the incident
	 * server-pool size, since an incident always occupies exactly one operator regardless of its capacity, so it is the correct
	 * <em>planned</em> utilisation denominator (a numerator that momentarily exceeds it reflects an operator retained
	 * past its planned end to finish a queued incident).
	 */
	public int plannedOnDutyCount(double now) {
		int count = 0;
		for (Operator operator : operators.values()) {
			if (operator.onDutyForActivation(now)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * @return the minimum activation capacity over the look-ahead window {@code [from, to]}. Since activation capacity
	 * is a step function that only changes at operator start / planned-end times, the minimum over the window is the
	 * minimum of its values at {@code from} and at every operator boundary that falls inside {@code (from, to]}. Used by
	 * the recall logic to start bringing vehicles home <em>before</em> an operator's planned end, so they reach a hub in
	 * time (a proactive recall lead).
	 */
	public int minActivationCapacity(double from, double to) {
		int min = activationCapacityAt(from);
		for (Operator operator : operators.values()) {
			if (operator.startTime > from && operator.startTime <= to) {
				min = Math.min(min, activationCapacityAt(operator.startTime));
			}
			if (operator.plannedEndTime > from && operator.plannedEndTime <= to) {
				min = Math.min(min, activationCapacityAt(operator.plannedEndTime));
			}
		}
		return min;
	}

	/**
	 * @return the maximum activation capacity reached at any point over the operator schedule — the largest
	 * number of vehicles that can ever be supervised simultaneously. Since activation capacity is a step function that
	 * only changes at operator start / planned-end times, the maximum is attained at one of the operator start times.
	 * Used only for a config sanity warning (a floor that exceeds this can never be met).
	 */
	public int maxActivationCapacity() {
		int max = 0;
		for (Operator operator : operators.values()) {
			max = Math.max(max, activationCapacityAt(operator.startTime));
		}
		return max;
	}

	public Map<Id<DrtShift>, Operator> getOperators() {
		return operators;
	}

	public int size() {
		return operators.size();
	}
}
