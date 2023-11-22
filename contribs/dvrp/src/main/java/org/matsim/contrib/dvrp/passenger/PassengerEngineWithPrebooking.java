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

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.PreplanningEngine;
import org.matsim.core.mobsim.qsim.interfaces.TripInfo;
import org.matsim.core.mobsim.qsim.interfaces.TripInfoWithRequiredBooking;
import org.matsim.core.modal.ModalProviders;
import org.matsim.facilities.FacilitiesUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public final class PassengerEngineWithPrebooking
		implements PassengerEngine, TripInfo.Provider, PassengerRequestRejectedEventHandler,
		PassengerRequestScheduledEventHandler {

	private final String mode;
	private final MobsimTimer mobsimTimer;
	private final EventsManager eventsManager;
	private final PreplanningEngine preplanningEngine;

	private final PassengerRequestCreator requestCreator;
	private final VrpOptimizer optimizer;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private InternalInterface internalInterface;

	private final Multimap<Id<Person>, PassengerRequest> advanceRequests = ArrayListMultimap.create();
	private final InternalPassengerHandling internalPassengerHandling;
	private final Map<Id<Request>, PassengerPickupActivity> awaitingPickups = new HashMap<>();

	private final Map<Id<Request>, RequestEntry> activeRequests = new HashMap<>();

	PassengerEngineWithPrebooking(String mode, EventsManager eventsManager, MobsimTimer mobsimTimer,
			PreplanningEngine preplanningEngine, PassengerRequestCreator requestCreator, VrpOptimizer optimizer,
			Network network, PassengerRequestValidator requestValidator) {
		this.mode = mode;
		this.mobsimTimer = mobsimTimer;
		this.preplanningEngine = preplanningEngine;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.network = network;
		this.requestValidator = requestValidator;
		this.eventsManager = eventsManager;

		internalPassengerHandling = new InternalPassengerHandling(mode, eventsManager);
	}

	@Override public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		internalPassengerHandling.setInternalInterface(internalInterface);
	}

	public String getMode() {
		return mode;
	}

	@Override public void onPrepareSim() { }

	@Override public void doSimStep(double time) {
		processPassengerRequestEvents();
	}

	@Override public void afterSim() { }

	@Override public final List<TripInfo> getTripInfos(TripInfo.Request tripInfoRequest) {
		// idea is to be able to return multiple trip options, cf. public transit router.  In the case here, we will need only one.  I.e. goals of this method are:

		// (1) fill out TripInfo

		// (2) possibly keep handle so that passenger can accept, or not-confirmed request is eventually deleted again.  Also see
		// doSimStep(...).  (In reality, it seems that real trips are not planned until the offer is accepted, which means that one does not
		// need to keep a handle.)

		Gbl.assertIf(tripInfoRequest.getTimeInterpretation() == TripInfo.Request.TimeInterpretation.departure);
		Link pickupLink = FacilitiesUtils.decideOnLink(tripInfoRequest.getFromFacility(), network);
		Link dropoffLink = FacilitiesUtils.decideOnLink(tripInfoRequest.getToFacility(), network);
		double now = this.mobsimTimer.getTimeOfDay();

		//FIXME we need to send TripInfoRequest to VrpOptimizer and actually get TripInfos from there
		// for the time being: generating TripInfo object that will be returned to the potential passenger:
		return ImmutableList.of( new DvrpTripInfo(mode, pickupLink, dropoffLink, tripInfoRequest.getTime(), now, tripInfoRequest, this));
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
		//TODO have a separate request creator for prebooking (accept TripInfo instead of Route)
		PassengerRequest request = requestCreator.createRequest(internalPassengerHandling.createRequestId(),
				List.of(passenger.getId()), tripInfo.getOriginalRequest().getPlannedRoute(),
				getLink(tripInfo.getPickupLocation().getLinkId()), getLink(tripInfo.getDropoffLocation().getLinkId()),
				tripInfo.getExpectedBoardingTime(), now);
		validateAndSubmitRequest(passenger, request, tripInfo.getOriginalRequest(), now);
		// hard assumption that with this engine, passenger ids is always a singleton. nkuehnel oct '23
		advanceRequests.put(request.getPassengerIds().stream().findFirst().orElseThrow(), request);
	}
	private Link getLink(Id<Link> linkId) {
		return Preconditions.checkNotNull(network.getLinks().get(linkId),
				"Link id=%s does not exist in network for mode %s. Agent departs from a link that does not belong to that network?",
				linkId, mode);
	}
	@Override public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> fromLinkId) {
		if (!agent.getMode().equals(mode)) {
			return false;
		}

		MobsimPassengerAgent passenger = (MobsimPassengerAgent)agent;
		internalInterface.registerAdditionalAgentOnLink(passenger);

		List<PassengerRequest> prebookedRequests = removeRequests(passenger.getId(), fromLinkId, passenger.getDestinationLinkId() );

		Preconditions.checkState(prebookedRequests.size() == 1, "Currently only one request is allowed for the same from-to link pair");
		// (but this is a local restriction, which could be locally changed.  kai, apr'23)

		//TODO what if it was already rejected while prebooking??

		PassengerRequest prebookedRequest = prebookedRequests.get(0);

		eventsManager.processEvent(new PassengerWaitingEvent(now, mode, prebookedRequest.getId(), prebookedRequest.getPassengerIds()));

		PassengerPickupActivity awaitingPickup = awaitingPickups.remove(prebookedRequest.getId());
		if (awaitingPickup != null) {
			awaitingPickup.notifyPassengersAreReadyForDeparture(List.of(passenger), now);
		}

		return true;
	}

	private List<PassengerRequest> removeRequests( Id<Person> passengerId, Id<Link> fromLinkId, Id<Link> toLinkId ) {
		Collection<PassengerRequest> allRequestsForThisPassenger = advanceRequests.get(passengerId);
		List<PassengerRequest> filteredRequests = advanceRequests.get(passengerId)
				.stream()
				.filter(r -> r.getFromLink().getId().equals(fromLinkId) && r.getToLink().getId().equals(toLinkId))
				.collect(Collectors.toList());

		allRequestsForThisPassenger.removeAll(filteredRequests);

		return filteredRequests;
	}

	// ================ REQUESTS HANDLING

	//TODO have not decided yet how VrpOptimizer determines if request is prebooked, maybe 'boolean prebooked'??
	private void validateAndSubmitRequest(MobsimPassengerAgent passenger, PassengerRequest request,
			TripInfo.Request originalRequest, double now) {
		activeRequests.put(request.getId(), new RequestEntry(request, passenger, originalRequest));
		if (internalPassengerHandling.validateRequest(request, requestValidator, now)) {
			optimizer.requestSubmitted(request);//optimizer can also reject request if cannot handle it
		}
	}

	// ================ PICKUP / DROPOFF

	@Override
	public boolean tryPickUpPassengers(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
			Id<Request> requestId, double now) {
		Id<Link> linkId = driver.getCurrentLinkId();
		RequestEntry requestEntry = activeRequests.get(requestId);
		MobsimPassengerAgent passenger = requestEntry.passenger;

		if (passenger.getCurrentLinkId() != linkId
				|| passenger.getState() != MobsimAgent.State.LEG
				|| !passenger.getMode().equals(mode)) {
			awaitingPickups.put(requestId, pickupActivity);
			return false;// wait for the passenger
		}

		if (!internalPassengerHandling.tryPickUpPassengers(driver, List.of(passenger), requestId, now)) {
			// the passenger has already been picked up and is on another taxi trip
			// seems there have been at least 2 requests made by this passenger for this location
			awaitingPickups.put(requestId, pickupActivity);
			return false;// wait for the passenger (optimistically, he/she should appear soon)
		}

		return true;
	}

	@Override
	public void dropOffPassengers(MobsimDriverAgent driver, Id<Request> requestId, double now) {
		internalPassengerHandling.dropOffPassengers(driver, List.of(activeRequests.remove(requestId).passenger), requestId, now);
	}

	// ================ REJECTED/SCHEDULED EVENTS

	private final Queue<PassengerRequestRejectedEvent> rejectedEvents = new ConcurrentLinkedQueue<>();
	private final Queue<PassengerRequestScheduledEvent> scheduledEvents = new ConcurrentLinkedQueue<>();

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			rejectedEvents.add(event);
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(mode)) {
			scheduledEvents.add(event);
		}
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
		RequestEntry requestEntry = activeRequests.remove(event.getRequestId());
		advanceRequests.get(requestEntry.passenger.getId()).removeIf(req -> req.getId().equals(event.getRequestId()));
		//let agent/preplanningEngine decide what to do next
		preplanningEngine.notifyChangedTripInformation(requestEntry.passenger, Optional.empty());
	}

	private void processRequestScheduledEvents(PassengerRequestScheduledEvent event) {
		RequestEntry requestEntry = activeRequests.get(event.getRequestId());
		if (requestEntry != null) {
			PassengerRequest request = requestEntry.request;
			preplanningEngine.notifyChangedTripInformation(requestEntry.passenger, Optional.of(
					new DvrpTripInfo(mode, request.getFromLink(), request.getToLink(), event.getPickupTime(),
							event.getTime(), requestEntry.originalRequest, this)));
		}
	}

	private static class RequestEntry {
		private final PassengerRequest request;
		private final MobsimPassengerAgent passenger;
		private final TripInfo.Request originalRequest;
		private RequestEntry(PassengerRequest request, MobsimPassengerAgent passenger, TripInfo.Request originalRequest) {
			this.request = request;
			this.passenger = passenger;
			this.originalRequest = originalRequest;
		}
	}

	public static Provider<PassengerEngine> createProvider(String mode) {
		return new ModalProviders.AbstractProvider<>(mode, DvrpModes::mode) {
			@Inject private EventsManager eventsManager;
			@Inject private MobsimTimer mobsimTimer;
			@Inject
			private PreplanningEngine preplanningEngine;
			@Override public PassengerEngineWithPrebooking get() {
				return new PassengerEngineWithPrebooking(getMode(), eventsManager, mobsimTimer, preplanningEngine,
						getModalInstance(PassengerRequestCreator.class), getModalInstance(VrpOptimizer.class),
						getModalInstance(Network.class), getModalInstance(PassengerRequestValidator.class));
			}
		};
	}

	/*
	 * This method has been retrofitted with the new prebooking implementation in
	 * Nov 2023. Not sure if this is the right way to do it, this class doesn't seem
	 * to be tested anywhere. /sebhoerl
	 */
	@Override
	public boolean notifyWaitForPassengers(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver, Id<Request> requestId) {
		Id<Link> linkId = driver.getCurrentLinkId();
		RequestEntry requestEntry = activeRequests.get(requestId);
		MobsimPassengerAgent passenger = requestEntry.passenger;

		if (passenger.getCurrentLinkId() != linkId
				|| passenger.getState() != MobsimAgent.State.LEG
				|| !passenger.getMode().equals(mode)) {
			awaitingPickups.put(requestId, pickupActivity);
			return false;// wait for the passenger
		}

		return true; // passenger present?
	}
}
