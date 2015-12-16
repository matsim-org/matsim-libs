/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 *                                                                         
 * *********************************************************************** */
package org.matsim.contrib.emissions;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.ColdEmissionAnalysisModule.ColdEmissionAnalysisModuleParameter;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;


/**
 * @author benjamin
 */
public class ColdEmissionHandler implements LinkLeaveEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private final Vehicles emissionVehicles;
    private final Network network;
    private final ColdEmissionAnalysisModule coldEmissionAnalysisModule;

    private final Map<Id<Person>, Double> personId2stopEngineTime = new TreeMap<>();
    private final Map<Id<Person>, Double> personId2accumulatedDistance = new TreeMap<>();
    private final Map<Id<Person>, Double> personId2parkingDuration = new TreeMap<>();
    private final Map<Id<Person>, Id<Link>> personId2coldEmissionEventLinkId = new TreeMap<>();
    
    private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;
    
    public ColdEmissionHandler(
            Vehicles emissionVehicles,
            Network network,
            ColdEmissionAnalysisModuleParameter parameterObject2,
            EventsManager emissionEventsManager, Double emissionEfficiencyFactor) {

        this.emissionVehicles = emissionVehicles;
        this.network = network;
        this.coldEmissionAnalysisModule = new ColdEmissionAnalysisModule(parameterObject2, emissionEventsManager, emissionEfficiencyFactor);
    }

    @Override
    public void reset(int iteration) {
        personId2stopEngineTime.clear();
        personId2accumulatedDistance.clear();
        personId2parkingDuration.clear();
        personId2coldEmissionEventLinkId.clear();
        coldEmissionAnalysisModule.reset();
        delegate.reset(iteration);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
		/*
		 * TODO Perspectively change calculation and analysis completely from
		 * vehicles to persons and use PersonEntersVehicle and
		 * PersonLeavesVehicle Events instead of PersonDeparture and
		 * PersonArrival Events to determine engine start and stop time. 
		 * Theresa Oct'2015
		 */
		Id<Person> personId = delegate.getDriverOfVehicle(event.getVehicleId());
        Id<Link> linkId = event.getLinkId();
        Link link = this.network.getLinks().get(linkId);
        double linkLength = link.getLength();
        Double previousDistance = this.personId2accumulatedDistance.get(personId);
        if (previousDistance != null) {
            double distance = previousDistance + linkLength;
            double parkingDuration = this.personId2parkingDuration.get(personId);
            Id<Link> coldEmissionEventLinkId = this.personId2coldEmissionEventLinkId.get(personId);
            Id<Vehicle> vehicleId = Id.create(personId, Vehicle.class);
            String vehicleInformation = getVehicleInformation(personId, vehicleId);
            if ((distance / 1000) > 1.0) {
                this.coldEmissionAnalysisModule.calculateColdEmissionsAndThrowEvent(
                        coldEmissionEventLinkId,
                        personId,
                        event.getTime(),
                        parkingDuration,
                        2,
                        vehicleInformation);
                this.personId2accumulatedDistance.remove(personId);
            } else {
                this.personId2accumulatedDistance.put(personId, distance);
            }
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (!event.getLegMode().equals("car")) { // no emissions to calculate...
            return;
        }
        Id<Person> personId = event.getPersonId();
        Double stopEngineTime = event.getTime();
        this.personId2stopEngineTime.put(personId, stopEngineTime);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (!event.getLegMode().equals("car")) { // no engine to start...
            return;
        }
        Id<Link> linkId = event.getLinkId();
        Id<Person> personId = event.getPersonId();
        double startEngineTime = event.getTime();
        this.personId2coldEmissionEventLinkId.put(personId, linkId);

        double parkingDuration;
        if (this.personId2stopEngineTime.containsKey(personId)) {
            double stopEngineTime = this.personId2stopEngineTime.get(personId);
            parkingDuration = startEngineTime - stopEngineTime;

        } else { //parking duration is assumed to be at least 12 hours when parking overnight
            parkingDuration = 43200.0;
        }
        this.personId2parkingDuration.put(personId, parkingDuration);
        this.personId2accumulatedDistance.put(personId, 0.0);
        this.coldEmissionAnalysisModule.calculateColdEmissionsAndThrowEvent(
                linkId,
                personId,
                startEngineTime,
                parkingDuration,
                1,
                getVehicleInformation(personId, Id.create(event.getPersonId(), Vehicle.class)));
    }

	private String getVehicleInformation(Id<Person> personId, Id<Vehicle> vehicleId) {
	    String vehicleInformation;
	
	    if (!this.emissionVehicles.getVehicles().containsKey(vehicleId)) {
	        throw new RuntimeException("No vehicle defined for person " + personId + ". " +
	                "Please make sure that requirements for emission vehicles in " +
	                EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
	    }
	
	    Vehicle vehicle = this.emissionVehicles.getVehicles().get(vehicleId);
	    VehicleType vehicleType = vehicle.getType();
	    vehicleInformation = vehicleType.getId().toString();
	    return vehicleInformation;
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		delegate.handleEvent(event);
	}
}