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

package playground.pieter.distributed.listeners.events.transit;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TransitPerformanceRecorder {

    private final Scenario scenario;
    private final RidershipHandler handler;
    private final Map<Id<Vehicle>, Vehicle> vehicles;
    private final Map<Id<VehicleType>, VehicleType> vehicleTypes;

    public TransitPerformance getTransitPerformance() {
        return transitPerformance;
    }

    private TransitPerformance transitPerformance;
    private VehicleTrackerCollection vehicletrackers;
    private Map<Id<Departure>, TransitRoute> departureIdToRoute;

    public TransitPerformanceRecorder(Scenario scenario, EventsManager eventsManager) {
//        identifyVehicleRoutes();
        vehicletrackers = new VehicleTrackerCollection(scenario.getVehicles().getVehicles().size());
        this.handler = new RidershipHandler();
        eventsManager.addHandler(handler);
        this.scenario = scenario;
        this.vehicles = scenario.getVehicles().getVehicles();
        this.vehicleTypes = scenario.getVehicles().getVehicleTypes();
        this.transitPerformance = new TransitPerformance();
    }

    private int getVehicleCapacity(Id<Vehicle> vehicleId) {
        VehicleType type = vehicles.get(vehicleId).getType();
        VehicleCapacity capacity = vehicleTypes.get(type.getId()).getCapacity();
        return capacity.getSeats() + capacity.getStandingRoom();

    }

//    private void identifyVehicleRoutes() {
//        departureIdToRoute = new HashMap<>();
//        Collection<TransitLine> lines = scenario.getTransitSchedule().getTransitLines().values();
//        for (TransitLine line : lines) {
//            Collection<TransitRoute> routes = line.getRoutes().values();
//            for (TransitRoute route : routes) {
//                Collection<Departure> departures = route.getDepartures().values();
//                for (Departure departure : departures) {
//                    departureIdToRoute.put(new FullDeparture(line.getId(), route.getId(), departure.getVehicleId(),
//                            departure.getId()).getFullDepartureId(), route);
//                }
//            }
//        }
//    }

    class RidershipHandler implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {


        @Override
        public void reset(int iteration) {
            vehicletrackers = new VehicleTrackerCollection(scenario.getVehicles().getVehicles().size());
            transitPerformance = new TransitPerformance();
        }


        @Override
        public void handleEvent(PersonEntersVehicleEvent event) {
            VehicleTracker tracker = vehicletrackers.get(event.getVehicleId().toString(), event.getTime());
            //skip car drivers
            if (tracker == null) return;
            tracker.ridershipIncrement(event);
        }

        @Override
        public void handleEvent(PersonLeavesVehicleEvent event) {
            VehicleTracker tracker = vehicletrackers.get(event.getVehicleId().toString(), event.getTime());
//			skip car drivers
            if (tracker == null) return;
            tracker.ridershipDecrement(event);
        }

        @Override
        public void handleEvent(TransitDriverStartsEvent event) {
            FullDeparture fullDeparture = new FullDeparture(event.getTransitLineId(), event.getTransitRouteId(),
                    event.getVehicleId(), event.getDepartureId());
            VehicleTracker tracker = new VehicleTracker(fullDeparture, event.getDriverId(), getVehicleCapacity(event.getVehicleId()));
            vehicletrackers.put(event.getVehicleId().toString(),event.getTime(), tracker);
        }

        @Override
        public void handleEvent(VehicleArrivesAtFacilityEvent event) {
            VehicleTracker tracker = vehicletrackers.get(event.getVehicleId().toString(),event.getTime());
            DwellEvent dwellEvent = tracker.registerArrival(event);
            transitPerformance.addVehicleDwellEventAtStop(tracker.getFullDeparture().getLineId(),tracker.getFullDeparture().getRouteId(),event.getFacilityId(),dwellEvent);

        }

        @Override
        public void handleEvent(VehicleDepartsAtFacilityEvent event) {
            VehicleTracker tracker = vehicletrackers.get(event.getVehicleId().toString(), event.getTime());
            tracker.registerDeparture(event);

        }
    }
}


