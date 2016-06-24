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

package playground.michalm.taxi.ev;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

import playground.michalm.ev.charging.FixedSpeedChargingWithQueueingLogic;
import playground.michalm.ev.data.*;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.vrpagent.ETaxiAtChargerActivity;


public class ETaxiChargingLogic
    extends FixedSpeedChargingWithQueueingLogic
{
    //fast charging up to 80% of the battery capacity
    private static final double MAX_RELATIVE_SOC = 0.8;

    protected final Map<Id<Vehicle>, ElectricVehicle> dispatchedVehicles = new HashMap<>();
    private final double effectivePower;


    public ETaxiChargingLogic(Charger charger, double chargingSpeedFactor, double auxPower)
    {
        super(charger);
        effectivePower = charger.getPower() * chargingSpeedFactor - auxPower;
    }


    //at this point ETaxiChargingTask should point to Charger
    public void addDispatchedVehicle(ElectricVehicle vehicle)
    {
        dispatchedVehicles.put(vehicle.getId(), vehicle);
    }


    //on deleting ETaxiChargingTask or vehicle arrival (the veh becomes plugged or queued)
    public void removeDispatchedVehicle(ElectricVehicle vehicle)
    {
        if (dispatchedVehicles.remove(vehicle.getId()) == null) {
            throw new IllegalArgumentException();
        }
    }


    @Override
    protected boolean doStopCharging(ElectricVehicle ev)
    {
        Battery b = ev.getBattery();
        return b.getSoc() >= MAX_RELATIVE_SOC * b.getCapacity();
    }


    private ETaxiAtChargerActivity getActivity(ElectricVehicle vehicle)
    {
        EvrpVehicle evrpVehicle = ((Ev)vehicle).getEvrpVehicle();
        return (ETaxiAtChargerActivity)evrpVehicle.getAgentLogic().getDynAgent().getCurrentAction();
    }


    @Override
    protected void notifyChargingStarted(ElectricVehicle vehicle)
    {
        getActivity(vehicle).notifyChargingStarted();
    }


    @Override
    protected void notifyChargingEnded(ElectricVehicle vehicle)
    {
        getActivity(vehicle).notifyChargingEnded();
    }


    public double getEnergyToCharge(ElectricVehicle vehicle)
    {
        Battery b = vehicle.getBattery();
        return Math.max(0, MAX_RELATIVE_SOC * b.getCapacity() - b.getSoc());
    }


    public double estimateChargeTime(ElectricVehicle vehicle)
    {
        System.err.println("energy to charge" + getEnergyToCharge(vehicle));
        System.err.println("effectivePower = " + effectivePower);
        return getEnergyToCharge(vehicle) / effectivePower;
    }

    //TODO using task timing from schedules will be more accurate in predicting charge demand


    //does not include further demand (AUX for queued vehs)
    public double estimateMaxWaitTimeOnArrival()
    {
        if (pluggedVehicles.size() < charger.getPlugs()) {
            return 0;
        }

        double sum = sumEnergyToCharge(pluggedVehicles.values())
                + sumEnergyToCharge(queuedVehicles);
        return sum / effectivePower / charger.getPlugs();
    }


    //does not include further demand (AUX for queued vehs; AUX+driving for dispatched vehs)
    public double estimateAssignedWorkloadPerCharger()
    {
        double total = sumEnergyToCharge(pluggedVehicles.values())
                + sumEnergyToCharge(queuedVehicles)
                + sumEnergyToCharge(dispatchedVehicles.values());
        return total / effectivePower / charger.getPlugs();
    }


    private double sumEnergyToCharge(Iterable<ElectricVehicle> evs)
    {
        double energyToCharge = 0;
        for (ElectricVehicle ev : evs) {
            energyToCharge += getEnergyToCharge(ev);
        }
        return energyToCharge;
    }


    int getPluggedCount()
    {
        return pluggedVehicles.size();
    }


    int getQueuedCount()
    {
        return queuedVehicles.size();
    }


    int getDispatchedCount()
    {
        return dispatchedVehicles.size();
    }
}
