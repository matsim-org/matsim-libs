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

package playground.jbischoff.energy.charging.taxi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.jbischoff.energy.charging.Charger;
import playground.jbischoff.taxi.vehicles.ElectricTaxi;


public class TaxiCharger implements Charger{

private final Id<Link> chargerLinkId;   
private final int CAPACITY;
private int powerInKW;
private List<ElectricTaxi> chargingVehicles;
private Queue<ElectricTaxi>  waitingVehicles;
private double maxsoc = 0.8;



public TaxiCharger(int capacity, int powerInkW, Id<Link> chargerLinkId)
{
    this.CAPACITY = capacity;
    this.powerInKW = powerInkW;
    this.chargerLinkId = chargerLinkId;
    
    this.chargingVehicles = new ArrayList<ElectricTaxi>();
    this.waitingVehicles = new LinkedList<ElectricTaxi>();
}

public int getPowerInKW()
{
    return powerInKW;
}

public void setPowerInKW(int powerInKW)
{
    this.powerInKW = powerInKW;
}

public int getCurrentOccupation()
{
    return chargingVehicles.size();
}

public Id<Link> getChargerLinkId()
{
    return chargerLinkId;
} 


public int getCapacity()
{
    return CAPACITY;
}

public boolean chargeOrQueueForCharging(ElectricTaxi taxi){
    if (!taxi.isElectric()) return false;
    if (this.getCurrentOccupation() < this.CAPACITY) 
        {
        this.chargingVehicles.add(taxi);
//        System.out.println("plugin" + taxi.getVehicleId());
        return true;
        }
    else {
        this.waitingVehicles.add(taxi);
//        System.out.println("wait" + taxi.getVehicleId());
        return false;
    }
}

public boolean removeFromCharger(ElectricTaxi taxi){
    if (this.chargingVehicles.contains(taxi)){
        this.chargingVehicles.remove(taxi);
//        System.out.println("unplug" + taxi.getVehicleId());
        addNextVehicleFromQueueToCharger();
        return true;
    }
    else if (this.waitingVehicles.contains(taxi)){
        this.waitingVehicles.remove(taxi);
        return true;
    }
    else return false;
    
}
private void addNextVehicleFromQueueToCharger(){
    ElectricTaxi taxi  = this.waitingVehicles.poll();
    if (taxi!=null) {
        this.chargingVehicles.add(taxi);
//        System.out.println("plugin" + taxi.getVehicleId());

    }
}

public void chargeVehicles(double chargeTime){
    
    double chargeInJoule = chargeTime * this.powerInKW*1000;
    
    for (ElectricTaxi taxi : this.chargingVehicles){
        double actualCharge = chargeInJoule;
        double maximumSOC =  maxsoc * taxi.getBev().getUsableBatteryCapacityInJoules();
        if ( chargeInJoule+taxi.getBev().getSocInJoules() > maximumSOC ) actualCharge = maximumSOC-taxi.getBev().getSocInJoules();
        taxi.getBev().chargeVehicle(actualCharge);
        
    }
}

@Override
public Id<Charger> getId()
{
    return (Id.create(this.chargerLinkId,Charger.class));
}

@Override
public Id<Link> getLinkId()
{
    return this.chargerLinkId;
}
public boolean hasCapacity(){
    if (this.chargingVehicles.size()<this.CAPACITY){
        return true;
    }
    else return false;
}


}
