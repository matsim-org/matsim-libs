/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import static java.lang.String.format;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
class InternalPassengerHandling {
	private static final Logger LOGGER = LogManager.getLogger(InternalPassengerHandling.class);

	private final String mode;
	private final EventsManager eventsManager;
	private final AtomicInteger currentRequestId = new AtomicInteger(-1);

	private InternalInterface internalInterface;

	InternalPassengerHandling(String mode, EventsManager eventsManager) {
		this.mode = mode;
		this.eventsManager = eventsManager;
	}

	void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	Id<Request> createRequestId() {
		return Id.create(mode + "_" + currentRequestId.incrementAndGet(), Request.class);
	}

	boolean validateRequest(PassengerRequest request, PassengerRequestValidator requestValidator, double now) {
		Set<String> violations = requestValidator.validateRequest(request);
		if (!violations.isEmpty()) {
			String cause = String.join(", ", violations);
			LOGGER.warn(format("Request: %s of mode: %s will not be served. Agent will get stuck. Cause: %s",
					request.getId(), mode, cause));
			eventsManager.processEvent(
					new PassengerRequestRejectedEvent(now, mode, request.getId(), request.getPassengerIds(), cause));
		}
		return violations.isEmpty();
	}

	boolean tryPickUpPassengers(MobsimDriverAgent driver, List<MobsimPassengerAgent> passengers, Id<Request> requestId,
			double now) {

		//ensure for every passenger first
		for (MobsimPassengerAgent passenger : passengers) {
			if (internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(), driver.getCurrentLinkId()) == null) {
				//only possible with prebooking
				return false;
			}
		}

		for (MobsimPassengerAgent passenger : passengers) {

			MobsimVehicle mobVehicle = driver.getVehicle();
			mobVehicle.addPassenger(passenger);
			passenger.setVehicle(mobVehicle);

			Id<DvrpVehicle> vehicleId = Id.create(mobVehicle.getId(), DvrpVehicle.class);
			eventsManager.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), mobVehicle.getId()));
			eventsManager.processEvent(new PassengerPickedUpEvent(now, mode, requestId, passenger.getId(), vehicleId));
		}

		return true;
	}

	void dropOffPassengers(MobsimDriverAgent driver, List<MobsimPassengerAgent> passengers, Id<Request> requestId, double now) {
		MobsimVehicle mobVehicle = driver.getVehicle();
		Id<DvrpVehicle> vehicleId = Id.create(mobVehicle.getId(), DvrpVehicle.class);

		for (MobsimPassengerAgent passenger : passengers) {
			mobVehicle.removePassenger(passenger);
			passenger.setVehicle(null);

			eventsManager.processEvent(new PassengerDroppedOffEvent(now, mode, requestId, passenger.getId(), vehicleId));
			eventsManager.processEvent(new PersonLeavesVehicleEvent(now, passenger.getId(), mobVehicle.getId()));

			passenger.notifyArrivalOnLinkByNonNetworkMode(passenger.getDestinationLinkId());
			passenger.endLegAndComputeNextState(now);
			internalInterface.arrangeNextAgentState(passenger);
		}
	}
}
