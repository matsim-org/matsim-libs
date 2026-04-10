package org.matsim.contrib.ev.withinday;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.ev.charging.ChargingListener;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.withinday.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.DistributedActivityHandler;
import org.matsim.core.mobsim.dsim.DistributedMobsimEngine;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * DSim-compatible port of {@link WithinDayEvEngine}.
 *
 * <p>Key differences from the original:
 * <ul>
 *   <li><b>Lazy slot insertion</b>: charging slots are inserted into the plan on the agent's first
 *       activity (in {@link #handleActivity}), because {@code beforeMobsim()} in DSim fires before
 *       agents are created.</li>
 *   <li><b>No event-queue polling</b>: {@link ChargingListener} callbacks are synchronous in DSim,
 *       so the 1-second lag and multi-step state machine are replaced by direct callback handling.</li>
 *   <li><b>En-route redirect via InternalInterfaceDelegate</b>: the approaching logic (originally
 *       triggered by {@code PersonDepartureEvent}) runs in {@link InternalInterfaceDelegate#arrangeNextAgentState}
 *       where the agent reference is directly available.</li>
 * </ul>
 *
 * <p>Note: {@code updateInitialVehicleLocation()} from the original (which relocates QVehicles in
 * the network for overnight/whole-day charging) is not ported here as it depends on QSim-internal
 * network APIs. Add a TODO if needed.
 *
 * @author ported from WithinDayEvEngine by Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayEvEngine2 implements DistributedActivityHandler, DistributedMobsimEngine, ChargingListener {

	private static final Logger log = LogManager.getLogger(WithinDayEvEngine2.class);

	/**
	 * Stored in plan element attribute: the initial planned end time of the overnight activity.
	 */
	private static final String INITIAL_ACTIVITY_END_TIME_ATTRIBUTE = "ev:initialActivityEndTime";

	private final String chargingMode;

	private final ElectricFleet electricFleet;
	private final ChargingAlternativeProvider alternativeProvider;
	private final ChargingSlotProvider slotProvider;
	private final EventsManager eventsManager;
	private final TimeInterpretation timeInterpretation;
	private final ChargingScheduler chargingScheduler;
	private final Scenario scenario;
	private final WithinDayChargingStrategy.Factory chargingStrategyFactory;
	private final ActivityEngine delegateEngine;

	private final boolean performAbort;
	private final double maximumQueueWaitTime;
	private final boolean allowSpontaneousCharging;

	// --- agent eligibility ---
	private final IdSet<Person> relevantPersons = new IdSet<>(Person.class);
	private final IdSet<Vehicle> relevantVehicles = new IdSet<>(Vehicle.class);
	/** Agents whose first activity has already been processed for slot insertion. */
	private final IdSet<Person> initializedPersons = new IdSet<>(Person.class);
	/** Agents currently sitting at a plug activity (delegated to delegateEngine). */
	private final IdSet<Person> personsAtPlugActivity = new IdSet<>(Person.class);

	// --- charging process lifecycle ---
	/** Vehicles submitted to a charger but not yet confirmed plugged. */
	private final IdMap<Vehicle, ChargingProcess> plugging = new IdMap<>(Vehicle.class);
	/** Agents that have been successfully plugged (between plug and unplug activity). */
	private final IdMap<Person, ChargingProcess> active = new IdMap<>(Person.class);

	private final IdMap<Person, Integer> chargingProcessIndex = new IdMap<>(Person.class);

	private InternalInterface internalInterface;

	WithinDayEvEngine2(WithinDayEvConfigGroup config, TimeInterpretation timeInterpretation,
	                   ElectricFleet electricFleet, ChargingAlternativeProvider alternativeProvider,
	                   ChargingSlotProvider slotProvider, EventsManager eventsManager,
	                   ChargingScheduler chargingScheduler, Scenario scenario,
	                   WithinDayChargingStrategy.Factory chargingStrategyFactory,
	                   ActivityEngine delegateEngine) {
		this.chargingMode = config.getCarMode();
		this.performAbort = config.isAbortAgents();
		this.maximumQueueWaitTime = config.getMaximumQueueTime();
		this.allowSpontaneousCharging = config.isAllowSpoantaneousCharging();
		this.timeInterpretation = timeInterpretation;
		this.electricFleet = electricFleet;
		this.alternativeProvider = alternativeProvider;
		this.slotProvider = slotProvider;
		this.eventsManager = eventsManager;
		this.chargingScheduler = chargingScheduler;
		this.scenario = scenario;
		this.chargingStrategyFactory = chargingStrategyFactory;
		this.delegateEngine = delegateEngine;
	}

	// ===========================================================
	// DistributedActivityHandler
	// ===========================================================

	@Override
	public boolean handleActivity(MobsimAgent agent) {
		if (!(agent instanceof PlanAgent pa)) return false;

		double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		// --- Lazy initialization on first activity ---
		if (!initializedPersons.contains(agent.getId())) {
			initializedPersons.add(agent.getId());

			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
			Person person = plan.getPerson();

			if (WithinDayEvEngine.isActive(person) && VehicleUtils.hasVehicleId(person, chargingMode)) {
				relevantPersons.add(person.getId());
				relevantVehicles.add(VehicleUtils.getVehicleId(person, chargingMode));

				SlotInsertionResult result = insertChargingSlots(agent, plan, person, now);

				// Overnight case: we must claim this first activity so the agent is in
				// delegateEngine and can be rescheduled when the vehicle is plugged.
				if (result.overnightProcess() != null) {
					ChargingProcess process = result.overnightProcess();
					delegateEngine.handleActivity(agent);
					process.agentInEngine = true;
					if (process.isPlugged) {
						// Plugged synchronously during addVehicle() — safe to finalize now
						finalizePlugging(process, now);
					}
					// If queued: notifyChargingStarted fires later; agent is in delegateEngine
					return true;
				}
			}
			// All other first activities (no overnight slot): let default engine handle it
			return false;
		}

		if (!relevantPersons.contains(agent.getId())) return false;

		Activity act = (Activity) pa.getCurrentPlanElement();

		if (WithinDayEvEngine.PLUG_ACTIVITY_TYPE.equals(act.getType())) {
			return handlePlugActivity(agent, act, now);
		} else if (WithinDayEvEngine.UNPLUG_ACTIVITY_TYPE.equals(act.getType())) {
			return handleUnplugActivity(agent, act, now);
		}
		return false;
	}

	// ===========================================================
	// Lazy slot insertion
	// ===========================================================

	private record SlotInsertionResult(ChargingProcess overnightProcess) {}

	private SlotInsertionResult insertChargingSlots(MobsimAgent agent, Plan plan, Person person, double now) {
		Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);
		ElectricVehicle vehicle = electricFleet.getVehicle(vehicleId);

		Activity firstActivity = (Activity) plan.getPlanElements().get(0);
		Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
		Activity overnightActivity = findOvernightActivity(plan);

		ChargingSlot overnightSlot = null;
		ChargingSlot wholeDaySlot = null;

		List<ChargingSlot> slots = slotProvider.findSlots(person, plan, vehicle);
		slots.sort((a, b) -> {
			int ai = plan.getPlanElements().indexOf(a.isLegBased() ? a.leg() : a.startActivity());
			int bi = plan.getPlanElements().indexOf(b.isLegBased() ? b.leg() : b.startActivity());
			return Integer.compare(ai, bi);
		});

		for (ChargingSlot slot : slots) {
			if (slot.startActivity() == firstActivity && slot.endActivity() == lastActivity) {
				Preconditions.checkState(slot.leg() == null && wholeDaySlot == null && overnightSlot == null);
				wholeDaySlot = slot;
			} else if (slot.startActivity() == firstActivity && slot.endActivity() == overnightActivity) {
				Preconditions.checkState(slot.leg() == null && overnightSlot == null && wholeDaySlot == null);
				overnightSlot = slot;
			} else if (slot.startActivity() != null && slot.endActivity() != null) {
				Preconditions.checkState(slot.leg() == null && wholeDaySlot == null);
				Activity plugActivity = chargingScheduler.scheduleInitialPlugActivity(agent, slot.startActivity(), slot.charger());
				plugActivity.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_SLOT_ATTRIBUTE, slot);
			} else {
				Preconditions.checkState(slot.startActivity() == null && slot.endActivity() == null && slot.leg() != null);
				Activity plugActivity = chargingScheduler.scheduleOnroutePlugActivity(agent, slot.leg(), slot.charger());
				plugActivity.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_SLOT_ATTRIBUTE, slot);
			}
		}

		ChargingProcess overnightProcess = null;
		if (overnightSlot != null) overnightProcess = startOvernightCharging(agent, overnightSlot, now);
		if (wholeDaySlot != null) startWholeDayCharging(agent, wholeDaySlot, now);

		return new SlotInsertionResult(overnightProcess);
	}

	private Activity findOvernightActivity(Plan plan) {
		for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(plan)) {
			String mode = TripStructureUtils.getRoutingModeIdentifier().identifyMainMode(trip.getTripElements());
			if (mode.equals(chargingMode)) {
				Id<Link> originLinkId = PopulationUtils.decideOnLinkIdForActivity(trip.getOriginActivity(), scenario);
				Id<Link> destinationLinkId = PopulationUtils.decideOnLinkIdForActivity(trip.getDestinationActivity(), scenario);
				if (!originLinkId.equals(destinationLinkId)) {
					return trip.getOriginActivity();
				}
			}
		}
		return null;
	}

	/**
	 * Starts overnight charging. The vehicle is submitted to the charger immediately.
	 * The overnight activity end time is set to {@link Double#MAX_VALUE} so the agent
	 * waits until the vehicle is plugged (handled in {@link #notifyChargingStarted}).
	 */
	private ChargingProcess startOvernightCharging(MobsimAgent agent, ChargingSlot slot, double now) {
		ChargingProcess process = createChargingProcess(agent, now, slot, null, false);
		process.isOvernight = true;

		Activity endActivity = slot.endActivity();
		endActivity.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_SLOT_ATTRIBUTE, slot);

		OptionalTime plannedEndTime = timeInterpretation.decideOnActivityEndTimeAlongPlan(
			endActivity, WithinDayAgentUtils.getModifiablePlan(agent));
		Preconditions.checkState(plannedEndTime.isDefined());
		endActivity.getAttributes().putAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE, plannedEndTime.seconds());
		endActivity.setEndTime(Double.MAX_VALUE);

		// TODO: updateInitialVehicleLocation() — relocate parked QVehicle to charger link.
		//       Original uses qsim.getNetsimNetwork() which is QSim-specific.

		plugging.put(process.vehicle.getId(), process);
		submitToCharger(process, slot.charger(), now);

		return process;
	}

	private void startWholeDayCharging(MobsimAgent agent, ChargingSlot slot, double now) {
		ChargingProcess process = createChargingProcess(agent, now, slot, null, false);
		process.isWholeDay = true;

		plugging.put(process.vehicle.getId(), process);
		submitToCharger(process, slot.charger(), now);
	}

	// ===========================================================
	// Plug activity
	// ===========================================================

	private boolean handlePlugActivity(MobsimAgent agent, Activity plugActivity, double now) {
		ChargingProcess process = createOrFindChargingProcess(agent, now, plugActivity, false);
		plugging.put(process.vehicle.getId(), process);
		personsAtPlugActivity.add(agent.getId());

		submitToCharger(process, process.currentSlot.charger(), now);

		delegateEngine.handleActivity(agent);
		process.agentInEngine = true;

		if (process.isPlugged) {
			// Plugged synchronously during addVehicle() — finalize now that agent is in engine
			finalizePlugging(process, now);
		}
		// If not plugged: either queued (notifyChargingStarted fires later) or we wait

		return true;
	}

	/**
	 * Submits the vehicle to the charger. Sets {@code isSubmitted} before calling
	 * {@code addVehicle()} so that {@link #notifyChargingStarted} can distinguish
	 * a synchronous callback (agentInEngine still false) from a deferred one.
	 */
	private void submitToCharger(ChargingProcess process, Charger charger, double now) {
		ChargingStrategy strategy = chargingStrategyFactory.createStrategy(charger.getSpecification(), process.vehicle);
		process.isSubmitted = true;
		process.latestPlugTime = now + getMaxQueueWaitTime(process.agent);
		charger.getLogic().addVehicle(process.vehicle, strategy, this, now);
	}

	// ===========================================================
	// ChargingListener callbacks (synchronous in DSim)
	// ===========================================================

	@Override
	public void notifyChargingStarted(ElectricVehicle ev, double now) {
		ChargingProcess process = plugging.get(ev.getId());
		if (process == null) return;
		process.isPlugged = true;

		if (process.agentInEngine) {
			// Agent is already in delegateEngine — safe to finalize
			finalizePlugging(process, now);
		}
		// else: callback fired synchronously during addVehicle(), before delegateEngine.handleActivity().
		// handleActivity() will check process.isPlugged after delegating and call finalizePlugging().
	}

	@Override
	public void notifyVehicleQueued(ElectricVehicle ev, double now) {
		ChargingProcess process = plugging.get(ev.getId());
		if (process != null) process.isQueued = true;
	}

	@Override
	public void notifyVehicleQuitChargerQueue(ElectricVehicle ev, double now) {
		// Vehicle left queue without charging (e.g., external removal). Mark for timeout handling.
		ChargingProcess process = plugging.get(ev.getId());
		if (process != null) process.isQueued = false;
	}

	// notifyChargingEnded: charging completed naturally via ChargingStrategy.
	// The unplug activity handles cleanup; nothing to do here.

	/**
	 * Called when a vehicle is confirmed plugged and the agent is in the delegate engine.
	 * Modifies the plan (inserts unplug activity etc.) and ends the plug activity.
	 */
	private void finalizePlugging(ChargingProcess process, double now) {
		Activity plugActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(process.agent);

		if (!process.isWholeDay) {
			// Set plug activity end time so the engine wakes the agent
			plugActivity.setEndTime(now);
		}

		if (process.isWholeDay) {
			// Vehicle stays plugged the whole day — no further plan changes needed
		} else if (process.isOvernight) {
			double plannedEndTime = (Double) process.currentSlot.endActivity().getAttributes()
				.getAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE);
			process.currentSlot.endActivity().setEndTime(Math.max(now, plannedEndTime));
			WithinDayAgentUtils.resetCaches(process.agent);
			chargingScheduler.scheduleUnplugActivityAfterOvernightCharge(process.agent,
				process.currentSlot.endActivity(), process.currentSlot.charger());
		} else if (process.currentSlot.isLegBased()) {
			chargingScheduler.scheduleUnplugActivityAtCharger(process.agent, process.currentSlot.duration());
		} else {
			chargingScheduler.scheduleUntilUnplugActivity(process.agent,
				process.currentSlot.startActivity(), process.currentSlot.endActivity());
		}

		WithinDayAgentUtils.resetCaches(process.agent);
		delegateEngine.rescheduleActivityEnd(process.agent);

		personsAtPlugActivity.remove(process.agent.getId());
		active.put(process.agent.getId(), process);
		plugging.remove(process.vehicle.getId());
	}

	// ===========================================================
	// Unplug activity
	// ===========================================================

	private boolean handleUnplugActivity(MobsimAgent agent, Activity unplugActivity, double now) {
		ChargingProcess process = active.remove(agent.getId());
		Preconditions.checkNotNull(process, "No active charging process for agent %s at unplug activity", agent.getId());

		boolean stillPlugged = process.currentSlot.charger().getLogic().getPluggedVehicles().stream()
			.anyMatch(c -> process.vehicle.getId().equals(c.ev().getId()));
		if (stillPlugged) {
			process.currentSlot.charger().getLogic().removeVehicle(process.vehicle, now);
		} else {
			log.warn("Agent {} tried to unplug vehicle {} at charger {}, but it was already unplugged.",
				agent.getId(), process.vehicle.getId(), process.currentSlot.charger().getId());
		}

		unplugActivity.setEndTime(now);
		chargingScheduler.scheduleDriveToNextActivity(agent);

		eventsManager.processEvent(new FinishChargingAttemptEvent(now, agent.getId(), process.vehicle.getId()));
		eventsManager.processEvent(new FinishChargingProcessEvent(now, agent.getId(), process.vehicle.getId()));

		WithinDayAgentUtils.resetCaches(agent);
		delegateEngine.handleActivity(agent); // endTime=now → engine immediately schedules departure
		return true;
	}

	// ===========================================================
	// doSimStep: queue timeout handling only
	// ===========================================================

	@Override
	public void doSimStep(double time) {
		List<ChargingProcess> timedOut = null;
		for (ChargingProcess process : plugging.values()) {
			if (process.isQueued && time > process.latestPlugTime) {
				if (timedOut == null) timedOut = new ArrayList<>();
				timedOut.add(process);
			}
		}
		if (timedOut != null) {
			for (ChargingProcess process : timedOut) {
				plugging.remove(process.vehicle.getId());
				handleQueueTimeout(process, time);
			}
		}
		delegateEngine.doSimStep(time);
	}

	private void handleQueueTimeout(ChargingProcess process, double now) {
		process.currentSlot.charger().getLogic().removeVehicle(process.vehicle, now);
		eventsManager.processEvent(new AbortChargingAttemptEvent(now, process.agent.getId(), process.vehicle.getId()));

		if (process.isWholeDay || process.isOvernight) {
			eventsManager.processEvent(new AbortChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

			if (performAbort) {
				process.currentSlot.endActivity().setEndTime(Double.POSITIVE_INFINITY);
				WithinDayAgentUtils.resetCaches(process.agent);
				delegateEngine.rescheduleActivityEnd(process.agent);
				process.agent.setStateToAbort(now);
				internalInterface.arrangeNextAgentState(process.agent);
			} else if (process.isOvernight) {
				Activity endActivity = process.currentSlot.endActivity();
				double initialEndTime = (Double) endActivity.getAttributes()
					.getAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE);
				endActivity.setEndTime(Math.max(now, initialEndTime));
				WithinDayAgentUtils.resetCaches(process.agent);
				delegateEngine.rescheduleActivityEnd(process.agent);
				chargingScheduler.scheduleAccessAfterOvernightCharge(process.agent,
					process.currentSlot.endActivity(), process.currentSlot.charger());
			}
		} else {
			// Standard case: agent is at a plug activity
			Activity plugActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(process.agent);
			Preconditions.checkState(plugActivity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE));

			process.attemptIndex++;

			Plan plan = WithinDayAgentUtils.getModifiablePlan(process.agent);
			ChargingAlternative alternative = alternativeProvider.findAlternative(now,
				plan.getPerson(), plan, process.vehicle, process.initialSlot, process.trace);

			if (alternative != null) {
				if (process.currentSlot.isLegBased() && !alternative.isLegBased()) {
					throw new IllegalStateException(
						"Cannot switch from a leg-based charging slot to an activity-based alternative");
				}

				// End current plug activity, drive to alternative charger
				plugActivity.setEndTime(now);
				WithinDayAgentUtils.resetCaches(process.agent);
				personsAtPlugActivity.remove(process.agent.getId());
				delegateEngine.rescheduleActivityEnd(process.agent);

				Activity newPlugActivity = chargingScheduler.scheduleSubsequentPlugActivity(
					process.agent, plugActivity, alternative.charger(), now);
				newPlugActivity.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_PROCESS_ATTRIBUTE, process);

				process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
					process.currentSlot.endActivity(), process.currentSlot.leg(),
					alternative.duration(), alternative.charger());
				process.isSubmitted = false;
				process.isPlugged = false;
				process.isQueued = false;
				process.agentInEngine = false;
				process.trace.add(alternative);

				// Process re-enters plugging map when agent arrives at the new plug activity (handlePlugActivity)

				eventsManager.processEvent(new StartChargingAttemptEvent(now, process.agent.getId(),
					process.vehicle.getId(), alternative.charger().getId(), process.attemptIndex,
					process.processIndex, alternative.isLegBased(), false, alternative.duration()));
			} else {
				eventsManager.processEvent(new AbortChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

				personsAtPlugActivity.remove(process.agent.getId());

				if (performAbort) {
					plugActivity.setEndTime(Double.POSITIVE_INFINITY);
					WithinDayAgentUtils.resetCaches(process.agent);
					delegateEngine.rescheduleActivityEnd(process.agent);
					process.agent.setStateToAbort(now);
					internalInterface.arrangeNextAgentState(process.agent);
				} else {
					plugActivity.setEndTime(now);
					WithinDayAgentUtils.resetCaches(process.agent);
					delegateEngine.rescheduleActivityEnd(process.agent);
					chargingScheduler.scheduleDriveToNextActivity(process.agent);
				}
			}
		}
	}

	// ===========================================================
	// Approaching logic (replaces PersonDepartureEvent handling)
	// ===========================================================

	/**
	 * Called from {@link InternalInterfaceDelegate#arrangeNextAgentState} when a relevant agent
	 * is about to depart on a charging-mode leg. Replicates {@code processApproachingProcesses}
	 * from the original engine: checks for en-route charger alternatives and spontaneous charging.
	 */
	private void handleApproaching(MobsimAgent agent, double now) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		if (!(plan.getPlanElements().get(currentIndex) instanceof Leg leg)) return;
		if (!leg.getMode().equals(chargingMode)) return;

		Activity plugActivity = findFollowingPlugActivity(agent, plan);

		if (plugActivity != null) {
			// Create or find the process for this plug activity (also attaches it to the plug activity).
			ChargingProcess process = createOrFindChargingProcess(agent, now, plugActivity, false);

			if (process.isFirstAttempt) {
				ElectricVehicle vehicle = electricFleet.getVehicle(
					VehicleUtils.getVehicleId(plan.getPerson(), chargingMode));

				ChargingAlternative alternative = alternativeProvider.findEnrouteAlternative(now,
					plan.getPerson(), plan, vehicle, process.currentSlot);

				if (alternative != null && alternative.charger() != process.currentSlot.charger()) {
					if (process.currentSlot.isLegBased() && !alternative.isLegBased()) {
						throw new IllegalStateException(
							"Cannot switch from a leg-based charging slot to an activity-based alternative");
					}
					Activity newPlug = chargingScheduler.changePlugActivity(agent, plugActivity, alternative.charger(), now);
					newPlug.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_PROCESS_ATTRIBUTE, process);
					process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
						process.currentSlot.endActivity(), process.currentSlot.leg(),
						alternative.duration(), alternative.charger());

					eventsManager.processEvent(new UpdateChargingAttemptEvent(now, agent.getId(),
						vehicle.getId(), alternative.charger().getId(),
						alternative.isLegBased(), alternative.duration()));
				} else if (alternative != null && alternative.duration() != process.currentSlot.duration()) {
					process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
						process.currentSlot.endActivity(), process.currentSlot.leg(),
						alternative.duration(), process.currentSlot.charger());

					eventsManager.processEvent(new UpdateChargingAttemptEvent(now, agent.getId(),
						vehicle.getId(), process.currentSlot.charger().getId(),
						alternative.isLegBased(), alternative.duration()));
				}
			}
		} else if (allowSpontaneousCharging) {
			ElectricVehicle vehicle = electricFleet.getVehicle(
				VehicleUtils.getVehicleId(plan.getPerson(), chargingMode));
			ChargingAlternative alternative = alternativeProvider.findEnrouteAlternative(now,
				plan.getPerson(), plan, vehicle, null);

			if (alternative != null) {
				ChargingSlot slot = new ChargingSlot(leg, alternative.duration(), alternative.charger());
				Activity newPlug = chargingScheduler.insertPlugActivity(agent, alternative.charger(), now);
				newPlug.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_SLOT_ATTRIBUTE, slot);
				createChargingProcess(agent, now, slot, newPlug, true);
			}
		}
	}

	private Activity findFollowingPlugActivity(MobsimAgent agent, Plan plan) {
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		for (int k = currentIndex + 1; k < plan.getPlanElements().size(); k++) {
			PlanElement element = plan.getPlanElements().get(k);
			if (element instanceof Activity activity) {
				if (!TripStructureUtils.isStageActivityType(activity.getType())
					|| WithinDayEvEngine.isManagedActivityType(activity.getType())) {
					return activity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE) ? activity : null;
				}
			}
		}
		return null;
	}

	// ===========================================================
	// ChargingProcess creation helpers
	// ===========================================================

	/**
	 * Finds an existing process on the plug activity (continuing a search), or creates a new one.
	 * Also attaches the process to the plug activity attribute for later lookup.
	 */
	private ChargingProcess createOrFindChargingProcess(MobsimAgent agent, double now,
	                                                    Activity plugActivity, boolean isSpontaneous) {
		ChargingProcess existing = (ChargingProcess) plugActivity.getAttributes()
			.getAttribute(WithinDayEvEngine.CHARGING_PROCESS_ATTRIBUTE);
		if (existing != null) {
			existing.isFirstAttempt = false;
			return existing;
		}
		ChargingSlot slot = (ChargingSlot) plugActivity.getAttributes()
			.getAttribute(WithinDayEvEngine.CHARGING_SLOT_ATTRIBUTE);
		return createChargingProcess(agent, now, slot, plugActivity, isSpontaneous);
	}

	private ChargingProcess createChargingProcess(MobsimAgent agent, double now, ChargingSlot slot,
	                                              Activity plugActivity, boolean isSpontaneous) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		Person person = plan.getPerson();
		Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);
		ElectricVehicle vehicle = electricFleet.getVehicle(vehicleId);

		ChargingProcess process = new ChargingProcess();
		process.processIndex = chargingProcessIndex.compute(person.getId(),
			(id, v) -> v == null ? 0 : v + 1);
		process.agent = agent;
		process.vehicle = vehicle;
		process.currentSlot = slot;
		process.initialSlot = slot;

		eventsManager.processEvent(new StartChargingProcessEvent(now, person.getId(), vehicleId, process.processIndex));
		eventsManager.processEvent(new StartChargingAttemptEvent(now, person.getId(), vehicleId,
			slot.charger().getId(), process.attemptIndex, process.processIndex,
			slot.isLegBased(), isSpontaneous, slot.duration()));

		if (plugActivity != null) {
			plugActivity.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_PROCESS_ATTRIBUTE, process);
		}
		return process;
	}

	private double getMaxQueueWaitTime(MobsimAgent agent) {
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
		if (plan != null) {
			Double personMax = WithinDayEvEngine.getMaximumQueueTime(plan.getPerson());
			if (personMax != null) return personMax;
		}
		return maximumQueueWaitTime;
	}

	// ===========================================================
	// InternalInterface / setInternalInterface
	// ===========================================================

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		delegateEngine.setInternalInterface(new InternalInterfaceDelegate());
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		// Guard: only delegate for agents that are in our plug activity state machine.
		// ActivityEngineDefaultImpl would create spurious registrations for unknown agents.
		if (personsAtPlugActivity.contains(agent.getId())) {
			delegateEngine.rescheduleActivityEnd(agent);
		}
	}

	private class InternalInterfaceDelegate implements InternalInterface {

		@Override
		public void arrangeNextAgentState(MobsimAgent agent) {
			// En-route charger redirect: intercept agents about to depart on a charging-mode leg
			if (relevantPersons.contains(agent.getId()) && agent.getState() == MobsimAgent.State.LEG) {
				if (agent instanceof PlanAgent pa) {
					PlanElement current = pa.getCurrentPlanElement();
					if (current instanceof Leg departureLeg && departureLeg.getMode().equals(chargingMode)) {
						double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
						handleApproaching(agent, now);
					}
				}
			}
			internalInterface.arrangeNextAgentState(agent);
		}

		@Override
		public void registerAdditionalAgentOnLink(MobsimAgent agent) {
			internalInterface.registerAdditionalAgentOnLink(agent);
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
			return internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
		}

		@Override
		public Netsim getMobsim() {
			return internalInterface.getMobsim();
		}

		@Override
		public Collection<? extends DepartureHandler> getDepartureHandlers() {
			return internalInterface.getDepartureHandlers();
		}
	}

	// ===========================================================
	// Lifecycle
	// ===========================================================

	@Override
	public void beforeMobsim() {
		delegateEngine.beforeMobsim();
	}

	@Override
	public void afterMobsim() {
		delegateEngine.afterMobsim();
	}

	// ===========================================================
	// ChargingProcess inner class
	// ===========================================================

	private static class ChargingProcess {
		MobsimAgent agent;
		ElectricVehicle vehicle;

		boolean isFirstAttempt = true;
		int attemptIndex = 0;
		int processIndex;

		double latestPlugTime;

		ChargingSlot initialSlot;
		ChargingSlot currentSlot;
		List<ChargingAlternative> trace = new LinkedList<>();

		/** True after {@code addVehicle()} has been called. */
		boolean isSubmitted = false;
		/** True once the vehicle is in the charger queue (not yet plugged). */
		boolean isQueued = false;
		/** True once {@code notifyChargingStarted} fires. */
		boolean isPlugged = false;
		/**
		 * True after {@code delegateEngine.handleActivity(agent)} has been called.
		 * Used to distinguish a synchronous {@link #notifyChargingStarted} callback
		 * (fired during {@code addVehicle()}, before delegating) from a deferred one
		 * (vehicle was queued first, agent already in engine).
		 */
		boolean agentInEngine = false;

		boolean isOvernight = false;
		boolean isWholeDay = false;
	}
}
