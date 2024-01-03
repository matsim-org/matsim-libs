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

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.fleet.ElectricFleet;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * Because in QSim vehicles enter and leave traffic at the end of links, we skip the first link when
 * calculating the drive-related energy consumption. However, the time spent on the first link is used by the time-based
 * idle discharge process (see {@link IdleDischargingHandler}).
 */
public final class DriveDischargingHandler
	implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, MobsimScopeEventHandler, MobsimEngine {

	private static class EvDrive {
		private final Id<Vehicle> vehicleId;
		private final ElectricVehicle ev;
		private double movedOverNodeTime;

		public EvDrive(Id<Vehicle> vehicleId, ElectricVehicle ev) {
			this.vehicleId = vehicleId;
			this.ev = ev;
			movedOverNodeTime = Double.NaN;
		}

		private boolean isOnFirstLink() {
			return Double.isNaN(movedOverNodeTime);
		}
	}

	private final Network network;
	private final EventsManager eventsManager;
	private final Map<Id<Vehicle>, ? extends ElectricVehicle> eVehicles;
	private final Map<Id<Vehicle>, EvDrive> evDrives;

	private final Queue<LinkLeaveEvent> linkLeaveEvents = new ConcurrentLinkedQueue<>();
	private final Queue<VehicleLeavesTrafficEvent> trafficLeaveEvents = new ConcurrentLinkedQueue<>();

	@Inject
	DriveDischargingHandler(ElectricFleet data, Network network, EventsManager eventsManager) {
		this.network = network;
		this.eventsManager = eventsManager;
		eVehicles = data.getElectricVehicles();
		evDrives = new ConcurrentHashMap<>(eVehicles.size() / 10);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<Vehicle> vehicleId = event.getVehicleId();
		ElectricVehicle ev = eVehicles.get(vehicleId);
		if (ev != null) {// handle only our EVs
			evDrives.put(vehicleId, new EvDrive(vehicleId, ev));
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (evDrives.containsKey(event.getVehicleId())) {// handle only our EVs
			linkLeaveEvents.add(event);
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (evDrives.containsKey(event.getVehicleId())) {// handle only our EVs
			trafficLeaveEvents.add(event);
		}
	}

	@Override
	public void onPrepareSim() {
	}

	@Override
	public void afterSim() {
		// process remaining events
		doSimStep(Double.POSITIVE_INFINITY);
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
	}

	@Override
	public void doSimStep(double time) {
		handleQueuedEvents(linkLeaveEvents, time, false);
		handleQueuedEvents(trafficLeaveEvents, time, true);
	}

	private <E extends Event & HasVehicleId & HasLinkId> void handleQueuedEvents(Queue<E> queue, double time, boolean leftTraffic) {
		// We want to process events in the main thread (instead of the event handling threads).
		// This is to eliminate race conditions, where the battery is read/modified by many threads without proper synchronisation
		while (!queue.isEmpty()) {
			var event = queue.peek();
			if (event.getTime() == time) {
				// There is a potential race condition wrt processing events between doSimStep() and handleEvent().
				// To ensure a deterministic behaviour, we only process events from the previous time step.
				break;
			}

			var evDrive = dischargeVehicle(event.getVehicleId(), event.getLinkId(), event.getTime(), time);
			if (leftTraffic) {
				evDrives.remove(evDrive.vehicleId);
			} else {
				evDrive.movedOverNodeTime = event.getTime();
			}
			queue.remove();
		}
	}

	private EvDrive dischargeVehicle(Id<Vehicle> vehicleId, Id<Link> linkId, double eventTime, double now) {
		EvDrive evDrive = evDrives.get(vehicleId);
		if (!evDrive.isOnFirstLink()) {// skip the first link
			Link link = network.getLinks().get(linkId);
			double tt = eventTime - evDrive.movedOverNodeTime;
			ElectricVehicle ev = evDrive.ev;
			double energy = ev.getDriveEnergyConsumption().calcEnergyConsumption(link, tt, eventTime - tt) + ev.getAuxEnergyConsumption()
				.calcEnergyConsumption(eventTime - tt, tt, linkId);
			//Energy consumption may be negative on links with negative slope
			ev.getBattery()
				.dischargeEnergy(energy,
					missingEnergy -> eventsManager.processEvent(new MissingEnergyEvent(now, ev.getId(), link.getId(), missingEnergy)));
			eventsManager.processEvent(new DrivingEnergyConsumptionEvent(now, vehicleId, linkId, energy, ev.getBattery().getCharge()));
		}
		return evDrive;
	}
}
