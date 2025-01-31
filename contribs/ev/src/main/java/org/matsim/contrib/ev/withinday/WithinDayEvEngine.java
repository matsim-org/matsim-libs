package org.matsim.contrib.ev.withinday;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.charging.QueuedAtChargerEvent;
import org.matsim.contrib.ev.charging.QueuedAtChargerEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.withinday.events.AbortChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.AbortChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.FinishChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.FinishChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.StartChargingAttemptEvent;
import org.matsim.contrib.ev.withinday.events.StartChargingProcessEvent;
import org.matsim.contrib.ev.withinday.events.UpdateChargingAttemptEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkI;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.common.base.Preconditions;

/**
 * This engine is the core of the within-day electric vehicle charging package.
 * It mainly works with two interfaces, ChargingSlotProvider and
 * ChargingAlternativeProvider. In the beginning of the day, the
 * ChargingSlotProvider is called to integate planned charging activities into
 * the schedule of each viable agent. The ChargingAlternativeProvider is called
 * throghout the day, for instance, to change the charger when an agent notices
 * that a planned charger is blocked. The engine maanges the successive
 * adaptation of the plan to the actions intended by the agent (pluggin the car,
 * unpluggin the car, ...).
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class WithinDayEvEngine implements MobsimEngine, ActivityStartEventHandler, ChargingStartEventHandler,
		QueuedAtChargerEventHandler, PersonDepartureEventHandler, MobsimScopeEventHandler {
	static public final String ACTIVE_PERSON_ATTRIBUTE = "wevc:active";
	static public final String MAXIMUM_QUEUE_TIME_PERSON_ATTRIBUTE = "wevc:maximumQueueTime";

	static public final String PLUG_ACTIVITY_TYPE = "ev:plug interaction";
	static public final String UNPLUG_ACTIVITY_TYPE = "ev:unplug interaction";
	static public final String WAIT_ACTIVITY_TYPE = "ev:wait interaction";

	// used when charging at the first acitivty fails and the person needs to
	// recover the vehicle
	static public final String ACCESS_ACTIVITY_TYPE = "ev:access interaction";

	static public final String CHARGING_SLOT_ATTRIBUTE = "ev:chargingSlot";
	static public final String CHARGING_PROCESS_ATTRIBUTE = "ev:chargingProcess";
	static private final String INITIAL_ACTIVITY_END_TIME_ATTRIBUTE = "ev:initialActivityEndTime";

	private final String chargingMode;
	private final QSim qsim;

	private final Vehicles vehicles;
	private final QVehicleFactory qVehicleFactory;
	private final Scenario scenario;
	private final WithinDayChargingStrategy.Factory chargingStrategyFactory;

	private final ElectricFleet electricFleet;
	private final ChargingAlternativeProvider alternativeProvider;
	private final ChargingSlotProvider slotProvider;
	private final EventsManager eventsManager;
	private ChargingScheduler chargingScheduler;

	private final boolean performAbort;
	private final double maximumQueueWaitTime;
	private final boolean allowSpontaneousCharging;

	private final Logger logger = LogManager.getLogger(WithinDayEvEngine.class);

	public WithinDayEvEngine(WithinDayEvConfigGroup config, QSim qsim, ElectricFleet electricFleet,
			ChargingAlternativeProvider onlineSlotProvider, ChargingSlotProvider offlineSlotProvider,
			EventsManager eventsManager,
			ChargingScheduler chargingScheduler, Vehicles vehicles, QVehicleFactory qVehicleFactory,
			Scenario scenario, WithinDayChargingStrategy.Factory chargingStrategyFactory) {
		this.qsim = qsim;
		this.electricFleet = electricFleet;
		this.alternativeProvider = onlineSlotProvider;
		this.slotProvider = offlineSlotProvider;
		this.eventsManager = eventsManager;
		this.chargingScheduler = chargingScheduler;
		this.vehicles = vehicles;
		this.qVehicleFactory = qVehicleFactory;
		this.scenario = scenario;
		this.chargingStrategyFactory = chargingStrategyFactory;

		this.chargingMode = config.carMode;
		this.performAbort = config.abortAgents;
		this.maximumQueueWaitTime = config.maximumQueueTime;
		this.allowSpontaneousCharging = config.allowSpoantaneousCharging;
	}

	// INITIALIZATION

	private final IdSet<Person> relevantPersons = new IdSet<>(Person.class);
	private final IdSet<Vehicle> relevantVehicles = new IdSet<>(Vehicle.class);

	@Override
	public void onPrepareSim() {
		logger.info("Implementing charging slots ..");

		int activityBasedCount = 0;
		int legBasedCount = 0;
		int overnightCount = 0;
		int wholeDayCount = 0;

		for (MobsimAgent agent : qsim.getAgents().values()) {
			if (agent instanceof HasModifiablePlan) {
				Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

				// only active agents
				// only those agents that actually have a proper vehicle
				if (isActive(plan.getPerson()) && VehicleUtils.hasVehicleId(plan.getPerson(), chargingMode)) {
					relevantPersons.add(plan.getPerson().getId());

					Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(plan.getPerson(), chargingMode);
					ElectricVehicle vehicle = electricFleet.getElectricVehicles().get(vehicleId);
					relevantVehicles.add(vehicleId);

					Activity firstActivity = (Activity) plan.getPlanElements().get(0);
					Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
					Activity overnightActivity = findOvernightActivity(plan);

					ChargingSlot overnightSlot = null;
					ChargingSlot wholeDaySlot = null;
					boolean foundRegularSlot = false;

					List<ChargingSlot> slots = slotProvider.findSlots(plan.getPerson(), plan, vehicle);
					Collections.sort(slots, (first, second) -> {
						int firstIndex = plan.getPlanElements()
								.indexOf(first.isLegBased() ? first.leg() : first.startActivity());

						int secondIndex = plan.getPlanElements()
								.indexOf(second.isLegBased() ? second.leg() : second.startActivity());

						return Integer.compare(firstIndex, secondIndex);
					});

					for (ChargingSlot slot : slots) {
						if (slot.startActivity() == firstActivity && slot.endActivity() == lastActivity) {
							// special case: this is a slot spanning the whole plan
							Preconditions.checkState(slot.leg() == null);
							Preconditions.checkState(!foundRegularSlot);
							Preconditions.checkState(overnightSlot == null);
							Preconditions.checkState(wholeDaySlot == null);
							wholeDaySlot = slot;
							wholeDayCount++;
						} else if (slot.startActivity() == firstActivity && slot.endActivity() == overnightActivity) {
							// special case: this slot has started on the "previous day" and the vehicle
							// only needs to be unplugged after the overnight activity. In order to simplify
							// time calculation for scheduling, we treat this slot last
							Preconditions.checkState(slot.leg() == null);
							Preconditions.checkState(overnightSlot == null);
							Preconditions.checkState(wholeDaySlot == null);
							overnightSlot = slot;
							overnightCount++;
						} else if (slot.startActivity() != null && slot.endActivity() != null) {
							// standard case: schedule a plug activity along the plan
							Preconditions.checkState(slot.leg() == null);
							Preconditions.checkState(wholeDaySlot == null);
							Activity plugActivity = chargingScheduler.scheduleInitialPlugActivity(agent,
									slot.startActivity(), slot.charger());
							plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);
							activityBasedCount++;
						} else {
							// leg case: schedule a plug activity along a leg
							Preconditions.checkState(slot.startActivity() == null);
							Preconditions.checkState(slot.endActivity() == null);
							Preconditions.checkState(slot.leg() != null);

							Activity plugActivity = chargingScheduler.scheduleOnroutePlugActivity(agent, slot.leg(),
									slot.charger());
							plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);
							legBasedCount++;
						}
					}

					if (overnightSlot != null) {
						startOvernightCharging(agent, overnightSlot);
						updateInitialVehicleLocation(plan, vehicleId, overnightSlot);
					}

					if (wholeDaySlot != null) {
						startWholeDayCharging(agent, wholeDaySlot);
						updateInitialVehicleLocation(plan, vehicleId, wholeDaySlot);
					}
				}
			}
		}

		logger.info(String.format("  activity: %d, leg: %d, overnight: %d, whole day: %d", activityBasedCount,
				legBasedCount, overnightCount, wholeDayCount));
	}

	private Activity findOvernightActivity(Plan plan) {
		for (Trip trip : TripStructureUtils.getTrips(plan)) {
			String mode = TripStructureUtils.getRoutingModeIdentifier().identifyMainMode(trip.getTripElements());

			if (mode.equals(chargingMode)) {
				Id<Link> originLinkId = PopulationUtils.decideOnLinkIdForActivity(trip.getOriginActivity(), scenario);
				Id<Link> destinationLinkId = PopulationUtils.decideOnLinkIdForActivity(trip.getDestinationActivity(),
						scenario);

				if (!originLinkId.equals(destinationLinkId)) {
					return trip.getOriginActivity();
				}
			}
		}

		return null;
	}

	private void updateInitialVehicleLocation(Plan plan, Id<Vehicle> vehicleId, ChargingSlot slot) {
		MobsimVehicle vehicle = qsim.getVehicles().get(vehicleId);

		if (vehicle == null) {
			Vehicle vehicleData = vehicles.getVehicles().get(vehicleId);
			vehicle = qVehicleFactory.createQVehicle(vehicleData);
			qsim.addParkedVehicle(vehicle, slot.charger().getLink().getId());
		}

		Id<Link> initialLinkId = vehicle.getCurrentLink().getId();

		QLinkI originalLink = (QLinkI) qsim.getNetsimNetwork().getNetsimLink(initialLinkId);
		QVehicle qVehicle = originalLink.removeParkedVehicle(vehicleId);
		Preconditions.checkNotNull(qVehicle);

		QLinkI updatedLink = (QLinkI) qsim.getNetsimNetwork().getNetsimLink(slot.charger().getLink().getId());
		updatedLink.addParkedVehicle(qVehicle);
	}

	private void startOvernightCharging(MobsimAgent agent, ChargingSlot slot) {
		Activity endActivity = slot.endActivity();
		endActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);

		double now = internalInterface.getMobsim().getSimTimer().getSimStartTime();
		ChargingProcess process = createChargingProcess(agent.getId(), now, slot, null, false);
		process.isOvernight = true;

		// Handle end time from here

		Preconditions.checkState(endActivity.getEndTime().isDefined());
		double endTime = endActivity.getEndTime().seconds();

		endActivity.getAttributes().putAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE, endTime);
		endActivity.setEndTime(Double.MAX_VALUE);

		WithinDayAgentUtils.resetCaches(agent);

		/*
		 * We would usually include the following line, but we must not call it here
		 * because it is called automatically by the QSim at simulation startup.
		 * Otherwise, the agent will appear twice in the agent queue:
		 * 
		 * WithinDayAgentUtils.rescheduleActivityEnd(agent, qsim);
		 */

		plugging.put(process.vehicle.getId(), process);
	}

	private void startWholeDayCharging(MobsimAgent agent, ChargingSlot slot) {
		double now = internalInterface.getMobsim().getSimTimer().getSimStartTime();
		ChargingProcess process = createChargingProcess(agent.getId(), now, slot, null, false);
		process.isWholeDay = true;

		plugging.put(process.vehicle.getId(), process);
	}

	// EVENT COLLECTION

	private final List<PersonDepartureEvent> personDepartureEvents = new LinkedList<>();
	private final List<ActivityStartEvent> activityStartEvents = new LinkedList<>();
	private final List<QueuedAtChargerEvent> queuedAtChargerEvents = new LinkedList<>();
	private final List<ChargingStartEvent> chargingStartEvents = new LinkedList<>();

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(chargingMode) && relevantPersons.contains(event.getPersonId())) {
			synchronized (personDepartureEvents) {
				personDepartureEvents.add(event);
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals(PLUG_ACTIVITY_TYPE) || event.getActType().equals(UNPLUG_ACTIVITY_TYPE)) {
			synchronized (activityStartEvents) {
				activityStartEvents.add(event);
			}
		}
	}

	@Override
	public void handleEvent(QueuedAtChargerEvent event) {
		if (relevantVehicles.contains(event.getVehicleId())) {
			synchronized (queuedAtChargerEvents) {
				queuedAtChargerEvents.add(event);
			}
		}
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		if (relevantVehicles.contains(event.getVehicleId())) {
			synchronized (chargingStartEvents) {
				chargingStartEvents.add(event);
			}
		}
	}

	// MANAGING CHARGING PROCESSES

	private class ChargingProcess {
		MobsimAgent agent;
		ElectricVehicle vehicle;

		// search process
		boolean isFirstAttempt = true;
		int attemptIndex = 0;
		int processIndex;

		// plugging process
		double latestPlugTime;

		// charging slots
		ChargingSlot initialSlot;
		ChargingSlot currentSlot;
		List<ChargingAlternative> trace = new LinkedList<>();

		// state variables trigger by events
		boolean isSubmitted = false;
		boolean isQueued = false;
		boolean isPlugged = false;

		// markers for special cases
		boolean isOvernight = false;
		boolean isWholeDay = false;
	}

	private ChargingProcess createChargingProcessFromPlugActivity(Id<Person> personId, double now) {
		MobsimAgent agent = qsim.getAgents().get(personId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		int plugActivityIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Activity plugActivity = (Activity) plan.getPlanElements().get(plugActivityIndex);

		return createChargingProcessFromPlugActivity(personId, now, plugActivity, false);
	}

	private ChargingProcess createChargingProcessFromLeg(Id<Person> personId, double now) {
		MobsimAgent agent = qsim.getAgents().get(personId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		int legIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		Leg leg = (Leg) plan.getPlanElements().get(legIndex);
		Preconditions.checkState(leg.getMode().equals(chargingMode));

		Activity plugActivity = findFollowingPlugActivity(agent, plan);
		if (plugActivity == null) {
			// can only happening when creating charging process from a leg
			return null;
		}

		return createChargingProcessFromPlugActivity(personId, now, plugActivity, false);
	}

	private ChargingProcess createChargingProcessFromPlugActivity(Id<Person> personId, double now,
			Activity plugActivity,
			boolean isSpontaneous) {
		Preconditions.checkState(plugActivity.getType().equals(PLUG_ACTIVITY_TYPE));

		ChargingProcess chargingProcess = (ChargingProcess) plugActivity.getAttributes()
				.getAttribute(CHARGING_PROCESS_ATTRIBUTE);
		if (chargingProcess != null) {
			// we are continuing an ongoing search process
			chargingProcess.isFirstAttempt = false;
			return chargingProcess;
		}

		ChargingSlot slot = (ChargingSlot) plugActivity.getAttributes().getAttribute(CHARGING_SLOT_ATTRIBUTE);
		return createChargingProcess(personId, now, slot, plugActivity, isSpontaneous);
	}

	private final IdMap<Person, Integer> chargingProcessIndex = new IdMap<>(Person.class);

	private ChargingProcess createChargingProcess(Id<Person> personId, double now, ChargingSlot slot,
			Activity plugActivity,
			boolean isSpontaneous) {
		MobsimAgent agent = qsim.getAgents().get(personId);
		Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);

		Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(plan.getPerson(), chargingMode);
		ElectricVehicle vehicle = electricFleet.getElectricVehicles().get(vehicleId);

		ChargingProcess process = new ChargingProcess();
		process.processIndex = chargingProcessIndex.compute(personId, (id, value) -> {
			return value == null ? 0 : value + 1;
		});
		process.agent = agent;
		process.vehicle = vehicle;
		process.currentSlot = slot;
		process.initialSlot = slot;

		eventsManager.processEvent(new StartChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId(),
				process.processIndex));
		eventsManager.processEvent(
				new StartChargingAttemptEvent(now, personId, vehicleId, process.currentSlot.charger().getId(),
						process.attemptIndex, process.processIndex, process.currentSlot.isLegBased(), isSpontaneous,
						process.currentSlot.duration()));

		if (plugActivity != null) {
			plugActivity.getAttributes().putAttribute(CHARGING_PROCESS_ATTRIBUTE, process);
		}

		return process;
	}

	private Activity findFollowingPlugActivity(MobsimAgent agent, Plan plan) {
		int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		PlanElement currentElement = plan.getPlanElements().get(currentIndex);
		Preconditions.checkState(currentElement instanceof Leg);

		for (int k = currentIndex + 1; k < plan.getPlanElements().size(); k++) {
			PlanElement element = plan.getPlanElements().get(k);

			if (element instanceof Activity activity) {
				if (!TripStructureUtils.isStageActivityType(activity.getType())
						|| isManagedActivityType(activity.getType())) {
					if (activity.getType().equals(PLUG_ACTIVITY_TYPE)) {
						return activity;
					} else {
						return null; // there is no plug activity between here and next main activity
					}
				}
			}
		}

		return null;
	}

	// ENGINE LOGIC

	@Override
	public void doSimStep(double time) {
		// first process collected events
		processPersonDepartureEvents(time);
		processActivityStartEvents(time);
		processQueuedAtChargerEvents(time);
		processChargingStartEvents(time);

		// next advance logic
		processApproachingProcesses(time);
		processPluggingProcesses(time);
		processUnpluggingProcesses(time);
	}

	private IdSet<Person> approaching = new IdSet<>(Person.class);
	private IdMap<Vehicle, ChargingProcess> plugging = new IdMap<>(Vehicle.class);
	private IdMap<Person, ChargingProcess> active = new IdMap<>(Person.class);
	private IdMap<Vehicle, ChargingProcess> unplugging = new IdMap<>(Vehicle.class);

	private void processPersonDepartureEvents(double now) {
		synchronized (personDepartureEvents) {
			var iterator = personDepartureEvents.iterator();

			while (iterator.hasNext()) {
				PersonDepartureEvent event = iterator.next();

				if (event.getTime() < now) {
					iterator.remove();
					approaching.add(event.getPersonId());
				}
			}
		}
	}

	private void processActivityStartEvents(double now) {
		synchronized (activityStartEvents) {
			var iterator = activityStartEvents.iterator();

			while (iterator.hasNext()) {
				ActivityStartEvent event = iterator.next();

				if (event.getTime() < now) {
					iterator.remove();

					if (event.getActType().equals(PLUG_ACTIVITY_TYPE)) {
						ChargingProcess process = createChargingProcessFromPlugActivity(event.getPersonId(), now);
						plugging.put(process.vehicle.getId(), process);
					} else if (event.getActType().equals(UNPLUG_ACTIVITY_TYPE)) {
						ChargingProcess process = active.remove(event.getPersonId());
						Preconditions.checkNotNull(process);
						unplugging.put(process.vehicle.getId(), process);
					}
				}
			}
		}
	}

	private void processQueuedAtChargerEvents(double now) {
		synchronized (queuedAtChargerEvents) {
			var iterator = queuedAtChargerEvents.iterator();

			while (iterator.hasNext()) {
				QueuedAtChargerEvent event = iterator.next();

				if (event.getTime() < now - 1.0) { // -1.0 because event is generated in afterSimStepListener
					iterator.remove();

					ChargingProcess process = plugging.get(event.getVehicleId());

					if (process != null) {
						process.isQueued = true;
					}
				}
			}
		}
	}

	private void processChargingStartEvents(double now) {
		synchronized (chargingStartEvents) {
			var iterator = chargingStartEvents.iterator();

			while (iterator.hasNext()) {
				ChargingStartEvent event = iterator.next();

				if (event.getTime() < now - 1.0) { // -1.0 because event is generated in afterSimStepListener
					iterator.remove();

					ChargingProcess process = plugging.get(event.getVehicleId());

					if (process != null) {
						process.isPlugged = true;
					}
				}
			}
		}
	}

	private void processApproachingProcesses(double time) {
		for (Id<Person> personId : approaching) {
			MobsimAgent agent = qsim.getAgents().get(personId);
			Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
			int currentIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

			if (plan.getPlanElements().get(currentIndex) instanceof Leg leg) {
				if (leg.getMode().equals(chargingMode)) {
					ChargingProcess process = createChargingProcessFromLeg(personId, time);

					if (process != null && process.isFirstAttempt) {
						// a plug activity was found and we may implement an alternative proposal

						ChargingAlternative alternative = alternativeProvider.findEnrouteAlternative(time,
								plan.getPerson(),
								plan,
								process.vehicle, process.currentSlot);

						if (alternative != null) {
							if (process.currentSlot.isLegBased() && !alternative.isLegBased()) {
								throw new IllegalStateException(
										"Cannot switch from a leg-based charging slot to an activity-based alternative because activities are not known");
							}

							if (alternative.charger() != process.currentSlot.charger()) {
								Activity followingPlugActivity = findFollowingPlugActivity(agent, plan);

								// drive to different charger and schedule a plug activity
								Activity plugActivity = chargingScheduler.changePlugActivity(process.agent,
										followingPlugActivity, alternative.charger(),
										time);
								plugActivity.getAttributes().putAttribute(CHARGING_PROCESS_ATTRIBUTE, process);

								// update slot
								process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
										process.currentSlot.endActivity(),
										process.currentSlot.leg(), alternative.duration(),
										alternative.charger());

								// send event for scoring
								eventsManager.processEvent(new UpdateChargingAttemptEvent(time, process.agent.getId(),
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
								eventsManager.processEvent(new UpdateChargingAttemptEvent(time, process.agent.getId(),
										process.vehicle.getId(), alternative.charger().getId(),
										alternative.isLegBased(), alternative.duration()));
							}
						}
					} else if (process == null && allowSpontaneousCharging) {
						// no upcoming plug activity is found, this is a completely spantaneous charging
						// attempt

						Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(plan.getPerson(), chargingMode);
						ElectricVehicle vehicle = electricFleet.getElectricVehicles().get(vehicleId);

						ChargingAlternative alternative = alternativeProvider.findEnrouteAlternative(time,
								plan.getPerson(), plan, vehicle, null);

						if (alternative != null) {
							ChargingSlot slot = new ChargingSlot(leg, alternative.duration(),
									alternative.charger());

							Activity plugActivity = chargingScheduler.insertPlugActivity(agent,
									alternative.charger(), time);
							plugActivity.getAttributes().putAttribute(CHARGING_SLOT_ATTRIBUTE, slot);

							createChargingProcessFromPlugActivity(personId, time, plugActivity, true);
						}
					}
				}
			}
		}

		approaching.clear();
	}

	private void processPluggingProcesses(double now) {
		Iterator<ChargingProcess> iterator = plugging.values().iterator();

		while (iterator.hasNext()) {
			ChargingProcess process = iterator.next();

			if (!process.isSubmitted) {
				Double personMaximumQueueWaitTime = getMaximumQueueTime(
						((HasModifiablePlan) process.agent).getModifiablePlan()
								.getPerson());

				if (personMaximumQueueWaitTime == null) {
					personMaximumQueueWaitTime = maximumQueueWaitTime;
				}

				// add vehicle to charger, it will be either queued or plugged
				ChargingStrategy chargingStrategy = chargingStrategyFactory
						.createStrategy(process.currentSlot.charger().getSpecification(), process.vehicle);
				process.currentSlot.charger().getLogic().addVehicle(process.vehicle, chargingStrategy, now);
				process.latestPlugTime = now + personMaximumQueueWaitTime;
				process.isSubmitted = true;
			} else if (process.isPlugged) {
				// vehicle has been plugged -> continue to the main activity

				if (process.isWholeDay) {
					// do nothing, vehicle stays plugged the whole day
				} else if (process.isOvernight) {
					// vehicle was plugged overnight, reset end time of the first activity after
					// which the vehilce will be picked up and schedule the pickup walk
					double plannedEndTime = (Double) process.currentSlot.endActivity().getAttributes()
							.getAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE);
					process.currentSlot.endActivity().setEndTime(Math.max(now, plannedEndTime));

					// following only necessary if this is the very first activity of the day
					WithinDayAgentUtils.resetCaches(process.agent);
					WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

					chargingScheduler.scheduleUnplugActivityAfterOvernightCharge(process.agent,
							process.currentSlot.endActivity(), process.currentSlot.charger());
				} else {
					// stadard case, we are in a plug activity, need to end it, and let agent to to
					// main activity
					Activity plugActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(process.agent);
					Preconditions.checkState(plugActivity.getType().equals(PLUG_ACTIVITY_TYPE));

					// end activity
					plugActivity.setEndTime(now);
					WithinDayAgentUtils.resetCaches(process.agent);
					WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

					if (process.currentSlot.isLegBased()) {
						// schedule unplug at the charger then continue to main activity
						chargingScheduler.scheduleUnplugActivityAtCharger(process.agent,
								process.currentSlot.duration());
					} else {
						// walk to main activity, perform it, walk back to charger and unplug
						chargingScheduler.scheduleUntilUnplugActivity(process.agent,
								process.currentSlot.startActivity(),
								process.currentSlot.endActivity());
					}
				}

				active.put(process.agent.getId(), process);
				iterator.remove();
			} else if (process.isQueued) {
				if (now > process.latestPlugTime) {
					// remove vehicle from charger
					process.currentSlot.charger().getLogic().removeVehicle(process.vehicle, now);

					// remove from plugging processes
					iterator.remove();

					eventsManager
							.processEvent(
									new AbortChargingAttemptEvent(now, process.agent.getId(), process.vehicle.getId()));

					if (process.isWholeDay || process.isOvernight) {
						// did not succeed charging overnight
						// agent may be in any potential state along the plan

						// send event for scoring
						eventsManager.processEvent(
								new AbortChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

						if (performAbort) {
							// abort the agent
							process.currentSlot.endActivity().setEndTime(Double.POSITIVE_INFINITY);
							WithinDayAgentUtils.resetCaches(process.agent);
							WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

							process.agent.setStateToAbort(now);
							internalInterface.arrangeNextAgentState(process.agent);
						} else if (process.isOvernight) {
							Activity endActivity = process.currentSlot.endActivity();
							double initialEndTime = (Double) endActivity.getAttributes()
									.getAttribute(INITIAL_ACTIVITY_END_TIME_ATTRIBUTE);

							// end current plug activity
							endActivity.setEndTime(Math.max(now, initialEndTime));
							WithinDayAgentUtils.resetCaches(process.agent);
							WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

							chargingScheduler.scheduleAccessAfterOvernightCharge(process.agent,
									process.currentSlot.endActivity(),
									process.currentSlot.charger());
						}
					} else {
						// stadnard case
						Activity plugActivity = (Activity) WithinDayAgentUtils
								.getCurrentPlanElement(process.agent);
						Preconditions.checkState(plugActivity.getType().equals(PLUG_ACTIVITY_TYPE));

						// reset charging process
						process.attemptIndex++;

						// try to find next charger
						Plan plan = WithinDayAgentUtils.getModifiablePlan(process.agent);
						ChargingAlternative alternative = alternativeProvider.findAlternative(now,
								plan.getPerson(),
								plan,
								process.vehicle, process.initialSlot, process.trace);

						if (alternative != null) {
							// found an alternative charger
							if (process.currentSlot.isLegBased() && !alternative.isLegBased()) {
								throw new IllegalStateException(
										"Cannot switch from a leg-based charging slot to an activity-based alternative because activities are not known");
							}

							// end current plug activity
							plugActivity.setEndTime(now);
							WithinDayAgentUtils.resetCaches(process.agent);
							WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

							// drive to the next charger and schedule a plug activity
							plugActivity = chargingScheduler.scheduleSubsequentPlugActivity(process.agent,
									plugActivity, alternative.charger(), now);
							plugActivity.getAttributes().putAttribute(CHARGING_PROCESS_ATTRIBUTE, process);

							// reset process for next attempt
							process.currentSlot = new ChargingSlot(process.currentSlot.startActivity(),
									process.currentSlot.endActivity(),
									process.currentSlot.leg(), alternative.duration(),
									alternative.charger());
							process.isSubmitted = false;
							process.isPlugged = false;
							process.isQueued = false;
							process.trace.add(alternative);

							// send event for scoring
							eventsManager.processEvent(
									new StartChargingAttemptEvent(now, process.agent.getId(), process.vehicle.getId(),
											alternative.charger().getId(), process.attemptIndex, process.processIndex,
											alternative.isLegBased(), false, alternative.duration()));
						} else {
							// send event for scoring
							eventsManager.processEvent(
									new AbortChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

							if (performAbort) {
								// we abort the agent
								plugActivity.setEndTime(Double.POSITIVE_INFINITY);
								WithinDayAgentUtils.resetCaches(process.agent);
								WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

								process.agent.setStateToAbort(now);
								internalInterface.arrangeNextAgentState(process.agent);
							} else {
								// end current plug activity
								plugActivity.setEndTime(now);
								WithinDayAgentUtils.resetCaches(process.agent);
								WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

								chargingScheduler.scheduleDriveToNextActivity(process.agent);
							}
						}
					}
				}
			} // else: we are waiting to be queued or plugged, don't do anything
		}

	}

	private void processUnpluggingProcesses(double now) {
		Iterator<ChargingProcess> iterator = unplugging.values().iterator();

		while (iterator.hasNext()) {
			ChargingProcess process = iterator.next();

			// remove vehicle from charger, but may already be done
			if (process.currentSlot.charger().getLogic().getPluggedVehicles().stream()
					.anyMatch(candiate -> process.vehicle.getId().equals(candiate.ev().getId()))) {
				process.currentSlot.charger().getLogic().removeVehicle(process.vehicle, now);
			} else {
				logger.warn(String.format(
						"Agent %s tried to unplug vehicle %s at charger %s, but was already unplugged. Is the correct ChargingStrategy configured?",
						process.agent.getId().toString(), process.vehicle.getId().toString(),
						process.currentSlot.charger().getId().toString()));
			}

			Activity unplugActivity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(process.agent);
			Preconditions.checkState(unplugActivity.getType().equals(UNPLUG_ACTIVITY_TYPE));

			unplugActivity.setEndTime(now);
			WithinDayAgentUtils.resetCaches(process.agent);
			WithinDayAgentUtils.rescheduleActivityEnd(process.agent, qsim);

			chargingScheduler.scheduleDriveToNextActivity(process.agent);

			eventsManager
					.processEvent(new FinishChargingAttemptEvent(now, process.agent.getId(), process.vehicle.getId()));
			eventsManager
					.processEvent(new FinishChargingProcessEvent(now, process.agent.getId(), process.vehicle.getId()));

			iterator.remove();
		}
	}

	// BOILERPLATE

	@Override
	public void afterSim() {
	}

	private InternalInterface internalInterface;

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	/**
	 * Checks whether a person is managed by within-day electric vehicle charging
	 */
	static public boolean isActive(Person person) {
		Boolean isActive = (Boolean) person.getAttributes().getAttribute(ACTIVE_PERSON_ATTRIBUTE);
		return isActive != null && isActive;
	}

	/**
	 * Sets a person to be active in within-day electric vehicle charging or not
	 */
	static public void setActive(Person person, boolean isActive) {
		person.getAttributes().putAttribute(ACTIVE_PERSON_ATTRIBUTE, isActive);
	}

	/**
	 * Activates a person for within-day electric vehicle charging
	 */
	static public void activate(Person person) {
		setActive(person, true);
	}

	/**
	 * Retrieves the maximum queue time for a person before an attempt is aborted
	 */
	static public Double getMaximumQueueTime(Person person) {
		return (Double) person.getAttributes().getAttribute(MAXIMUM_QUEUE_TIME_PERSON_ATTRIBUTE);
	}

	/**
	 * Sets the maximum queue time for a person before an attempt is aborted
	 */
	static public void setMaximumQueueTime(Person person, double maximumQueueTime) {
		person.getAttributes().putAttribute(MAXIMUM_QUEUE_TIME_PERSON_ATTRIBUTE, maximumQueueTime);
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
