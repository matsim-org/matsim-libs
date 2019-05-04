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

package electric.edrt.energyconsumption;/*
 * created by jbischoff, 12.01.2019
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.contrib.ev.data.ChargingInfrastructure;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.api.experimental.events.EventsManager;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks vehicles that park at charger locations. These will not have any AUX consumption while parked.
 * A vehicle is assumed to be at a charger until it first leaves one.
 * Deliberately does not use Charging Events
 */
public class VehicleAtChargerLinkTracker implements VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {


    private final ChargingInfrastructure chargingInfrastructure;
    private final ElectricFleet evFleet;
    private final Set<Id<ElectricVehicle>> evsAtChargers = new HashSet<>();

    @Inject
    public VehicleAtChargerLinkTracker(ChargingInfrastructure chargingInfrastructure, EventsManager events, ElectricFleet evFleet) {
        this.chargingInfrastructure = chargingInfrastructure;
        events.addHandler(this);
        this.evFleet = evFleet;
    }

    @Override
    public void reset(int iteration) {
        evsAtChargers.clear();
        evsAtChargers.addAll(evFleet.getElectricVehicles().keySet());

    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        Id<ElectricVehicle> evId = Id.create(event.getVehicleId(), ElectricVehicle.class);
        evsAtChargers.remove(evId);
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if (chargingInfrastructure.getChargersAtLink(event.getLinkId()) != null) {
            Id<ElectricVehicle> evId = Id.create(event.getVehicleId(), ElectricVehicle.class);
            if (evFleet.getElectricVehicles().containsKey(evId)) {
                evsAtChargers.add(evId);
            }
        }
    }

    public boolean isAtCharger(ElectricVehicle electricVehicle) {

        return evsAtChargers.contains(electricVehicle.getId());
    }
}
