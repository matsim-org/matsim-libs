/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.ev.charging;/*
 * created by jbischoff, 09.10.2018
 *  This is an events based approach to trigger vehicle charging. Vehicles will be charged as soon as a person begins a charging activity.
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.ev.data.Charger;
import org.matsim.contrib.ev.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.ev.data.ElectricFleet;
import org.matsim.contrib.ev.ev.data.ElectricVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class VehicleChargingHandler implements ActivityStartEventHandler, ActivityEndEventHandler, PersonLeavesVehicleEventHandler, ChargingEndEventHandler {

    public static final String CHARGING_IDENTIFIER = " charging";
    private Map<Id<Person>, Id<Vehicle>> lastVehicleUsed = new HashMap<>();
    private Map<Id<ElectricVehicle>, Id<Charger>> vehiclesAtChargers = new HashMap<>();

    private final ChargingInfrastructure chargingInfrastructure;

    private final ElectricFleet electricFleet;

    @Inject
    public VehicleChargingHandler(ChargingInfrastructure chargingInfrastructure, ElectricFleet electricFleet, EventsManager events) {
        this.chargingInfrastructure = chargingInfrastructure;
        this.electricFleet = electricFleet;
        events.addHandler(this);
    }


    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().endsWith(CHARGING_IDENTIFIER)) {
            Id<Vehicle> vehicleId = lastVehicleUsed.get(event.getPersonId());
            if (vehicleId != null) {
                Id<ElectricVehicle> evId = Id.create(vehicleId, ElectricVehicle.class);
                if (electricFleet.getElectricVehicles().containsKey(evId)) {
                    ElectricVehicle ev = electricFleet.getElectricVehicles().get(evId);
                    Map<Id<Charger>, Charger> chargers = chargingInfrastructure.getChargersAtLink(event.getLinkId());
                    Charger c = chargers.values().stream().filter(ch -> ev.getChargingTypes().contains(ch.getChargerType())).findAny().get(); //this assumes no liability which charger is used, as long as the type matches.
                    c.getLogic().addVehicle(ev, event.getTime());
                    vehiclesAtChargers.put(evId, c.getId());
                }
            }
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().endsWith(CHARGING_IDENTIFIER)) {
            Id<Vehicle> vehicleId = lastVehicleUsed.get(event.getPersonId());
            if (vehicleId != null) {
                Id<ElectricVehicle> evId = Id.create(vehicleId, ElectricVehicle.class);
                Id<Charger> chargerId = vehiclesAtChargers.remove(evId);
                if (chargerId != null) {
                    Charger c = chargingInfrastructure.getChargers().get(chargerId);
                    c.getLogic().removeVehicle(electricFleet.getElectricVehicles().get(evId), event.getTime());

                }
            }
        }
    }

    @Override
    public void reset(int iteration) {
        lastVehicleUsed.clear();
        vehiclesAtChargers.clear();
    }


    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        lastVehicleUsed.put(event.getPersonId(), event.getVehicleId());
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        vehiclesAtChargers.remove(event.getVehicleId());
        //Charging has ended before activity ends
    }
}
