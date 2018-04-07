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

package org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.RunPSim;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

import java.util.Map;

public class TransitPerformanceRecorder {

    private final Scenario scenario;
    private final Map<Id<Vehicle>, Vehicle> vehicles;
    private final Map<Id<VehicleType>, VehicleType> vehicleTypes;
    private MobSimSwitcher switcher;
    private TransitPerformance transitPerformance;
    private VehicleTrackerCollection vehicletrackers;
    private Map<Id<Departure>, TransitRoute> departureIdToRoute;



    private boolean shouldReset() {
        if(switcher == null)
            return true;
        return switcher.isQSimIteration();
    }
    public TransitPerformanceRecorder(Scenario scenario, EventsManager eventsManager, MobSimSwitcher switcher) {
        this(scenario, eventsManager);
        this.switcher = switcher;
    }
    public TransitPerformanceRecorder(Scenario scenario, EventsManager eventsManager) {
//        identifyVehicleRoutes();
        vehicletrackers = new VehicleTrackerCollection(scenario.getTransitVehicles().getVehicles().size());
        RidershipHandler handler = new RidershipHandler();
        eventsManager.addHandler(handler);
        this.scenario = scenario;
        this.vehicles = scenario.getTransitVehicles().getVehicles();
        this.vehicleTypes = scenario.getTransitVehicles().getVehicleTypes();
        this.transitPerformance = new TransitPerformance();
    }

    public TransitPerformance getTransitPerformance() {
        return transitPerformance;
    }

    private int getVehicleCapacity(Id<Vehicle> vehicleId) {
        VehicleType type = vehicles.get(vehicleId).getType();
        VehicleCapacity capacity = vehicleTypes.get(type.getId()).getCapacity();
        return capacity.getSeats() + capacity.getStandingRoom();

    }


    class RidershipHandler implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {


        @Override
        public void reset(int iteration) {
            if(shouldReset()) {
                vehicletrackers = new VehicleTrackerCollection(scenario.getTransitVehicles().getVehicles().size());
                transitPerformance = new TransitPerformance();
            }
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


