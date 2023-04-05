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

package org.matsim.contrib.ev.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/*
 * created by jbischoff, 26.10.2018
 */
public class ChargerPowerCollector implements ChargingStartEventHandler, ChargingEndEventHandler, MobsimScopeEventHandler {

	private final ChargingInfrastructure chargingInfrastructure;
	private final ElectricFleet fleet;

	private record TimeCharge(double time, double charge) {
	}

	private final Map<Id<Vehicle>, TimeCharge> chargeBeginCharge = new HashMap<>();

	public record ChargingLogEntry(double chargeStart, double chargeEnd, Charger charger, double transmitted_Energy, Id<Vehicle> vehicleId) {
	}

	private final List<ChargingLogEntry> logList = new ArrayList<>();

	@Inject
	public ChargerPowerCollector(ElectricFleet fleet, ChargingInfrastructure chargingInfrastructure) {
		this.fleet = fleet;
		this.chargingInfrastructure = chargingInfrastructure;
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		var chargeStart = chargeBeginCharge.remove(event.getVehicleId());
		Preconditions.checkNotNull(chargeStart, "%s has never started charging", event.getVehicleId());

		double energy = fleet.getElectricVehicles().get(event.getVehicleId()).getBattery().getCharge() - chargeStart.charge;
		ChargingLogEntry loge = new ChargingLogEntry(chargeStart.time, event.getTime(),
				chargingInfrastructure.getChargers().get(event.getChargerId()), energy, event.getVehicleId());
		logList.add(loge);
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		ElectricVehicle ev = fleet.getElectricVehicles().get(event.getVehicleId());
		Preconditions.checkNotNull(ev, "%s is not in the EV fleet", event.getVehicleId());
		chargeBeginCharge.put(event.getVehicleId(), new TimeCharge(event.getTime(), ev.getBattery().getCharge()));
	}

	public List<ChargingLogEntry> getLogList() {
		return logList;
	}
}
