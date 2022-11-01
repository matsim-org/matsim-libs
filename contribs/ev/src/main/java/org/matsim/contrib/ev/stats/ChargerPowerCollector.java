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

package org.matsim.contrib.ev.stats;/*
 * created by jbischoff, 26.10.2018
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructure;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

public class ChargerPowerCollector
		implements ChargingStartEventHandler, ChargingEndEventHandler, MobsimScopeEventHandler {

	private final ChargingInfrastructure chargingInfrastructure;
	private final ElectricFleet fleet;

	private record TimeCharge(double time, double charge) {
	}

	private final Map<Id<Vehicle>, TimeCharge> chargeBeginCharge = new HashMap<>();

	private final List<ChargingLogEntry> logList = new ArrayList<>();

	@Inject
	public ChargerPowerCollector(ElectricFleet fleet, ChargingInfrastructure chargingInfrastructure) {
		this.fleet = fleet;
		this.chargingInfrastructure = chargingInfrastructure;
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		var chargeStart = chargeBeginCharge.remove(event.getVehicleId());
		if (chargeStart != null) {
			double energy = this.fleet.getElectricVehicles().get(event.getVehicleId()).getBattery().getCharge()
					- chargeStart.charge;
			ChargingLogEntry loge = new ChargingLogEntry(chargeStart.time, event.getTime(),
					chargingInfrastructure.getChargers().get(event.getChargerId()), energy, event.getVehicleId());
			logList.add(loge);
		} else
			throw new NullPointerException(event.getVehicleId().toString() + " has never started charging");
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		ElectricVehicle ev = this.fleet.getElectricVehicles().get(event.getVehicleId());
		if (ev != null) {
			this.chargeBeginCharge.put(event.getVehicleId(),
					new TimeCharge(event.getTime(), ev.getBattery().getCharge()));
		} else
			throw new NullPointerException(event.getVehicleId().toString() + " is not in list");

	}

	public List<ChargingLogEntry> getLogList() {
		return logList;
	}

	public static class ChargingLogEntry implements Comparable<ChargingLogEntry> {
		private final double chargeStart;
		private final double chargeEnd;
		private final Charger charger;
		private final double transmitted_Energy;
		private final Id<Vehicle> vehicleId;
		static final String HEADER = "chargerId;chargingStart;chargingEnd;chargingDuration;chargerX;chargerY;vehicleId;transmittedEnergy_kWh";

		public ChargingLogEntry(double chargeStart, double chargeEnd, Charger charger, double transmitted_Energy,
				Id<Vehicle> vehicleId) {
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
			double energyKWh = Math.round(EvUnits.J_to_kWh(transmitted_Energy) * 10.) / 10.;
			return charger.getId().toString()
					+ ";"
					+ Time.writeTime(chargeStart)
					+ ";"
					+ Time.writeTime(chargeEnd)
					+ ";"
					+ Time.writeTime(chargeEnd - chargeStart)
					+ ";"
					+ charger.getCoord().getX()
					+ ";"
					+ charger.getCoord().getY()
					+ ";"
					+ vehicleId.toString()
					+ ";"
					+ energyKWh;
		}

		@Override
		public int compareTo(ChargingLogEntry o) {
			return Double.compare(chargeStart, o.chargeStart);
		}

		public Id<Vehicle> getVehicleId() {
			return vehicleId;
		}
	}
}
