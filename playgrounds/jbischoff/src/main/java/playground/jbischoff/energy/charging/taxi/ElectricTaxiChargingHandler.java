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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.jbischoff.energy.log.ChargeLogRow;
import playground.jbischoff.energy.log.ChargerLog;
import playground.jbischoff.energy.log.SoCLog;
import playground.jbischoff.energy.log.SocLogRow;
import playground.jbischoff.taxi.vehicles.ElectricTaxi;


public class ElectricTaxiChargingHandler
    implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler,
    PersonArrivalEventHandler

{

    private Map<Id<TaxiCharger>, TaxiCharger> chargers;
    private Map<Id<ElectricTaxi>, ElectricTaxi> vehicles;
    private Map<Id<ElectricTaxi>, ElectricTaxi> activeVehicles;
    private SoCLog soCLog;
    private ChargerLog chargerLog;
    private long underChargeMinutes = 0;
    private static final Logger log = Logger.getLogger(ElectricTaxiChargingHandler.class);
    private EventsManager events;


    public ElectricTaxiChargingHandler(EventsManager events)
    {
        this.vehicles = new HashMap<>();
        this.activeVehicles = new HashMap<>();
        this.chargers = new HashMap<>();
        this.soCLog = new SoCLog();
        this.chargerLog = new ChargerLog();
        this.events = events;
    }


    @Override
    public void reset(int iteration)
    {

    }


    public void doSimStep(double time)
    {
        if (time % 60 == 0) {
            for (TaxiCharger charger : this.chargers.values()) {
                charger.chargeVehicles(60);

            }

            refreshLog(time);

        }

    }


    @Override
    public void handleEvent(PersonArrivalEvent event)
    {
        Id<ElectricTaxi> taxiId = Id.create(event.getPersonId(),ElectricTaxi.class);
        if (!isActiveVehicle(taxiId))
            return;
        if (!isAtCharger(event.getLinkId()))
            return;
        if (this.getRelativeTaxiSoC(taxiId) >= 0.8 ) return;
        this.chargers.get(event.getLinkId()).chargeOrQueueForCharging(
                this.activeVehicles.get(event.getPersonId()));
        this.events.processEvent(new ActivityStartEvent(event.getTime(), event.getPersonId(), event
                .getLinkId(), null, "Charging"));

    }


    @Override
    public void handleEvent(PersonDepartureEvent event)
    {
        if (!isActiveVehicle(Id.create(event.getPersonId(),ElectricTaxi.class)))
            return;
        if (!isAtCharger(event.getLinkId()))
            return;
        if (this.chargers.get(event.getLinkId()).removeFromCharger(
                this.activeVehicles.get(event.getPersonId())))
            this.events.processEvent(new ActivityEndEvent(event.getTime(), event.getPersonId(),
                    event.getLinkId(), null, "Charging"));
        ;
    }


    @Override
    public void handleEvent(ActivityEndEvent event)
    {      
        Id<ElectricTaxi> taxiid =  Id.create(event.getPersonId(),ElectricTaxi.class);
        if (!this.vehicles.containsKey(taxiid))
            return;
        if (event.getActType().startsWith("Before schedule:")) {
            this.activeVehicles.put(taxiid, this.vehicles.get(event.getPersonId()));
        }

    }


    @Override
    public void handleEvent(ActivityStartEvent event)
    {
        Id<ElectricTaxi> taxiid =  Id.create(event.getPersonId(),ElectricTaxi.class);

        if (!this.activeVehicles.containsKey(taxiid))
            return;
        if (event.getActType().startsWith("After schedule:")){
            if (isAtCharger(event.getLinkId())){
                this.chargers.get(event.getLinkId()).removeFromCharger(this.activeVehicles.get(event.getPersonId()));
            }
            this.activeVehicles.remove(taxiid);
            }
            

    }


    public void addCharger(TaxiCharger charger)
    {
        this.chargers.put(Id.create(charger.getChargerLinkId(),TaxiCharger.class), charger);

    }


    public void addVehicle(ElectricTaxi taxi)
    {
        this.vehicles.put(taxi.getVehicleId(), taxi);
    }


    public boolean isAtCharger(Id<Link> linkId)
    {
        return this.chargers.containsKey(Id.create(linkId,TaxiCharger.class));
    }


    private boolean isActiveVehicle(Id<ElectricTaxi> taxiId)
    {
        return this.activeVehicles.containsKey(taxiId);
    }


    public void refreshLog(double time)
    {
        List<Double> currentSoc = new ArrayList<Double>();

        for (ElectricTaxi et : this.activeVehicles.values()) {
            if (!et.isElectric())
                continue;
            double soc = et.getBev().getSocInJoules();
            double rsoc = soc / et.getBev().getUsableBatteryCapacityInJoules();
            this.soCLog.add(new SocLogRow(et.getVehicleId(), time, soc, rsoc));
            if (rsoc < 0.1)
                this.underChargeMinutes++;
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
        for (TaxiCharger c : this.chargers.values()) {
            int occ = c.getCurrentOccupation();
            double rocc = occ / c.getCapacity();
            this.chargerLog.add(new ChargeLogRow(c.getChargerLinkId(), time, occ, rocc));
        }
    }


    public SoCLog getSoCLog()
    {
        log.info("Undercharge minutes in total: " + this.underChargeMinutes);
        return soCLog;
    }


    public Map<Id<ElectricTaxi>, ElectricTaxi> getVehicles()
    {
        return vehicles;
    }


    public ChargerLog getChargerLog()
    {
        return chargerLog;
    }
    public Map<Id<TaxiCharger>, TaxiCharger> getChargers()
    {
        return chargers;
    }
    
    public double getRelativeTaxiSoC(Id<ElectricTaxi> vid){
        double rsoc = 1.;
        if (this.vehicles.containsKey(vid)){
            ElectricTaxi taxi = this.vehicles.get(vid);
            if (taxi.isElectric()){
                rsoc = taxi.getBev().getSocInJoules() / taxi.getBev().getUsableBatteryCapacityInJoules();
            }
        }
        return rsoc;
    }
    
}
