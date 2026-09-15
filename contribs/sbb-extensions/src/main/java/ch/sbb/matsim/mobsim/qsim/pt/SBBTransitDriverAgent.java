/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package ch.sbb.matsim.mobsim.qsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.*;

/**
 * @author mrieser / SBB
 */
public class SBBTransitDriverAgent extends TransitDriverAgentImpl {

	private final EventsManager eventsManager;
	private final SBBPassengerAccessEgress accessEgress;

	SBBTransitDriverAgent(Umlauf umlauf, String transportMode, TransitStopAgentTracker agentTracker, InternalInterface internalInterface) {
		super(umlauf, transportMode, agentTracker, internalInterface);
		this.eventsManager = internalInterface.getMobsim().getEventsManager();
		this.accessEgress = new SBBPassengerAccessEgress(internalInterface, agentTracker, internalInterface.getMobsim().getScenario(),
			this.eventsManager);
	}

	SBBTransitDriverAgent(SBBTransitDriverMessage message, Umlauf umlauf, String transportMode, TransitStopAgentTracker thisAgentTracker,
		InternalInterface internalInterface) {
		super(message.delegateMessage(), umlauf, transportMode, thisAgentTracker, internalInterface);
		eventsManager = internalInterface.getMobsim().getEventsManager();
		accessEgress = new SBBPassengerAccessEgress(internalInterface, thisAgentTracker, internalInterface.getMobsim().getScenario(),
			this.eventsManager);
	}

	void arrive(TransitRouteStop stop, double now) {
		assertExpectedStop(stop.getStopFacility());
		processVehicleArrival(stop.getStopFacility(), now);
	}

	void arriveAtNextStop(double now) {
		arrive(stops.get(stopIndex), now);
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now) {
		assertExpectedStop(stop);

		double stopTime = this.accessEgress.handlePassengersWithPhysicalLimits(stop, this.getVehicle(), this.getTransitLine(),
			getTransitRoute(), stops.subList(stopIndex + 1, stops.size()), now);

		if (stopTime <= 0.0) {
			// figure out if it's already time to depart or not
			double departureOffset = currentStop.getDepartureOffset().or(currentStop::getArrivalOffset).seconds();
			double scheduledDepartureTime = this.getDeparture().getDepartureTime() + departureOffset;
			if (scheduledDepartureTime > now) {
				stopTime = 1.0; // allow agents arriving in the next time step to board
			}
		}

		return stopTime;
	}

	@Override
	protected void handleEndRoute(double now) {
		if (getDeparture() != null)
			accessEgress.relocatePassengers(this, getDeparture().getChainedDepartures(), now);
	}

	void departAtStop(double now) {
		depart(getCurrentStop().getStopFacility(), now);
	}

	void depart(TransitStopFacility stop, double now) {
		assertExpectedStop(stop);
		processVehicleDeparture(stop, now);
	}

	TransitRouteStop getNextStop() {
		return (stops != null && stopIndex < stops.size()) ? stops.get(stopIndex) : null;
	}

	TransitRouteStop getPreviousStop() {
		return (stops != null && stopIndex > 0) ? stops.get(stopIndex - 1) : null;
	}

	TransitRouteStop getCurrentStop() {
		return currentStop;
	}

	private void assertExpectedStop(final TransitStopFacility stop) {
		if (currentStop != null) {
			return; // already dwelling at this stop, re-entry is fine
		}
		if (stops == null || stopIndex >= stops.size() || stop != stops.get(stopIndex).getStopFacility()) {
			String expected = (stops != null && stopIndex < stops.size())
				? stops.get(stopIndex).getStopFacility().getId().toString() : "none";
			throw new RuntimeException("Expected stop " + expected + ", got " + stop.getId());
		}
	}

	private void processVehicleArrival(final TransitStopFacility stop, final double now) {
		if (currentStop == null) {
			currentStop = stops.get(stopIndex);
			eventsManager.processEvent(new VehicleArrivesAtFacilityEvent(now, this.getVehicle().getId(), stop.getId(), 0.0));
		}
	}

	private void processVehicleDeparture(final TransitStopFacility stop, final double now) {
		if (currentStop != null) {
			currentStop = null;
			// directly set the index of the base class.
			stopIndex++;
			eventsManager.processEvent(new VehicleDepartsAtFacilityEvent(now, this.getVehicle().getId(), stop.getId(), 0.0));
		}
	}

	@Override
	public Message toMessage() {
		// SAFETY: We know that we inherit from TransitDriverAgentImpl which creates this message.
		var delegateMessage = (TransitDriverMessage) super.toMessage();

		return new SBBTransitDriverMessage(
			delegateMessage,
			getTransitLine().getId(),
			getTransitRoute().getId(),
			getDeparture().getId()
		);
	}

	public record SBBTransitDriverMessage(
		TransitDriverMessage delegateMessage,
		Id<TransitLine> transitLineId,
		Id<TransitRoute> transitRouteId,
		Id<Departure> departureId
	) implements Message {
	}
}
