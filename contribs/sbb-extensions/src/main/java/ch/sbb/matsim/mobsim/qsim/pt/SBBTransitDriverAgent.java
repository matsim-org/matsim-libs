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

import java.util.LinkedList;

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
	private TransitRouteStop currentStop;
	private TransitRouteStop nextStop;
	private TransitRouteStop previousStop;
	private TransitRoute currentTransitRoute;
	private LinkedList<TransitRouteStop> remainingRouteStops = null;

	SBBTransitDriverAgent(Umlauf umlauf, String transportMode, TransitStopAgentTracker agentTracker, InternalInterface internalInterface) {
		super(umlauf, transportMode, agentTracker, internalInterface);
		this.eventsManager = internalInterface.getMobsim().getEventsManager();
		this.accessEgress = new SBBPassengerAccessEgress(internalInterface, agentTracker, internalInterface.getMobsim().getScenario(),
			this.eventsManager);
		checkCurrentRoute();
	}

	SBBTransitDriverAgent(SBBTransitDriverMessage message, Umlauf umlauf, String transportMode, TransitStopAgentTracker thisAgentTracker,
		InternalInterface internalInterface) {
		super(message.delegateMessage(), umlauf, transportMode, thisAgentTracker, internalInterface);
		eventsManager = internalInterface.getMobsim().getEventsManager();
		accessEgress = new SBBPassengerAccessEgress(internalInterface, thisAgentTracker, internalInterface.getMobsim().getScenario(),
			this.eventsManager);

		// below does what checkRoute does, bot for inbetween states.
		this.currentTransitRoute = getTransitRoute();
		this.remainingRouteStops = new LinkedList<>();
		for (var stop : this.currentTransitRoute.getStops()) {
			if (stop.getStopFacility().getId().equals(message.previousStop)) {
				this.previousStop = stop;
				continue; // don't add this stop to remaining, but all after this one.
			}
			// add all stops after previous stop, or all stops in case there was no previous stop
			if (this.previousStop != null || message.previousStop == null) {
				remainingRouteStops.add(stop);
			}
			// skip all elements until one of the conditions above is true
		}
		this.nextStop = this.remainingRouteStops.getFirst();
	}

	void arrive(TransitRouteStop stop, double now) {
		TransitStopFacility facility = stop.getStopFacility();
		assertExpectedStop(facility);
		processVehicleArrival(facility, now);
	}

	void arriveAtNextStop(double now) {
		arrive(this.nextStop, now);
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now) {
		assertExpectedStop(stop);

		double stopTime = this.accessEgress.handlePassengersWithPhysicalLimits(stop, this.getVehicle(), this.getTransitLine(),
			this.currentTransitRoute, this.remainingRouteStops, now);

		if (stopTime <= 0.0) {
			// figure out if it's already time to depart or not
			double departureOffset = this.currentStop.getDepartureOffset().or(this.currentStop.getArrivalOffset()).seconds();
			double scheduledDepartureTime = this.getDeparture().getDepartureTime() + departureOffset;
			if (scheduledDepartureTime > now) {
				stopTime = 1.0; // allow agents arriving in the next time step to board
			}
		}

		return stopTime;
	}

	@Override
	protected void handleEndRoute(double now) {

		// Delegates call to the correct access egress
		if (getDeparture() != null)
			accessEgress.relocatePassengers(this, getDeparture().getChainedDepartures(), now);

	}

	void departAtStop(double now) {
		depart(getCurrentStop().getStopFacility(), now);
	}

	void depart(TransitStopFacility stop, double now) {
		handleDeparture(stop, now);
	}

	private void handleDeparture(TransitStopFacility stop, double now) {
		assertExpectedStop(stop);
		processVehicleDeparture(stop, now);
	}

	TransitRouteStop getNextStop() {
		return this.nextStop;
	}

	TransitRouteStop getPreviousStop() {
		return this.previousStop;
	}

	TransitRouteStop getCurrentStop() {
		return this.currentStop;
	}

	private void assertExpectedStop(final TransitStopFacility stop) {
		checkCurrentRoute();
		if (this.currentStop != null && stop == this.currentStop.getStopFacility()) {
			return;
		}
		if (stop != this.nextStop.getStopFacility() || this.currentStop != null) {
			throw new RuntimeException("Expected stop " + this.nextStop.getStopFacility().getId() + ", got " + stop.getId());
		}
	}

	/**
	 * This does not really check, but sets a lot of state. I found it this way and I'll keep it this way, I guess.
	 */
	private void checkCurrentRoute() {
		TransitRoute route = super.getTransitRoute();
		if (route != null && route != this.currentTransitRoute) {
			this.currentTransitRoute = route;
			this.remainingRouteStops = new LinkedList<>(route.getStops());
			this.nextStop = this.remainingRouteStops.getFirst();
		}
	}

	private void processVehicleArrival(final TransitStopFacility stop, final double now) {
		if (this.currentStop == null) {
			this.currentStop = this.nextStop;
			this.eventsManager.processEvent(new VehicleArrivesAtFacilityEvent(now, this.getVehicle().getId(), stop.getId(), 0.0));
			this.remainingRouteStops.removeFirst();
			this.nextStop = this.remainingRouteStops.isEmpty() ? null : this.remainingRouteStops.getFirst();
		}
	}

	private void processVehicleDeparture(final TransitStopFacility stop, final double now) {
		if (this.currentStop != null) {
			this.previousStop = this.currentStop;
			this.currentStop = null;
			this.eventsManager.processEvent(new VehicleDepartsAtFacilityEvent(now, this.getVehicle().getId(), stop.getId(), 0.0));
		}
	}

	@Override
	public Message toMessage() {
		// SAFETY: We know that we inherit fom TransitDriverAgentImpl which creates this message.
		var delegateMessage = (TransitDriverMessage) super.toMessage();

		return new SBBTransitDriverMessage(
			delegateMessage,
			previousStop.getStopFacility().getId(),
			getTransitLine().getId(),
			getTransitRoute().getId(),
			getDeparture().getId()
		);
	}

	public record SBBTransitDriverMessage(
		TransitDriverMessage delegateMessage,
		Id<TransitStopFacility> previousStop,
		Id<TransitLine> transitLineId,
		Id<TransitRoute> transitRouteId,
		Id<Departure> departureId
	) implements Message {
	}
}
