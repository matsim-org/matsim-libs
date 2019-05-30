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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.PreplanningEngine;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoRequest;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoWithRequiredBooking;
import org.matsim.facilities.FacilitiesUtils;

import com.google.common.collect.ImmutableList;

public final class PassengerEngine implements MobsimEngine, DepartureHandler, TripInfo.Provider {

	private static final Logger LOGGER = Logger.getLogger(PassengerEngine.class);

	private final String mode;
	private final EventsManager eventsManager;
	private final MobsimTimer mobsimTimer;
	private final PreplanningEngine bookingEngine;

	private final PassengerRequestCreator requestCreator;
	private final VrpOptimizer optimizer;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private InternalInterface internalInterface;

	private final AdvanceRequestStorage advanceRequestStorage = new AdvanceRequestStorage();
	private final AwaitingPickupStorage awaitingPickupStorage = new AwaitingPickupStorage();

	//keeps all received requests until rejection or dropoff
	private final Map<Id<Request>, RequestEntry> requests = new HashMap<>();

	//package protected -> meant to be instantiated via PassengerEngineQSimModule
	PassengerEngine(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer, PreplanningEngine bookingEngine,
			PassengerRequestCreator requestCreator, VrpOptimizer optimizer, Network network, PassengerRequestValidator requestValidator,
			PassengerRequestEventToPassengerEngineForwarder passengerRequestEventForwarder) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.mobsimTimer = mobsimTimer;
		this.bookingEngine = bookingEngine;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.network = network;
		this.requestValidator = requestValidator;
		passengerRequestEventForwarder.registerPassengerEngineEventsHandler(this);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	public String getMode() {
		return mode;
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void doSimStep(double time) {
		processPassengerRequestEvents();
	}

	@Override
	public void afterSim() {
	}

	@Override
	public final List<TripInfo> getTripInfos(TripInfoRequest tripInfoRequest) {
		// idea is to be able to return multiple trip options, cf. public transit router.  In the case here, we will need only one.  I.e. goals of this method are:
		// (1) fill out TripInfo
		// (2) keep handle so that passenger can accept, or not-confirmed request is eventually deleted again.  Also see doSimStet(...) ;

		Gbl.assertIf(tripInfoRequest.getTimeInterpretation() == TripInfo.TimeInterpretation.departure);
		Link pickupLink = FacilitiesUtils.decideOnLink(tripInfoRequest.getFromFacility(), network);
		Link dropoffLink = FacilitiesUtils.decideOnLink(tripInfoRequest.getToFacility(), network);
		double now = this.mobsimTimer.getTimeOfDay();

		//FIXME we need to send TripInfoRequest to VrpOptimizer and actually get TripInfos from there
		// for the time being: generating TripInfo object that will be returned to the potential passenger:
		return ImmutableList.of(
				new DvrpTripInfo(mode, pickupLink, dropoffLink, tripInfoRequest.getTime(), now, tripInfoRequest));
	}

	/**
	 * @param passenger will be changed to passengerId
	 * @param tripInfo
	 * @return true if request has not been rejected during booking (still can be rejected later)
	 */
	public void bookTrip(MobsimPassengerAgent passenger, TripInfoWithRequiredBooking tripInfo) {
		// this is the handle by which the passenger can accept.  This would, we think, easiest go to a container that keeps track of unconfirmed
		// offers.  We cannot say if advanceRequestStorage is the correct container for this, probably not and you will need yet another one.
		double now = mobsimTimer.getTimeOfDay();

		PassengerRequest request = createValidateAndSubmitRequest(passenger, tripInfo.getPickupLocation().getLinkId(),
				tripInfo.getDropoffLocation().getLinkId(), tripInfo.getExpectedBoardingTime(), now, true,
				tripInfo.getOriginalRequest());
		advanceRequestStorage.storeRequest(request);
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId) {
		if (!agent.getMode().equals(mode)) {
			return false;
		}

		MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;
		Id<Link> toLinkId = passenger.getDestinationLinkId();
		double departureTime = now;
		internalInterface.registerAdditionalAgentOnLink(passenger);

		List<PassengerRequest> prebookedRequests = advanceRequestStorage.retrieveRequests(passenger.getId(), fromLinkId,
				toLinkId);

		if (prebookedRequests.isEmpty()) {// this is an immediate request
			//TODO what if it was already rejected while prebooking??
			createValidateAndSubmitRequest(passenger, fromLinkId, toLinkId, departureTime, now, false, null);
		} else if (prebookedRequests.size() == 1) {
			PassengerRequest prebookedRequest = prebookedRequests.get(0);
			PassengerPickupActivity awaitingPickup = awaitingPickupStorage.retrieveAwaitingPickup(
					prebookedRequest.getId());
			if (awaitingPickup != null) {
				awaitingPickup.notifyPassengerIsReadyForDeparture(passenger, now);
			}
		} else {
			//FIXME
			throw new UnsupportedOperationException(
					"The agent has submitted more then 1 request the same from-to links");
		}

		return true;
	}

	// ================ REQUESTS HANDLING

	private PassengerRequest createValidateAndSubmitRequest(MobsimPassengerAgent passenger, Id<Link> fromLinkId,
			Id<Link> toLinkId, double departureTime, double now, boolean prebooked, TripInfoRequest originalRequest) {
		// yyyy remove parameter MobsimPassengerAgent. kai/gregor, jan'19
		PassengerRequest request = createRequest(passenger, fromLinkId, toLinkId, departureTime, now);
		requests.put(request.getId(), new RequestEntry(request, passenger, prebooked, originalRequest));
		if (validateRequest(request)) {
			optimizer.requestSubmitted(request);//optimizer can also reject request if cannot handle it
		}
		return request;
	}

	private long nextId = 0;

	private PassengerRequest createRequest(MobsimPassengerAgent passenger, Id<Link> fromLinkId, Id<Link> toLinkId,
			double departureTime, double now) {
		// yyyy remove parameter MobsimPassengerAgent. kai/gregor, jan'19

		Map<Id<Link>, ? extends Link> links = network.getLinks();
		Link fromLink = links.get(fromLinkId);
		if (fromLink == null) {
			throw new RuntimeException("fromLink does not exist. Id " + fromLinkId);
		}
		Link toLink = links.get(toLinkId);

		if (toLink == null) {
			throw new RuntimeException("toLink does not exist. Id " + toLinkId);
		}
		Id<Request> id = Id.create(mode + "_" + nextId++, Request.class);
		//TODO have not decided yet how VrpOptimizer determines if request is prebooked, maybe 'boolean prebooked'??
		return requestCreator.createRequest(id, passenger, fromLink, toLink, departureTime, now);
	}

	private boolean validateRequest(PassengerRequest request) {
		Set<String> violations = requestValidator.validateRequest(request);
		if (!violations.isEmpty()) {
			String causes = violations.stream().collect(Collectors.joining(", "));
			LOGGER.warn("Request: "
					+ request.getId()
					+ " of mode: "
					+ mode
					+ " will not be served. The agent will get stuck. Causes: "
					+ causes);
			eventsManager.processEvent(
					new PassengerRequestRejectedEvent(mobsimTimer.getTimeOfDay(), mode, request.getId(), causes,
							request.getPassengerId()));
		}
		return violations.isEmpty();
	}

	// ================ PICKUP / DROPOFF

	public boolean pickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
			PassengerRequest request, double now) {
		Id<Link> linkId = driver.getCurrentLinkId();
		RequestEntry requestEntry = requests.get(request.getId());
		MobsimPassengerAgent passenger = requestEntry.passenger;

		if (passenger.getCurrentLinkId() != linkId || passenger.getState() != State.LEG || !passenger.getMode()
				.equals(mode)) {
			awaitingPickupStorage.storeAwaitingPickup(request.getId(), pickupActivity);
			return false;// wait for the passenger
		}

		if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(), driver.getCurrentLinkId()) == null) {
			// the passenger has already been picked up and is on another taxi trip
			// seems there have been at least 2 requests made by this passenger for this location
			awaitingPickupStorage.storeAwaitingPickup(request.getId(), pickupActivity);
			return false;// wait for the passenger (optimistically, he/she should appear soon)
		}

		MobsimVehicle mobVehicle = driver.getVehicle();
		mobVehicle.addPassenger(passenger);
		passenger.setVehicle(mobVehicle);

		eventsManager.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), mobVehicle.getId()));
		return true;
	}

	public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now) {
		MobsimPassengerAgent passenger = requests.remove(request.getId()).passenger;
		MobsimVehicle mobVehicle = driver.getVehicle();
		mobVehicle.removePassenger(passenger);
		passenger.setVehicle(null);

		eventsManager.processEvent(new PersonLeavesVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

		passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
		passenger.endLegAndComputeNextState(now);
		internalInterface.arrangeNextAgentState(passenger);
	}

	// ================ REJECTED/SCHEDULED EVENTS

	private final Queue<PassengerRequestRejectedEvent> rejectedEvents = new ConcurrentLinkedQueue<>();
	private final Queue<PassengerRequestScheduledEvent> scheduledEvents = new ConcurrentLinkedQueue<>();

	void notifyPassengerRequestRejected(PassengerRequestRejectedEvent event) {
		rejectedEvents.add(event);
	}

	void notifyPassengerRequestScheduled(PassengerRequestScheduledEvent event) {
		scheduledEvents.add(event);
	}

	private void processPassengerRequestEvents() {
		while (!rejectedEvents.isEmpty()) {
			processRequestRejectedEvents(rejectedEvents.poll());
		}
		while (!scheduledEvents.isEmpty()) {
			processRequestScheduledEvents(scheduledEvents.poll());
		}
	}

	private void processRequestRejectedEvents(PassengerRequestRejectedEvent event) {
		RequestEntry requestEntry = requests.remove(event.getRequestId());
		if (requestEntry.prebooked) {
			advanceRequestStorage.removeRequest(requestEntry.passenger.getId(), event.getRequestId());
			//let agent/BookingEngine decide what to do next
			bookingEngine.notifyChangedTripInformation(requestEntry.passenger, Optional.empty());
		} else {
			//not much else can be done for immediate requests
			PassengerRequest request = requestEntry.request;
			eventsManager.processEvent(new PersonStuckEvent(mobsimTimer.getTimeOfDay(), request.getPassengerId(),
					request.getFromLink().getId(), request.getMode()));
		}
	}

	private void processRequestScheduledEvents(PassengerRequestScheduledEvent event) {
		RequestEntry requestEntry = requests.get(event.getRequestId());
		if (requestEntry.prebooked) {
			PassengerRequest request = requestEntry.request;
			bookingEngine.notifyChangedTripInformation(requestEntry.passenger, Optional.of(
					new DvrpTripInfo(mode, request.getFromLink(), request.getToLink(), event.getPickupTime(),
							event.getTime(), requestEntry.originalRequest)));
		}
	}

	private static class RequestEntry {
		private final PassengerRequest request;
		private final MobsimPassengerAgent passenger;
		private final boolean prebooked; // != immediate request
		private final TripInfoRequest originalRequest;

		private RequestEntry(PassengerRequest request, MobsimPassengerAgent passenger, boolean prebooked,
				TripInfoRequest originalRequest) {
			this.request = request;
			this.passenger = passenger;
			this.prebooked = prebooked;
			this.originalRequest = originalRequest;
		}
	}
}
