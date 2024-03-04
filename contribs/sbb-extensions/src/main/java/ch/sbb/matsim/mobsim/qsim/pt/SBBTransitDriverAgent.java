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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / SBB
 */
public class SBBTransitDriverAgent extends TransitDriverAgentImpl {

    private final EventsManager eventsManager;
    private final SBBPassengerAccessEgress accessEgress;
    private TransitRouteStop currentStop;
    private TransitRouteStop nextStop;
    private TransitRoute currentTransitRoute;
    private LinkedList<TransitRouteStop> remainingRouteStops = null;

    SBBTransitDriverAgent(Umlauf umlauf, String transportMode, TransitStopAgentTracker agentTracker, InternalInterface internalInterface) {
        super(umlauf, transportMode, agentTracker, internalInterface);
        this.eventsManager = internalInterface.getMobsim().getEventsManager();
        this.accessEgress = new SBBPassengerAccessEgress(internalInterface, agentTracker, internalInterface.getMobsim().getScenario(), this.eventsManager);
        checkCurrentRoute();
    }

    void arrive(TransitRouteStop stop, double now) {
        TransitStopFacility facility = stop.getStopFacility();
        assertExpectedStop(facility);
        processVehicleArrival(facility, now);
    }

    @Override
    public double handleTransitStop(TransitStopFacility stop, double now) {
        assertExpectedStop(stop);

        double stopTime = this.accessEgress.handlePassengersWithPhysicalLimits(stop, this.getVehicle(), this.getTransitLine(), this.currentTransitRoute, this.remainingRouteStops, now);

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

    void depart(TransitStopFacility stop, double now) {
        handleDeparture(stop, now);
    }

    private void handleDeparture(TransitStopFacility stop, double now) {
        assertExpectedStop(stop);
        processVehicleDeparture(stop, now);
    }

    TransitRouteStop getNextRouteStop() {
        return this.nextStop;
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
            this.currentStop = null;
            this.eventsManager.processEvent(new VehicleDepartsAtFacilityEvent(now, this.getVehicle().getId(), stop.getId(), 0.0));
        }
    }
}
