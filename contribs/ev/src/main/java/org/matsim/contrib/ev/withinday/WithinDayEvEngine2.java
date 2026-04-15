package org.matsim.contrib.ev.withinday;

import com.google.common.base.Preconditions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
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
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

import static org.matsim.contrib.ev.withinday.WithinDayEvEngine.ACCESS_ACTIVITY_TYPE;

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
	static public final String ACTIVE_PERSON_ATTRIBUTE = "wevc:active";
	static public final String CHARGING_SLOT_ATTRIBUTE = "ev:chargingSlot";
	static public final String CHARGING_PROCESS_ATTRIBUTE = "ev:chargingProcess";

	static public final String PLUG_ACTIVITY_TYPE = "ev:plug interaction";
	static public final String UNPLUG_ACTIVITY_TYPE = "ev:unplug interaction";
	static public final String WAIT_ACTIVITY_TYPE = "ev:wait interaction";

	private final String chargingMode;

	private final ElectricFleet electricFleet;
	private final ChargingAlternativeProvider alternativeProvider;
	private final ChargingSlotProvider slotProvider;
	private final EventsManager em;
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

	private final Map<Id<Vehicle>, ChargingProcess> vehiclesToProcess = new HashMap<>();
	private final Map<Id<Person>, ChargingProcess> personsToProcess = new HashMap<>();


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
		this.em = eventsManager;
		this.chargingScheduler = chargingScheduler;
		this.scenario = scenario;
		this.chargingStrategyFactory = chargingStrategyFactory;
		this.delegateEngine = delegateEngine;
	}

	// ===========================================================
	// Activity Start Handling
	// ===========================================================

	@Override
	public boolean handleActivity(MobsimAgent agent) {

		if (agent instanceof PlanAgent pa && agent instanceof HasModifiablePlan) {
			if (pa.getPreviousPlanElement() == null) {
				// take care of agents that have their first activity of the day. We cannot maintain bookeeping about this,
				// as agents might have been initialized on another partition.
				integratePlannedChargingActivities(agent);
			}
			return handleModifiablePlanActivity(agent);
		}
		return false;
	}

	private boolean handleModifiablePlanActivity(MobsimAgent agent) {

		var activity = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
		if (activity.getType().equals(PLUG_ACTIVITY_TYPE)) {
			return handlePlugActivity(agent, activity, true);
		} else if (activity.getType().equals(UNPLUG_ACTIVITY_TYPE)) {
			var process = active.remove(agent.getId());
			Preconditions.checkNotNull(process);
			// TODO agent goes into activity engine
		}
		return false;
	}

	private boolean handlePlugActivity(MobsimAgent agent, Activity activity, boolean isSpontaneous) {

		var process = getChargingProcessForPlugActivity(agent, activity, isSpontaneous);
		// add vehicle to charger, it will be either queued or plugged
		var spec = process.currentSlot.charger().getSpecification();
		var ev = process.vehicle;
		var strategy = chargingStrategyFactory.createStrategy(spec, ev);
		var chargingLogic = process.currentSlot.charger().getLogic();
		var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		chargingLogic.addVehicle(ev, strategy, this, now);
		process.latestPlugTime = now + getMaxQueueWaitTime(agent);
		process.isSubmitted = true;
		if (!delegateEngine.handleActivity(agent)) {
			throw new RuntimeException("Activity engine is expected to accept agent");
		}
		return true;
	}

	// TODO handle unplug activity goes here

	private boolean unplug(MobsimAgent agent, Activity activity) {

		// TODO: this is not really an activity yet. This will probably be called from activity end or charging end
		var process = getChargingProcessForPlugActivity(agent, activity, false);
		var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		if (isPlugged(process)) {
			var chargingLogic = process.currentSlot.charger().getLogic();
			chargingLogic.removeVehicle(process.vehicle, now);
		} else {
			log.warn("Agent {} tried to unplug vehicle {} that is not plugged", agent.getId(), process.vehicle.getId());
		}

		activity.setEndTime(now);
		WithinDayAgentUtils.resetCaches(agent);
		WithinDayAgentUtils.rescheduleActivityEnd(agent, internalInterface.getMobsim());

		chargingScheduler.scheduleDriveToNextActivity(agent);

		em.processEvent(new FinishChargingAttemptEvent(now, process.agent.getId(), process.vehicle.getId()));
		em.processEvent(new FinishChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

		return true;
	}

	//=======================
	// Activity End handling
	// =====================

	private void endActivity(MobsimAgent agent) {

		// SAFETY: we have only accepted PlanAgents/HasModifieablePlan
		// At this stage, we assume that the agent has progressed to the next leg. Hence, the current element is a leg
		var pa = (PlanAgent) agent;
		var currentPlan = pa.getCurrentPlan();
		var currentLeg = (Leg) pa.getCurrentPlanElement();
		var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		if (currentLeg.getMode().equals(chargingMode)) {
			var process = getChargingProcessFromLeg(agent);
			if (process != null && process.isFirstAttempt) {
				var alternative = alternativeProvider.findEnrouteAlternative(now, currentPlan.getPerson(), currentPlan, process.vehicle, process.currentSlot);
				if (alternative != null) {
					if (process.currentSlot.isLegBased() != alternative.isLegBased()) {
						throw new IllegalStateException("Cannot switch from a leg-based charging slot to an activity-based alternative because activities are not known");
					}

					if (alternative.charger() != process.currentSlot.charger()) {
						Activity followingPlugActivity = findFollowingPlugActivity(agent);

						// drive to different charger and schedule a plug activity
						Activity plugActivity = chargingScheduler.changePlugActivity(process.agent,
							followingPlugActivity, alternative.charger(),
							now);
						plugActivity.getAttributes().putAttribute(CHARGING_PROCESS_ATTRIBUTE, process);

						// update slot
						process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
							process.currentSlot.endActivity(),
							process.currentSlot.leg(), alternative.duration(),
							alternative.charger());

						// send event for scoring
						em.processEvent(new UpdateChargingAttemptEvent(now, process.agent.getId(),
							process.vehicle.getId(), alternative.charger().getId(),
							alternative.isLegBased(), alternative.duration()));
					} else if (alternative.duration() != process.currentSlot.duration()) {
						// update slot with custom duration (either switch between leg- and
						// activity-based slot, or change of duration)
						process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
							process.currentSlot.endActivity(),
							process.currentSlot.leg(), alternative.duration(),
							process.currentSlot.charger());

						// send event for scoring
						em.processEvent(new UpdateChargingAttemptEvent(now, process.agent.getId(),
							process.vehicle.getId(), alternative.charger().getId(),
							alternative.isLegBased(), alternative.duration()));
					}
				}
			} else if (process == null && allowSpontaneousCharging) {
				// no upcoming plug activity is found, this is a completely spantaneous charging
				// attempt

				var vehicleId = VehicleUtils.getVehicleId(currentPlan.getPerson(), chargingMode);
				ElectricVehicle vehicle = electricFleet.getVehicle(vehicleId);

				ChargingAlternative alternative = alternativeProvider.findEnrouteAlternative(now,
					currentPlan.getPerson(), currentPlan, vehicle, null);

				if (alternative != null) {
					ChargingSlot slot = new ChargingSlot(currentLeg, alternative.duration(),
						alternative.charger());

					Activity plugActivity = chargingScheduler.insertPlugActivity(agent,
						alternative.charger(), now);
					plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);

					getChargingProcessForPlugActivity(agent, plugActivity, true);
				}
			}
		}
	}


	//====================
	// Charging Handler
	// ===================

	@Override
	public void notifyChargingStarted(ElectricVehicle ev, double now) {

		var process = vehiclesToProcess.get(ev.getId());
		if (process == null) {
			throw new IllegalStateException("Vehicle " + ev.getId() + " is not registered at withinday activity handler");
		}

		process.isPlugged = true;

		var plugActivity = (Activity) ((PlanAgent) process.agent).getCurrentPlanElement();
		plugActivity.setEndTime(now);

		WithinDayAgentUtils.resetCaches(process.agent);
		delegateEngine.rescheduleActivityEnd(process.agent);

		if (process.currentSlot.isLegBased()) {
			chargingScheduler.scheduleUnplugActivityAtCharger(process.agent, process.currentSlot.duration());
		} else {
			chargingScheduler.scheduleUntilUnplugActivity(process.agent, process.currentSlot.startActivity(), process.currentSlot.endActivity());
		}
	}

	@Override
	public void notifyVehicleQueued(ElectricVehicle ev, double now) {
		var process = vehiclesToProcess.get(ev.getId());
		if (process == null) {
			throw new IllegalStateException("Vehicle " + ev.getId() + " is not registered at withinday activity handler");
		}
		process.isQueued = true;
	}

	@Override
	public void notifyVehicleQuitChargerQueue(ElectricVehicle ev, double now) {
		throw new UnsupportedOperationException(" Vehicle Quit charging not yet implemented. The old code didn't do anything about it...");
	}


	private boolean isPlugged(ChargingProcess process) {
		return process.currentSlot.charger().getLogic().getPluggedVehicles().stream()
			.anyMatch(c -> process.vehicle.getId().equals(c.ev().getId()));
	}


	private ChargingProcess getChargingProcessForPlugActivity(MobsimAgent agent, Activity activity, boolean isSpontaneous) {

		// check our cache
		var process = personsToProcess.get(agent.getId());

		// if not chached check whether activity has it
		if (process == null) {
			process = (ChargingProcess) activity.getAttributes().getAttribute(CHARGING_PROCESS_ATTRIBUTE);
		}
		// if not found, create one.
		if (process == null) {
			var slot = (ChargingSlot) activity.getAttributes().getAttribute(CHARGING_SLOT_ATTRIBUTE);
			process = createChargingProcess(agent, slot, activity, isSpontaneous);
		} else {
			// if we found one, this is not the first attempt
			process.isFirstAttempt = false;
		}
		return process;
	}

	private ChargingProcess getChargingProcessFromLeg(MobsimAgent agent) {

		var nextPlugActivity = findFollowingPlugActivity(agent);
		if (nextPlugActivity != null) {
			return getChargingProcessForPlugActivity(agent, nextPlugActivity, false);
		} else {
			return null;
		}
	}


	private void integratePlannedChargingActivities(MobsimAgent agent) {
		var plan = ((HasModifiablePlan) agent).getModifiablePlan();
		var person = plan.getPerson();

		if (isActive(person) && VehicleUtils.hasVehicleId(plan.getPerson(), chargingMode)) {
			relevantPersons.add(person.getId());

			var vehicleId = VehicleUtils.getVehicleId(person, chargingMode);
			relevantVehicles.add(vehicleId);

			var firstActivity = plan.getPlanElements().getFirst();
			var lastActivity = plan.getPlanElements().getLast();
			var overnightActivity = findOvernightActivity(plan);

			var ev = electricFleet.getVehicle(vehicleId);
			var slots = findSlotsSorted(plan, ev);

			ChargingSlot overnightSlot = null;
			ChargingSlot wholeDaySlot = null;
			boolean foundRegularSlot = false;

			for (var slot : slots) {
				if (slot.startActivity() == firstActivity && slot.endActivity() == lastActivity) {
					// special case: this is a slot spanning the whole plan
					Preconditions.checkState(slot.leg() == null);
					Preconditions.checkState(!foundRegularSlot);
					Preconditions.checkState(overnightSlot == null);
					Preconditions.checkState(wholeDaySlot == null);
					wholeDaySlot = slot;
					//wholeDayCount++;
				} else if (slot.startActivity() == firstActivity && slot.endActivity() == overnightActivity) {
					// special case: this slot has started on the "previous day" and the vehicle
					// only needs to be unplugged after the overnight activity. In order to simplify
					// time calculation for scheduling, we treat this slot last
					Preconditions.checkState(slot.leg() == null);
					Preconditions.checkState(overnightSlot == null);
					Preconditions.checkState(wholeDaySlot == null);
					overnightSlot = slot;
					//overnightCount++;
				} else if (slot.startActivity() != null && slot.endActivity() != null) {
					// standard case: schedule a plug activity along the plan
					Preconditions.checkState(slot.leg() == null);
					Preconditions.checkState(wholeDaySlot == null);
					Activity plugActivity = chargingScheduler.scheduleInitialPlugActivity(agent,
						slot.startActivity(), slot.charger());
					plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);
					//activityBasedCount++;
				} else {
					// leg case: schedule a plug activity along a leg
					Preconditions.checkState(slot.startActivity() == null);
					Preconditions.checkState(slot.endActivity() == null);
					Preconditions.checkState(slot.leg() != null);

					Activity plugActivity = chargingScheduler.scheduleOnroutePlugActivity(agent, slot.leg(),
						slot.charger());
					plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);
					//legBasedCount++;
				}
			}

			if (overnightSlot != null) {
				startOvernightCharging(agent, overnightSlot);
				// TODO figure out relocation of vehicles
				//updateInitialVehicleLocation(plan, vehicleId, overnightSlot);
			}

			if (wholeDaySlot != null) {
				startWholeDayCharging(agent, wholeDaySlot);
				// TODO figure out relocation of vehicles.
				//updateInitialVehicleLocation(plan, vehicleId, wholeDaySlot);
			}
		}

		// in any case this agent has been handled and never needs to see this method again.
		initializedPersons.add(agent.getId());
	}

	private void startOvernightCharging(MobsimAgent agent, ChargingSlot slot) {
		Activity endActivity = slot.endActivity();
		endActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);

		// figure out end time
		var plan = ((PlanAgent) agent).getCurrentPlan();
		OptionalTime endTime = timeInterpretation.decideOnActivityEndTimeAlongPlan(endActivity, plan);
		Preconditions.checkState(endTime.isDefined());

		// put end time into attributes and set as end time on activity
		// possibly, we dont need the attribute anymore
		endActivity.getAttributes().putAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE, endTime.seconds());
		endActivity.setEndTime(endTime.seconds());
		WithinDayAgentUtils.resetCaches(agent);// don't know whether this is strictly necessary anymore

		// start a charging process
		ChargingProcess process = createChargingProcess(agent, slot, null, false);
		process.isOvernight = true;

		// schedule unplug and let agent perform activity
		chargingScheduler.scheduleUnplugActivityAfterOvernightCharge(agent, endActivity, process.currentSlot.charger());
	}

	private List<ChargingSlot> findSlotsSorted(Plan plan, ElectricVehicle ev) {

		// we have to sort the results. Actually, the slotProvider could give us the indices of the
		// plan elements. however it is not included in the api at the moment and I don't want to
		// touch all the things in the contrib.
		Comparator<ChargingSlot> slotComparator = (first, second) -> {
			int firstIndex = plan.getPlanElements()
				.indexOf(first.isLegBased() ? first.leg() : first.startActivity());

			int secondIndex = plan.getPlanElements()
				.indexOf(second.isLegBased() ? second.leg() : second.startActivity());

			return Integer.compare(firstIndex, secondIndex);
		};
		var slots = slotProvider.findSlots(plan.getPerson(), plan, ev);
		slots.sort(slotComparator);
		return slots;
	}

	private static boolean isActive(Person person) {
		Boolean isActive = (Boolean) person.getAttributes().getAttribute(ACTIVE_PERSON_ATTRIBUTE);
		return isActive != null && isActive;
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
		if (wholeDaySlot != null) startWholeDayCharging(agent, wholeDaySlot);

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
		ChargingProcess process = createChargingProcess(agent, slot, null, false);
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

	private void startWholeDayCharging(MobsimAgent agent, ChargingSlot slot) {
		double now = internalInterface.getMobsim().getSimTimer().getSimStartTime();
		var process = createChargingProcess(agent, slot, null, false);
		process.isWholeDay = true;

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

		em.processEvent(new FinishChargingAttemptEvent(now, agent.getId(), process.vehicle.getId()));
		em.processEvent(new FinishChargingProcessEvent(now, agent.getId(), process.vehicle.getId()));

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
		em.processEvent(new AbortChargingAttemptEvent(now, process.agent.getId(), process.vehicle.getId()));

		if (process.isWholeDay || process.isOvernight) {
			em.processEvent(new AbortChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

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

				em.processEvent(new StartChargingAttemptEvent(now, process.agent.getId(),
					process.vehicle.getId(), alternative.charger().getId(), process.attemptIndex,
					process.processIndex, alternative.isLegBased(), false, alternative.duration()));
			} else {
				em.processEvent(new AbortChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

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

		Activity plugActivity = findFollowingPlugActivity(agent);

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

					em.processEvent(new UpdateChargingAttemptEvent(now, agent.getId(),
						vehicle.getId(), alternative.charger().getId(),
						alternative.isLegBased(), alternative.duration()));
				} else if (alternative != null && alternative.duration() != process.currentSlot.duration()) {
					process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
						process.currentSlot.endActivity(), process.currentSlot.leg(),
						alternative.duration(), process.currentSlot.charger());

					em.processEvent(new UpdateChargingAttemptEvent(now, agent.getId(),
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
				createChargingProcess(agent, slot, newPlug, true);
			}
		}
	}

	private Activity findFollowingPlugActivity(MobsimAgent agent) {

		var pa = (PlanAgent) agent;
		var currentElement = pa.getCurrentPlanElement();
		var plan = pa.getCurrentPlan();
		var currentIndex = plan.getPlanElements().indexOf(currentElement);

		return plan.getPlanElements().stream()
			.skip(currentIndex)
			.filter(e -> e instanceof Activity)
			.map(e -> (Activity) e)
			.takeWhile(a -> !TripStructureUtils.isStageActivityType(a.getType()) || isManagedActivityType(a.getType()))
			.filter(a -> a.getType().equals(PLUG_ACTIVITY_TYPE))
			.findFirst()
			.orElse(null);

//		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
//		for (int k = currentIndex + 1; k < plan.getPlanElements().size(); k++) {
//			PlanElement element = plan.getPlanElements().get(k);
//			if (element instanceof Activity activity) {
//				if (!TripStructureUtils.isStageActivityType(activity.getType())
//					|| WithinDayEvEngine.isManagedActivityType(activity.getType())) {
//					return activity.getType().equals(WithinDayEvEngine.PLUG_ACTIVITY_TYPE) ? activity : null;
//				}
//			}
//		}
//		return null;
	}

	static public boolean isManagedActivityType(String activityType) {
		return activityType.equals(PLUG_ACTIVITY_TYPE) || activityType.equals(UNPLUG_ACTIVITY_TYPE)
			|| activityType.equals(WAIT_ACTIVITY_TYPE) || activityType.equals(ACCESS_ACTIVITY_TYPE);
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
		return createChargingProcess(agent, slot, plugActivity, isSpontaneous);
	}


	private ChargingProcess createChargingProcess(
		MobsimAgent agent, ChargingSlot slot, Activity plugActivity, boolean isSpontaneous) {

		var plan = ((HasModifiablePlan) agent).getModifiablePlan();
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

		var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		em.processEvent(new StartChargingProcessEvent(now, person.getId(), vehicleId, process.processIndex));
		em.processEvent(new StartChargingAttemptEvent(now, person.getId(), vehicleId,
			slot.charger().getId(), process.attemptIndex, process.processIndex,
			slot.isLegBased(), isSpontaneous, slot.duration()));

		// store the process at various places
		if (plugActivity != null) {
			plugActivity.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_PROCESS_ATTRIBUTE, process);
		}
		vehiclesToProcess.put(vehicleId, process);
		personsToProcess.put(person.getId(), process);

		return process;
	}

	private double getMaxQueueWaitTime(MobsimAgent agent) {
		var plan = ((HasModifiablePlan) agent).getModifiablePlan();
		Double personMax = WithinDayEvEngine.getMaximumQueueTime(plan.getPerson());
		return personMax == null ? maximumQueueWaitTime : personMax;
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
			var process = personsToProcess.remove(agent.getId());
			if (process != null) {
				endActivity(agent);
				var vehicleId = process.vehicle.getId();
				vehiclesToProcess.remove(vehicleId);
			}

			internalInterface.arrangeNextAgentState(agent);


//			// En-route charger redirect: intercept agents about to depart on a charging-mode leg
//			if (relevantPersons.contains(agent.getId()) && agent.getState() == MobsimAgent.State.LEG) {
//				if (agent instanceof PlanAgent pa) {
//					PlanElement current = pa.getCurrentPlanElement();
//					if (current instanceof Leg departureLeg && departureLeg.getMode().equals(chargingMode)) {
//						double now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
//						handleApproaching(agent, now);
//					}
//				}
//			}
//			internalInterface.arrangeNextAgentState(agent);
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
