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

package playground.michalm.ev.discharging;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import playground.michalm.ev.data.*;

/**
 * Because in QSim and JDEQSim vehicles enter and leave traffic at the end of links, we skip the first link when
 * calculating the drive-related energy consumption. However, the time spent on the first link is used by the time-based
 * aux discharge process (see {@link playground.michalm.ev.discharging.AuxDischargingHandler}).
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
	private final Map<Id<Vehicle>, ? extends ElectricVehicle> eVehicles;
	private final Map<Id<Vehicle>, EVDrive> evDrives;

	@Inject
	public DriveDischargingHandler(EvData data, Network network) {
		this.eVehicles = data.getElectricVehicles();
		this.network = network;

		// at least 10% of vehicles are driving during peak
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
			double energy = evDrive.ev.getDriveEnergyConsumption().calcEnergyConsumption(link, tt);
			evDrive.ev.getBattery().discharge(energy);
		}
		return evDrive;
	}

	@Override
	public void reset(int iteration) {
		evDrives.clear();
	}
}
