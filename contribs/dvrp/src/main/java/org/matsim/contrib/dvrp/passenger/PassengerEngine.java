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
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PassengerEngine implements MobsimEngine, DepartureHandler {
	private static final Logger LOGGER = Logger.getLogger(PassengerEngine.class);

	private final String mode;
	private final EventsManager eventsManager;
	private final PassengerRequestCreator requestCreator;
	private final VrpOptimizer optimizer;
	private final Network network;
	private final PassengerRequestValidator requestValidator;

	private InternalInterface internalInterface;

	private final AdvanceRequestStorage advanceRequestStorage;
	private final AwaitingPickupStorage awaitingPickupStorage;
	private final Map<Id<Request>, MobsimPassengerAgent> passengersByRequestId = new HashMap<>();

	public PassengerEngine(String mode, EventsManager eventsManager, PassengerRequestCreator requestCreator,
			VrpOptimizer optimizer, Network network, PassengerRequestValidator requestValidator) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.requestCreator = requestCreator;
		this.optimizer = optimizer;
		this.network = network;
		this.requestValidator = requestValidator;

		advanceRequestStorage = new AdvanceRequestStorage();
		awaitingPickupStorage = new AwaitingPickupStorage();
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
	}

	@Override
	public void afterSim() {
	}

	/**
	 * This is to register an advance booking. The method is called when, in reality, the request is made.
	 */
	public boolean prebookTrip(double now, MobsimPassengerAgent passenger, Id<Link> fromLinkId, Id<Link> toLinkId,
			double departureTime) {
		if (departureTime <= now) {
			throw new IllegalStateException("This is not a call ahead");
		}

		PassengerRequest request = createValidateAndSubmitRequest(passenger, fromLinkId, toLinkId, departureTime, now);
		if (!request.isRejected()) {
			advanceRequestStorage.storeAdvanceRequest(request);
		}

		return !request.isRejected();
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

		PassengerRequest prebookedRequest = advanceRequestStorage.retrieveAdvanceRequest(passenger, fromLinkId,
				toLinkId, now);
		if (prebookedRequest == null) {// this is an immediate request
			//TODO what if it was already rejected while prebooking??
			createValidateAndSubmitRequest(passenger, fromLinkId, toLinkId, departureTime, now);
		} else {
			passengersByRequestId.put(prebookedRequest.getId(), passenger);
			PassengerPickupActivity awaitingPickup = awaitingPickupStorage.retrieveAwaitingPickup(prebookedRequest);
			if (awaitingPickup != null) {
				awaitingPickup.notifyPassengerIsReadyForDeparture(passenger, now);
			}
		}

		// always mark the departure as handled, even if rejected, in order to get more consistency with rejections
		// that are decided later (for instance, during optimisation which is usually called in the next sim step)
		// michalm, sep'18
		// See: github.com/matsim-org/matsim/pull/362 for some more discussion
		return true;//!request.isRejected();
	}

	// ================ REQUESTS HANDLING

	private PassengerRequest createValidateAndSubmitRequest(MobsimPassengerAgent passenger, Id<Link> fromLinkId,
			Id<Link> toLinkId, double departureTime, double now) {
		PassengerRequest request = createRequest(passenger, fromLinkId, toLinkId, departureTime, now);
		rejectInvalidRequest(request);
		if (!request.isRejected()) {
			passengersByRequestId.put(request.getId(), passenger);
			optimizer.requestSubmitted(request);//optimizer can also reject request if cannot handle it
		}
		return request;
	}

	private long nextId = 0;

	private PassengerRequest createRequest(MobsimPassengerAgent passenger, Id<Link> fromLinkId, Id<Link> toLinkId,
			double departureTime, double now) {
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

		PassengerRequest request = requestCreator.createRequest(id, passenger, fromLink, toLink, departureTime, now);
		return request;
	}

	private void rejectInvalidRequest(PassengerRequest request) {
		Set<String> violations = requestValidator.validateRequest(request);

		if (!violations.isEmpty()) {
			String causes = violations.stream().collect(Collectors.joining(", "));
			LOGGER.warn("Request: "
					+ request.getId()
					+ "of mode: "
					+ mode
					+ " will not be served. The agent will get stuck. Causes: "
					+ causes);
			request.setRejected(true);
			eventsManager.processEvent(
					new PassengerRequestRejectedEvent(request.getSubmissionTime(), mode, request.getId(), causes));
			eventsManager.processEvent(new PersonStuckEvent(request.getSubmissionTime(), request.getPassengerId(),
					request.getFromLink().getId(), request.getMode()));
		}
	}

	// ================ PICKUP / DROPOFF

	public boolean pickUpPassenger(PassengerPickupActivity pickupActivity, MobsimDriverAgent driver,
			PassengerRequest request, double now) {
		Id<Link> linkId = driver.getCurrentLinkId();
		MobsimPassengerAgent passenger = passengersByRequestId.get(request.getId());

		if (passenger.getCurrentLinkId() != linkId || passenger.getState() != State.LEG || !passenger.getMode()
				.equals(mode)) {
			awaitingPickupStorage.storeAwaitingPickup(request, pickupActivity);
			return false;// wait for the passenger
		}

		if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(), driver.getCurrentLinkId()) == null) {
			// the passenger has already been picked up and is on another taxi trip
			// seems there have been at least 2 requests made by this passenger for this location
			awaitingPickupStorage.storeAwaitingPickup(request, pickupActivity);
			return false;// wait for the passenger (optimistically, he/she should appear soon)
		}

		MobsimVehicle mobVehicle = driver.getVehicle();
		mobVehicle.addPassenger(passenger);
		passenger.setVehicle(mobVehicle);

		eventsManager.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

		return true;
	}

	public void dropOffPassenger(MobsimDriverAgent driver, PassengerRequest request, double now) {
		MobsimPassengerAgent passenger = passengersByRequestId.remove(request.getId());
		MobsimVehicle mobVehicle = driver.getVehicle();
		mobVehicle.removePassenger(passenger);
		passenger.setVehicle(null);

		eventsManager.processEvent(new PersonLeavesVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

		passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
		passenger.endLegAndComputeNextState(now);
		internalInterface.arrangeNextAgentState(passenger);
	}
}
