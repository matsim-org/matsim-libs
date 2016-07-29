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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.jbischoff.energy.charging.taxi.TaxiCharger;
import playground.jbischoff.energy.log.ChargeLogRow;
import playground.jbischoff.energy.log.ChargerLog;
import playground.jbischoff.energy.log.SoCLog;
import playground.jbischoff.energy.log.SocLogRow;
import playground.jbischoff.taxi.vehicles.ElectricTaxi;

public class ChargingHandler implements  PersonDepartureEventHandler,
PersonArrivalEventHandler, MobsimEngine
{
    
    private Map<Id<Link>,HashMap<Id<Charger>,ChargerImpl>> chargerRegister;
    private Map<Id<Vehicle>,Vehicle> vehicleRegister;
    private Map<Id<Vehicle>,Id<Charger>> chargingRegister;
    private SoCLog soCLog;
    private ChargerLog chargerLog; 
    
    public ChargingHandler()
    {
     
     chargerRegister = new HashMap<>();
     vehicleRegister = new HashMap<>();
     chargingRegister = new HashMap<>();
     this.soCLog = new SoCLog();
     this.chargerLog = new ChargerLog();
    }

    @Override
    public void reset(int iteration)
    {
        
    }

    @Override
    public void handleEvent(PersonArrivalEvent event)
    {
     if (!event.getLegMode().equals(TransportMode.car)) return;
     Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
     if (!this.vehicleRegister.containsKey(vid)) return;
     if (!this.chargerRegister.containsKey(event.getLinkId())) return;
     for (ChargerImpl c : this.chargerRegister.get(event.getLinkId()).values()){
         if (c.addToCharger(this.vehicleRegister.get(vid))){
            this.chargingRegister.put(vid, c.getId());
             break;
         }
         
     }
     
    }

    @Override
    public void handleEvent(PersonDepartureEvent event)
    {
        if (!event.getLegMode().equals("car")) return;
        Id<Vehicle> vid = Id.create(event.getPersonId(), Vehicle.class);
        if (!chargingRegister.containsKey(vid)) return;  
        
    }

    
    public void addCharger(ChargerImpl c){
        if (! this.chargerRegister.containsKey(c.getLinkId())) this.chargerRegister.put(c.getLinkId(),new HashMap<Id<Charger>,ChargerImpl>());
        if (this.chargerRegister.get(c.getLinkId()).containsKey(c.getId())) throw new IllegalStateException("Charger Id "+c.getId()+" does already exist at Link "+c.getLinkId());
        chargerRegister.get(c.getLinkId()).put(c.getId(), c);
    }
    
    public void addVehicle(VehicleWithBattery bev){
        this.vehicleRegister.put(bev.getId(), bev);
    }
    
    public void refreshLog(double time)
    {
        List<Double> currentSoc = new ArrayList<Double>();
        
        for (Vehicle bev : this.vehicleRegister.values()) {
            if (! (bev instanceof BatteryElectricVehicle)) continue;
            double soc = ((VehicleWithBattery)bev).getSocInJoules();
            double rsoc = soc / ((VehicleWithBattery)bev).getUsableBatteryCapacityInJoules();
            this.soCLog.add(new SocLogRow(bev.getId(), time, soc, rsoc));
            currentSoc.add(soc);
        }
        if (currentSoc.size() > 0) {
            this.soCLog.add(new SocLogRow(Id.create("max",ElectricTaxi.class), time, Collections.max(currentSoc), 0));
            this.soCLog.add(new SocLogRow(Id.create("min",ElectricTaxi.class), time, Collections.min(currentSoc), 0));
            double socs = 0;
            for (Double d : currentSoc) {
                socs += d;
            }
            double average = socs / currentSoc.size();
            this.soCLog.add(new SocLogRow(Id.create("ave",ElectricTaxi.class), time, average, 0));
            Collections.sort(currentSoc);
            int p05 = (int)Math.floor(currentSoc.size() * 0.05);
            double p05v = currentSoc.get(p05);

            double worst5pc = 0.;
            for (int i = 0; i <= p05; i++) {
                worst5pc += currentSoc.get(i);

            }
            worst5pc = worst5pc / p05;

            this.soCLog.add(new SocLogRow(Id.create("p05",ElectricTaxi.class), time, p05v, 0));
            this.soCLog.add(new SocLogRow(Id.create("w05av",ElectricTaxi.class), time, worst5pc, 0));

        }
        for (HashMap<Id<Charger>, ChargerImpl> l : this.chargerRegister.values()) {
            for (ChargerImpl c : l.values()){
            int occ = c.getCurrentOccupation();
            double rocc = occ / c.getCapacity();
            this.chargerLog.add(new ChargeLogRow(c.getId(), time, occ, rocc));
        }
    }
    }

    @Override
    public void doSimStep(double time)
    {
        if (time % 60.0 == 0){
            
            for (HashMap<Id<Charger>, ChargerImpl> l : this.chargerRegister.values()) {
                for (ChargerImpl c : l.values()){
                    c.chargeVehicles(60.0);
                }}
            refreshLog(time);
        }
    }

    @Override
    public void onPrepareSim()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void afterSim()
    {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setInternalInterface(InternalInterface internalInterface)
    {
        // TODO Auto-generated method stub
        
    }

    public SoCLog getSoCLog()
    {
        return soCLog;
    }

    public ChargerLog getChargerLog()
    {
        return chargerLog;
    }

    public void addVehicles(HashMap<Id<Vehicle>, Vehicle> bevs)
    {
        this.vehicleRegister.putAll(bevs);
    }
    
    

}
