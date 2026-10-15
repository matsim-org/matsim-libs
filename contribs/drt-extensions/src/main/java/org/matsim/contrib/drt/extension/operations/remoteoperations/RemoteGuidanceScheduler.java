/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.ActivationReconciler;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.ActivationTrigger;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.GuidanceState;
import org.matsim.contrib.drt.extension.operations.remoteoperations.activation.IdleBufferActivation;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.RemoteGuidanceParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.RemoteGuidanceOperatorEndedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.RemoteGuidanceOperatorStartedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleActivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.VehicleDeactivatedForRemoteGuidanceEvent.DeactivationReason;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DefaultShiftScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.ShiftScheduler;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftSchedules;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftImpl;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * A {@link ShiftScheduler} for remote guidance, where vehicles are supervised remotely instead of being driven.
 * <p>
 * Shifts of the configured operator shift type are not assigned to vehicles. They define the aggregate supervision
 * capacity of the operator pool, held by {@link RemoteGuidanceOperators}. Instead, this scheduler emits "virtual"
 * driver shifts for vehicles waiting idle at a hub, which the regular
 * {@link org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher} assigns and starts like any
 * other shift. A virtual shift has no scheduled end of its own: it ends at the simulation horizon and is ended early on
 * demand by {@link RemoteGuidanceShiftEndLogic}. Non-operator shifts are passed through unchanged, so a fleet may
 * combine driver shifts and remote guidance.
 * <p>
 * How many shifts to emit is decided by the configured {@link ActivationTrigger}s, which an
 * {@link ActivationReconciler} combines into a single target active fleet size, clamped to the operator pool's
 * capacity. The default policy is the configured floor plus one {@link IdleBufferActivation} responsiveness buffer.
 * {@link RemoteGuidanceShiftEndLogic} reads an identical reconciler, so activation and deactivation share one target.
 * <p>
 * The scheduler also fires {@link VehicleActivatedForRemoteGuidanceEvent} and
 * {@link VehicleDeactivatedForRemoteGuidanceEvent}, detecting the transitions by scanning the fleet's shift queues each
 * step. The deactivation reason is inferred from the aggregate signal: a release while the supervised fleet is still at
 * or above capacity was forced by a capacity drop ({@link DeactivationReason#capacityExceeded}), a release below
 * capacity is a demand-slack recall ({@link DeactivationReason#idleTimeout}).
 *
 * @author nkuehnel / MOIA
 */
public final class RemoteGuidanceScheduler implements ShiftScheduler {

	private static final Logger logger = LogManager.getLogger(RemoteGuidanceScheduler.class);

	static final String VIRTUAL_SHIFT_TYPE = "remoteGuidanceVirtual";

	private final ShiftScheduler delegate;
	private final RemoteGuidanceOperators operators;
	private final RemoteGuidanceOperatorState operatorState;
	private final RemoteGuidanceParams params;
	private final EventsManager eventsManager;
	private final String mode;
	private final double changeoverDuration;
	private final ActivationReconciler activationReconciler;
	// demand-pressure source for the rejection-rate trigger; null when no rejection activation is configured
	private final RejectionRateTracker rejectionRateTracker;
	// trailing-window busy-count smoother, shared with the deactivation side. A window of 0 makes it a pass-through.
	private final BusyWindowTracker busyWindowTracker;

	// runtime state, (re)initialized on each initialSchedule() (i.e. per iteration)
	private Map<Id<DrtShift>, Id<DvrpVehicle>> liveVirtualShifts;
	private long virtualShiftCounter;
	// the simulation-horizon end assigned to every virtual shift, lazily derived from the fleet on the first schedule()
	// call as (earliest vehicle service end − changeover duration). A virtual shift materialises no changeover tail, so
	// this time is never reached as a changeover: it is the far-future bound the dispatcher's end-time-keyed lifecycle
	// sorts on and against which the recall look-ahead is evaluated. The margin below service end only keeps the shift
	// end sorting strictly before the vehicle's service end. A recall materialises its own changeover with a fresh
	// [arrival, serviceEnd] reservation and does not depend on this offset.
	private double virtualShiftEndTime = Double.NaN;

	public RemoteGuidanceScheduler(ShiftScheduler delegate, RemoteGuidanceOperators operators,
								   RemoteGuidanceOperatorState operatorState, RemoteGuidanceParams params,
								   EventsManager eventsManager, String mode, double changeoverDuration,
								   ActivationReconciler activationReconciler, RejectionRateTracker rejectionRateTracker,
								   BusyWindowTracker busyWindowTracker) {
		this.delegate = delegate;
		this.operators = operators;
		this.operatorState = operatorState;
		this.params = params;
		this.eventsManager = eventsManager;
		this.mode = mode;
		this.changeoverDuration = changeoverDuration;
		this.activationReconciler = activationReconciler;
		this.rejectionRateTracker = rejectionRateTracker;
		this.busyWindowTracker = busyWindowTracker;
	}

	@Override
	public ImmutableMap<Id<DrtShift>, DrtShift> initialSchedule() {
		// (re)init runtime state for this iteration. Virtual shifts are transient: they live only in the QSim lifecycle
		// and are deliberately not written into the persistent shift specification, so nothing has to be purged across
		// iterations and no id can collide.
		liveVirtualShifts = new HashMap<>();
		virtualShiftCounter = 0;
		// the operator state is a single cross-iteration instance, so its per-iteration lifecycle (released /
		// incident-busy / effective end) has to be cleared here or a previous iteration's releases would starve
		// coverage. The immutable spec registry needs no reset.
		operatorState.reset();

		// operator shifts define capacity only (via the registry) and are NOT handed to the dispatcher for assignment
		ImmutableMap.Builder<Id<DrtShift>, DrtShift> driverShifts = ImmutableMap.builder();
		for (DrtShift shift : delegate.initialSchedule().values()) {
			if (!isOperatorShift(shift)) {
				driverShifts.put(shift.getId(), shift);
			}
		}
		logger.info("Initialized remote guidance with {} operator shifts (capacity {} each).", operators.size(),
				params.getDefaultOperatorCapacity());
		return driverShifts.build();
	}

	@Override
	public List<DrtShift> schedule(double now, Fleet fleet) {
		if (Double.isNaN(virtualShiftEndTime)) {
			double minServiceEnd = fleet.getVehicles().values().stream()
					.mapToDouble(DvrpVehicle::getServiceEndTime)
					.min()
					.orElse(now);
			virtualShiftEndTime = minServiceEnd - changeoverDuration;
			warnIfMinActiveFleetUnreachable(fleet);
		}

		List<DrtShift> emitted = new ArrayList<>(delegate.schedule(now, fleet));

		// operator lifecycle: fire a started event for every operator whose planned start has been reached (today the
		// actual start == planned start; the emission point exists for a future delayed start). Idempotent per operator.
		for (RemoteGuidanceOperators.Operator started : operatorState.markStarted(now)) {
			eventsManager.processEvent(new RemoteGuidanceOperatorStartedEvent(now, mode, started.id(),
					started.capacity()));
		}

		// single fleet scan: reconciles which virtual shifts are live (firing activation/deactivation events) and
		// collects the idle counts the activation triggers need. Folded together to avoid a second pass over the whole
		// fleet each step, which matters at large fleet sizes.
		IdleCounts idleCounts = reconcileSupervisions(now, fleet);

		// release operators that have passed their planned end, but only as far as the (now reconciled) active
		// supervised fleet allows without breaking coverage: a retained operator keeps supervising its vehicles and
		// finishing any pending incident until its vehicles have gone home. Runs before emission so freshly-released
		// operators no longer count toward the activation ceiling below. Each release fires an ended event carrying the
		// planned end, so the retention (effective − planned) is observable.
		for (RemoteGuidanceOperators.Operator released : operatorState.releaseElapsedOperators(now, liveVirtualShifts.size())) {
			eventsManager.processEvent(new RemoteGuidanceOperatorEndedEvent(now, mode, released.id(),
					released.plannedEndTime()));
		}

		// the triggers decide how many vehicles should be active; the reconciler combines them and clamps to the floor
		// and the activation capacity. The ceiling uses the planned-window capacity rather than coverage, so that an
		// operator retained past its planned end cannot pull new vehicles in while winding down.
		double rejectionRate = rejectionRateTracker == null ? 0.0 : rejectionRateTracker.rejectionRate(now);
		// busy counts only started virtual vehicles doing passenger work, not the queue-membership activeCount below.
		// A replacement shift assigned to a still-recalling vehicle at end of day is live (so activeCount does not
		// re-emit it) but not yet started; feeding it into busy would re-inflate the target.
		int busy = busy(idleCounts.startedVirtual(), idleCounts.idleInService(), idleCounts.leaving());
		int smoothedBusy = busyWindowTracker.sample(now, busy);
		GuidanceState state = new GuidanceState(liveVirtualShifts.size(), operators.activationCapacityAt(now),
				idleCounts.idleAtHub(), idleCounts.idleInService(), smoothedBusy, rejectionRate);
		int toEmit = activationReconciler.toEmit(state, now);
		for (int i = 0; i < toEmit; i++) {
			emitted.add(createVirtualShift(now));
		}
		return emitted;
	}

	/**
	 * Counts collected during the single fleet scan.
	 *
	 * @param idleAtHub      out of service and waiting at a hub, i.e. available for activation
	 * @param idleInService  active but idle, i.e. the ready buffer
	 * @param leaving        active and already recalled, routing home on a materialised changeover tail
	 * @param startedVirtual vehicles whose virtual shift has actually started
	 */
	private record IdleCounts(int idleAtHub, int idleInService, int leaving, int startedVirtual) {}

	/**
	 * The busy count fed into the shared {@link BusyWindowTracker}: started virtual vehicles doing passenger work, i.e.
	 * neither idle in service nor already recalled.
	 * <p>
	 * Both exclusions guard against an end-of-day recall/re-activate loop. {@code startedVirtual} rather than the
	 * queue-membership active count is the base, because a replacement shift assigned to a still-recalling vehicle is
	 * live (so it is not re-emitted) while its vehicle is not doing passenger work. {@code leaving} is subtracted
	 * because a recalled vehicle is no longer idle in service, yet counting it as busy would activate a replacement for
	 * a vehicle that is going home. {@link RemoteGuidanceShiftEndLogic} derives busy the same way, since both margins
	 * feed the same tracker and have to agree on the signal.
	 */
	static int busy(int startedVirtual, int idleInService, int leaving) {
		return startedVirtual - idleInService - leaving;
	}

	/**
	 * Warns once, at the first schedule call, if the configured {@code minActiveFleet} floor can never be met because it
	 * exceeds either the maximum activation capacity the operator schedule ever reaches or the number of shift-capable
	 * vehicles in the fleet. Both are hard upper bounds on the active count. The reconciler clamps the floor to them, so
	 * this is a diagnostic rather than an error: a silently truncated floor would otherwise read as being in effect.
	 */
	private void warnIfMinActiveFleetUnreachable(Fleet fleet) {
		int minActiveFleet = params.getMinActiveFleet();
		if (minActiveFleet <= 0) {
			return;
		}
		int maxCapacity = operators.maxActivationCapacity();
		if (minActiveFleet > maxCapacity) {
			logger.warn("minActiveFleet ({}) exceeds the maximum operator activation capacity ({}); the floor is "
					+ "capped by capacity and can never be fully met.", minActiveFleet, maxCapacity);
		}
		long shiftVehicles = fleet.getVehicles().values().stream()
				.filter(vehicle -> vehicle instanceof ShiftDvrpVehicle)
				.count();
		if (minActiveFleet > shiftVehicles) {
			logger.warn("minActiveFleet ({}) exceeds the number of shift-capable vehicles in the fleet ({}); the floor "
					+ "is capped by the fleet size and can never be fully met.", minActiveFleet, shiftVehicles);
		}
	}

	/**
	 * Single pass over the fleet that recomputes which virtual shifts are live (present in a vehicle's shift queue),
	 * fires activation and deactivation events on the transitions, and tallies the idle counts the activation triggers
	 * read.
	 */
	private IdleCounts reconcileSupervisions(double now, Fleet fleet) {
		Map<Id<DrtShift>, Id<DvrpVehicle>> currentlyLive = new HashMap<>();
		int idleAtHub = 0;
		int idleInService = 0;
		int leaving = 0;
		int startedVirtual = 0;
		for (DvrpVehicle vehicle : fleet.getVehicles().values()) {
			if (!(vehicle instanceof ShiftDvrpVehicle shiftVehicle)) {
				continue;
			}
			// every virtual shift in the queue counts as live: a shift emitted this step is assigned and started within
			// the same dispatcher step (scheduleShifts → assignShifts → startShifts), so keying on queue membership
			// rather than isStarted() keeps the active count from briefly undercounting and re-emitting a duplicate.
			for (DrtShift shift : shiftVehicle.getShifts()) {
				if (isVirtualShift(shift)) {
					currentlyLive.put(shift.getId(), vehicle.getId());
				}
			}
			// idle counts, only meaningful for a started schedule
			Schedule schedule = vehicle.getSchedule();
			if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
				continue;
			}
			Task currentTask = schedule.getCurrentTask();
			if (shiftVehicle.getShifts().isEmpty()) {
				// no shift assigned at all: out of service, waiting at a hub, so available for activation. Any queued
				// shift, started or not, virtual or a driver shift, makes the vehicle unavailable, which is why this
				// keys on the empty queue rather than on the absence of a started virtual shift.
				if (currentTask instanceof WaitForShiftTask) {
					idleAtHub++;
				}
			} else if (startedVirtualShift(shiftVehicle).isPresent()) {
				// a virtual shift that has actually started, i.e. the vehicle is supervised and able to do passenger
				// work. Narrower than queue membership: at end of day a recalled vehicle still mid-changeover can have a
				// replacement shift assigned but not started, which counts as live but must not feed busy.
				startedVirtual++;
				if (ShiftSchedules.getNextShiftChangeover(schedule).isPresent()) {
					// already recalled and routing home. A virtual shift has no eager changeover tail, so a materialised
					// changeover can only come from a recall. Such a vehicle is winding down rather than doing passenger
					// work, and counting it as busy would immediately re-activate a replacement for it.
					leaving++;
				} else if (currentTask instanceof DrtStayTask && currentTask.equals(Schedules.getLastTask(schedule))) {
					// active but idle in service (a stay task that is the last task), i.e. part of the ready buffer.
					// Keyed on the started virtual shift, not on the queue head, which may be a future shift.
					idleInService++;
				}
			}
		}

		// newly activated: live now but not tracked before
		for (Map.Entry<Id<DrtShift>, Id<DvrpVehicle>> entry : currentlyLive.entrySet()) {
			if (!liveVirtualShifts.containsKey(entry.getKey())) {
				eventsManager.processEvent(new VehicleActivatedForRemoteGuidanceEvent(now, mode, entry.getValue()));
			}
		}

		// deactivated: tracked before but no longer live (virtual shift ended)
		Set<Id<DrtShift>> ended = new HashSet<>(liveVirtualShifts.keySet());
		ended.removeAll(currentlyLive.keySet());
		int capacity = operatorState.coverageCapacityAt(now);
		int stillLive = currentlyLive.size();
		for (Id<DrtShift> endedShiftId : ended) {
			Id<DvrpVehicle> vehicleId = liveVirtualShifts.get(endedShiftId);
			// the trigger is inferred from the aggregate signal, as in RemoteGuidanceShiftEndLogic: still at or over
			// capacity means the release was forced by a capacity drop, below capacity means a demand-slack recall.
			DeactivationReason reason = stillLive >= capacity
					? DeactivationReason.capacityExceeded
					: DeactivationReason.idleTimeout;
			eventsManager.processEvent(new VehicleDeactivatedForRemoteGuidanceEvent(now, mode, vehicleId, reason));
		}

		liveVirtualShifts = currentlyLive;
		return new IdleCounts(idleAtHub, idleInService, leaving, startedVirtual);
	}

	private DrtShift createVirtualShift(double now) {
		Id<DrtShift> id = Id.create("rg_" + (virtualShiftCounter++) + "_" + (long) now, DrtShift.class);
		// no fixed facility: the vehicle is activated from and returns to any hub;
		// no designated vehicle: the dispatcher matches an idle vehicle;
		// end at the simulation horizon: the shift runs until a deactivation trigger recalls it early;
		// committedEnd false: the horizon end is discretionary, so startShift materialises no changeover or wait tail
		// and the vehicle stays in service on a plain stay task until a recall materialises the end.
		return new DrtShiftImpl(id, now, virtualShiftEndTime, null, null, null, VIRTUAL_SHIFT_TYPE, false);
	}

	private boolean isOperatorShift(DrtShift shift) {
		return shift.getShiftType().map(params.getOperatorShiftType()::equals).orElse(false);
	}

	static boolean isVirtualShift(DrtShift shift) {
		return shift.getShiftType().map(VIRTUAL_SHIFT_TYPE::equals).orElse(false);
	}

	/**
	 * The vehicle's currently running virtual shift, if any. A {@link ShiftDvrpVehicle}'s shift queue is ordered by start
	 * time and holds shifts from assignment rather than from start, so its head may be a future shift while a different
	 * one is running. Idle and recall decisions therefore have to key on the shift that has
	 * {@link DrtShift#isStarted() started} and not {@link DrtShift#isEnded() ended}, not on the queue head. At most one
	 * such shift exists, since a vehicle runs one shift at a time.
	 */
	static Optional<DrtShift> startedVirtualShift(ShiftDvrpVehicle vehicle) {
		for (DrtShift shift : vehicle.getShifts()) {
			if (shift.isStarted() && !shift.isEnded() && isVirtualShift(shift)) {
				return Optional.of(shift);
			}
		}
		return Optional.empty();
	}

	@Override
	public DrtShiftsSpecification get() {
		return delegate.get();
	}

	/**
	 * Convenience factory wrapping a {@link DefaultShiftScheduler} over the given specification, with the activation
	 * policy built from {@code params}: the {@code minActiveFleet} floor, an {@link IdleBufferActivation} responsiveness
	 * buffer, and, if rejection activation is configured, a demand-driven trigger fed by {@code rejectionRateTracker}.
	 * {@link RemoteGuidanceShiftEndLogic} builds an identical reconciler from the same params and reads the same
	 * trackers, so both margins share one target.
	 *
	 * @param rejectionRateTracker shared demand-pressure source, {@code null} when no rejection activation is configured
	 * @param busyWindowTracker    shared trailing-window busy smoother read by both margins; a window of 0 makes it a
	 *                             pass-through
	 */
	public static RemoteGuidanceScheduler create(DrtShiftsSpecification specification, RemoteGuidanceOperators operators,
												 RemoteGuidanceOperatorState operatorState, RemoteGuidanceParams params,
												 EventsManager eventsManager, String mode, double changeoverDuration,
												 RejectionRateTracker rejectionRateTracker, BusyWindowTracker busyWindowTracker) {
		ActivationReconciler reconciler = ActivationReconciler.create(params.getActivationPolicy(),
				params.getMinActiveFleet(), params.getReadyBufferSize(), rejectionThreshold(params));
		return new RemoteGuidanceScheduler(new DefaultShiftScheduler(specification), operators, operatorState, params,
				eventsManager, mode, changeoverDuration, reconciler, rejectionRateTracker, busyWindowTracker);
	}

	/**
	 * The configured rejection-rate threshold for the demand-driven trigger, or empty when no rejection activation is
	 * configured. Read by both margins so they build the identical trigger set.
	 */
	public static OptionalDouble rejectionThreshold(RemoteGuidanceParams params) {
		return params.getRejectionActivationParams()
				.map(p -> OptionalDouble.of(p.getRejectionRateThreshold()))
				.orElseGet(OptionalDouble::empty);
	}
}
