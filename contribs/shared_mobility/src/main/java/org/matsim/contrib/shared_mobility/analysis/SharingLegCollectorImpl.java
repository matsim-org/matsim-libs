package org.matsim.contrib.shared_mobility.analysis;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.events.*;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * @author steffenaxer
 */
public class SharingLegCollectorImpl implements TeleportationArrivalEventHandler, LinkLeaveEventHandler,
        PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, SharingLegCollector, SharingVehicleEventHandler,
        SharingPickupEventHandler, SharingDropoffEventHandler, IterationStartsListener {

    Scenario scenario;

    @Inject
    SharingLegCollectorImpl(Scenario scenario) {
        this.scenario = scenario;
    }

    private final IdMap<Person, SharingLegBuilder> person2CurrentSharingLegBuilder = new IdMap<>(Person.class);
    private final IdMap<SharingService, Collection<SharingLeg>> sharingService2SharingLegs = new IdMap<>(SharingService.class);
    private final IdMap<Vehicle, Set<Id<Person>>> vehicle2Person = new IdMap<>(Vehicle.class);
    private final IdMap<Vehicle, Id<SharingService>> vehicle2ServiceId = new IdMap<>(Vehicle.class);

    @Override
    public void handleEvent(SharingDropoffEvent event) {

        // The SharingLeg ends

        // Ensure to delete this SharingLegBuilder after the pickup
        SharingLegBuilder sharingLegBuilder = this.person2CurrentSharingLegBuilder.remove(event.getPersonId());
        sharingLegBuilder.setArrivalTime(event.getTime());
        sharingLegBuilder.setToCoord(scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord());
        SharingLeg sharingLeg = sharingLegBuilder.build();

        if (sharingLeg != null) {
            Id<SharingService> sharingServiceId = sharingLeg.getSharingServiceId();
            this.sharingService2SharingLegs.putIfAbsent(sharingServiceId, new ArrayList<>());
            this.sharingService2SharingLegs.get(sharingServiceId).add(sharingLeg);
        }

    }

    @Override
    public void handleEvent(SharingPickupEvent event) {

        SharingLegBuilder currentSharingLegBuilder = this.person2CurrentSharingLegBuilder.get(event.getPersonId());

        if (currentSharingLegBuilder != null) {
            throw new IllegalStateException("Adding new SharingLegBuilder without finishing previous instance");
        }

        // The SharingLeg starts
        addSharingLegBuilder(event.getPersonId());
        SharingLegBuilder sharingLegBuilder = this.person2CurrentSharingLegBuilder.get(event.getPersonId());
        sharingLegBuilder.setDepartureTime(event.getTime());
        sharingLegBuilder.setPersonId(event.getPersonId());
        sharingLegBuilder.setSharingServiceId(event.getServiceId());
        sharingLegBuilder.setFromCoord(scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord());

        // Set vehicleId, need regular Id<Vehicle> to be inline with PersonEntersVehicleEvent
        Id<Vehicle> vehicleId = Id.createVehicleId(event.getSharingVehicleId().toString());
        sharingLegBuilder.setVehicleId(vehicleId);
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        if (person2CurrentSharingLegBuilder.containsKey(event.getPersonId())) {
            SharingLegBuilder sharingLegBuilder = this.person2CurrentSharingLegBuilder.get(event.getPersonId());
            sharingLegBuilder.addDistance(event.getDistance());
        }
    }

    private void addSharingLegBuilder(Id<Person> personId) {
        this.person2CurrentSharingLegBuilder.put(personId, new SharingLegBuilder());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        // Increment distances
        Set<Id<Person>> personsForVehicle = this.vehicle2Person.get(event.getVehicleId());

        if (personsForVehicle != null) {
            for (Id<Person> personId : this.vehicle2Person.get(event.getVehicleId())) {
                double length = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
                SharingLegBuilder sharingLegBuilder = this.person2CurrentSharingLegBuilder.get(personId);

                if (sharingLegBuilder != null) {
                    sharingLegBuilder.addDistance(length);
                }
            }
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        // Only observe service vehicles
        if (this.vehicle2ServiceId.containsKey(event.getVehicleId())) {
            // Remove a person from the vehicle
            vehicle2Person.get(event.getVehicleId()).remove(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        // Only observe service vehicles
        if (this.vehicle2ServiceId.containsKey(event.getVehicleId())) {
            // Add a person to vehicle
            vehicle2Person.putIfAbsent(event.getVehicleId(), new HashSet<>());
            vehicle2Person.get(event.getVehicleId()).add(event.getPersonId());
        }
    }

    @Override
    public Map<Id<SharingService>, Collection<SharingLeg>> getSharingLegs() {
        return this.sharingService2SharingLegs;
    }

    @Override
    public void handleEvent(SharingVehicleEvent event) {
        Id<Vehicle> vehicleId = Id.createVehicleId(event.getSharingVehicleId().toString());
        Id<SharingService> sharingServiceId = event.getServiceId();
        this.vehicle2ServiceId.put(vehicleId, sharingServiceId);
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        person2CurrentSharingLegBuilder.clear();
        sharingService2SharingLegs.clear();
        vehicle2Person.clear();
        vehicle2ServiceId.clear();
    }

}
