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

package playground.michalm.ev.charging;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import com.google.common.collect.*;

import playground.michalm.ev.data.*;


public class FixedSpeedChargingWithQueueingLogic
    implements ChargingLogic
{
    protected final Charger charger;
    protected final Map<Id<Vehicle>, ElectricVehicle> pluggedVehicles;
    protected final Queue<ElectricVehicle> queuedVehicles = new LinkedList<>();
    protected final Iterable<ElectricVehicle> allVehicles;


    public FixedSpeedChargingWithQueueingLogic(Charger charger)
    {
        this.charger = charger;
        pluggedVehicles = Maps.newHashMapWithExpectedSize(charger.getCapacity());
        allVehicles = Iterables.concat(pluggedVehicles.values(), queuedVehicles);
        charger.setLogic(this);
    }


    @Override
    public void chargeVehicles(double chargePeriod)
    {
        for (ElectricVehicle ev : pluggedVehicles.values()) {
            //we charge around 4% of SOC per minute, so when updating SOC every 10 seconds,
            //SOC will never reach 81% 
            chargeVehicle(ev, chargePeriod);
        }
    }


    protected void chargeVehicle(ElectricVehicle ev, double chargePeriod)
    {
        Battery b = ev.getBattery();
        double energy = charger.getPower() * chargePeriod;
        double freeCapacity = b.getCapacity() - b.getSoc();
        b.charge(Math.min(energy, freeCapacity));
    }


    @Override
    public void addVehicle(ElectricVehicle vehicle)
    {
        if (pluggedVehicles.size() < charger.getCapacity()) {
            plugVehicle(vehicle);
        }
        else {
            queuedVehicles.add(vehicle);
        }
    }


    @Override
    public void removeVehicle(ElectricVehicle vehicle)
    {
        if (pluggedVehicles.remove(vehicle.getId()) != null) {//successfully removed
            notifyChargingEnded(vehicle);

            if (!queuedVehicles.isEmpty()) {
                plugVehicle(queuedVehicles.poll());
            }
        }
        else if (!queuedVehicles.remove(vehicle)) {//neither plugged nor queued
            throw new IllegalArgumentException("Vehicle: " + vehicle.getId()
                    + " is neither queued nor plugged at charger: " + charger.getId());
        }
    }


    protected void plugVehicle(ElectricVehicle vehicle)
    {
        pluggedVehicles.put(vehicle.getId(), vehicle);
        notifyChargingStarted(vehicle);
    }


    protected void unplugVehicle(ElectricVehicle vehicle)
    {
        pluggedVehicles.remove(vehicle.getId());
        notifyChargingEnded(vehicle);
    }


    //meant for overriding
    protected void notifyChargingStarted(ElectricVehicle vehicle)
    {}


    //meant for overriding
    protected void notifyChargingEnded(ElectricVehicle vehicle)
    {}


    @Override
    public boolean isPlugged(ElectricVehicle ev)
    {
        return pluggedVehicles.containsKey(ev.getId());
    }


    @Override
    public Charger getCharger()
    {
        return charger;
    }


    @Override
    public void reset()
    {
        queuedVehicles.clear();
        pluggedVehicles.clear();
    }
}
