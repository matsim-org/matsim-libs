/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.dvrp.passenger;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class DefaultPassengerEngine implements PassengerEngine, PassengerRequestRejectedEventHandler {

	private final String mode;
	private final MobsimTimer mobsimTimer;

	private final PassengerRequestCreator requestCreator;
	private final VrpOptimizer optimizer;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private final InternalPassengerHandling internalPassengerHandling;

	private InternalInterface internalInterface;

	//accessed in doSimStep() and handleDeparture() (no need to sync)
	private final Map<Id<Request>, MobsimPassengerAgent> activePassengers = new HashMap<>();

	//accessed in doSimStep() and handleEvent() (potential data races)
	private final Queue<PassengerRequestRejectedEvent> rejectedRequestsEvents = new ConcurrentLinkedQueue<>();

	DefaultPassengerEngine(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer,
			PassengerRequestCreator requestCreator, VrpOptimizer optimizer, Network network,
			PassengerRequestValidator requestValidator) {
		this.mode = mode;
		this.mobsimTimer = mobsimTimer;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.network = network;
		this.requestValidator = requestValidator;

		internalPassengerHandling = new InternalPassengerHandling(mode, eventsManager);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		internalPassengerHandling.setInternalInterface(internalInterface);
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void doSimStep(double time) {
		while (!rejectedRequestsEvents.isEmpty()) {
			MobsimPassengerAgent passenger = activePassengers.remove(rejectedRequestsEvents.poll().getRequestId());
			//not much else can be done for immediate requests
			//set the passenger agent to abort - the event will be thrown by the QSim
			passenger.setStateToAbort(mobsimTimer.getTimeOfDay());
			internalInterface.arrangeNextAgentState(passenger);
		}
	}

	@Override
	public void afterSim() {
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId) {
		if (!agent.getMode().equals(mode)) {
			return false;
		}

		MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;
		internalInterface.registerAdditionalAgentOnLink(passenger);

		Id<Link> toLinkId = passenger.getDestinationLinkId();

		Route route = ((Leg)((PlanAgent)passenger).getCurrentPlanElement()).getRoute();
		PassengerRequest request = requestCreator.createRequest(internalPassengerHandling.createRequestId(),
				passenger.getId(), route, getLink(fromLinkId), getLink(toLinkId), now, now);
		validateAndSubmitRequest(passenger, request, now);
		return true;
	}

	private void validateAndSubmitRequest(MobsimPassengerAgent passenger, PassengerRequest request, double now) {
		activePassengers.put(request.getId(), passenger);
		if (internalPassengerHandling.validateRequest(request, requestValidator, now)) {
			//need to synchronise to address cases where requestSubmitted() may:
			// - be called from outside DepartureHandlers
			// - interfere with VrpOptimizer.nextTask()
			// - impact VrpAgentLogic.computeNextAction()
			synchronized (optimizer) {
				//optimizer can also reject request if cannot handle it
				// (async operation, notification comes via the events channel)
				optimizer.requestSubmitted(request);
			}
		}
	}

	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
				"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
				linkId, mode);
	}

	@Override
	public boolean tryPickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
			PassengerRequest request, double now) {
		boolean pickedUp = internalPassengerHandling.tryPickUpPassenger(driver, activePassengers.get(request.getId()),
				request.getId(), now);
		Verify.verify(pickedUp, "Not possible without prebooking");
		return pickedUp;
	}

	@Override
	public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now) {
		internalPassengerHandling.dropOffPassenger(driver, activePassengers.remove(request.getId()), request.getId(),
				now);
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			rejectedRequestsEvents.add(event);
		}
	}

	public static Provider<PassengerEngine> createProvider(String mode) {
		return new ModalProviders.AbstractProvider<>(mode) {
			@Inject
			private EventsManager eventsManager;

			@Inject
			private MobsimTimer mobsimTimer;

			@Override
			public DefaultPassengerEngine get() {
				return new DefaultPassengerEngine(getMode(), eventsManager, mobsimTimer,
						getModalInstance(PassengerRequestCreator.class), getModalInstance(VrpOptimizer.class),
						getModalInstance(Network.class), getModalInstance(PassengerRequestValidator.class));
			}
		};
	}
}
