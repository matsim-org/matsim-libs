/*
 * Copyright (C) 2026 MOIA GmbH
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.remoteoperations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.IncidentAssignmentPolicy;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.IncidentParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.config.IncidentSeverityParams;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentAssignedToOperatorEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentResolvedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.events.IncidentStartedEvent;
import org.matsim.contrib.drt.extension.operations.remoteoperations.schedule.IncidentHoldTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.DrtOperationsTaskFactory;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.vehicles.Vehicle;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates and processes stochastic remote-guidance incidents, and is the runtime counterpart of the incident
 * configuration ({@link IncidentParams} / {@link IncidentSeverityParams}). It is invoked once per sim step (mirroring
 * how {@code ShiftDrtOptimizer} drives the {@code DrtShiftDispatcher}) and does three things each step:
 * <ol>
 *     <li><b>resolve</b> incidents whose processing has finished (release operator + vehicle, fire
 *         {@link IncidentResolvedEvent});</li>
 *     <li><b>sample</b> new incidents for driving vehicles as a Poisson process over the distance driven this step,
 *         with one independent process per severity class (fire {@link IncidentStartedEvent}, start the vehicle hold);
 *         </li>
 *     <li><b>assign</b> queued incidents to free operators according to the configured
 *         {@link IncidentAssignmentPolicy} (fire {@link IncidentAssignedToOperatorEvent}).</li>
 * </ol>
 * <p>
 * Incident processing is a fully <em>decoupled</em> M/M/m queue over all operators: an incident
 * occupies exactly one free operator for a severity-dependent duration; if none is free it queues and the vehicle keeps
 * holding. Severity steers only the duration, never how much capacity an incident consumes. Crucially, incident
 * processing is independent of passive supervision: the operator pool here is derived from the authoritative
 * {@link RemoteGuidanceOperators} registry, one server per operator, and an incident-busy operator still
 * passively supervises its vehicles.
 * <p>
 * <b>VKT accumulation (step 2)</b> is event-driven, not polled: the dispatcher is a {@link LinkLeaveEventHandler} and
 * sums the length of each link a fleet vehicle fully traverses into a per-vehicle counter. This is MATSim's natural VKT
 * granularity — there is no within-link position, a {@link LinkLeaveEvent} fires exactly when a link has been driven in
 * full — and it only touches vehicles that actually moved, instead of scanning the whole fleet every sim step. Each
 * {@link #dispatch(double)} then reads and resets that counter and rolls the Poisson sampling once over the step's total
 * distance (equivalent to a per-link roll, since {@code 1 − exp(−λ·ΣLᵢ) = 1 − Π exp(−λ·Lᵢ)}). The schedule mutation
 * itself is deliberately kept out of the event handler and done in {@code dispatch()} (a safe seam), never mid-mobsim-step.
 * <p>
 * <b>The vehicle hold ({@link #beginHold}):</b> a driving vehicle is stopped <em>in place</em> by diverting its running
 * drive onto a zero-length path ({@link OnlineDriveTaskTracker} / {@code VrpPaths.createZeroLengthPathForDiversion}),
 * inserting an {@link IncidentHoldTask} and a re-routed continuation drive to the original destination. There is no
 * separate hold {@code DynActivity}: as an empty-request {@code STOP} task the hold becomes a plain {@code DrtStopActivity}
 * that idles until the task end time, and that end time is owned here — left open (pushed just past {@code now} each step
 * via {@link #extendQueuedHolds}) while the incident waits in the operator queue, then fixed to the service end once an
 * operator is assigned. Resolution needs no schedule surgery: the continuation drive already trails the hold, so the
 * vehicle drives on by itself when the hold ends.
 *
 * @author nkuehnel / MOIA
 */
public final class IncidentDispatcher implements MobsimBeforeSimStepListener, LinkLeaveEventHandler,
		MobsimScopeEventHandler {

	private static final Logger logger = LogManager.getLogger(IncidentDispatcher.class);

	// Placeholder step [s] by which a still-queued incident hold is pushed forward each time its end catches up with now.
	// This value carries NO physical meaning and does not affect the reported timing of any incident: while an incident
	// waits in the operator queue its true end is unknown, and the moment an operator is assigned assignQueuedIncidents()
	// fixes the hold end to the exact service end (with a full re-propagation). The push exists only so the HOLD stop
	// activity does not run out while the vehicle waits. Each push triggers a schedule-tail re-propagation, so a 1 s step
	// makes it O(sim steps × queued vehicles) — the dominant cost in the rho >= 1 regime (a8/a16 ran 4-5.5 h vs 0.3 h for
	// the drained cases). Coarsening to 60 s cuts those re-propagations ~60x with no loss of accuracy: assignQueuedIncidents()
	// runs every step and immediately overrides the placeholder with the exact service end when an operator frees up, so
	// the bump granularity never delays an assignment — it only controls how often a still-waiting hold is nudged past now.
	private static final double QUEUED_HOLD_EXTENSION_STEP = 60.0;

	private final String mode;
	private final IncidentParams params;
	private final RemoteGuidanceOperators operatorRegistry;
	private final RemoteGuidanceOperatorState operatorState;

	private final Fleet fleet;
	private final EventsManager eventsManager;
	private final MobsimTimer timer;

	// used for the hold insertion / continuation routing done in step 1 (mid-drive diversion + timing fix-up)
	private final ScheduleTimingUpdater scheduleTimingUpdater;
	private final Network network;
	private final TravelTime travelTime;
	private final DrtOperationsTaskFactory taskFactory;
	private final LeastCostPathCalculator router;

	private final Random random = MatsimRandom.getLocalInstance();

	// runtime state
	private final Map<Id<DrtShift>, OperatorServer> operators = new LinkedHashMap<>();
	private final Deque<Incident> queue = new ArrayDeque<>();
	private final Map<Id<DvrpVehicle>, Incident> activeIncidents = new LinkedHashMap<>();
	private boolean initialized = false;

	// VKT accrued (by fleet vehicle) since the last dispatch() — filled by link-leave events, drained per sim step.
	// Keyed by the mobsim Vehicle id, whose string equals the DvrpVehicle id (VrpAgentSource creates it as such).
	private final Map<Id<Vehicle>, Double> distanceSinceLastStep = new LinkedHashMap<>();
	// membership set so link-leave events for non-fleet (or other-mode) vehicles are ignored cheaply
	private final Map<Id<Vehicle>, Id<DvrpVehicle>> fleetVehicleIds = new LinkedHashMap<>();

	public IncidentDispatcher(String mode, IncidentParams params, RemoteGuidanceOperators operatorRegistry,
							  RemoteGuidanceOperatorState operatorState, Fleet fleet, EventsManager eventsManager,
							  MobsimTimer timer, ScheduleTimingUpdater scheduleTimingUpdater, Network network,
							  TravelTime travelTime, DrtOperationsTaskFactory taskFactory) {
		this.mode = mode;
		this.params = params;
		this.operatorRegistry = operatorRegistry;
		this.operatorState = operatorState;
		this.fleet = fleet;
		this.eventsManager = eventsManager;
		this.timer = timer;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.network = network;
		this.travelTime = travelTime;
		this.taskFactory = taskFactory;
		this.router = new SpeedyALTFactory().createPathCalculator(network, new TimeAsTravelDisutility(travelTime), travelTime);
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		dispatch(e.getSimulationTime());
	}

	void dispatch(double now) {
		ensureInitialized();
		resolveDueIncidents(now);
		sampleNewIncidents(now);
		assignQueuedIncidents(now);
		extendQueuedHolds(now);
	}

	/**
	 * Builds the independent incident-server pool from the {@link RemoteGuidanceOperators} registry — the single
	 * authoritative source of the operator roster. Each on-duty operator becomes one server, available for
	 * incident processing during its shift window and busy for the duration of the incident it is currently handling.
	 * The operator's passive-supervision capacity κ is irrelevant here: an incident always occupies exactly one operator
	 * regardless of its supervision capacity, so the server pool has one server per operator.
	 */
	private void ensureInitialized() {
		if (initialized) {
			return;
		}
		for (RemoteGuidanceOperators.Operator operator : operatorRegistry.getOperators().values()) {
			operators.put(operator.id(), new OperatorServer(operator));
		}
		for (Id<DvrpVehicle> dvrpVehicleId : fleet.getVehicles().keySet()) {
			// mobsim vehicle id string == dvrp vehicle id string (VrpAgentSource); map both ways for the handler + drain
			fleetVehicleIds.put(Id.create(dvrpVehicleId, Vehicle.class), dvrpVehicleId);
		}
		logger.info("Initialized incident processing with {} operator servers and {} severity classes.",
				operators.size(), params.getSeverityParams().size());
		initialized = true;
	}

	/** Releases operators (and vehicles) whose incident has finished processing. */
	private void resolveDueIncidents(double now) {
		for (OperatorServer operator : operators.values()) {
			Incident incident = operator.current;
			if (incident != null && now >= incident.serviceEndTime) {
				double actualDuration = now - incident.holdStartTime;
				boolean queued = incident.assignTime > incident.holdStartTime;
				double queueDelay = incident.assignTime - incident.holdStartTime;

				// no schedule surgery needed to resume: the continuation drive already sits right after the hold task,
				// so the vehicle drives on by itself once the hold end (== now) is reached.

				eventsManager.processEvent(new IncidentResolvedEvent(now, mode, incident.vehicleId, operator.id,
						incident.severity, actualDuration, queued, queueDelay));

				operator.current = null;
				operatorState.setIncidentBusy(operator.id, false); // free to be released now
				activeIncidents.remove(incident.vehicleId);
			}
		}
	}

	/**
	 * Draws new incidents for driving vehicles: each severity class is an independent Poisson process over the distance
	 * driven this step, so the per-step incident probability for class s is {@code 1 - exp(-lambda_s * deltaMeters)}.
	 */
	private void sampleNewIncidents(double now) {
		for (DvrpVehicle vehicle : fleet.getVehicles().values()) {
			if (activeIncidents.containsKey(vehicle.getId())) {
				continue; // a vehicle already holding does not accrue new incidents
			}
			double deltaMeters = distanceDrivenThisStep(vehicle, now);
			if (deltaMeters <= 0.0) {
				continue;
			}
			int severityIndex = 1;
			for (IncidentSeverityParams severityClass : params.getSeverityParams()) {
				double p = 1.0 - Math.exp(-severityClass.getLambdaPerMeter() * deltaMeters);
				if (random.nextDouble() < p) {
					startIncident(vehicle, severityIndex, severityClass, now);
					break; // at most one incident per vehicle per step
				}
				severityIndex++;
			}
		}
	}

	private void startIncident(DvrpVehicle vehicle, int severityIndex, IncidentSeverityParams severityClass, double now) {
		IncidentHoldTask holdTask = beginHold(vehicle, now);
		if (holdTask == null) {
			// the vehicle could not be stopped in place this step (e.g. already on the last link of its drive, or not
			// currently driving) — drop this draw. Statistically negligible; a vehicle only accrues VKT while driving.
			return;
		}

		double duration = sampleDuration(severityClass);
		Incident incident = new Incident(vehicle.getId(), severityIndex, duration, now, holdTask);
		activeIncidents.put(vehicle.getId(), incident);
		queue.addLast(incident);

		eventsManager.processEvent(new IncidentStartedEvent(now, mode, vehicle.getId(), severityIndex, duration,
				holdTask.getLink().getId()));
	}

	/** Assigns queued incidents to free operators (FIFO over the queue), one per free operator, per the policy. */
	private void assignQueuedIncidents(double now) {
		while (!queue.isEmpty()) {
			OperatorServer operator = pickFreeOperator(now);
			if (operator == null) {
				break; // all operators saturated → the head of the queue keeps waiting (vehicle holds)
			}
			Incident incident = queue.pollFirst();
			incident.operatorId = operator.id;
			incident.assignTime = now;
			incident.serviceEndTime = now + incident.expectedDuration;
			operator.current = incident;
			operator.handledCount++;
			// mark the operator busy in the runtime state: it must not be released mid-incident, there is no handover
			operatorState.setIncidentBusy(operator.id, true);

			// the operator is now handling the incident: fix the hold's end to the (formerly open) service end and
			// propagate the new timing to the trailing continuation drive + downstream tasks
			setHoldEnd(incident, incident.serviceEndTime);

			eventsManager.processEvent(new IncidentAssignedToOperatorEvent(now, mode, incident.vehicleId, operator.id,
					incident.severity));
		}
	}

	/** Chooses a free operator (on duty for coverage, not currently processing an incident) per the policy. */
	private OperatorServer pickFreeOperator(double now) {
		List<OperatorServer> free = new ArrayList<>();
		for (OperatorServer operator : operators.values()) {
			// available = free (no current incident) AND on duty for coverage in the runtime sense (a pending-release
			// operator can still take an incident until it is actually released). Coverage state lives on operatorState.
			if (operator.current == null && operatorState.onDutyForCoverage(operator.operator, now)) {
				free.add(operator);
			}
		}
		if (free.isEmpty()) {
			return null;
		}
		if (params.getAssignmentPolicy() == IncidentAssignmentPolicy.LEAST_LOADED) {
			// with one incident per operator at a time, balance by cumulative incidents handled (fairness)
			return free.stream().min((a, b) -> Long.compare(a.handledCount, b.handledCount)).orElseThrow();
		}
		return free.get(random.nextInt(free.size())); // RANDOM_FREE (default)
	}

	/**
	 * Samples an incident duration [s]. All three families share the SAME mean exp(mu + sigma^2/2) - so the service rate
	 * is held fixed - and differ only in variability: LOGNORMAL keeps the empirical long tail (SCV = exp(sigma^2)-1),
	 * EXPONENTIAL is memoryless (SCV = 1, reduces the operator pool to an exact M/M/m queue), DETERMINISTIC is constant
	 * (SCV = 0, M/D/m), so the service-time variability can be varied while the mean is held fixed.
	 */
	private double sampleDuration(IncidentSeverityParams severityClass) {
		return sampleDuration(severityClass.getDurationDistribution(), severityClass.getDurationMu(),
				severityClass.getDurationSigma(), random);
	}

	/**
	 * Pure sampling math, separated from the runtime {@link #random} so it can be exercised deterministically with a
	 * seeded {@link Random} (see {@code IncidentDurationSamplingTest}). All three families share the SAME mean
	 * {@code exp(mu + sigma^2/2)} so only the SCV changes; see the caller's javadoc for the queueing rationale.
	 */
	static double sampleDuration(IncidentSeverityParams.DurationDistribution distribution, double mu, double sigma,
								 Random random) {
		double mean = Math.exp(mu + 0.5 * sigma * sigma);
		return switch (distribution) {
			case LOGNORMAL -> Math.exp(mu + sigma * random.nextGaussian());
			// inverse-CDF of Exponential(1/mean); nextDouble() in [0,1) so 1 - u is in (0,1], avoiding log(0)
			case EXPONENTIAL -> -mean * Math.log(1.0 - random.nextDouble());
			case DETERMINISTIC -> mean;
		};
	}

	/**
	 * Distance [m] the vehicle has driven since the previous sim step, drained from the link-leave accumulator (see
	 * {@link #handleEvent(LinkLeaveEvent)}). Reading resets the counter so each metre is sampled exactly once. Whole-link
	 * granularity: the last (partially driven) link of a leg is not counted until it is fully traversed — the standard
	 * MATSim VKT approximation, immaterial for a Poisson rate.
	 */
	private double distanceDrivenThisStep(DvrpVehicle vehicle, double now) {
		Double meters = distanceSinceLastStep.remove(Id.create(vehicle.getId(), Vehicle.class));
		return meters == null ? 0.0 : meters;
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (!fleetVehicleIds.containsKey(event.getVehicleId())) {
			return; // not one of our fleet vehicles (or another mode) — ignore
		}
		double length = network.getLinks().get(event.getLinkId()).getLength();
		distanceSinceLastStep.merge(event.getVehicleId(), length, Double::sum);
	}

	/**
	 * Stops the driving {@code vehicle} in place and splices an {@link IncidentHoldTask} into its schedule, followed by
	 * a fresh continuation drive to the drive's original destination. Mid-drive diversion (no coarse task-boundary wait:
	 * a single DRT drive can span many minutes):
	 * <ol>
	 *     <li>get the {@link OnlineDriveTaskTracker}'s diversion point (null ⇒ cannot divert this step ⇒ return null,
	 *         the caller drops the draw);</li>
	 *     <li>remember the drive's original destination B;</li>
	 *     <li>divert the running drive onto a zero-length path ⇒ it now ends at the diversion link {@code dp};</li>
	 *     <li>insert {@code HOLD@dp} (end left open — pushed forward each step until an operator is assigned) and a
	 *         re-routed continuation {@code DRIVE dp→B} right after it;</li>
	 *     <li>propagate timings to the (shifted) rest of the schedule.</li>
	 * </ol>
	 * Only a vehicle whose current task is a {@link DrtDriveTask} can be held — which is exactly the population that
	 * accrues VKT and can therefore draw an incident.
	 *
	 * @return the inserted hold task, or {@code null} if the vehicle could not be stopped in place this step.
	 */
	private IncidentHoldTask beginHold(DvrpVehicle vehicle, double now) {
		Schedule schedule = vehicle.getSchedule();
		Task currentTask = schedule.getCurrentTask();
		if (!(currentTask instanceof DrtDriveTask driveTask)
				|| !(driveTask.getTaskTracker() instanceof OnlineDriveTaskTracker tracker)) {
			return null;
		}
		LinkTimePair diversionPoint = tracker.getDiversionPoint();
		if (diversionPoint == null) {
			return null; // too late to divert (already committed to the last link) — retry on a later draw
		}

		Link destination = driveTask.getPath().getToLink();

		// stop in place: curtail the running drive to end at the diversion link. The diverted drive ends at the
		// zero-length path's arrival time (diversionPoint.time − NODE_TRANSITION_TIME), so read the actual end back
		// rather than assuming diversionPoint.time — the hold must begin exactly where the drive now ends.
		tracker.divertPath(VrpPaths.createZeroLengthPathForDiversion(diversionPoint));
		double holdBegin = driveTask.getEndTime();

		// HOLD @ diversion link. End is initially open (holdBegin + 1): we don't yet know when an operator frees up, so
		// the hold is kept "just past now" and pushed forward step by step (see extendQueuedHolds) until assigned. We
		// deliberately do NOT use a far-future end — that would push a shift vehicle's trailing WaitForShiftTask past
		// its service end and trip the timing verifier.
		IncidentHoldTask holdTask = taskFactory.createIncidentHoldTask(vehicle, holdBegin, holdBegin + 1, diversionPoint.link);
		int holdIdx = driveTask.getTaskIdx() + 1;
		schedule.addTask(holdIdx, holdTask);

		// continuation DRIVE diversion link → original destination, departing at the (current, open) hold end. Created
		// via the task factory so the electric fleet gets the energy-tracking EDrtDriveTask variant (the eDRT data-entry
		// factory casts every task to ETask); the non-electric factory returns a plain DrtDriveTask.
		VrpPathWithTravelData continuation = VrpPaths.calcAndCreatePath(diversionPoint.link, destination,
				holdTask.getEndTime(), router, travelTime);
		schedule.addTask(holdIdx + 1, taskFactory.createDriveTask(vehicle, continuation, DrtDriveTask.TYPE));

		// fix downstream timings from the continuation drive onward (the original tasks after B shift back)
		scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicle, holdIdx + 1, holdTask.getEndTime());
		return holdTask;
	}

	/**
	 * Keeps still-queued vehicles standing: pushes each unassigned hold's end past {@code now} whenever it has caught up.
	 * The push is a placeholder (the real end is set on assignment), so it is coarsened to {@link #QUEUED_HOLD_EXTENSION_STEP}
	 * to avoid a per-second schedule-tail re-propagation for every queued vehicle — the dominant cost under queue saturation.
	 */
	private void extendQueuedHolds(double now) {
		for (Incident incident : queue) {
			if (incident.holdTask.getEndTime() <= now) {
				setHoldEnd(incident, now + QUEUED_HOLD_EXTENSION_STEP);
			}
		}
	}

	/**
	 * Sets an incident hold's end time and re-propagates the timing of everything after it (the continuation drive and
	 * the rest of the schedule move with it). The continuation drive's path is unchanged; only its begin/end shift.
	 */
	private void setHoldEnd(Incident incident, double endTime) {
		DvrpVehicle vehicle = fleet.getVehicles().get(incident.vehicleId);
		IncidentHoldTask holdTask = incident.holdTask;
		holdTask.setEndTime(endTime);
		scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicle, holdTask.getTaskIdx() + 1, endTime);
	}

	/**
	 * One M/M/m server = one operator; processes at most one incident at a time. Wraps the registry
	 * {@link RemoteGuidanceOperators.Operator} so availability follows the same runtime coverage window as everything
	 * else (a pending-release operator can still finish / take an incident until it is actually released).
	 */
	private static final class OperatorServer {
		private final RemoteGuidanceOperators.Operator operator;
		private final Id<DrtShift> id;
		private Incident current; // null when free
		private long handledCount;

		private OperatorServer(RemoteGuidanceOperators.Operator operator) {
			this.operator = operator;
			this.id = operator.id();
		}
	}

	/** A single incident, from the moment the vehicle starts holding until it is resolved. */
	private static final class Incident {
		private final Id<DvrpVehicle> vehicleId;
		private final int severity;
		private final double expectedDuration;
		private final double holdStartTime;
		private final IncidentHoldTask holdTask; // the STAY-in-place task inserted into the vehicle's schedule
		private Id<DrtShift> operatorId; // null while queued
		private double assignTime = Double.NaN; // when an operator took it (== holdStartTime if not queued)
		private double serviceEndTime = Double.NaN; // assignTime + expectedDuration

		private Incident(Id<DvrpVehicle> vehicleId, int severity, double expectedDuration, double holdStartTime,
						 IncidentHoldTask holdTask) {
			this.vehicleId = vehicleId;
			this.severity = severity;
			this.expectedDuration = expectedDuration;
			this.holdStartTime = holdStartTime;
			this.holdTask = holdTask;
		}
	}
}