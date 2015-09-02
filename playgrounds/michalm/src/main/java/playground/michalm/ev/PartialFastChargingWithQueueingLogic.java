/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.michalm.ev;

import java.util.*;

import com.google.common.collect.Iterables;


/**
 * Fast charging up to 80% of the battery capacity
 */
public class PartialFastChargingWithQueueingLogic
    implements ChargingLogic
{
    private static final double MAX_RELATIVE_SOC = 0.8;

    private final Charger charger;
    private final Queue<ElectricVehicle> vehicles = new LinkedList<>();
    private final Iterable<ElectricVehicle> pluggedVehicles;


    public PartialFastChargingWithQueueingLogic(Charger charger)
    {
        this.charger = charger;
        pluggedVehicles = Iterables.limit(vehicles, charger.getCapacity());
        charger.setLogic(this);
    }


    public Charger getCharger()
    {
        return charger;
    }


    public void addVehicle(ElectricVehicle vehicle)
    {
        vehicles.add(vehicle);
    }


    public void removeVehicle(ElectricVehicle vehicle)
    {
        vehicles.remove(vehicle);
    }


    @Override
    public void chargeVehicles(double chargeTime)
    {
        double energy = charger.getPower() * chargeTime;
        for (ElectricVehicle v : pluggedVehicles) {
            //we charge around 4% of SOC per minute, so when updating SOC every 10 seconds,
            //SOC will never reach 81% 
            Battery b = v.getBattery();
            b.charge(energy);

            if (b.getSoc() >= MAX_RELATIVE_SOC * b.getCapacity()) {
                removeVehicle(v);
            }
        }
    }


    public double getEnergyToCharge(ElectricVehicle vehicle)
    {
        Battery b = vehicle.getBattery();
        return Math.max(0, MAX_RELATIVE_SOC * b.getCapacity() - b.getSoc());
    }


    public double estimateMaxWaitTime(ChargerImpl charger)
    {
        if (vehicles.size() < charger.getCapacity()) {
            return 0;
        }

        double energyToCharge = 0;
        for (ElectricVehicle v : vehicles) {
            Battery b = v.getBattery();
            energyToCharge += MAX_RELATIVE_SOC * b.getCapacity() - b.getSoc();
        }

        return energyToCharge / charger.getPower() / charger.getCapacity();
    }
}
