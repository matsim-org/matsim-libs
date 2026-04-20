package org.matsim.contrib.ev.withinday;

import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.MobsimMessageCollector;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.ev.charging.ChargingListener;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.contrib.ev.withinday.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.dsim.simulation.net.ParkedVehicles;
import org.matsim.dsim.simulation.net.SimNetwork;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.*;

public class DistributedWithinDayEvEngine implements DistributedActivityHandler, DistributedDepartureHandler, DistributedMobsimEngine, NotifyAgentPartitionTransfer, ChargingListener {

	static public final String PLUG_ACTIVITY_TYPE = "ev:plug interaction";
	static public final String UNPLUG_ACTIVITY_TYPE = "ev:unplug interaction";
	static public final String WAIT_ACTIVITY_TYPE = "ev:wait interaction";
	static public final String ACCESS_ACTIVITY_TYPE = "ev:access interaction";

	static public final String ACTIVE_PERSON_ATTRIBUTE = "wevc:active";
	static public final String CHARGING_PROCESS_ATTRIBUTE = "ev:chargingProcess";
	static public final String CHARGING_SLOT_ATTRIBUTE = "ev:chargingSlot";
	static private final String INITIAL_ACTIVITY_END_TIME_ATTRIBUTE = "ev:initialActivityEndTime";

	private final Map<Id<Person>, ChargingProcess> personsToProcess = new HashMap<>();
	private final Map<Id<Vehicle>, ChargingProcess> vehiclesToProcess = new HashMap<>();

	private final Map<Id<Person>, MobsimAgent> personsAtCharger = new HashMap<>();
	private final Set<Id<Vehicle>> queuedVehicles = new HashSet<>();

	private final WithinDayChargingStrategy.Factory chargingStrategyFactory;
	private final ActivityEngine delegateEngine;
	private final ElectricFleet electricFleet;
	private final boolean performAbort;

	private InternalInterface internalInterface;
	private final Vehicles vehicles;
	private final double maximumQueueWaitTime;
	private final boolean allowSpontaneousCharging;
	private final String chargingMode;
	private final ChargingSlotProvider slotProvider;
	private final ChargingScheduler chargingScheduler;
	private final EventsManager em;
	private final Scenario scenario;
	private final TimeInterpretation timeInterpretation;
	private final ParkedVehicles parkedVehicles;
	private final MobsimMessageCollector partitionTransfer;
	private final SimNetwork simNetwork;
	private final ChargingAlternativeProvider alternativeProvider;
	private final ChargingInfrastructure chargingInfrastructure;
	private final QVehicleFactory qVehicleFactory;

	@Inject
	public DistributedWithinDayEvEngine(WithinDayEvConfigGroup config, WithinDayChargingStrategy.Factory chargingStrategyFactory, ActivityEngine delegateEngine, ElectricFleet electricFleet, Vehicles vehicles, ChargingSlotProvider slotProvider, ChargingScheduler chargingScheduler, EventsManager em, Scenario scenario, TimeInterpretation timeInterpretation, ParkedVehicles parkedVehicles, MobsimMessageCollector partitionTransfer, SimNetwork simNetwork, ChargingAlternativeProvider alternativeProvider, ChargingInfrastructure chargingInfrastructure, QVehicleFactory qVehicleFactory) {
		this.chargingStrategyFactory = chargingStrategyFactory;
		this.delegateEngine = delegateEngine;
		this.maximumQueueWaitTime = config.getMaximumQueueTime();
		this.allowSpontaneousCharging = config.isAllowSpoantaneousCharging();
		this.chargingMode = config.getCarMode();
		this.performAbort = config.isAbortAgents();
		this.electricFleet = electricFleet;
		this.vehicles = vehicles;
		this.slotProvider = slotProvider;
		this.chargingScheduler = chargingScheduler;
		this.em = em;
		this.scenario = scenario;
		this.timeInterpretation = timeInterpretation;
		this.parkedVehicles = parkedVehicles;
		this.partitionTransfer = partitionTransfer;
		this.simNetwork = simNetwork;
		this.alternativeProvider = alternativeProvider;
		this.chargingInfrastructure = chargingInfrastructure;
		this.qVehicleFactory = qVehicleFactory;
	}


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

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {

		// we use this departure handler to insert plug activities into the plan. This handler actually
		// never handles departures but always returns false.
		if (agent instanceof PlanAgent pa) {
			// we need to hook in on departure, as we need to replace the unplug activity BEFORE the agent
			// is passed to handleActivity. This is because the agent initializes its next activity at the end
			// of the leg, while it is still managed by the leg engine and dispatches the corresponding ActivityStartEvent.
			// Conceptually, it would make more sense if the activity engine (this one) would do this on handleActivity, but
			// this is how things are done at the moment.
			checkForFailedOvernightCharge(agent);
			if (pa.getCurrentPlanElement() instanceof Leg leg && leg.getMode().equals(chargingMode)) {
				proposeAlternative(agent, leg);
			}
		}

		return false;
	}

	private void proposeAlternative(MobsimAgent agent, Leg leg) {

		var process = getChargingProcessForLeg(agent);
		var departureTime = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		var plan = ((HasModifiablePlan) agent).getModifiablePlan();

		if (process != null && process.isFirstAttempt()) {
			var ev = electricFleet.getVehicle(process.vehicleId);
			// the callback can be called either immediately or +1 second. Technically, this can cause a crash as an agent with a leg of 0 seconds
			// could arrive at the charging activity before the alternative is found. However, the existing code did not hedge against this case either,
			// so we ignore this case as well. If this ever becomes an issue, one would have to check the agent state within the callback and decide
			// whether applying the alternative is still feasible. janek apr' 26
			alternativeProvider.findEnrouteAlternativeAsync(departureTime, (PlanAgent) agent, ev, process.currentSlot, optAlternative -> {
				if (optAlternative.isPresent()) {
					var alternative = optAlternative.get();

					if (process.currentSlot.isLegBased() && !alternative.isLegBased()) {
						throw new IllegalStateException(
							"Cannot switch from a leg-based charging slot to an activity-based alternative because activities are not known");
					}

					var callbackTime = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

					if (alternative.charger() != process.currentSlot.charger()) {

						Activity followingPlugActivity = findFollowingActivity(agent, PLUG_ACTIVITY_TYPE);

						// drive to different charger and schedule a plug activity
						var charger = getLiveCharger(alternative.charger());
						Activity plugActivity = chargingScheduler.changePlugActivity(agent,
							followingPlugActivity, charger,
							callbackTime);
						plugActivity.getAttributes().putAttribute(CHARGING_PROCESS_ATTRIBUTE, process);

						// update slot
						process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
							process.currentSlot.endActivity(),
							process.currentSlot.leg(), alternative.duration(),
							charger);

						// send event for scoring
						em.processEvent(new UpdateChargingAttemptEvent(callbackTime, agent.getId(),
							process.vehicleId, alternative.charger(),
							alternative.isLegBased(), alternative.duration()));

					} else if (alternative.duration() != process.currentSlot.duration()) {

						// update slot with custom duration (either switch between leg- and
						// activity-based slot, or change of duration)
						process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
							process.currentSlot.endActivity(),
							process.currentSlot.leg(), alternative.duration(),
							process.currentSlot.charger());

						// send event for scoring
						em.processEvent(new UpdateChargingAttemptEvent(callbackTime, agent.getId(),
							ev.getId(), alternative.charger(),
							alternative.isLegBased(), alternative.duration()));
					}
				}
			});
		} else if (process == null && allowSpontaneousCharging) {
			// no upcoming plug activity is found, this is a completely spantaneous charging
			// attempt

			var vehicleId = VehicleUtils.getVehicleId(plan.getPerson(), chargingMode);
			ElectricVehicle vehicle = electricFleet.getVehicle(vehicleId);

			alternativeProvider.findEnrouteAlternativeAsync(departureTime, (PlanAgent) agent, vehicle, null,
				optAlternative -> {
					if (optAlternative.isPresent()) {
						var callbackTime = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
						var alternative = optAlternative.get();
						var charger = getLiveCharger(alternative.charger());
						ChargingSlot slot = new ChargingSlot(leg, alternative.duration(),
							charger);

						Activity plugActivity = chargingScheduler.insertPlugActivity(agent,
							charger, callbackTime);
						plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);

						var spontaneousProcess = getChargingProcessForPlugActivity(agent, plugActivity, true);
						vehiclesToProcess.put(spontaneousProcess.vehicleId, spontaneousProcess);
						personsToProcess.put(agent.getId(), spontaneousProcess);
					}
				});
		}
	}

	private boolean handleModifiablePlanActivity(MobsimAgent agent) {

		var activity = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
		if (activity.getType().equals(PLUG_ACTIVITY_TYPE)) {
			return handlePlugActivity(agent, activity, true);
		} else if (activity.getType().equals(UNPLUG_ACTIVITY_TYPE)) {
			return handleUnplugActivity(agent, activity);
		} else if (activity.getType().equals(ACCESS_ACTIVITY_TYPE)) {
			return handleAccessActivity(agent);
		}
		return false;
	}

	private boolean handlePlugActivity(MobsimAgent agent, Activity activity, boolean isSpontaneous) {

		var process = getChargingProcessForPlugActivity(agent, activity, isSpontaneous);
		process.isPersonAtCharger = true;

		// we know that the process takes place at a charger on this partition. So, we can cache
		// the process in our bookeeping instead of the activity attributes.
		personsAtCharger.put(agent.getId(), agent);
		vehiclesToProcess.put(process.vehicleId, process);
		personsToProcess.put(agent.getId(), process);

		submitToCharger(process);
		if (!delegateEngine.handleActivity(agent)) {
			throw new RuntimeException("Activity engine is expected to accept agent");
		}
		return true;
	}

	private boolean handleUnplugActivity(MobsimAgent agent, Activity activity) {

		var process = personsToProcess.remove(agent.getId());
		var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		if (process.isAborted && performAbort) {
			// set state to abort and hand it back to the mobsim, which should detect the abort state.
			var time = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
			agent.setStateToAbort(time);
			internalInterface.arrangeNextAgentState(agent);
			return true; // we have kinda handled the agent.
		}
		// remove the vehicle from the charger. This will also clear the charging process from vehicleToProcesses
		if (process.isPlugged) {
			var charger = getLiveCharger(process.currentSlot);
			var ev = electricFleet.getVehicle(process.vehicleId);
			charger.getLogic().removeVehicle(ev, now);
			process.isPlugged = false;
		}

		// update bookkeeping, add plan elements for what's to come and make sure the unplug activity is ended immediately
		personsAtCharger.remove(agent.getId());
		chargingScheduler.scheduleDriveToNextActivity(agent);
		activity.setEndTime(now);
		WithinDayAgentUtils.resetCaches(agent);

		// dispatch some events and pass the agent to the activity engine.
		em.processEvent(new FinishChargingAttemptEvent(now, agent.getId(), process.vehicleId));
		em.processEvent(new FinishChargingProcessEvent(now, agent.getId(), process.vehicleId));
		return delegateEngine.handleActivity(agent);
	}

	private boolean handleAccessActivity(MobsimAgent agent) {
		chargingScheduler.scheduleDriveToNextActivity(agent);
		return delegateEngine.handleActivity(agent);
	}

	@Override
	public void rescheduleActivityEnd(MobsimAgent agent) {
		delegateEngine.rescheduleActivityEnd(agent);
	}

	@Override
	public void notifyVehicleQueued(ElectricVehicle ev, double now) {
		var process = vehiclesToProcess.get(ev.getId());
		process.isQueued = true;
		queuedVehicles.add(ev.getId());
	}

	@Override
	public void notifyChargingStarted(ElectricVehicle ev, double now) {

		var process = vehiclesToProcess.get(ev.getId());
		queuedVehicles.remove(ev.getId());
		process.isPlugged = true;

		if (process.isPersonAtCharger) {
			var agent = personsAtCharger.get(process.personId);
			var plugActivity = (Activity) ((PlanAgent) agent).getCurrentPlanElement();

			// end activity
			plugActivity.setEndTime(now);
			WithinDayAgentUtils.resetCaches(agent);
			delegateEngine.rescheduleActivityEnd(agent);

			if (process.currentSlot.isLegBased()) {
				// schedule unplug at the charger then continue to main activity
				chargingScheduler.scheduleUnplugActivityAtCharger(agent,
					process.currentSlot.duration());
			} else {
				// walk to main activity, perform it, walk back to charger and unplug
				chargingScheduler.scheduleUntilUnplugActivity(agent,
					process.currentSlot.startActivity(),
					process.currentSlot.endActivity());
			}
		}
		// all other options don't require any rescheduling
	}

	@Override
	public void notifyChargingEnded(ElectricVehicle ev, double now) {

		var process = vehiclesToProcess.remove(ev.getId());
		process.isPlugged = false;
	}

	@Override
	public void notifyVehicleQuitChargerQueue(ElectricVehicle ev, double now) {
		queuedVehicles.remove(ev.getId());
		var process = vehiclesToProcess.get(ev.getId());
		process.isQueued = false;
	}

	private void integratePlannedChargingActivities(MobsimAgent agent) {
		var plan = ((HasModifiablePlan) agent).getModifiablePlan();
		var person = plan.getPerson();

		if (isActive(person) && VehicleUtils.hasVehicleId(plan.getPerson(), chargingMode)) {

			var vehicleId = VehicleUtils.getVehicleId(person, chargingMode);

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
					var charger = getLiveCharger(slot);
					Activity plugActivity = chargingScheduler.scheduleInitialPlugActivity(agent,
						slot.startActivity(), charger);
					plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);
					//activityBasedCount++;
				} else {
					// leg case: schedule a plug activity along a leg
					Preconditions.checkState(slot.startActivity() == null);
					Preconditions.checkState(slot.endActivity() == null);
					Preconditions.checkState(slot.leg() != null);

					var charger = getLiveCharger(slot);
					Activity plugActivity = chargingScheduler.scheduleOnroutePlugActivity(agent, slot.leg(), charger);
					plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);
					//legBasedCount++;
				}
			}

			if (overnightSlot != null) {
				integrateOvernightUnplugActivity(agent, overnightSlot);
				initializeAlreadyCharging(agent, ev, overnightSlot, AlreadyCharging.OVERNIGHT);
			}

			if (wholeDaySlot != null) {
				initializeAlreadyCharging(agent, ev, wholeDaySlot, AlreadyCharging.WHOLE_DAY);
			}
		}
	}

	private static boolean isActive(Person person) {
		Boolean isActive = (Boolean) person.getAttributes().getAttribute(ACTIVE_PERSON_ATTRIBUTE);
		return isActive != null && isActive;
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
		}

		return process;
	}

	private ChargingProcess getChargingProcessForLeg(MobsimAgent agent) {
		var plugActivity = findFollowingActivity(agent, PLUG_ACTIVITY_TYPE);
		if (plugActivity != null)
			return getChargingProcessForPlugActivity(agent, plugActivity, false);
		else return null;
	}

	private Activity findFollowingActivity(MobsimAgent agent, String actType) {

		var pa = (PlanAgent) agent;
		var currentElement = pa.getCurrentPlanElement();
		var plan = pa.getCurrentPlan();
		var currentIndex = plan.getPlanElements().indexOf(currentElement);

		return plan.getPlanElements().stream()
			.skip(currentIndex)
			.filter(e -> e instanceof Activity)
			.map(e -> (Activity) e)
			.takeWhile(a -> !TripStructureUtils.isStageActivityType(a.getType()) || isManagedActivityType(a.getType()))
			.filter(a -> a.getType().equals(actType))
			.findFirst()
			.orElse(null);
	}

	private ChargingProcess createChargingProcess(
		MobsimAgent agent, ChargingSlot slot, Activity plugActivity, boolean isSpontaneous) {

		var plan = ((HasModifiablePlan) agent).getModifiablePlan();
		Person person = plan.getPerson();
		Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(person, chargingMode);

		var maxQueueTime = getMaxQueueWaitTime(agent);
		ChargingProcess process = new ChargingProcess(agent.getId(), vehicleId, maxQueueTime, slot);

		var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		em.processEvent(new StartChargingProcessEvent(now, person.getId(), vehicleId, process.processIndex));
		em.processEvent(new StartChargingAttemptEvent(now, person.getId(), vehicleId,
			slot.charger().getId(), process.attemptIndex, process.processIndex,
			slot.isLegBased(), isSpontaneous, slot.duration()));

		// store the process at various places
		if (plugActivity != null) {
			plugActivity.getAttributes().putAttribute(WithinDayEvEngine.CHARGING_PROCESS_ATTRIBUTE, process);
		}

		return process;
	}

	private double getMaxQueueWaitTime(MobsimAgent agent) {
		var plan = ((HasModifiablePlan) agent).getModifiablePlan();
		Double personMax = WithinDayEvEngine.getMaximumQueueTime(plan.getPerson());
		return personMax == null ? maximumQueueWaitTime : personMax;
	}

	private void integrateOvernightUnplugActivity(MobsimAgent agent, ChargingSlot slot) {

		// everything planned here stays with the agent's plan. Therefore, it can be done
		// regardless of which partition the plug activity is located on.
		Activity endActivity = slot.endActivity();
		endActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);

		OptionalTime endTime = timeInterpretation.decideOnActivityEndTimeAlongPlan(endActivity, WithinDayAgentUtils.getModifiablePlan(agent));
		Preconditions.checkState(endTime.isDefined());

		endActivity.getAttributes().putAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE, endTime.seconds());
		endActivity.setEndTime(endTime.seconds());

		WithinDayAgentUtils.resetCaches(agent);

		var charger = getLiveCharger(slot);
		var unplugActivity = chargingScheduler.scheduleUnplugActivityAfterOvernightCharge(agent, endActivity, charger);
		unplugActivity.setEndTime(unplugActivity.getStartTime().seconds());
	}

	private void submitToCharger(ChargingProcess process) {

		var charger = getLiveCharger(process.currentSlot);
		var spec = charger.getSpecification();
		var ev = electricFleet.getVehicle(process.vehicleId);
		var strategy = chargingStrategyFactory.createStrategy(spec, ev);
		var chargingLogic = charger.getLogic();
		var now = internalInterface.getMobsim().getSimTimer().getTimeOfDay();

		// add vehicle to charger, it will be either queued or plugged
		chargingLogic.addVehicle(ev, strategy, this, now);
		process.latestPlugTime = now + process.maxQueueTime;
		process.isSubmitted = true;
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

	private enum AlreadyCharging {OVERNIGHT, WHOLE_DAY}

	private void initializeAlreadyCharging(MobsimAgent agent, ElectricVehicle ev, ChargingSlot slot, AlreadyCharging alreadyCharging) {

		// create a charging process
		ChargingProcess process = createChargingProcess(agent, slot, null, false);
		process.isOvernight = alreadyCharging == AlreadyCharging.OVERNIGHT;
		process.isWholeDay = alreadyCharging == AlreadyCharging.WHOLE_DAY;

		var mobsimVehicle = getOrCreateMobsimVehicle(agent, ev.getId());
		var chargerLinkId = slot.charger().getLink().getId();
		mobsimVehicle.setCurrentLinkId(chargerLinkId);
		// when things happen here, park the vehicle and put the process into the bookkeeping
		if (partitionTransfer.isLocal(chargerLinkId)) {
			var simLink = simNetwork.getLinks().get(chargerLinkId);
			parkedVehicles.park(mobsimVehicle, simLink);
			personsToProcess.put(agent.getId(), process);
			vehiclesToProcess.put(process.vehicleId, process);
			submitToCharger(process);
		} else {
			// send charging process and vehicle to other partition
			// Will this work? I guess the vehicle will only arrive one second after we schedule its sending.
			// However, this method should usually be called when agents are put into activity engines before the simulation
			// starts.
			var msg = new OvernightChargingMessage(mobsimVehicle, chargerLinkId, process);
			var toPartition = partitionTransfer.getPartitionIndex(chargerLinkId);
			internalInterface.notifyVehicleLeavesPartition(mobsimVehicle, toPartition);
			partitionTransfer.collect(msg, chargerLinkId);
		}
	}

	private DistributedMobsimVehicle getOrCreateMobsimVehicle(MobsimAgent agent, Id<Vehicle> vehicleId) {
		// usually, a vehicle is used at the first link of a plan. This will break if anything else is done.
		var firstVehicleLeg = ((PlanAgent) agent).getCurrentPlan().getPlanElements().stream()
			.filter(e -> e instanceof Leg)
			.map(e -> (Leg) e)
			.filter(e -> e.getMode().equals(chargingMode))
			.findFirst();

		if (firstVehicleLeg.isPresent()) {
			var startLinkId = firstVehicleLeg.get().getRoute().getStartLinkId();
			// try to retrieve a vehicle from that link
			return parkedVehicles.unpark(vehicleId, startLinkId);
		} else {
			Vehicle vehicleData = vehicles.getVehicles().get(vehicleId);
			// SAFETY: This code is intended to be use with DSim, which operates on such vehicles.
			return (DistributedMobsimVehicle) qVehicleFactory.createQVehicle(vehicleData);
		}
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

	@Override
	public double priority() {
		return 100.;
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = new InternalInterfaceDelegate(internalInterface);
		this.delegateEngine.setInternalInterface(this.internalInterface);
	}

	@Override
	public void doSimStep(double time) {

		// check queued vehicles
		for (var id : queuedVehicles) {
			var process = vehiclesToProcess.get(id);
			if (process.latestPlugTime >= time) continue;

			var charger = getLiveCharger(process.currentSlot);
			var ev = electricFleet.getVehicle(process.vehicleId);
			var agent = personsAtCharger.get(process.personId);

			charger.getLogic().removeVehicle(ev, time);
			em.processEvent(new AbortChargingAttemptEvent(time, process.personId, process.vehicleId));

			if (process.isWholeDay || process.isOvernight) {
				// notify that we abort the charging process. Mark the process as aborted so that the
				// agent can figure out what to do once it arrives for the unplug activity
				em.processEvent(
					new AbortChargingProcessEvent(time, process.personId, process.vehicleId));
				process.isAborted = true;
			} else {
				var pa = (PlanAgent) agent;
				final var plugActivity = (Activity) pa.getCurrentPlanElement();
				var plan = pa.getCurrentPlan();

				alternativeProvider.findAlternativeAsync(
					time, plan.getPerson(), plan, ev, process.initialSlot, process.trace, optAlternative -> {
						if (optAlternative.isPresent()) {
							var alternative = optAlternative.get();
							// found an alternative charger
							if (process.currentSlot.isLegBased() && !alternative.isLegBased()) {
								throw new IllegalStateException(
									"Cannot switch from a leg-based charging slot to an activity-based alternative because activities are not known");
							}

							// end current plug activity
							plugActivity.setEndTime(time);
							WithinDayAgentUtils.resetCaches(agent);
							delegateEngine.rescheduleActivityEnd(agent);

							// drive to the next charger and schedule a plug activity
							var alternativeCharger = getLiveCharger(alternative.charger());
							var nextPlugActivity = chargingScheduler.scheduleSubsequentPlugActivity(agent,
								plugActivity, alternativeCharger, time);
							nextPlugActivity.getAttributes().putAttribute(CHARGING_PROCESS_ATTRIBUTE, process);

							// reset process for next attempt
							process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
								process.currentSlot.endActivity(),
								process.currentSlot.leg(), alternative.duration(),
								alternativeCharger);
							process.isSubmitted = false;
							process.isPlugged = false;
							process.isQueued = false;
							process.attemptIndex++;
							process.trace.add(alternative);

							// send event for scoring
							em.processEvent(
								new StartChargingAttemptEvent(time, process.personId, process.vehicleId,
									alternative.charger(), process.attemptIndex, process.processIndex,
									alternative.isLegBased(), false, alternative.duration()));
						} else {
							// send event for scoring
							em.processEvent(
								new AbortChargingProcessEvent(time, process.personId, process.vehicleId));

							if (performAbort) {
								// we abort the agent
								plugActivity.setEndTime(Double.POSITIVE_INFINITY);
								WithinDayAgentUtils.resetCaches(agent);
								delegateEngine.rescheduleActivityEnd(agent);

								agent.setStateToAbort(time);
								internalInterface.arrangeNextAgentState(agent);
							} else {
								// end current plug activity
								plugActivity.setEndTime(time);
								WithinDayAgentUtils.resetCaches(agent);
								delegateEngine.rescheduleActivityEnd(agent);

								chargingScheduler.scheduleDriveToNextActivity(agent);
							}
						}

						// make sure the bookkeeping is cleared
						personsToProcess.remove(process.personId);
						vehiclesToProcess.remove(process.vehicleId);
					});
			}
		}

		delegateEngine.doSimStep(time);
	}

	@Override
	public Map<Class<? extends Message>, DistributedMobsimEngine.MessageHandler> getMessageHandlers() {
		return Map.of(
			OvernightChargingMessage.class, this::handleOvernightChargingMessage
		);
	}

	private void handleOvernightChargingMessage(List<Message> messages, double now) {
		for (var m : messages) {
			var ocm = (OvernightChargingMessage) m;
			parkedVehicles.park(ocm.vehicle, simNetwork.getLinks().get(ocm.startLinkId));
			internalInterface.notifyVehicleEntersPartition(ocm.vehicle);
			personsToProcess.put(ocm.process.personId, ocm.process);
			vehiclesToProcess.put(ocm.process.vehicleId, ocm.process);
			submitToCharger(ocm.process);
		}
	}

	@Override
	public void onAgentLeavesPartition(DistributedMobsimAgent agent, int toPartition) {
		var process = personsToProcess.remove(agent.getId());
		if (process == null) return;

		var msg = new ChargingProcessMessage(
			process.personId,
			process.vehicleId,
			process.attemptIndex,
			process.latestPlugTime,
			process.initialSlot,
			process.currentSlot,
			process.isSubmitted,
			process.isQueued,
			process.isPlugged,
			process.isOvernight,
			process.isWholeDay
		);
		partitionTransfer.collect(msg, toPartition);
	}

	record ChargingProcessMessage(
		Id<Person> personId,
		Id<Vehicle> evId,

		int attemptIndex,

		double latestPlugTime,
		ChargingSlot initialSlot,
		ChargingSlot currentSlot,

		boolean isSubmitted,
		boolean isQueued,
		boolean isPlugged,

		boolean isOvernight,
		boolean isWholeDay
	) implements Message {}

	@Override
	public void onAgentEntersPartition(DistributedMobsimAgent agent) {
		checkForFailedOvernightCharge(agent);
	}

	public void checkForFailedOvernightCharge(MobsimAgent agent) {

		var process = personsToProcess.get(agent.getId());
		if (process != null && process.isAborted && process.isOvernight) {
			// SAFETY: We only store processes for plan agents
			var pa = (PlanAgent) agent;
			// SAFETY: Agents can only change partitions during legs. (I hope)
			var nextActivity = nextActivity(pa);
			if (nextActivity.getType().equals(UNPLUG_ACTIVITY_TYPE)) {
				var charger = getLiveCharger(process.currentSlot);
				chargingScheduler.replaceUnplugWithAccessAfterOvernightCharge(agent, nextActivity, charger);
			}
		}
	}

	private Activity nextActivity(PlanAgent pa) {
		var ce = pa.getCurrentPlanElement();
		var cei = pa.getCurrentPlan().getPlanElements().indexOf(ce);
		for (var e : pa.getCurrentPlan().getPlanElements().subList(cei + 1, pa.getCurrentPlan().getPlanElements().size())) {
			if (e instanceof Activity a) {
				return a;
			}
		}
		return null;
	}

	/**
	 * We store chargers in slots and other places. However, those can be stale instances of chargers on other partitions
	 *  have this helper to obtain a live instance from the infrastructure for this partition
	 */
	private Charger getLiveCharger(ChargingSlot slot) {
		return getLiveCharger(slot.charger().getId());
	}

	private Charger getLiveCharger(Id<Charger> chargerId) {
		return chargingInfrastructure.getChargers().get(chargerId);
	}

	private record PendingAlternative(Id<ChargingAlternative> id, MobsimAgent agent) {}


	private static class ChargingProcess {

		final Id<Person> personId;
		final Id<Vehicle> vehicleId;

		// search process
		int attemptIndex = 0;
		int processIndex;

		// plugging process
		double latestPlugTime;
		final double maxQueueTime;

		// charging slots
		final ChargingSlot initialSlot;
		ChargingSlot currentSlot;
		List<ChargingAlternative> trace = new LinkedList<>();

		// state variables trigger by events
		boolean isSubmitted = false;
		boolean isQueued = false;
		boolean isPlugged = false;
		boolean isPersonAtCharger = false;
		public boolean isAborted = false;

		// markers for special cases
		boolean isOvernight = false;
		boolean isWholeDay = false;

		private ChargingProcess(Id<Person> personId, Id<Vehicle> vehicleId, double maxQueueTime, ChargingSlot initialSlot) {
			this.personId = personId;
			this.vehicleId = vehicleId;
			this.maxQueueTime = maxQueueTime;
			this.initialSlot = initialSlot;
			this.currentSlot = initialSlot;
		}

		boolean isFirstAttempt() {return attemptIndex == 0;}
	}

	public record OvernightChargingMessage(DistributedMobsimVehicle vehicle, Id<Link> startLinkId, ChargingProcess process) implements Message {}

	private class InternalInterfaceDelegate implements InternalInterface {

		private final InternalInterface delegate;

		private InternalInterfaceDelegate(InternalInterface delegate) {this.delegate = delegate;}

		@Override
		public void arrangeNextAgentState(MobsimAgent agent) {

			// person is leaving the activity. However, the vehicle might still charge.
			personsAtCharger.remove(agent.getId());
			var process = personsToProcess.get(agent.getId());
			if (process != null) {
				process.isPersonAtCharger = false;
			}

			delegate.arrangeNextAgentState(agent);
		}

		@Override
		public void registerAdditionalAgentOnLink(MobsimAgent agent) {
			delegate.registerAdditionalAgentOnLink(agent);
		}

		@Override
		public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> agentId, Id<Link> linkId) {
			return delegate.unregisterAdditionalAgentOnLink(agentId, linkId);
		}

		@Override
		public Netsim getMobsim() {
			return delegate.getMobsim();
		}

		@Override
		public Collection<? extends DepartureHandler> getDepartureHandlers() {
			return delegate.getDepartureHandlers();
		}

		@Override
		public void notifyVehicleLeavesPartition(DistributedMobsimVehicle vehicle, int toPartition) {
			delegate.notifyVehicleLeavesPartition(vehicle, toPartition);
		}

		@Override
		public void notifyVehicleEntersPartition(DistributedMobsimVehicle vehicle) {
			delegate.notifyVehicleEntersPartition(vehicle);
		}
	}

	/**
	 * Determines whether an activity type is managed in a special way by within-day
	 * electric vehicle charging
	 */
	static public boolean isManagedActivityType(String activityType) {
		return activityType.equals(PLUG_ACTIVITY_TYPE) || activityType.equals(UNPLUG_ACTIVITY_TYPE)
			|| activityType.equals(WAIT_ACTIVITY_TYPE) || activityType.equals(ACCESS_ACTIVITY_TYPE);
	}
}
