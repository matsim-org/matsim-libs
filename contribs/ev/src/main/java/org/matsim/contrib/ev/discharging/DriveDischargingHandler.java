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

package org.matsim.contrib.ev.discharging;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * Because in QSim and JDEQSim vehicles enter and leave traffic at the end of links, we skip the first link when
 * calculating the drive-related energy consumption. However, the time spent on the first link is used by the time-based
 * aux discharge process (see {@link AuxDischargingHandler}).
 */
public class DriveDischargingHandler
		implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private static class EVDrive {
		private final Id<Vehicle> vehicleId;
		private final ElectricVehicle ev;
		private double movedOverNodeTime;

		public EVDrive(Id<Vehicle> vehicleId, ElectricVehicle ev) {
			this.vehicleId = vehicleId;
			this.ev = ev;
			movedOverNodeTime = Double.NaN;
		}

		private boolean isOnFirstLink() {
			return Double.isNaN(movedOverNodeTime);
		}
	}

	private final Network network;
	private final Map<Id<ElectricVehicle>, ? extends ElectricVehicle> eVehicles;
	private final boolean handleAuxDischarging;
	private final Map<Id<Vehicle>, EVDrive> evDrives;
    private Map<Id<Link>, Double> energyConsumptionPerLink = new HashMap<>();

	@Inject
	public DriveDischargingHandler(ElectricFleet data, Network network, EvConfigGroup evCfg) {
		this.network = network;
		eVehicles = data.getElectricVehicles();
		handleAuxDischarging = evCfg
                .getAuxDischargingSimulation() == EvConfigGroup.AuxDischargingSimulation.insideDriveDischargingHandler;
		evDrives = new HashMap<>(eVehicles.size() / 10);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		ElectricVehicle ev = eVehicles.get(vehicleId);
		if (ev != null) {// handle only our EVs
			evDrives.put(vehicleId, new EVDrive(vehicleId, ev));
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		EVDrive evDrive = dischargeVehicle(event.getVehicleId(), event.getLinkId(), event.getTime());
		if (evDrive != null) {
			evDrive.movedOverNodeTime = event.getTime();
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		EVDrive evDrive = dischargeVehicle(event.getVehicleId(), event.getLinkId(), event.getTime());
		if (evDrive != null) {
			evDrives.remove(evDrive.vehicleId);
		}
	}

	private EVDrive dischargeVehicle(Id<Vehicle> vehicleId, Id<Link> linkId, double eventTime) {
		EVDrive evDrive = evDrives.get(vehicleId);
		if (evDrive != null && !evDrive.isOnFirstLink()) {// handle only our EVs, except for the first link
			Link link = network.getLinks().get(linkId);
			double tt = eventTime - evDrive.movedOverNodeTime;
			ElectricVehicle ev = evDrive.ev;
			double energy = ev.getDriveEnergyConsumption().calcEnergyConsumption(link, tt);
			if (handleAuxDischarging) {
				energy += ev.getAuxEnergyConsumption().calcEnergyConsumption(tt);
			}
			ev.getBattery().discharge(energy);
            double linkConsumption = energy + energyConsumptionPerLink.getOrDefault(linkId, 0.0);
            energyConsumptionPerLink.put(linkId, linkConsumption);
		}
		return evDrive;
	}

	@Override
    public void reset(int iteration) {
        evDrives.clear();
        energyConsumptionPerLink.clear();
    }

    public Map<Id<Link>, Double> getEnergyConsumptionPerLink() {
        return energyConsumptionPerLink;
    }
}
