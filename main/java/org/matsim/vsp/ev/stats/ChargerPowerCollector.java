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

package org.matsim.vsp.ev.stats;/*
 * created by jbischoff, 26.10.2018
 */


import com.google.inject.Inject;
import javafx.util.Pair;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.Time;
import org.matsim.vsp.ev.EvUnitConversions;
import org.matsim.vsp.ev.charging.ChargingEndEvent;
import org.matsim.vsp.ev.charging.ChargingEndEventHandler;
import org.matsim.vsp.ev.charging.ChargingStartEvent;
import org.matsim.vsp.ev.charging.ChargingStartEventHandler;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.data.ElectricFleet;
import org.matsim.vsp.ev.data.ElectricVehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChargerPowerCollector implements ChargingEndEventHandler, ChargingStartEventHandler {


    private final ChargingInfrastructure chargingInfrastructure;
    private final ElectricFleet fleet;
    private Map<Id<ElectricVehicle>, Pair<Double, Double>> chargeBeginSoc = new HashMap<>();
    private List<ChargingLogEntry> logList = new ArrayList<>();

    @Inject
    public ChargerPowerCollector(ElectricFleet fleet, ChargingInfrastructure chargingInfrastructure, EventsManager events) {
        events.addHandler(this);
        this.fleet = fleet;
        this.chargingInfrastructure = chargingInfrastructure;
    }

    @Override
    public void handleEvent(ChargingEndEvent event) {
        Pair<Double, Double> chargeStart = chargeBeginSoc.remove(event.getVehicleId());
        if (chargeStart != null) {
            double energy = this.fleet.getElectricVehicles().get(event.getVehicleId()).getBattery().getSoc() - chargeStart.getValue();
            ChargingLogEntry loge = new ChargingLogEntry(chargeStart.getKey(), event.getTime(), chargingInfrastructure.getChargers().get(event.getChargerId()), energy, event.getVehicleId());
            logList.add(loge);
        } else throw new NullPointerException(event.getVehicleId().toString() + " has never started charging");
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        ElectricVehicle ev = this.fleet.getElectricVehicles().get(event.getVehicleId());
        if (ev != null) {
            this.chargeBeginSoc.put(event.getVehicleId(), new Pair<>(event.getTime(), ev.getBattery().getSoc()));
        } else throw new NullPointerException(event.getVehicleId().toString() + " is not in list");

    }

    @Override
    public void reset(int iteration) {
        chargeBeginSoc.clear();
        logList.clear();
    }

    public List<ChargingLogEntry> getLogList() {
        return logList;
    }

    static class ChargingLogEntry implements Comparable<ChargingLogEntry> {
        private final double chargeStart;
        private final double chargeEnd;
        private final Charger charger;
        private final double transmitted_Energy;
        private final Id<ElectricVehicle> vehicleId;
        static final String HEADER = "chargerId;chargingStart;chargingEnd;chargingDuration;chargerX;chargerY;vehicleId;transmittedEnergy_kWh";

        public ChargingLogEntry(double chargeStart, double chargeEnd, Charger charger, double transmitted_Energy, Id<ElectricVehicle> vehicleId) {
            this.chargeStart = chargeStart;
            this.chargeEnd = chargeEnd;
            this.charger = charger;
            this.transmitted_Energy = transmitted_Energy;
            this.vehicleId = vehicleId;
        }

        public double getChargeStart() {
            return chargeStart;
        }

        public double getChargeEnd() {
            return chargeEnd;
        }

        public Charger getCharger() {
            return charger;
        }

        public double getTransmitted_Energy() {
            return transmitted_Energy;
        }

        @Override
        public String toString() {
            double energyKWh = Math.round(transmitted_Energy / EvUnitConversions.J_PER_kWh * 10.) / 10.;
            return charger.getId().toString() + ";" + Time.writeTime(chargeStart) + ";" + Time.writeTime(chargeEnd) + ";" + Time.writeTime(chargeEnd - chargeStart)
                    + ";" + charger.getCoord().getX() + ";" + charger.getCoord().getY() + ";" + vehicleId.toString() + ";" + energyKWh;
        }

        @Override
        public int compareTo(ChargingLogEntry o) {
            return Double.valueOf(chargeStart).compareTo(o.chargeStart);
        }
    }
}
