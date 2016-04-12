/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.energy.charging;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;




public class ChargerImpl
    implements Charger
{
    private final Id<Charger> chargerId;
    private final int powerInKW;
    private final int capacity;
    private List<BatteryElectricVehicle> chargingVehicles = new ArrayList<>();
    private Id<Link> linkId;

    public ChargerImpl(Id<Charger> chargerId, Id<Link> linkId, int powerInKW, int capacity){
        this.chargerId = chargerId;
        this.linkId = linkId;
        this.powerInKW = powerInKW;
        this.capacity = capacity;
    }

    @Override
    public int getPowerInKW()
    {
        return this.powerInKW;
        
    }

    @Override
    public int getCapacity()
    {
        return capacity;
    }

    @Override
    public Id<Charger> getId()
    {
        return this.chargerId;
    }

    @Override
    public void chargeVehicles(double chargeTime)
    {
        double chargeInJoule = chargeTime * this.powerInKW*1000;
        
        for (BatteryElectricVehicle bev : this.chargingVehicles){
            double actualCharge = chargeInJoule;
            double maximumSOC =  bev.getUsableBatteryCapacityInJoules();
            if ( chargeInJoule+bev.getSocInJoules() > maximumSOC ) actualCharge = maximumSOC-bev.getSocInJoules();
            bev.chargeVehicle(actualCharge);
            
        }   
    }
    
    public boolean addToCharger(Vehicle vehicle){
        if (! (vehicle instanceof BatteryElectricVehicle)) return false;
        if (this.chargingVehicles.size() >= this.capacity) return false;
        else {
            this.chargingVehicles.add((BatteryElectricVehicle)vehicle);
            System.out.println("plugin " + vehicle.getId().toString());
            return true;
        }
        
    }
    
    public boolean removeFromCharger(BatteryElectricVehicle bev){
        if (this.chargingVehicles.contains(bev)){
            this.chargingVehicles.remove(bev);
            return true;
        }
        else return false;
        
    }

    @Override
    public Id<Link> getLinkId()
    {
        return this.linkId;
    }

    public int getCurrentOccupation()
    {
        return this.chargingVehicles.size();
    }


}
