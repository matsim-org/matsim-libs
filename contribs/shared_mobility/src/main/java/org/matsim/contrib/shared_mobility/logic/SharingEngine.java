package org.matsim.contrib.shared_mobility.logic;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingUtils;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.contrib.shared_mobility.service.events.SharingVehicleEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

public class SharingEngine implements ActivityStartEventHandler, MobsimEngine, MobsimBeforeSimStepListener {
	private final SharingService service;
	private final SharingLogic logic;

	private final EventsManager eventsManager;
	private InternalInterface internalInterface;

	private boolean vehiclesPlaced = false;

	private final List<MobsimAgent> bookingAgents = new LinkedList<>();
	private final List<MobsimAgent> pickupAgents = new LinkedList<>();
	private final List<MobsimAgent> dropoffAgents = new LinkedList<>();

	private final List<MobsimAgent> processBookingAgents = new LinkedList<>();
	private final List<MobsimAgent> processPickupAgents = new LinkedList<>();
	private final List<MobsimAgent> processDropoffAgents = new LinkedList<>();

	public SharingEngine(SharingService service, SharingLogic logic, EventsManager eventsManager) {
		this.service = service;
		this.logic = logic;
		this.eventsManager = eventsManager;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		// make sure to handle only those rentals that belong to this SharingService		
		
		if (event.getActType().equals(SharingUtils.BOOKING_ACTIVITY)) {
			
			MobsimAgent agent = internalInterface.getMobsim().getAgents().get(event.getPersonId());

			Activity activity = (Activity)((PlanAgent)agent).getCurrentPlanElement();
			if (activity.getAttributes().getAttribute(SharingUtils.SERVICE_ID_ATTRIBUTE) != null &&
					activity.getAttributes().getAttribute(SharingUtils.SERVICE_ID_ATTRIBUTE).equals(this.service.getId().toString()))
				bookingAgents.add(agent);
		} else if (event.getActType().equals(SharingUtils.PICKUP_ACTIVITY)) {
			MobsimAgent agent = internalInterface.getMobsim().getAgents().get(event.getPersonId());
			Activity activity = (Activity)((PlanAgent)agent).getCurrentPlanElement();
			if (activity.getAttributes().getAttribute(SharingUtils.SERVICE_ID_ATTRIBUTE) != null &&
					activity.getAttributes().getAttribute(SharingUtils.SERVICE_ID_ATTRIBUTE).equals(this.service.getId().toString()))
				pickupAgents.add(agent);
		} else if (event.getActType().equals(SharingUtils.DROPOFF_ACTIVITY)) {
			MobsimAgent agent = internalInterface.getMobsim().getAgents().get(event.getPersonId());
			Activity activity = (Activity)((PlanAgent)agent).getCurrentPlanElement();
			if (activity.getAttributes().getAttribute(SharingUtils.SERVICE_ID_ATTRIBUTE) != null &&
					activity.getAttributes().getAttribute(SharingUtils.SERVICE_ID_ATTRIBUTE).equals(this.service.getId().toString()))
				dropoffAgents.add(agent);
		}
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		processPickupAgents.clear();
		processDropoffAgents.clear();
		processBookingAgents.clear();

		processBookingAgents.addAll(bookingAgents);
		processPickupAgents.addAll(pickupAgents);
		processDropoffAgents.addAll(dropoffAgents);

		bookingAgents.clear();
		pickupAgents.clear();
		dropoffAgents.clear();
	}

	@Override
	public void doSimStep(double time) {
		if (!vehiclesPlaced) {
			vehiclesPlaced = true;

			for (SharingVehicle vehicle : service.getVehicles()) {
				eventsManager.processEvent(new SharingVehicleEvent(time, service.getId(), vehicle.getLink().getId(),
						vehicle.getId(), Optional.empty()));

			}
		}

		List<MobsimAgent> stuckAgents = new LinkedList<>();
		
		for (MobsimAgent agent : processBookingAgents) {
			if (!logic.tryBookVehicle(time, agent)) {
				stuckAgents.add(agent);
			}
		}

		for (MobsimAgent agent : processPickupAgents) {
			Activity activity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(agent);

			if (service.getId().equals(SharingUtils.getServiceId(activity))) {
				if (!logic.tryPickupVehicle(time, agent)) {
					stuckAgents.add(agent);
				}
			}
		}

		for (MobsimAgent agent : processDropoffAgents) {
			Activity activity = (Activity) WithinDayAgentUtils.getCurrentPlanElement(agent);

			if (service.getId().equals(SharingUtils.getServiceId(activity))) {
				logic.tryDropoffVehicle(time, agent);
			}
		}

		stuckAgents.forEach(agent -> {
			((Activity) WithinDayAgentUtils.getCurrentPlanElement(agent)).setEndTime(Double.POSITIVE_INFINITY);
			((Activity) WithinDayAgentUtils.getCurrentPlanElement(agent)).setMaximumDurationUndefined();
			((HasModifiablePlan) agent).resetCaches();
			internalInterface.getMobsim().rescheduleActivityEnd(agent);

			eventsManager.processEvent(new PersonStuckEvent(time, agent.getId(), agent.getCurrentLinkId(),
					SharingUtils.getServiceMode(service.getId())));
			internalInterface.getMobsim().getAgentCounter().incLost();
		});

		stuckAgents.clear();
	}

	@Override
	public void onPrepareSim() {
		eventsManager.addHandler(this);
	}

	@Override
	public void afterSim() {
		eventsManager.removeHandler(this);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
