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

import playground.michalm.ev.charging.FixedSpeedChargingWithQueueingLogic;
import playground.michalm.ev.data.*;
import playground.michalm.taxi.data.EvrpVehicle;
import playground.michalm.taxi.data.EvrpVehicle.Ev;
import playground.michalm.taxi.vrpagent.ETaxiAtChargerActivity;


/**
 * Fast charging up to 80% of the battery capacity
 */
public class ETaxiChargingWithQueueingLogic
    extends FixedSpeedChargingWithQueueingLogic
{
    private static final double MAX_RELATIVE_SOC = 0.8;
    private List<ElectricVehicle> evsToUnplug = new ArrayList<>();


    public ETaxiChargingWithQueueingLogic(Charger charger)
    {
        super(charger);
    }


    @Override
    public void chargeVehicles(double chargePeriod)
    {
        super.chargeVehicles(chargePeriod);

        for (ElectricVehicle ev : evsToUnplug) {
            unplugVehicle(ev);
        }
        evsToUnplug.clear();

        int fromQueuedToPluggedCount = Math.min(queuedVehicles.size(),
                charger.getCapacity() - pluggedVehicles.size());
        for (int i = 0; i < fromQueuedToPluggedCount; i++) {
            plugVehicle(queuedVehicles.poll());
        }
    }


    @Override
    protected void chargeVehicle(ElectricVehicle ev, double chargePeriod)
    {
        super.chargeVehicle(ev, chargePeriod);

        Battery b = ev.getBattery();
        if (b.getSoc() >= MAX_RELATIVE_SOC * b.getCapacity()) {
            evsToUnplug.add(ev);
        }
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


    //============= ????

    public double getEnergyToCharge(ElectricVehicle vehicle)
    {
        Battery b = vehicle.getBattery();
        return Math.max(0, MAX_RELATIVE_SOC * b.getCapacity() - b.getSoc());
    }


    public double estimateChargeTime(ElectricVehicle vehicle)
    {
        return getEnergyToCharge(vehicle) / charger.getPower();
    }


    public double estimateMaxWaitTime()
    {
        if (queuedVehicles.size() < charger.getCapacity()) {
            return 0;
        }

        double energyToCharge = 0;
        for (ElectricVehicle ev : allVehicles) {
            energyToCharge += getEnergyToCharge(ev);
        }

        return energyToCharge / charger.getPower() / charger.getCapacity();
    }
}
